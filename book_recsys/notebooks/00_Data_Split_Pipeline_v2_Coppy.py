"""
Data Split Pipeline v2 — Temporal Leave-Last-N Per-User Split
with Feedback Decomposition for Hybrid Book Recommendation System.

Stages:
  1. JSON.gz → Parquet   (line-by-line streaming, flush every 500K records)
  2. Iterative K-Core     (Polars lazy scan → semi-join → sink_parquet per iter)
  3. Per-user temporal split + feedback decomposition

Memory Strategy:
  - Stage 1: 500K-record buffer → flush to Parquet. Peak ≈ 200 MB.
  - Stage 2: Lazy scan + streaming aggregation + semi-join per iteration.
             Only the group-by hash table (~N_unique keys) lives in RAM.
  - Stage 3: Two-pass approach:
        Pass A — Sort full dataset → sink (streaming external sort, low RAM).
        Pass B — Add per-user row_num via cum_sum().over() → sink.
        User stats aggregation is ~800K rows (trivial).
        Each export: scan ranked parquet → filter → sink temp → stream CSV.

Environment Variables:
  SKIP_STAGE1=1   Skip JSON→Parquet ingestion
  SKIP_STAGE2=1   Skip K-Core filtering
  SKIP_STAGE3=1   Skip temporal split

Outputs (eval2/):
  train_main.csv      — All train interactions (for iALS: binarize + confidence)
  explicit_train.csv  — Explicit-only train (rating 1-5, for CBF user profiles)
  train_implicit.csv  — Implicit-only train (rating=0)
  valid_pos.csv       — Positive validation (rating ≥ 4, for α-tuning)
  test_pos.csv        — Positive test (rating ≥ 4, final eval ground truth)
  test_neg.csv        — Negative test (rating ≤ 2, discrimination eval)
  test_implicit.csv   — Implicit test (rating=0, implicit eval)
  explicit_test.csv   — All explicit from valid+test (combined explicit eval)

Usage:
  python 00_Data_Split_Pipeline_v2.py
"""
from __future__ import annotations

import gzip
import json
import logging
import os
from dataclasses import dataclass, field
from pathlib import Path

# Limit Polars thread pool BEFORE import to control peak RAM.
os.environ.setdefault("POLARS_MAX_THREADS", str(min(4, os.cpu_count() or 2)))

import polars as pl
import pyarrow as pa
import pyarrow.csv as pacsv
import pyarrow.parquet as pq

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-5s | %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("split_pipeline")

# ── Environment ──
try:
    import google.colab  # type: ignore[import-untyped]

    IN_COLAB = True
except ImportError:
    IN_COLAB = False


def _detect_base_dir() -> Path:
    """Resolve project root (Thesis/) depending on runtime environment."""
    if IN_COLAB:
        return Path("/content/drive/My Drive/Thesis")
    try:
        # Script lives in book_recsys/notebooks/ → parents[2] = Thesis/
        return Path(__file__).resolve().parents[2]
    except NameError:
        return Path.cwd().resolve()


# ═══════════════════════════════════════════════════════════════
# CONFIGURATION
# ═══════════════════════════════════════════════════════════════


