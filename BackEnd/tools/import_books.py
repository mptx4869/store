import argparse
import datetime as dt
import os
import random
import re
import sys

import pandas as pd
import psycopg2
from psycopg2 import extras
import pyarrow.parquet as pq
from tqdm import tqdm


def parse_args() -> argparse.Namespace:
    base_dir = os.path.dirname(os.path.abspath(__file__))
    default_parquet = os.path.join(
        base_dir, "src", "main", "resources", "data", "books_cleaned.parquet"
    )

    parser = argparse.ArgumentParser(description="Import books from parquet into Postgres")
    parser.add_argument("--parquet", default=default_parquet, help="Path to parquet file")
    parser.add_argument("--batch-size", type=int, default=100_000, help="Rows per batch")
    parser.add_argument("--min-price", type=int, default=100000, help="Min base price")
    parser.add_argument("--max-price", type=int, default=500000, help="Max base price")
    parser.add_argument("--min-stock", type=int, default=10, help="Min inventory stock")
    parser.add_argument("--max-stock", type=int, default=200, help="Max inventory stock")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of rows")

    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "172.25.47.154"))
    parser.add_argument("--db-port", type=int, default=int(os.getenv("DB_PORT", "5432")))
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "thesis"))
    parser.add_argument("--db-user", default=os.getenv("DB_USER", "nhanhoa"))
    parser.add_argument("--db-password", default=os.getenv("DB_PASSWORD", "123"))

    return parser.parse_args()


def normalize_key(value: str) -> str:
    return value.strip().lower()


def truncate(value: str | None, max_len: int) -> str | None:
    if value is None:
        return None
    value = value.strip()
    if not value:
        return None
    if len(value) <= max_len:
        return value
    return value[:max_len]


def safe_str(value) -> str | None:
    if value is None or pd.isna(value):
        return None
    text = str(value).strip()
    return text if text else None


def safe_int(value) -> int | None:
    if value is None or pd.isna(value):
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def safe_date(year, month, day) -> dt.date | None:
    year_val = safe_int(year)
    if not year_val:
        return None
    month_val = safe_int(month) or 1
    day_val = safe_int(day) or 1
    try:
        return dt.date(year_val, month_val, day_val)
    except ValueError:
        return None


def parse_bool(value) -> bool:
    if value is None or pd.isna(value):
        return False
    if isinstance(value, bool):
        return value
    text = str(value).strip().lower()
    return text in {"1", "true", "t", "yes", "y"}


def split_authors(text: str | None) -> list[str]:
    if not text:
        return []
    parts = re.split(r"\s*(?:;|/|\|)\s*", text)
    names: list[str] = []
    for part in parts:
        if not part:
            continue
        subparts = re.split(r"\s+(?:&|and)\s+", part)
        for name in subparts:
            clean = name.strip()
            if clean:
                names.append(clean)
    return names


def split_genres(text: str | None) -> list[str]:
    if not text:
        return []
    parts = re.split(r"\s*(?:,|;|/|\|)\s*", text)
    genres: list[str] = []
    for part in parts:
        if not part:
            continue
        subparts = re.split(r"\s+(?:&|and)\s+", part)
        for name in subparts:
            clean = name.strip()
            if clean:
                genres.append(clean)
    return genres


def chunked_iter(values: list[str], size: int):
    for i in range(0, len(values), size):
        yield values[i : i + size]


def iter_parquet_batches(parquet_file: pq.ParquetFile, args):
    processed = 0
    for batch in parquet_file.iter_batches(batch_size=args.batch_size):
        df = batch.to_pandas()
        if args.limit is not None:
            remaining = args.limit - processed
            if remaining <= 0:
                break
            if len(df) > remaining:
                df = df.iloc[:remaining]
        processed += len(df)
        yield df


def insert_case_insensitive_values(
    cur, table: str, name_col: str, values: set[str], use_conflict: bool
) -> None:
    if not values:
        return

    unique_map: dict[str, str] = {}
    for value in values:
        clean = safe_str(value)
        if not clean:
            continue
        key = normalize_key(clean)
        if key not in unique_map:
            unique_map[key] = clean

    if not unique_map:
        return

    keys = list(unique_map.keys())
    existing: set[str] = set()
    for chunk in chunked_iter(keys, 1000):
        cur.execute(
            f"SELECT LOWER({name_col}) FROM {table} WHERE LOWER({name_col}) = ANY(%s)",
            (chunk,),
        )
        existing.update(row[0] for row in cur.fetchall())

    missing = [unique_map[key] for key in keys if key not in existing]
    if not missing:
        return

    query = f"INSERT INTO {table} ({name_col}) VALUES %s"
    if use_conflict:
        query += f" ON CONFLICT ({name_col}) DO NOTHING"
    extras.execute_values(cur, query, [(value,) for value in missing])


