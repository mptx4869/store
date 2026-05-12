"""
CB Recommender Service
Builds recommender instances from app.recommenders classes and exposes
service-level methods used by API routes.
"""

from __future__ import annotations

import gc
import logging
import os
from typing import Optional

import pandas as pd

from app.recommenders.factory import RecommenderFactory
from app.recommenders.item_profile import ItemProfile
from app.recommenders.mean_user_profile import MeanProfileManager
from app.recommenders.sbert_recommender import FaissContentBasedRecommender
from app.recommenders.types import RecommendRequest

logger = logging.getLogger(__name__)


class CBRecommender:
    """Service wrapper for the content-based recommender classes.

    The service keeps API-facing methods stable while internally using:
    - ItemProfile (item id <-> vector mapping)
    - MeanProfileManager (user vector/history store)
    - FaissContentBasedRecommender (ranking engine)
    """

    def __init__(
        self,
        index_path: str,
        item_mapping_path: str,
        item_vectors_path: str,
        books_parquet_path: str,
        user_profiles_path: str,
        user_mapping_path: str,
        user_history_path: str,
        model_type: str = "faiss",
        model_name: str = "cbf_sbert_faiss",
        vector_dim: int = 384,
        use_gpu: bool = False,
        gpu_device_id: int = 0,
        fallback_to_cpu_on_gpu_error: bool = True,
    ) -> None:
        self.model_type = model_type
        self.model_name = model_name
        self.vector_dim = int(vector_dim)
        self.use_gpu = bool(use_gpu)
        self.gpu_device_id = int(gpu_device_id)
        self.fallback_to_cpu_on_gpu_error = bool(fallback_to_cpu_on_gpu_error)

        self._paths = {
            "index": index_path,
            "item_mapping": item_mapping_path,
            "item_vectors": item_vectors_path,
            "books_parquet": books_parquet_path,
            "user_profiles": user_profiles_path,
            "user_mapping": user_mapping_path,
            "user_history": user_history_path,
        }

        self.item_profile: Optional[ItemProfile] = None
        self.user_profile_manager: Optional[MeanProfileManager] = None
        self.recommender: Optional[FaissContentBasedRecommender] = None
        self.df_books: Optional[pd.DataFrame] = None

        self._loaded = False

    # ------------------------------------------------------------------
    # Load / Reload
    # ------------------------------------------------------------------

    def load(self) -> None:
        """Load item store, user profile store, and recommender runtime."""
        logger.info("Loading recommender service artifacts...")

        item_profile = ItemProfile(
            mapping_path=self._paths["item_mapping"],
            vectors_path=self._paths["item_vectors"],
            vector_dim=self.vector_dim,
        )
        item_profile.load()

        user_profile_manager = MeanProfileManager(
            user_vectors_path=self._paths["user_profiles"],
            user_mapping_path=self._paths["user_mapping"],
            user_history_path=self._paths["user_history"],
            vector_dim=self.vector_dim,
        )
        user_profile_manager.load()

        created = RecommenderFactory.create(
            self.model_type,
            index_path=self._paths["index"],
            item_profile=item_profile,
            user_profile_manager=user_profile_manager,
            model_name=self.model_name,
            gpu_device_id=self.gpu_device_id,
            fallback_to_cpu_on_gpu_error=self.fallback_to_cpu_on_gpu_error,
        )

        if not isinstance(created, FaissContentBasedRecommender):
            raise TypeError(
                "CB service expects FaissContentBasedRecommender. "
                f"Got: {type(created).__name__}."
            )

        created.load()
        if self.use_gpu:
            created.set_runtime(device="gpu", use_gpu=True)
        else:
            created.set_runtime(device="cpu", use_gpu=False)

        self.item_profile = item_profile
        self.user_profile_manager = user_profile_manager
        self.recommender = created

        self._load_books_metadata()

        self._loaded = True
        logger.info("CB recommender service ready.")

    def reload(self) -> None:
        """Release runtime objects and reload all artifacts."""
        self.item_profile = None
        self.user_profile_manager = None
        self.recommender = None
        self.df_books = None
        self._loaded = False
        gc.collect()
        self.load()

    # ------------------------------------------------------------------
    # Public API for Routes
    # ------------------------------------------------------------------

    def recommend_similar(self, book_id: str, k: int = 10) -> list[dict]:
        self._require_loaded()
        assert self.item_profile is not None
        assert self.recommender is not None

        item_id = str(book_id)
        query_vec = self.item_profile.get_vector(item_id)
        if query_vec is None:
            raise ValueError(
                f"book_id '{book_id}' not found in item profile mapping."
            )

        req = RecommendRequest(
            user_id=f"item::{item_id}",
            seen_item_ids={item_id},
            user_vector=query_vec,
        )
        topn = max(100, k + 30)
        ranked = self.recommender.recommend(req, k=k, topn=topn)

        return [
            self._build_result(item_id=item.item_id, similarity=item.score)
            for item in ranked
        ]

    def recommend_for_user(self, user_id: str, k: int = 10) -> list[dict]:
        self._require_loaded()
        assert self.user_profile_manager is not None
        assert self.recommender is not None

        uid = str(user_id)
        user_vec = self.user_profile_manager.get_profile(uid)
        seen = self.user_profile_manager.get_seen_items(uid)

        if user_vec is None and not seen:
            raise ValueError(
                f"user_id '{user_id}' not found or has no available profile/history."
            )

        req = RecommendRequest(user_id=uid)
        topn = max(120, k + len(seen) + 20)
        ranked = self.recommender.recommend(req, k=k, topn=topn)

        return [
            self._build_result(item_id=item.item_id, similarity=item.score)
            for item in ranked
        ]

    def get_book_info(self, book_id: str) -> dict:
        return self._get_book_meta(str(book_id))

    def get_user_profile_size(self, user_id: str) -> int:
        if self.user_profile_manager is None:
            return 0
        try:
            return len(self.user_profile_manager.get_seen_items(str(user_id)))
        except RuntimeError:
            return 0

    # ------------------------------------------------------------------
    # Properties / Status
    # ------------------------------------------------------------------

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    @property
    def n_books(self) -> int:
        if self.item_profile is None:
            return 0
        return int(self.item_profile.total_items)

    @property
    def n_users(self) -> int:
        if self.user_profile_manager is None:
            return 0
        return int(len(getattr(self.user_profile_manager, "_user_id_to_idx", {})))

    def healthcheck(self) -> dict:
        recommender_health = {}
        if self.recommender is not None:
            recommender_health = self.recommender.healthcheck()

        return {
            "service_loaded": self.is_loaded,
            "n_books": self.n_books,
            "n_users": self.n_users,
            "has_books_metadata": self.df_books is not None,
            "paths": self._paths,
            "recommender": recommender_health,
        }

    # ------------------------------------------------------------------
    # Private Helpers
    # ------------------------------------------------------------------

    def _load_books_metadata(self) -> None:
        books_path = self._paths["books_parquet"]
        if not os.path.exists(books_path):
            logger.warning("books.parquet not found at %s", books_path)
            self.df_books = None
            return

        df = pd.read_parquet(books_path)
        expected_cols = [
            "book_id",
            "title",
            "author",
            "genres",
            "average_rating",
            "image_url",
        ]

        if "book_id" not in df.columns:
            logger.warning("books metadata missing 'book_id' column at %s", books_path)
            self.df_books = None
            return

        for col in expected_cols:
            if col not in df.columns:
                df[col] = ""

        self.df_books = (
            df[expected_cols]
            .fillna("")
            .astype(str)
            .set_index("book_id")
        )
        logger.info("Loaded books metadata: %d rows", len(self.df_books))

    def _build_result(self, item_id: str, similarity: float) -> dict:
        meta = self._get_book_meta(item_id)
        return {
            "book_id": str(item_id),
            "title": meta.get("title", ""),
            "author": meta.get("author", ""),
            "genres": meta.get("genres", ""),
            "average_rating": meta.get("average_rating", ""),
            "image_url": meta.get("image_url", ""),
            "similarity_score": round(float(similarity), 4),
        }

    def _get_book_meta(self, item_id: str) -> dict:
        if self.df_books is None or item_id not in self.df_books.index:
            return {}

        row_or_rows = self.df_books.loc[item_id]
        if isinstance(row_or_rows, pd.DataFrame):
            return row_or_rows.iloc[0].to_dict()
        return row_or_rows.to_dict()

    def _require_loaded(self) -> None:
        if not self._loaded or self.recommender is None or self.item_profile is None:
            raise RuntimeError(
                "CB service is not loaded. Call load() first or POST /api/v1/model/reload."
            )