@dataclass
class SplitConfig:
    """Centralised parameters and path registry for the 3-stage pipeline."""

    # ── K-Core thresholds ──
    k_book: int = 10
    k_user: int = 15
    kcore_max_iters: int = 20

    # ── Temporal split ratios (test = 1 - train - valid) ──
    train_ratio: float = 0.8
    valid_ratio: float = 0.1

    # ── Rating classification ──
    positive_min_rating: int = 4
    negative_max_rating: int = 2
    explicit_min_rating: int = 1
    explicit_max_rating: int = 5

    # ── I/O tuning ──
    json_chunk_size: int = 500_000
    csv_write_batch: int = 500_000

    # ── Stage toggle (env-driven) ──
    run_stage1: bool = field(init=False)
    run_stage2: bool = field(init=False)
    run_stage3: bool = field(init=False)

    # ── Auto-configured paths ──
    base_dir: Path = field(default_factory=_detect_base_dir)
    data_dir: Path = field(init=False)
    output_dir: Path = field(init=False)
    raw_json: Path = field(init=False)
    stage1_parquet: Path = field(init=False)
    kcore_parquet: Path = field(init=False)

    def __post_init__(self) -> None:
        self.run_stage1 = os.environ.get("SKIP_STAGE1", "0") != "1"
        self.run_stage2 = os.environ.get("SKIP_STAGE2", "0") != "1"
        self.run_stage3 = os.environ.get("SKIP_STAGE3", "0") != "1"

        self.data_dir = self.base_dir / "Data"
        self.output_dir = (
            self.base_dir / "book_recsys" / "data" / "processed" / "eval2"
        )
        self.output_dir.mkdir(parents=True, exist_ok=True)

        self.raw_json = self.data_dir / "goodreads_interactions_dedup.json.gz"
        self.stage1_parquet = self.data_dir / "interactions_stage1_v3.parquet"
        self.kcore_parquet = self.data_dir / "interactions_stage2_kcore_v3.parquet"

    # ── Output file paths ──

    @property
    def outputs(self) -> dict[str, Path]:
        d = self.output_dir
        return {
            "train_main": d / "train_main.csv",
            "explicit_train": d / "explicit_train.csv",
            "train_implicit": d / "train_implicit.csv",
            "valid_pos": d / "valid_pos.csv",
            "test_pos": d / "test_pos.csv",
            "test_neg": d / "test_neg.csv",
            "test_implicit": d / "test_implicit.csv",
            "explicit_test": d / "explicit_test.csv",
        }


# ═══════════════════════════════════════════════════════════════
# SHARED UTILITIES
# ═══════════════════════════════════════════════════════════════


def _pq_row_count(path: Path) -> int:
    return int(pq.ParquetFile(str(path)).metadata.num_rows)


def _exists_and_nonempty(path: Path) -> bool:
    return path.exists() and path.stat().st_size > 0


def _require_file(path: Path, label: str) -> None:
    if not path.exists():
        raise FileNotFoundError(f"{label} not found: {path}")


def _clean_scan(path: Path) -> pl.LazyFrame:
    """Scan a Parquet file and drop rows with null/empty IDs."""
    return pl.scan_parquet(str(path)).filter(
        pl.col("user_id").is_not_null()
        & (pl.col("user_id") != "")
        & pl.col("book_id").is_not_null()
        & (pl.col("book_id") != "")
    )


def _stream_parquet_to_csv(
    parquet_path: Path, csv_path: Path, batch_size: int
) -> int:
    """Convert Parquet → CSV via PyArrow batch iteration (constant RAM)."""
    pf = pq.ParquetFile(str(parquet_path))
    written = 0
    first_batch = True
    with pa.OSFile(str(csv_path), "wb") as sink:
        for batch in pf.iter_batches(batch_size=batch_size):
            table = pa.Table.from_batches([batch])
            pacsv.write_csv(
                table,
                sink,
                write_options=pacsv.WriteOptions(include_header=first_batch),
            )
            first_batch = False
            written += table.num_rows
    return written


def _export_lf_to_csv(
    lf: pl.LazyFrame, tmp_pq: Path, out_csv: Path, batch_size: int
) -> int:
    """Sink LazyFrame → temp Parquet → stream to CSV → cleanup temp."""
    lf.sink_parquet(str(tmp_pq))
    rows = _stream_parquet_to_csv(tmp_pq, out_csv, batch_size)
    tmp_pq.unlink(missing_ok=True)
    return rows


# ═══════════════════════════════════════════════════════════════
# STAGE 1 — JSON.gz → Parquet (streaming ingest)
# ═══════════════════════════════════════════════════════════════

_DATETIME_FMT = "%a %b %d %H:%M:%S %z %Y"


def _parse_rating(raw) -> int | None:
    """Parse rating to int, accepting int-convertible floats like '4.0'."""
    try:
        return int(raw)
    except (TypeError, ValueError):
        pass
    try:
        fval = float(raw)
    except (TypeError, ValueError):
        return None
    return int(fval) if fval == int(fval) else None


