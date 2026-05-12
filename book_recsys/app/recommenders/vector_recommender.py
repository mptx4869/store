import faiss
import numpy as np
from typing import List, Any,Dict
from dataclasses import dataclass
from app.recommenders.processer import BookProcessor, UserProcessor
from app.recommenders.types import RecommendItem
# @dataclass(slots=True, frozen=True)
# class RecommendItem:
#     item_id: str
#     score: float
#     rank: int

#     def to_api_dict(self) -> dict[str, Any]:
#         return {
#             "item_id": self.item_id,
#             "score": float(self.score), 
#             "rank": int(self.rank)
#         }

class VectorRecommender:

    def __init__(
        self,
        user_processor, 
        book_processor, 
        user_vectors_path: str,
        faiss_index_path: str,
        use_gpu: bool = True
    ):
        self.user_proc = user_processor
        self.book_proc = book_processor
        self.use_gpu = use_gpu
        
        print(f"Loading Data & CPU FAISS Index...")
        #self.user_vectors = np.load(user_vectors_path).astype('float32')
        self.user_vectors = np.load(user_vectors_path, mmap_mode = 'r')
        self.faiss_index = faiss.read_index(faiss_index_path,faiss.IO_FLAG_READ_ONLY)
        
        if self.use_gpu:
            print("Cloning FAISS Index to GPU VRAM (for Batch Processing)...")
            res = faiss.StandardGpuResources()
            self.gpu_index = faiss.index_cpu_to_gpu(res, 0, self.faiss_index)
        else:
            self.gpu_index = self.faiss_index

        print(f"Recommender Engine Ready (Users: {len(self.user_vectors):,})")

    # REAL-TIME SERVING (API)
    def recommend_realtime(
        self,
        user_id: str,
        k: int = 10,
        nprobe: int = 20,
        user_history: set = None,
        user_history_length: int = 0,
    ) -> List[RecommendItem]:
        """
        Tìm kiếm top-k sách gợi ý cho user.

        Args:
            user_id: ID người dùng.
            k: Số lượng kết quả mong muốn.
            nprobe: Số cluster FAISS duyệt (độ chính xác vs tốc độ).
            user_history: Tập index sách user đã đọc (pre-fetched từ HybridRecommender).
            user_history_length: Số lượng sách trong lịch sử (để mở rộng search_k).
        """
        user_history = user_history or set()

        user_idx = self.user_proc.get_idx(user_id)
        if user_idx == -1:
            return []

        self.faiss_index.nprobe = nprobe

        query_vector = self.user_vectors[user_idx].reshape(1, -1)

        search_k = k + user_history_length
        distances, indices = self.faiss_index.search(query_vector, search_k)

        user_recs = []
        current_rank = 1

        for rank_idx in range(search_k):
            item_idx = indices[0][rank_idx]

            if item_idx == -1 or int(item_idx) in user_history:
                continue

            book_id = self.book_proc.get_id(item_idx)
            if book_id:
                user_recs.append(RecommendItem(
                    item_id=book_id,
                    score=float(distances[0][rank_idx]),
                    rank=current_rank
                ))
                current_rank += 1

            if len(user_recs) == k:
                break

        return user_recs

    # BATCH EVALUATION 
    # def recommend_batch(
    #     self, 
    #     user_ids: List[str], 
    #     k: int = 100, 
    #     nprobe: int = 50
    # ) -> Dict[str, List[RecommendItem]]:

    #     self.faiss_index.nprobe = nprobe
        
    #     valid_uids = [uid for uid in user_ids if self.user_proc.get_idx(uid) != -1]
    #     if not valid_uids:
    #         return {uid: [] for uid in user_ids}
            
    #     user_indices = [self.user_proc.get_idx(uid) for uid in valid_uids]
    #     query_vectors = self.user_vectors[user_indices]
        
    #     search_k = k * 2
    #     distances, indices = self.faiss_index.search(query_vectors, search_k)
        
    #     results = {}
    #     for i, uid in enumerate(valid_uids):
    #         history_indices = self.user_proc.get_history_idx(uid)
    #         user_recs = []
    #         current_rank = 1
            
    #         for rank_idx in range(search_k):
    #             item_idx = indices[i][rank_idx]
                
    #             if item_idx == -1 or int(item_idx) in history_indices:
    #                 continue
                
    #             book_id = self.book_proc.get_id(item_idx)
    #             if book_id:
    #                 user_recs.append(RecommendItem(
    #                     item_id=book_id,
    #                     score=float(distances[i][rank_idx]),
    #                     rank=current_rank
    #                 ))
    #                 current_rank += 1
                
    #             if len(user_recs) == k:
    #                 break
            
    #         results[uid] = user_recs
            
    #     for uid in user_ids:
    #         if uid not in results:
    #             results[uid] = []
                
    #     return results
    def recommend_batch(
        self, 
        user_ids: List[str], 
        k: int = 100, 
        nprobe: int = 50
    ) -> Dict[str, List[RecommendItem]]:
        
        # DÙNG BẢN SAO GPU
        self.gpu_index.nprobe = nprobe
        
        valid_uids = [uid for uid in user_ids if self.user_proc.get_idx(uid) != -1]
        if not valid_uids: return {uid: [] for uid in user_ids}
            
        user_indices = [self.user_proc.get_idx(uid) for uid in valid_uids]
        query_vectors = self.user_vectors[user_indices]
        batch_histories = [self.user_proc.get_history_idx(uid) for uid in valid_uids]
        max_history_len = max([len(h) for h in batch_histories]) if batch_histories else 0

        MAX_GPU_K = 2048
        search_k = int(min(k + max_history_len, MAX_GPU_K))
        self.gpu_index.nprobe = nprobe
        distances, indices = self.gpu_index.search(query_vectors, search_k)
        
        results = {}
        for i, uid in enumerate(valid_uids):
            history_indices = self.user_proc.get_history_idx(uid)
            user_recs = []
            current_rank = 1
            
            for rank_idx in range(search_k):
                item_idx = indices[i][rank_idx]
                if item_idx == -1 or int(item_idx) in history_indices:
                    continue
                
                book_id = self.book_proc.get_id(item_idx)
                if book_id:
                    user_recs.append(RecommendItem(book_id, float(distances[i][rank_idx]), current_rank))
                    current_rank += 1
                
                if len(user_recs) == k: break
            
            results[uid] = user_recs
            
        for uid in user_ids:
            if uid not in results: results[uid] = []
                
        return results