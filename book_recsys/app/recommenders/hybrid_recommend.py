from typing import List, Dict
from concurrent.futures import ThreadPoolExecutor
from app.recommenders.types import RecommendItem


class HybridRecommender:

    def __init__(
        self,
        cf_engine,         # VectorRecommender (CF)
        cbf_engine,        # VectorRecommender (CBF)
        user_proc,         # UserProcessor  — chịu trách nhiệm lấy lịch sử user
        book_proc,         # BookProcessor  — chịu trách nhiệm tra cứu book_id
        weight_cf: float = 0.65,
        weight_cbf: float = 0.35,
    ):
        self.cf_engine  = cf_engine
        self.cbf_engine = cbf_engine
        self.user_proc  = user_proc
        self.book_proc  = book_proc

        # Đảm bảo tổng trọng số luôn bằng 1.0
        total_weight = weight_cf + weight_cbf
        self.w_cf  = weight_cf  / total_weight
        self.w_cbf = weight_cbf / total_weight

        # Singleton executor: tạo 1 lần, tái sử dụng suốt vòng đời service
        self._executor = ThreadPoolExecutor(max_workers=2, thread_name_prefix="rec")

        print(f"Hybrid Engine Ready! (Weights: CF={self.w_cf:.2f}, CBF={self.w_cbf:.2f})")

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def get_user_history(self, user_id: str) -> set:
        return self.user_proc.get_history_idx(user_id)

    def get_user_history_length(self, user_id: str) -> int:
        return self.user_proc.get_history_length(user_id)

    def get_user_idx(self, user_id: str) -> int:
        """Lấy index vector của user (delegate tới user_proc)."""
        return self.user_proc.get_idx(user_id)

    def get_book_id(self, item_idx: int) -> str:
        """Tra cứu book_id từ index (delegate tới book_proc)."""
        return self.book_proc.get_id(item_idx)

    def _normalize_scores(self, items: List[RecommendItem]) -> Dict[str, float]:
        if not items:
            return {}

        scores = [item.score for item in items]
        min_s, max_s = min(scores), max(scores)

        normalized_dict = {}
        for item in items:
            if max_s == min_s:
                norm_score = 1.0
            else:
                norm_score = (item.score - min_s) / (max_s - min_s)
            normalized_dict[item.item_id] = norm_score

        return normalized_dict

    # ------------------------------------------------------------------
    # Core recommendation
    # ------------------------------------------------------------------

    def recommend(self, user_id: str, k: int = 10) -> List[RecommendItem]:

        # Step 0: Fetch user history ONCE (single DB call, no concurrency issue)
        user_history        = self.get_user_history(user_id)
        user_history_length = len(user_history)

        # Step 1: Run CF & CBF engines IN PARALLEL trên shared executor
        #         passing pre-fetched history so engines never touch the DB.
        search_k = k * 2
        future_cf = self._executor.submit(
            self.cf_engine.recommend_realtime,
            user_id, search_k,
            20,                   # nprobe
            user_history,
            user_history_length,
        )
        future_cbf = self._executor.submit(
            self.cbf_engine.recommend_realtime,
            user_id, search_k,
            20,                   # nprobe
            user_history,
            user_history_length,
        )
        cf_candidates  = future_cf.result()
        cbf_candidates = future_cbf.result()

        # Step 2: Normalize internal scores
        cf_norm_dict  = self._normalize_scores(cf_candidates)
        cbf_norm_dict = self._normalize_scores(cbf_candidates)

        # Step 3: Merge scores (Weighted Fusion)
        combined_scores: Dict[str, float] = {}

        for item_id, score in cf_norm_dict.items():
            combined_scores[item_id] = combined_scores.get(item_id, 0.0) + score * self.w_cf

        for item_id, score in cbf_norm_dict.items():
            combined_scores[item_id] = combined_scores.get(item_id, 0.0) + score * self.w_cbf

        # Step 4: Sort descending and cut Top-K
        sorted_items = sorted(combined_scores.items(), key=lambda x: x[1], reverse=True)

        # Step 5: Pack into standard RecommendItem list
        final_recs = [
            RecommendItem(item_id=item_id, score=final_score, rank=rank)
            for rank, (item_id, final_score) in enumerate(sorted_items[:k], start=1)
        ]

        return final_recs