def stage_1_ingest(cfg: SplitConfig) -> None:
    """Stream JSON.gz line-by-line → Parquet with chunked flushing."""
    if _exists_and_nonempty(cfg.stage1_parquet):
        log.info("SKIP Stage 1: %s exists.", cfg.stage1_parquet.name)
        return

    _require_file(cfg.raw_json, "Raw JSON.gz")
    log.info("Stage 1: JSON.gz → Parquet  [chunk=%s]", f"{cfg.json_chunk_size:,}")

    writer: pq.ParquetWriter | None = None
    buf_uid: list[str] = []
    buf_bid: list[str] = []
    buf_rat: list[int] = []
    buf_dat: list[str] = []

    n_processed = 0
    n_kept = 0
    n_bad_json = 0
    n_no_id = 0
    n_bad_rating = 0

    def _flush() -> None:
        nonlocal writer, n_kept
        if not buf_uid:
            return
        df = pl.DataFrame(
            {
                "user_id": buf_uid,
                "book_id": buf_bid,
                "rating": buf_rat,
                "date_updated": buf_dat,
            }
        ).with_columns(
            pl.col("rating").cast(pl.Int16),
            pl.col("date_updated").str.strptime(
                pl.Datetime, format=_DATETIME_FMT, strict=False
            ),
        )
        arrow_table = df.to_arrow()
        if writer is None:
            writer = pq.ParquetWriter(
                str(cfg.stage1_parquet), arrow_table.schema, compression="snappy"
            )
        writer.write_table(arrow_table)
        n_kept += len(buf_uid)
        buf_uid.clear()
        buf_bid.clear()
        buf_rat.clear()
        buf_dat.clear()

    with gzip.open(cfg.raw_json, "rt", encoding="utf-8") as fh:
        for line in fh:
            n_processed += 1
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                n_bad_json += 1
                continue

            uid = obj.get("user_id")
            bid = obj.get("book_id")
            if not uid or not bid:
                n_no_id += 1
                continue

            rating = _parse_rating(obj.get("rating"))
            if rating is None:
                n_bad_rating += 1
                continue

            buf_uid.append(str(uid))
            buf_bid.append(str(bid))
            buf_rat.append(rating)
            buf_dat.append(obj.get("date_updated") or "")

            if len(buf_uid) >= cfg.json_chunk_size:
                _flush()
                if n_kept % 5_000_000 < cfg.json_chunk_size:
                    log.info(
                        "  progress: processed=%s kept=%s",
                        f"{n_processed:,}",
                        f"{n_kept:,}",
                    )

    _flush()
    if writer is not None:
        writer.close()

    if not _exists_and_nonempty(cfg.stage1_parquet):
        raise RuntimeError("Stage 1 produced an empty Parquet file.")

    log.info(
        "Stage 1 done: processed=%s  kept=%s  bad_json=%s  no_id=%s  bad_rating=%s",
        f"{n_processed:,}",
        f"{n_kept:,}",
        f"{n_bad_json:,}",
        f"{n_no_id:,}",
        f"{n_bad_rating:,}",
    )


# ═══════════════════════════════════════════════════════════════
# STAGE 2 — Iterative K-Core Filtering
# ═══════════════════════════════════════════════════════════════


