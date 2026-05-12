# %% [markdown]
# # Data Split Pipeline (Out-Of-Core + Iterative K-Core)
# File nay giu dinh dang Jupyter cell (`# %%`) va co the chay tren VSCode Jupyter hoac Colab.

# %%
# Colab bootstrap
try:
    import google.colab

    IN_COLAB = True
except ImportError:
    IN_COLAB = False

if IN_COLAB:
    import IPython
    from google.colab import drive

    IPython.get_ipython().system("pip install -q polars pyarrow")
    drive.mount("/content/drive")

# %%
import gzip
import json
import logging
import os
from pathlib import Path

# Dat truoc khi import polars de bao dam co hieu luc.
os.environ.setdefault("POLARS_MAX_THREADS", str(min(2, os.cpu_count() or 2)))

import polars as pl
import pyarrow as pa
import pyarrow.csv as pacsv
import pyarrow.parquet as pq

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(message)s")
logger = logging.getLogger(__name__)

# =====================================================================
# THAM SO
# =====================================================================
K_BOOK = 10
K_USER = 15
TRAIN_RATIO = 0.8
VALID_RATIO = 0.1

EXPLICIT_MIN_RATING = 1
EXPLICIT_MAX_RATING = 5
POSITIVE_MIN_RATING = 4
NEGATIVE_MAX_RATING = 2

KCORE_MAX_ITERS = 20
JSON_CHUNK_SIZE = 500_000
CSV_WRITE_BATCH_SIZE = 500_000

RUN_STAGE1 = os.environ.get("RUN_STAGE1", "1") == "1"
RUN_STAGE2 = os.environ.get("RUN_STAGE2", "1") == "1"
RUN_STAGE3 = os.environ.get("RUN_STAGE3", "1") == "1"
# =====================================================================

# =====================================================================
# PATHS
# =====================================================================
if IN_COLAB:
    BASE_DIR = Path("/content/drive/My Drive/Thesis")
else:
    if "__file__" in globals():
        BASE_DIR = Path(__file__).resolve().parents[2]
    else:
        BASE_DIR = Path.cwd().resolve()

DATA_DIR = BASE_DIR / "Data"
PROCESSED_DIR = BASE_DIR / "book_recsys" / "data" / "processed"
EVAL2_DIR = PROCESSED_DIR / "eval2"

RAW_JSON_PATH = DATA_DIR / "goodreads_interactions_dedup.json.gz"

PARQUET_STAGE1 = DATA_DIR / "interactions_stage1_v3.parquet"
PARQUET_STAGE2_KCORE = DATA_DIR / "interactions_stage2_kcore_v3.parquet"

TMP_RANKED_PARQUET = DATA_DIR / "interactions_ranked_eval2_tmp.parquet"

OUT_TRAIN_MAIN = EVAL2_DIR / "train_main.csv"
OUT_VALID_POS = EVAL2_DIR / "valid_pos.csv"
OUT_TEST_POS = EVAL2_DIR / "test_pos.csv"
OUT_TEST_NEG = EVAL2_DIR / "test_neg.csv"
OUT_TRAIN_IMPLICIT = EVAL2_DIR / "train_implicit.csv"
OUT_EXPLICIT_TRAIN = EVAL2_DIR / "explicit_train.csv"
OUT_EXPLICIT_TEST = EVAL2_DIR / "explicit_test.csv"

REQUIRED_STAGE3_OUTPUTS = [
    OUT_TRAIN_MAIN,
    OUT_VALID_POS,
    OUT_TEST_POS,
    OUT_TEST_NEG,
    OUT_TRAIN_IMPLICIT,
    OUT_EXPLICIT_TRAIN,
    OUT_EXPLICIT_TEST,
]

EVAL2_DIR.mkdir(parents=True, exist_ok=True)