def preload_lookups(conn, parquet_path: str, args) -> None:
    parquet_file = pq.ParquetFile(parquet_path)
    total_rows = parquet_file.metadata.num_rows
    if args.limit is not None:
        total_rows = min(total_rows, args.limit)

    with tqdm(total=total_rows, unit="rows", desc="Preloading lookups") as progress:
        for df in iter_parquet_batches(parquet_file, args):
            publishers: set[str] = set()
            authors: set[str] = set()
            categories: set[str] = set()

            if "publisher" in df:
                for value in df["publisher"].dropna().tolist():
                    clean = truncate(safe_str(value), 255)
                    if clean:
                        publishers.add(clean)

            if "author_name" in df:
                for value in df["author_name"].dropna().tolist():
                    for author in split_authors(safe_str(value)):
                        clean = truncate(safe_str(author), 255)
                        if clean:
                            authors.add(clean)

            if "genres" in df:
                for value in df["genres"].dropna().tolist():
                    for genre in split_genres(safe_str(value)):
                        clean = truncate(safe_str(genre), 100)
                        if clean:
                            categories.add(clean)

            with conn.cursor() as cur:
                insert_case_insensitive_values(cur, "publishers", "name", publishers, True)
                insert_case_insensitive_values(cur, "categories", "name", categories, True)
                insert_case_insensitive_values(cur, "authors", "full_name", authors, False)

            conn.commit()
            progress.update(len(df))


def load_cache(conn, table: str, name_col: str) -> dict[str, int]:
    cache: dict[str, int] = {}
    with conn.cursor() as cur:
        cur.execute(f"SELECT id, {name_col} FROM {table}")
        for row_id, name in cur.fetchall():
            if not name:
                continue
            cache[normalize_key(name)] = row_id
    return cache


def get_or_create_case_insensitive(
    cur, cache: dict[str, int], table: str, name_col: str, value: str
) -> int:
    key = normalize_key(value)
    cached = cache.get(key)
    if cached:
        return cached

    cur.execute(
        f"SELECT id FROM {table} WHERE LOWER({name_col}) = LOWER(%s) LIMIT 1",
        (value,),
    )
    row = cur.fetchone()
    if row:
        cache[key] = row[0]
        return row[0]

    cur.execute(
        f"INSERT INTO {table} ({name_col}) VALUES (%s) RETURNING id",
        (value,),
    )
    new_id = cur.fetchone()[0]
    cache[key] = new_id
    return new_id


def get_or_create_author(cur, cache: dict[str, int], full_name: str) -> int:
    key = normalize_key(full_name)
    cached = cache.get(key)
    if cached:
        return cached

    cur.execute(
        "SELECT id FROM authors WHERE LOWER(full_name) = LOWER(%s) LIMIT 1",
        (full_name,),
    )
    row = cur.fetchone()
    if row:
        cache[key] = row[0]
        return row[0]

    cur.execute("INSERT INTO authors (full_name) VALUES (%s) RETURNING id", (full_name,))
    new_id = cur.fetchone()[0]
    cache[key] = new_id
    return new_id


def ensure_book(
    cur,
    book_id: int,
    title: str,
    subtitle: str | None,
    description: str | None,
    language: str | None,
    pages: int | None,
    publisher_id: int | None,
    published_date: dt.date | None,
    image_url: str | None,
    base_price: int,
) -> None:
    cur.execute(
        """
        INSERT INTO books (
            id, title, subtitle, description, language, pages,
            publisher_id, published_date, image_url, base_price
        )
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (id) DO NOTHING
        """,
        (
            book_id,
            title,
            subtitle,
            description,
            language,
            pages,
            publisher_id,
            published_date,
            image_url,
            base_price,
        ),
    )


def ensure_sku(cur, book_id: int, sku: str, fmt: str | None) -> int:
    cur.execute(
        """
        INSERT INTO product_skus (book_id, sku, format, price_override)
        VALUES (%s, %s, %s, %s)
        ON CONFLICT (sku) DO NOTHING
        RETURNING id
        """,
        (book_id, sku, fmt, None),
    )
    row = cur.fetchone()
    if row:
        return row[0]

    cur.execute("SELECT id FROM product_skus WHERE sku = %s", (sku,))
    return cur.fetchone()[0]


def ensure_default_sku(cur, book_id: int, sku_id: int) -> None:
    cur.execute(
        "UPDATE books SET default_sku_id = %s WHERE id = %s AND default_sku_id IS NULL",
        (sku_id, book_id),
    )


def ensure_inventory(cur, sku_id: int, stock: int) -> None:
    cur.execute(
        """
        INSERT INTO inventory (sku_id, stock, reserved)
        VALUES (%s, %s, %s)
        ON CONFLICT (sku_id) DO NOTHING
        """,
        (sku_id, stock, 0),
    )