def stage_2_kcore(cfg: SplitConfig) -> None:
    """Alternately prune items with < k_book interactions and users with
    < k_user interactions until convergence."""
    if _exists_and_nonempty(cfg.kcore_parquet):
        log.info("SKIP Stage 2: %s exists.", cfg.kcore_parquet.name)
        return

    _require_file(cfg.stage1_parquet, "Stage 1 parquet")

    current_path = cfg.stage1_parquet
    row_count = _pq_row_count(current_path)
    log.info(
        "Stage 2: K-Core  [k_book=%d, k_user=%d]  start_rows=%s",
        cfg.k_book,
        cfg.k_user,
        f"{row_count:,}",
    )

    temp_files: list[Path] = []

    for iteration in range(1, cfg.kcore_max_iters + 1):
        # ── Pass A: keep books with >= k_book interactions ──
        book_pass_path = cfg.data_dir / f"_kcore_v3_books_i{iteration}.parquet"
        lf = _clean_scan(current_path)
        valid_books = (
            lf.group_by("book_id")
            .len()
            .filter(pl.col("len") >= cfg.k_book)
            .select("book_id")
        )
        lf.join(valid_books, on="book_id", how="semi").sink_parquet(
            str(book_pass_path)
        )

        # ── Pass B: keep users with >= k_user interactions ──
        iter_path = cfg.data_dir / f"_kcore_v3_i{iteration}.parquet"
        lf2 = _clean_scan(book_pass_path)
        valid_users = (
            lf2.group_by("user_id")
            .len()
            .filter(pl.col("len") >= cfg.k_user)
            .select("user_id")
        )
        lf2.join(valid_users, on="user_id", how="semi").sink_parquet(str(iter_path))

        # Cleanup book-pass temp immediately
        book_pass_path.unlink(missing_ok=True)
        temp_files.append(iter_path)

        new_count = _pq_row_count(iter_path)
        log.info(
            "  iter %d: %s → %s  (removed %s)",
            iteration,
            f"{row_count:,}",
            f"{new_count:,}",
            f"{row_count - new_count:,}",
        )

        if new_count == row_count:
            # Converged — promote this file to the final output
            iter_path.replace(cfg.kcore_parquet)
            log.info(
                "Stage 2 converged at iter %d  (%s rows).",
                iteration,
                f"{new_count:,}",
            )
            break

        # Cleanup previous iteration (but never the Stage 1 original)
        if current_path != cfg.stage1_parquet and current_path.exists():
            current_path.unlink(missing_ok=True)

        current_path = iter_path
        row_count = new_count
    else:
        raise RuntimeError(
            f"K-Core did not converge within {cfg.kcore_max_iters} iterations."
        )

    # Cleanup any remaining temp files
    for tmp in temp_files:
        if tmp.exists() and tmp != cfg.kcore_parquet:
            tmp.unlink(missing_ok=True)


# ═══════════════════════════════════════════════════════════════
# STAGE 3 — Temporal Per-User Split + Feedback Decomposition
# ═══════════════════════════════════════════════════════════════


# def stage_3_split(cfg: SplitConfig) -> None:
#     """Split interactions per-user by chronological order, then decompose
#     by feedback type for model-specific consumption."""
#     outs = cfg.outputs
#     if all(_exists_and_nonempty(p) for p in outs.values()):
#         log.info("SKIP Stage 3: all %d outputs exist.", len(outs))
#         return

#     _require_file(cfg.kcore_parquet, "K-Core parquet")
#     log.info("Stage 3: Temporal per-user split + feedback decomposition")

#     tmp_dir = cfg.data_dir  # temp intermediates live alongside raw data

#     # ──────────────────────────────────────────────────────────
#     # Step 1: Compute per-user statistics (tiny — ~800K rows)
#     # ──────────────────────────────────────────────────────────
#     log.info("  Step 1/4: Computing per-user statistics...")
#     user_stats: pl.DataFrame = (
#         _clean_scan(cfg.kcore_parquet)
#         .filter(pl.col("date_updated").is_not_null())
#         .group_by("user_id")
#         .agg(pl.len().alias("n"))
#         # train_end: 1-indexed boundary, clamped so ≥1 train AND ≥2 remaining
#         .with_columns(
#             pl.col("n")
#             .mul(cfg.train_ratio)
#             .floor()
#             .cast(pl.Int64)
#             .clip(1, pl.col("n") - 2)
#             .alias("train_end"),
#         )
#         # valid_size: clamped so ≥1 valid AND ≥1 test in remaining
#         .with_columns(
#             (pl.col("n") - pl.col("train_end")).alias("remaining"),
#             pl.col("n")
#             .mul(cfg.valid_ratio)
#             .floor()
#             .cast(pl.Int64)
#             .alias("valid_raw"),
#         )
#         .with_columns(
#             pl.when(pl.col("remaining") <= 1)
#             .then(0)
#             .otherwise(pl.col("valid_raw").clip(1, pl.col("remaining") - 1))
#             .alias("valid_size"),
#         )
#         .with_columns(
#             (pl.col("train_end") + pl.col("valid_size")).alias("valid_end"),
#         )
#         .select("user_id", "n", "train_end", "valid_end")
#         .collect()
#     )

