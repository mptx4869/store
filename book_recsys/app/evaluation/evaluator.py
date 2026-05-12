import logging
from typing import Any

import numpy as np
import polars as pl
from ranx import Qrels, Run, evaluate

logger = logging.getLogger(__name__)

class RecommendationEvaluator:
    """
    Giám khảo đánh giá mô hình. 
    Chịu trách nhiệm tính toán các chỉ số khắt khe nhất (Metrics, Coverage, Segments).
    """
    def __init__(self, k: int = 10, total_catalog_size: int = 0):
        self.k = k
        self.total_catalog_size = total_catalog_size
        # Khai báo các bài thi tiêu chuẩn
        self.metrics_list = [f"hit_rate@{k}", f"recall@{k}", f"ndcg@{k}", "mrr"]

    def compute_metrics(self, qrels: Qrels, run: Run) -> dict[str, float]:
        """Tính toán các chỉ số xếp hạng tiêu chuẩn bằng thư viện ranx."""
        if len(run) == 0 or len(qrels) == 0:
            logger.warning("Qrels hoặc Run rỗng. Trả về điểm 0.")
            return {m: 0.0 for m in self.metrics_list}

        # threads=0 giúp ranx tự động dùng tối đa số CPU cores để tăng tốc
        report = evaluate(qrels, run, self.metrics_list, threads=0)
        return report

    def compute_coverage(self, run: Run) -> float:
        """
        Đo lường Độ phủ Catalog (Catalog Coverage).
        Cho biết mô hình có bị thiên vị (bias) chỉ gợi ý sách phổ biến hay không.
        """
        if self.total_catalog_size <= 0:
            return 0.0
            
        recommended_items = set()
        for user_id in run.keys():
            # ranx lưu kết quả dưới dạng dict {item_id: score}
            for item_id in run[user_id].keys():
                recommended_items.add(item_id)
                
        return float(len(recommended_items) / self.total_catalog_size)

    def generate_segmented_report(
        self, 
        qrels: Qrels, 
        run: Run, 
        user_segments: dict[str, str]
    ) -> pl.DataFrame:
        """
        Đánh giá chuyên sâu: Chia tách điểm số theo từng nhóm người dùng.
        user_segments: dict mapping từ user_id -> tên nhóm (VD: 'cold_start', 'heavy')
        """
        segment_qrels: dict[str, dict] = {}
        segment_runs: dict[str, dict] = {}

        # 1. Tách qrels và run theo từng segment
        for user_id, segment_name in user_segments.items():
            if user_id not in qrels or user_id not in run:
                continue
                
            if segment_name not in segment_qrels:
                segment_qrels[segment_name] = {}
                segment_runs[segment_name] = {}
                
            segment_qrels[segment_name][user_id] = qrels[user_id]
            segment_runs[segment_name][user_id] = run[user_id]

        # 2. Tính điểm cho từng segment
        report_rows = []
        for segment_name in segment_qrels.keys():
            sqrels = Qrels(segment_qrels[segment_name])
            srun = Run(segment_runs[segment_name])
            
            metrics = self.compute_metrics(sqrels, srun)
            
            row = {
                "segment": segment_name,
                "user_count": len(srun),
                **metrics
            }
            report_rows.append(row)

        # Trả về bảng Polars để dễ hiển thị và xuất file
        return pl.DataFrame(report_rows)