def _parse_rating_no_remap(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        # Cho phep chuoi dang "4.0" nhung khong map gia tri khac.
        try:
            float_value = float(value)
        except (TypeError, ValueError):
            return None
        if float_value.is_integer():
            return int(float_value)
        return None


def _parse_one_record(obj: dict):
    user_id = obj.get("user_id")
    book_id = obj.get("book_id")
    if not user_id or not book_id:
        return None

    rating = _parse_rating_no_remap(obj.get("rating"))
    if rating is None:
        return None

    date_updated = obj.get("date_updated") or ""
    return str(user_id), str(book_id), rating, str(date_updated)


def _write_parquet_chunk(
    writer,
    user_ids,
    book_ids,
    ratings,
    dates,
):
    if not user_ids:
        return writer, 0

    df = pl.DataFrame(
        {
            "user_id": user_ids,
            "book_id": book_ids,
            "rating": ratings,
            "date_updated": dates,
        }
    ).with_columns(
        [
            pl.col("rating").cast(pl.Int16),
            pl.col("date_updated").str.strptime(
                pl.Datetime,
                format="%a %b %d %H:%M:%S %z %Y",
                strict=False,
            ),
        ]
    )

    table = df.to_arrow()
    if writer is None:
        writer = pq.ParquetWriter(str(PARQUET_STAGE1), table.schema, compression="snappy")
    writer.write_table(table)
    return writer, len(user_ids)


# %% [markdown]
# # Stage 1: JSON.gz -> Parquet (Out-of-Core)

# %%
def stage_1_json_to_parquet():
    if PARQUET_STAGE1.exists() and PARQUET_STAGE1.stat().st_size > 0:
        logger.info("Skip Stage 1: %s already exists.", PARQUET_STAGE1)
        return

    if not RAW_JSON_PATH.exists():
        raise FileNotFoundError(f"Missing input JSON.gz: {RAW_JSON_PATH}")

    logger.info("Stage 1 start: streaming JSON.gz -> Parquet")

    writer = None
    lines_processed = 0
    lines_kept = 0
    bad_json = 0
    dropped_missing_id = 0
    dropped_invalid_rating = 0

    user_ids, book_ids, ratings, dates = [], [], [], []

    with gzip.open(RAW_JSON_PATH, "rt", encoding="utf-8") as file_obj:
        for line in file_obj:
            lines_processed += 1
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                bad_json += 1
                continue

            parsed = _parse_one_record(obj)
            if parsed is None:
                if not obj.get("user_id") or not obj.get("book_id"):
                    dropped_missing_id += 1
                else:
                    dropped_invalid_rating += 1
                continue

            u, b, r, d = parsed
            user_ids.append(u)
            book_ids.append(b)
            ratings.append(r)
            dates.append(d)

            if len(user_ids) >= JSON_CHUNK_SIZE:
                writer, written = _write_parquet_chunk(
                    writer, user_ids, book_ids, ratings, dates
                )
                lines_kept += written
                logger.info(
                    "Stage 1 progress: processed=%s kept=%s bad_json=%s dropped_missing_id=%s dropped_invalid_rating=%s",
                    f"{lines_processed:,}",
                    f"{lines_kept:,}",
                    f"{bad_json:,}",
                    f"{dropped_missing_id:,}",
                    f"{dropped_invalid_rating:,}",
                )
                user_ids.clear()
                book_ids.clear()
                ratings.clear()
                dates.clear()

    writer, written = _write_parquet_chunk(writer, user_ids, book_ids, ratings, dates)
    lines_kept += written

    if writer is not None:
        writer.close()

    if not PARQUET_STAGE1.exists() or PARQUET_STAGE1.stat().st_size == 0:
        raise RuntimeError("Stage 1 failed: output parquet is empty.")

    logger.info(
        "Stage 1 done: processed=%s kept=%s bad_json=%s dropped_missing_id=%s dropped_invalid_rating=%s",
        f"{lines_processed:,}",
        f"{lines_kept:,}",
        f"{bad_json:,}",
        f"{dropped_missing_id:,}",
        f"{dropped_invalid_rating:,}",
    )


if RUN_STAGE1:
    stage_1_json_to_parquet()
else:
    logger.info("Skip Stage 1 by env flag RUN_STAGE1=0")


def _scan_clean(path: Path) -> pl.LazyFrame:
    return pl.scan_parquet(str(path)).filter(
        pl.col("user_id").is_not_null()
        & (pl.col("user_id") != "")
        & pl.col("book_id").is_not_null()
        & (pl.col("book_id") != "")
    )


def _parquet_row_count(path: Path) -> int:
    parquet_file = pq.ParquetFile(str(path))
    return int(parquet_file.metadata.num_rows)


# %% [markdown]
# # Stage 2: Iterative K-Core to convergence

# %%
def stage_2_kcore_filtering():
    if PARQUET_STAGE2_KCORE.exists() and PARQUET_STAGE2_KCORE.stat().st_size > 0:
        logger.info("Skip Stage 2: %s already exists.", PARQUET_STAGE2_KCORE)
        return

    if not PARQUET_STAGE1.exists():
        raise FileNotFoundError(f"Missing Stage 1 parquet: {PARQUET_STAGE1}")

    current_path = PARQUET_STAGE1
    temp_paths: list[Path] = []
    before_rows = _parquet_row_count(current_path)

    logger.info("Stage 2 start: iterative K-core filtering (rows=%s)", f"{before_rows:,}")

    for iter_id in range(1, KCORE_MAX_ITERS + 1):
        base_lf = _scan_clean(current_path)

        valid_books = (
            base_lf.group_by("book_id")
            .len()
            .filter(pl.col("len") >= K_BOOK)
            .select("book_id")
        )
        after_book_lf = base_lf.join(valid_books, on="book_id", how="semi")

        # Tach pass theo book -> parquet trung gian de giam peak RAM.
        book_pass_path = DATA_DIR / f"interactions_stage2_kcore_v3_books_iter_{iter_id}.parquet"
        after_book_lf.sink_parquet(str(book_pass_path))

        after_book_scan = _scan_clean(book_pass_path)

        valid_users = (
            after_book_scan.group_by("user_id")
            .len()
            .filter(pl.col("len") >= K_USER)
            .select("user_id")
        )
        next_lf = after_book_scan.join(valid_users, on="user_id", how="semi")

        iter_path = DATA_DIR / f"interactions_stage2_kcore_v3_iter_{iter_id}.parquet"
        next_lf.sink_parquet(str(iter_path))
        book_pass_path.unlink(missing_ok=True)
        temp_paths.append(iter_path)

        after_rows = _parquet_row_count(iter_path)
        removed_rows = before_rows - after_rows

        logger.info(
            "Stage 2 iter %d: before_rows=%s after_rows=%s removed=%s",
            iter_id,
            f"{before_rows:,}",
            f"{after_rows:,}",
            f"{removed_rows:,}",
        )

        if after_rows == before_rows:
            iter_path.replace(PARQUET_STAGE2_KCORE)
            logger.info("Stage 2 converged at iter=%d with rows=%s", iter_id, f"{after_rows:,}")
            break

        if current_path != PARQUET_STAGE1 and current_path.exists():
            current_path.unlink(missing_ok=True)

        current_path = iter_path
        before_rows = after_rows
    else:
        raise RuntimeError(
            f"Stage 2 did not converge within {KCORE_MAX_ITERS} iterations."
        )

    for tmp in temp_paths:
        if tmp.exists() and tmp != PARQUET_STAGE2_KCORE:
            tmp.unlink(missing_ok=True)

    if not PARQUET_STAGE2_KCORE.exists() or PARQUET_STAGE2_KCORE.stat().st_size == 0:
        raise RuntimeError("Stage 2 failed: output k-core parquet is empty.")


if RUN_STAGE2:
    stage_2_kcore_filtering()
else:
    logger.info("Skip Stage 2 by env flag RUN_STAGE2=0")


def parquet_to_csv_stream(parquet_path: Path, csv_path: Path, batch_size: int):
    if not parquet_path.exists():
        raise FileNotFoundError(f"Missing parquet for CSV export: {parquet_path}")

    parquet_file = pq.ParquetFile(str(parquet_path))
    rows_written = 0
    first = True

    with pa.OSFile(str(csv_path), "wb") as sink:
        for batch in parquet_file.iter_batches(batch_size=batch_size):
            table = pa.Table.from_batches([batch])
            pacsv.write_csv(
                table,
                sink,
                write_options=pacsv.WriteOptions(include_header=first),
            )
            first = False
            rows_written += table.num_rows

    return rows_written


# %% [markdown]
# # Stage 3: Time-based split with low-memory multi-output export

# %%
def _all_outputs_exist(paths: list[Path]) -> bool:
    return all(p.exists() and p.stat().st_size > 0 for p in paths)


def _outputs_up_to_date(input_path: Path, output_paths: list[Path]) -> bool:
    if not _all_outputs_exist(output_paths):
        return False
    input_mtime = input_path.stat().st_mtime
    return all(out_path.stat().st_mtime >= input_mtime for out_path in output_paths)


def _export_lazyframe_to_csv(lf: pl.LazyFrame, tmp_parquet: Path, out_csv: Path) -> int:
    lf.sink_parquet(str(tmp_parquet))
    rows = parquet_to_csv_stream(tmp_parquet, out_csv, CSV_WRITE_BATCH_SIZE)
    tmp_parquet.unlink(missing_ok=True)
    return rows


def stage_3_time_based_split():
    if PARQUET_STAGE2_KCORE.exists() and _outputs_up_to_date(
        PARQUET_STAGE2_KCORE,
        REQUIRED_STAGE3_OUTPUTS,
    ):
        logger.info("Skip Stage 3: outputs already exist in eval2.")
        return

    if not PARQUET_STAGE2_KCORE.exists():
        raise FileNotFoundError(f"Missing Stage 2 parquet: {PARQUET_STAGE2_KCORE}")

    logger.info("Stage 3 start: per-user time split -> train/valid/test + streaming CSV")

    base_lf = (
        pl.scan_parquet(str(PARQUET_STAGE2_KCORE))
        .select(["user_id", "book_id", "rating", "date_updated"])
        .filter(
            pl.col("user_id").is_not_null()
            & (pl.col("user_id") != "")
            & pl.col("book_id").is_not_null()
            & (pl.col("book_id") != "")
            & pl.col("date_updated").is_not_null()
        )
        .sort(["user_id", "date_updated", "book_id"])
    )

    ranked_lf = (
        base_lf.with_columns(
            [
                pl.len().over("user_id").alias("total_count"),
                pl.col("date_updated").cum_count().over("user_id").alias("row_num"),
            ]
        )
        .with_columns(
            [
                (pl.col("total_count") * TRAIN_RATIO).floor().cast(pl.Int64).alias("train_raw"),
                (pl.col("total_count") * VALID_RATIO).floor().cast(pl.Int64).alias("valid_raw"),
            ]
        )
        .with_columns(
            [
                pl.when(pl.col("train_raw") < 1)
                .then(1)
                .when(pl.col("train_raw") > (pl.col("total_count") - 2))
                .then(pl.col("total_count") - 2)
                .otherwise(pl.col("train_raw"))
                .alias("train_end"),
            ]
        )
        .with_columns(
            [
                (pl.col("total_count") - pl.col("train_end")).alias("remaining_for_valid_test"),
            ]
        )
        .with_columns(
            [
                pl.when(pl.col("remaining_for_valid_test") <= 1)
                .then(0)
                .when(pl.col("valid_raw") < 1)
                .then(1)
                .when(pl.col("valid_raw") >= pl.col("remaining_for_valid_test"))
                .then(pl.col("remaining_for_valid_test") - 1)
                .otherwise(pl.col("valid_raw"))
                .alias("valid_size"),
            ]
        )
        .with_columns(
            [
                (pl.col("train_end") + pl.col("valid_size")).alias("valid_end"),
                (
                    (pl.col("rating") >= EXPLICIT_MIN_RATING)
                    & (pl.col("rating") <= EXPLICIT_MAX_RATING)
                ).alias("is_explicit"),
            ]
        )
    )

    ranked_lf.sink_parquet(str(TMP_RANKED_PARQUET))
    ranked_scan = pl.scan_parquet(str(TMP_RANKED_PARQUET))

    is_train = pl.col("row_num") <= pl.col("train_end")
    is_valid = (pl.col("row_num") > pl.col("train_end")) & (pl.col("row_num") <= pl.col("valid_end"))
    is_test = pl.col("row_num") > pl.col("valid_end")

    is_explicit = pl.col("is_explicit")
    is_positive = is_explicit & (pl.col("rating") >= POSITIVE_MIN_RATING)
    is_negative = is_explicit & (pl.col("rating") <= NEGATIVE_MAX_RATING)
    is_implicit = ~is_explicit

    keep_cols = ["user_id", "book_id", "rating"]

    split_specs = [
        (
            "train_main",
            ranked_scan.filter(is_train).select(keep_cols),
            DATA_DIR / "train_main_eval2_tmp.parquet",
            OUT_TRAIN_MAIN,
        ),
        (
            "valid_pos",
            ranked_scan.filter(is_valid & is_positive).select(keep_cols),
            DATA_DIR / "valid_pos_eval2_tmp.parquet",
            OUT_VALID_POS,
        ),
        (
            "test_pos",
            ranked_scan.filter(is_test & is_positive).select(keep_cols),
            DATA_DIR / "test_pos_eval2_tmp.parquet",
            OUT_TEST_POS,
        ),
        (
            "test_neg",
            ranked_scan.filter(is_test & is_negative).select(keep_cols),
            DATA_DIR / "test_neg_eval2_tmp.parquet",
            OUT_TEST_NEG,
        ),
        (
            "train_implicit",
            ranked_scan.filter(is_train & is_implicit).select(keep_cols),
            DATA_DIR / "train_implicit_eval2_tmp.parquet",
            OUT_TRAIN_IMPLICIT,
        ),
        (
            "explicit_train",
            ranked_scan.filter(is_train & is_explicit).select(keep_cols),
            DATA_DIR / "explicit_train_eval2_tmp.parquet",
            OUT_EXPLICIT_TRAIN,
        ),
        (
            "explicit_test",
            ranked_scan.filter((is_valid | is_test) & is_explicit).select(keep_cols),
            DATA_DIR / "explicit_test_eval2_tmp.parquet",
            OUT_EXPLICIT_TEST,
        ),
    ]

    row_stats = {}
    for split_name, split_lf, split_tmp_path, split_out_path in split_specs:
        rows = _export_lazyframe_to_csv(split_lf, split_tmp_path, split_out_path)
        row_stats[split_name] = rows
        logger.info("Stage 3 exported %-14s rows=%s -> %s", split_name, f"{rows:,}", split_out_path)

    TMP_RANKED_PARQUET.unlink(missing_ok=True)

    logger.info("Stage 3 done with outputs in %s", EVAL2_DIR)
    for split_name, rows in row_stats.items():
        logger.info("  - %s: %s rows", split_name, f"{rows:,}")


if RUN_STAGE3:
    stage_3_time_based_split()
else:
    logger.info("Skip Stage 3 by env flag RUN_STAGE3=0")