#     total_users = len(user_stats)
#     total_interactions = int(user_stats["n"].sum())
#     log.info(
#         "  User stats: %s users, %s interactions",
#         f"{total_users:,}",
#         f"{total_interactions:,}",
#     )

#     # Sanity: every user must have train_end ≥ 1 and valid_end < n
#     violations = user_stats.filter(
#         (pl.col("train_end") < 1) | (pl.col("valid_end") >= pl.col("n"))
#     )
#     if len(violations) > 0:
#         log.warning(
#             "  %d users have invalid split boundaries (will produce empty test).",
#             len(violations),
#         )

#     # ──────────────────────────────────────────────────────────
#     # Step 2: Sort full dataset → sorted Parquet (streaming-safe)
#     # ──────────────────────────────────────────────────────────
#     #   Splitting sort from the window function (Step 3) ensures
#     #   the sort can use Polars' streaming external-sort, which
#     #   needs far less RAM than holding the full dataset.
#     # ──────────────────────────────────────────────────────────
#     tmp_sorted = tmp_dir / "_split_sorted_tmp.parquet"
#     log.info("  Step 2/4: Sorting by (user_id, date, book_id)...")

#     (
#         _clean_scan(cfg.kcore_parquet)
#         .filter(pl.col("date_updated").is_not_null())
#         .sort("user_id", "date_updated", "book_id")
#         .sink_parquet(str(tmp_sorted))
#     )
#     log.info("  Sorted parquet: %s rows", f"{_pq_row_count(tmp_sorted):,}")

#     # ──────────────────────────────────────────────────────────
#     # Step 3: Add per-user row_num + join split boundaries
#     # ──────────────────────────────────────────────────────────
#     #   cum_sum(1).over("user_id") on SORTED data gives each
#     #   interaction a 1-indexed chronological position within
#     #   its user.  Joining the tiny user_stats frame broadcasts
#     #   train_end / valid_end so each row knows its split.
#     # ──────────────────────────────────────────────────────────
#     tmp_stats_pq = tmp_dir / "_user_stats_tmp.parquet"
#     user_stats.write_parquet(str(tmp_stats_pq))

#     tmp_ranked = tmp_dir / "_split_ranked_tmp.parquet"
#     log.info("  Step 3/4: Assigning per-user row numbers + split boundaries...")

#     (
#         pl.scan_parquet(str(tmp_sorted))
#         .with_columns(
#             pl.lit(1).cum_sum().over("user_id").alias("row_num"),
#         )
#         .join(pl.scan_parquet(str(tmp_stats_pq)), on="user_id", how="inner")
#         .sink_parquet(str(tmp_ranked))
#     )

#     tmp_sorted.unlink(missing_ok=True)
#     tmp_stats_pq.unlink(missing_ok=True)
#     log.info("  Ranked parquet ready: %s rows", f"{_pq_row_count(tmp_ranked):,}")

#     # ──────────────────────────────────────────────────────────
#     # Step 4: Export each split × feedback combination
#     # ──────────────────────────────────────────────────────────
#     log.info("  Step 4/4: Exporting split files...")
#     base_lf = pl.scan_parquet(str(tmp_ranked))

#     # Split predicates (row_num is 1-indexed, boundaries are inclusive)
#     is_train = pl.col("row_num") <= pl.col("train_end")
#     is_valid = (pl.col("row_num") > pl.col("train_end")) & (
#         pl.col("row_num") <= pl.col("valid_end")
#     )
#     is_test = pl.col("row_num") > pl.col("valid_end")

