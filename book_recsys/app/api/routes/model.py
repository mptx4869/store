from fastapi import APIRouter, Depends, HTTPException

from app.core.dependencies import get_hybrid_service
from app.services.hybrid_service import HybridService

router = APIRouter()


@router.post(
    "/reload",
    summary="Reload CB model artifacts",
    description="Clear the model cache and reload from disk. Use after uploading new model files to /models/.",
)
def reload_model(
    hybrid_recommender: HybridService = Depends(get_hybrid_service),
):
    """
    Hot-reload without restarting the server:
    1. Clear lru_cache so the next call creates a new instance
    2. Call reload() to clear old state and load fresh artifacts from disk
    """
    try:
        get_hybrid_service.cache_clear()
        hybrid_recommender.reload()
        
        return {
            "status":   "success",
            "message":  "Models reloaded successfully."
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Reload failed: {str(e)}")


@router.get(
    "/status",
    summary="Check model status",
)
def model_status(
    hybrid_recommender: HybridService = Depends(get_hybrid_service),
):
    """Return current model status and basic stats."""
    return {
        "hybrid_is_loaded": hybrid_recommender.is_loaded
    }
