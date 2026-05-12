"""
Dependency Injection — Singleton CBRecommender.
Initialized once at app startup and injected into route handlers.
"""

from functools import lru_cache

from app.services.hybrid_service import HybridService
from app.core.config import settings

@lru_cache(maxsize=1)
def get_hybrid_service() -> HybridService:
    service = HybridService(
        user_index_path=settings.HYBRID_USER_INDEX_PATH,
        user_history_db_path=settings.HYBRID_USER_HISTORY_DB_PATH,
        cb_book_index_path=settings.HYBRID_CB_BOOK_INDEX_PATH,
        cb_user_vectors_path=settings.HYBRID_CB_USER_VECTORS_PATH,
        cb_faiss_index_path=settings.HYBRID_CB_FAISS_INDEX_PATH,
        cf_user_vectors_path=settings.HYBRID_CF_USER_VECTORS_PATH,
        cf_faiss_index_path=settings.HYBRID_CF_FAISS_INDEX_PATH,
        use_gpu=settings.HYBRID_USE_GPU
    )
    service.load()
    return service