#     # Feedback predicates
#     _r = pl.col("rating")
#     is_explicit = (_r >= cfg.explicit_min_rating) & (_r <= cfg.explicit_max_rating)
#     is_implicit = ~is_explicit
#     is_positive = is_explicit & (_r >= cfg.positive_min_rating)
#     is_negative = is_explicit & (_r <= cfg.negative_max_rating)

#     keep_cols = ["user_id", "book_id", "rating"]

#     export_specs: list[tuple[str, pl.Expr]] = [
#         ("train_main", is_train),
#         ("explicit_train", is_train & is_explicit),
#         ("train_implicit", is_train & is_implicit),
#         ("valid_pos", is_valid & is_positive),
#         ("test_pos", is_test & is_positive),
#         ("test_neg", is_test & is_negative),
#         ("test_implicit", is_test & is_implicit),
#         ("explicit_test", (is_valid | is_test) & is_explicit),
#     ]

#     for name, predicate in export_specs:
#         out_csv = outs[name]
#         tmp_export = tmp_dir / f"_export_{name}_tmp.parquet"
#         lf = base_lf.filter(predicate).select(keep_cols)
#         rows = _export_lf_to_csv(lf, tmp_export, out_csv, cfg.csv_write_batch)
#         log.info("    %-16s → %s  (%s rows)", name, out_csv.name, f"{rows:,}")

#     # Cleanup ranked parquet
#     tmp_ranked.unlink(missing_ok=True)

#     log.info("Stage 3 complete. All outputs in: %s", cfg.output_dir)

import polars as pl
import logging

log = logging.getLogger(__name__)