def ensure_book_categories(
    cur, book_id: int, categories: list[str], category_cache: dict[str, int]
) -> None:
    for name in categories:
        clean = truncate(safe_str(name), 100)
        if not clean:
            continue
        category_id = get_or_create_case_insensitive(
            cur, category_cache, "categories", "name", clean
        )
        cur.execute(
            """
            INSERT INTO book_categories (book_id, category_id)
            VALUES (%s, %s)
            ON CONFLICT (book_id, category_id) DO NOTHING
            """,
            (book_id, category_id),
        )


def ensure_book_authors(
    cur, book_id: int, authors: list[str], author_cache: dict[str, int]
) -> None:
    position = 1
    for name in authors:
        clean = truncate(safe_str(name), 255)
        if not clean:
            continue
        author_id = get_or_create_author(cur, author_cache, clean)
        cur.execute(
            """
            INSERT INTO book_authors (book_id, author_id, author_position)
            VALUES (%s, %s, %s)
            ON CONFLICT (book_id, author_id) DO NOTHING
            """,
            (book_id, author_id, position),
        )
        position += 1


def process_row(row, cur, caches, args) -> None:
    book_id = safe_int(getattr(row, "book_id", None))
    if not book_id:
        return

    title = truncate(safe_str(getattr(row, "title", None)), 255)
    if not title:
        return

    title_without_series = safe_str(getattr(row, "title_without_series", None))
    subtitle = None
    if title_without_series and title_without_series != title:
        subtitle = truncate(title_without_series, 255)

    description = safe_str(getattr(row, "description", None))
    language = truncate(safe_str(getattr(row, "language_code", None)), 50)
    pages = safe_int(getattr(row, "num_pages", None))
    image_url = truncate(safe_str(getattr(row, "image_url", None)), 1000)

    publisher_name = truncate(safe_str(getattr(row, "publisher", None)), 255)
    publisher_id = None
    if publisher_name:
        publisher_id = get_or_create_case_insensitive(
            cur, caches["publishers"], "publishers", "name", publisher_name
        )

    published_date = safe_date(
        getattr(row, "publication_year", None),
        getattr(row, "publication_month", None),
        getattr(row, "publication_day", None),
    )

    base_price = random.randint(args.min_price, args.max_price)
    ensure_book(
        cur,
        book_id,
        title,
        subtitle,
        description,
        language,
        pages,
        publisher_id,
        published_date,
        image_url,
        base_price,
    )

    is_ebook = parse_bool(getattr(row, "is_ebook", None))
    fmt = "EBOOK" if is_ebook else "PRINT"
    sku = truncate(f"BOOK-{book_id}", 100)
    sku_id = ensure_sku(cur, book_id, sku, fmt)
    ensure_default_sku(cur, book_id, sku_id)
    stock = random.randint(args.min_stock, args.max_stock)
    ensure_inventory(cur, sku_id, stock)

    authors = split_authors(safe_str(getattr(row, "author_name", None)))
    if authors:
        ensure_book_authors(cur, book_id, authors, caches["authors"])

    genres = split_genres(safe_str(getattr(row, "genres", None)))
    if genres:
        ensure_book_categories(cur, book_id, genres, caches["categories"])


def process_batches(conn, parquet_path: str, args) -> None:
    parquet_file = pq.ParquetFile(parquet_path)
    total_rows = parquet_file.metadata.num_rows
    if args.limit is not None:
        total_rows = min(total_rows, args.limit)

    caches = {
        "publishers": load_cache(conn, "publishers", "name"),
        "categories": load_cache(conn, "categories", "name"),
        "authors": load_cache(conn, "authors", "full_name"),
    }

    with tqdm(total=total_rows, unit="rows", desc="Importing books") as progress:
        for df in iter_parquet_batches(parquet_file, args):

            with conn.cursor() as cur:
                for row in df.itertuples(index=False):
                    cur.execute("SAVEPOINT sp_row")
                    try:
                        process_row(row, cur, caches, args)
                        cur.execute("RELEASE SAVEPOINT sp_row")
                    except Exception as exc:
                        cur.execute("ROLLBACK TO SAVEPOINT sp_row")
                        cur.execute("RELEASE SAVEPOINT sp_row")
                        book_id = getattr(row, "book_id", None)
                        print(
                            f"Failed book_id={book_id}: {exc}",
                            file=sys.stderr,
                        )

            conn.commit()
            progress.update(len(df))


def main() -> None:
    args = parse_args()
    if not os.path.exists(args.parquet):
        raise SystemExit(f"Parquet file not found: {args.parquet}")

    conn = psycopg2.connect(
        host=args.db_host,
        port=args.db_port,
        dbname=args.db_name,
        user=args.db_user,
        password=args.db_password,
    )

    try:
        import gc
        preload_lookups(conn, args.parquet, args)
        gc.collect()
        process_batches(conn, args.parquet, args)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
