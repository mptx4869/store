import gc
import logging
from typing import List, Any

from app.recommenders.processer import BookProcessor, UserProcessor
from app.recommenders.vector_recommender import VectorRecommender
from app.recommenders.hybrid_recommend import HybridRecommender
from app.core.config import settings

logger = logging.getLogger(__name__)


class HybridService:
    def __init__(
        self,
        user_index_path: str,
        user_history_db_path: str,
        cb_book_index_path: str,
        cb_user_vectors_path: str,
        cb_faiss_index_path: str,
        cf_user_vectors_path: str,
        cf_faiss_index_path: str,
        use_gpu: bool = True
    ):
        self._paths = {
            "user_index":    user_index_path,
            "user_history":  user_history_db_path,
            "cb_book_index": cb_book_index_path,
            "cb_user_vectors": cb_user_vectors_path,
            "cb_faiss_index":  cb_faiss_index_path,
            "cf_user_vectors": cf_user_vectors_path,
            "cf_faiss_index":  cf_faiss_index_path,
        }
        self.use_gpu = use_gpu
        self._loaded = False

        self.user_processor = None
        self.book_processor = None
        self.cf_engine  = None
        self.cbf_engine = None
        self.hybrid_engine = None

    def load(self):
        logger.info("Loading Hybrid Service artifacts...")

        self.user_processor = UserProcessor(
            user_index_path=self._paths["user_index"],
            history_db_path=self._paths["user_history"]
        )

        self.book_processor = BookProcessor(
            item_index_path=self._paths["cb_book_index"]
        )

        print("Initializing CBF Engine...")
        self.cbf_engine = VectorRecommender(
            user_processor=self.user_processor,
            book_processor=self.book_processor,
            user_vectors_path=self._paths["cb_user_vectors"],
            faiss_index_path=self._paths["cb_faiss_index"],
            use_gpu=self.use_gpu
        )

        print("Initializing CF Engine...")
        self.cf_engine = VectorRecommender(
            user_processor=self.user_processor,
            book_processor=self.book_processor,
            user_vectors_path=self._paths["cf_user_vectors"],
            faiss_index_path=self._paths["cf_faiss_index"],
            use_gpu=self.use_gpu
        )
        
        self.hybrid_engine = HybridRecommender(
            cf_engine=self.cf_engine,
            cbf_engine=self.cbf_engine,
            user_proc=self.user_processor,
            book_proc=self.book_processor,
        )

        self._loaded = True
        logger.info("Hybrid Service loaded successfully.")

    def reload(self):
        self.user_processor = None
        self.book_processor  = None
        self.cf_engine  = None
        self.cbf_engine = None
        self.hybrid_engine = None
        self._loaded = False
        gc.collect()
        self.load()

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def recommend_for_user(self, user_id: str, k: int = 10) -> List[str]:
        """Return a ranked list of book_id strings for the given user."""
        if not self._loaded:
            raise RuntimeError("Service not loaded")

        if self.user_processor.get_idx(user_id) == -1:
            raise ValueError(f"user_id '{user_id}' not found.")

        results = self.hybrid_engine.recommend(user_id, k=k)
        return [item.item_id for item in results]

    def recommend_raw(self, user_id: str, k: int = 10) -> List[Any]:
        """Return raw RecommendItem objects (for internal/gRPC use)."""
        if not self._loaded:
            raise RuntimeError("Service not loaded")

        if self.user_processor.get_idx(user_id) == -1:
            raise ValueError(f"user_id '{user_id}' not found.")

        return self.hybrid_engine.recommend(user_id, k=k)

    def get_user_profile_size(self, user_id: str) -> int:
        if not self._loaded or not self.user_processor:
            return 0
        return self.user_processor.get_history_length(user_id)

    @property
    def is_loaded(self) -> bool:
        return self._loaded