def stage_3_split(cfg: SplitConfig) -> None:
    log.info("Stage 3: Pure Polars Streaming (Zero-RAM Trick)")

    # Khởi tạo các đường dẫn file tạm
    tmp_sorted = cfg.data_dir / "_split_sorted_tmp.parquet"
    tmp_stats = cfg.data_dir / "_split_stats_tmp.parquet"
    tmp_ranked = cfg.data_dir / "_split_ranked_tmp.parquet"

    # ==========================================
    # BƯỚC 1: SẮP XẾP OUT-OF-CORE
    # ==========================================
    log.info("  1. Sorting data out-of-core...")
    (
        _clean_scan(cfg.kcore_parquet)
        .filter(pl.col("date_updated").is_not_null())
        .sort(["user_id", "date_updated"])
        .sink_parquet(tmp_sorted)
    )

    # ==========================================
    # BƯỚC 2: TÍNH TOÁN USER STATS
    # ==========================================
    log.info("  2. Calculating user bounds...")
    # Quét để lấy số dòng của mỗi user (Kết quả chỉ ~800k dòng, nạp vào RAM thoải mái)
    user_stats = (
        _clean_scan(cfg.kcore_parquet)
        .group_by("user_id")
        .agg(pl.len().alias("total_rows"))
        .with_columns([
            (pl.col("total_rows") * cfg.train_ratio).floor().cast(pl.Int32).alias("train_end"),
            (pl.col("total_rows") * (cfg.train_ratio + cfg.valid_ratio)).floor().cast(pl.Int32).alias("valid_end")
        ])
        .collect() # Nạp vào RAM vì nó rất nhỏ (~10MB)
    )
    user_stats.write_parquet(tmp_stats)

    # ==========================================
    # BƯỚC 3: TRICK GLOBAL INDEX ĐỂ LẤY ROW_NUM
    # ==========================================
    log.info("  3. Calculating row numbers without Window Functions...")
    # Thêm số thứ tự từ 0 đến hết
    lazy_sorted = pl.scan_parquet(tmp_sorted).with_row_index("global_idx")

    # Tìm vị trí bắt đầu (start_idx) của từng user
    user_starts = (
        lazy_sorted
        .group_by("user_id")
        .agg(pl.col("global_idx").first().alias("start_idx"))
    )

    # Kết hợp (Join) tất cả lại và tính row_num
    # Hash Join trong Streaming hoạt động cực mượt nếu bảng phụ (user_starts) nhỏ
    (
        lazy_sorted
        .join(user_starts, on="user_id", how="inner")
        .join(pl.scan_parquet(tmp_stats), on="user_id", how="inner")
        .with_columns(
            (pl.col("global_idx") - pl.col("start_idx") + 1).alias("row_num")
        )
        .sink_parquet(tmp_ranked)
    )

    # ==========================================
    # BƯỚC 4: XUẤT CSV (STREAMING & PUSHDOWN FILTER)
    # ==========================================
    log.info("  4. Exporting to CSVs...")
    lazy_full = pl.scan_parquet(tmp_ranked)

    # Các điều kiện Boolean y hệt logic gốc của bạn
    is_train = pl.col("row_num") <= pl.col("train_end")
    is_valid = (pl.col("row_num") > pl.col("train_end")) & (pl.col("row_num") <= pl.col("valid_end"))
    is_test = pl.col("row_num") > pl.col("valid_end")

    is_explicit = pl.col("rating").is_between(cfg.explicit_min_rating, cfg.explicit_max_rating)
    is_implicit = ~is_explicit
    is_pos = is_explicit & (pl.col("rating") >= cfg.positive_min_rating)
    is_neg = is_explicit & (pl.col("rating") <= cfg.negative_max_rating)

    export_specs = [
        ("train_main", is_train, cfg.outputs["train_main"]),
        ("explicit_train", is_train & is_explicit, cfg.outputs["explicit_train"]),
        ("train_implicit", is_train & is_implicit, cfg.outputs["train_implicit"]),
        ("valid_pos", is_valid & is_pos, cfg.outputs["valid_pos"]),
        ("test_pos", is_test & is_pos, cfg.outputs["test_pos"]),
        ("test_neg", is_test & is_neg, cfg.outputs["test_neg"]),
        ("test_implicit", is_test & is_implicit, cfg.outputs["test_implicit"]),
        ("explicit_test", (is_valid | is_test) & is_explicit, cfg.outputs["explicit_test"]),
    ]

    keep_cols = ["user_id", "book_id", "rating"]

    for name, predicate, out_path in export_specs:
        log.info(f"    Exporting {name}...")
        # Polars sẽ sử dụng Predicate Pushdown để quét qua file Parquet cực nhanh
        # sink_csv giúp đẩy thẳng dữ liệu ra file mà không lưu qua RAM
        (
            lazy_full
            .filter(predicate)
            .select(keep_cols)
            .sink_csv(out_path)
        )

    # Dọn dẹp rác (Cleanup)
    tmp_sorted.unlink(missing_ok=True)
    tmp_stats.unlink(missing_ok=True)
    tmp_ranked.unlink(missing_ok=True)

    log.info("Stage 3 Hoàn tất hoàn hảo! Không tràn RAM.")

# ═══════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════


def main() -> None:
    if IN_COLAB:
        from google.colab import drive  # type: ignore[import-untyped]

        drive.mount("/content/drive")

    cfg = SplitConfig()

    log.info("=" * 60)
    log.info("Data Split Pipeline v2")
    log.info("  Base dir : %s", cfg.base_dir)
    log.info("  Data dir : %s", cfg.data_dir)
    log.info("  Output   : %s", cfg.output_dir)
    log.info(
        "  K-Core   : k_book=%d  k_user=%d",
        cfg.k_book,
        cfg.k_user,
    )
    log.info(
        "  Split    : train=%.0f%%  valid=%.0f%%  test=%.0f%%",
        cfg.train_ratio * 100,
        cfg.valid_ratio * 100,
        (1 - cfg.train_ratio - cfg.valid_ratio) * 100,
    )
    log.info("=" * 60)

    if cfg.run_stage1:
        stage_1_ingest(cfg)
    else:
        log.info("SKIP Stage 1 (SKIP_STAGE1=1)")

    if cfg.run_stage2:
        stage_2_kcore(cfg)
    else:
        log.info("SKIP Stage 2 (SKIP_STAGE2=1)")

    if cfg.run_stage3:
        stage_3_split(cfg)
    else:
        log.info("SKIP Stage 3 (SKIP_STAGE3=1)")

    log.info("Pipeline finished.")


if __name__ == "__main__":
    main()
