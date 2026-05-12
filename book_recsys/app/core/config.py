from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    PROJECT_NAME: str = "Book Recommendation API"
    API_V1_STR: str = "/api/v1"

    # Legacy TF-IDF/KNN paths (kept for backward compatibility)
    CB_KNN_PATH:        str = "models/knn_model.pkl"
    CB_MATRIX_PATH:     str = "models/TF_IDF_item_vector.npz"
    CB_BOOK_INDEX_PATH: str = "models/cb_book_index.csv"

    # Hybrid Recommender Paths (Dynamic based on MODEL_VERSION)
    MODEL_VERSION: str = "model_v1"
    MODELS_BASE_DIR: str = "models"
    
    @property
    def MODEL_DIR(self) -> str:
        return f"{self.MODELS_BASE_DIR}/{self.MODEL_VERSION}"
        
    @property
    def HYBRID_USER_INDEX_PATH(self) -> str:
        return f"{self.MODEL_DIR}/user_index.csv"

    @property
    def HYBRID_USER_HISTORY_DB_PATH(self) -> str:
        return f"{self.MODEL_DIR}/user_history.db"

    @property
    def HYBRID_CB_BOOK_INDEX_PATH(self) -> str:
        return f"{self.MODEL_DIR}/cb_sbert_book_index.csv"

    @property
    def HYBRID_CB_USER_VECTORS_PATH(self) -> str:
        return f"{self.MODEL_DIR}/cb_sbert_user_matrix.npy"

    @property
    def HYBRID_CB_FAISS_INDEX_PATH(self) -> str:
        return f"{self.MODEL_DIR}/cb_sbert_IVFFlat.index"

    @property
    def HYBRID_CF_USER_VECTORS_PATH(self) -> str:
        return f"{self.MODEL_DIR}/cf_als_user_profiles.npy"

    @property
    def HYBRID_CF_FAISS_INDEX_PATH(self) -> str:
        return f"{self.MODEL_DIR}/cf_als_item_matrix.index"
        
    HYBRID_USE_GPU: bool = False

    # Data paths
    BOOKS_PARQUET_PATH:       str = "data/processed_2/books_filtered.parquet"
    USER_INTERACTIONS_PATH:   str = "data/processed_2/user_interactions.parquet"

    class Config:
        env_file = ".env"


settings = Settings()
