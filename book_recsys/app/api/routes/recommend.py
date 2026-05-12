from fastapi import APIRouter, Depends, HTTPException, Query
from typing import Annotated

from app.schemas.recommend import (
    SimilarBooksResponse,
    UserRecommendationResponse,
)
from app.core.dependencies import get_hybrid_service
from app.services.hybrid_service import HybridService

router = APIRouter()


# -----------------------------------------------------------------
# GET /api/v1/recommend/item/{book_id}/similar
# -----------------------------------------------------------------

@router.get(
    "/item/{book_id}/similar",
    response_model=SimilarBooksResponse,
    summary="Similar books (Content-Based)",
    description="Currently disabled.",
)
def get_similar_books(
    book_id: str,
    k: Annotated[int, Query(ge=1, le=50, description="Number of results")] = 10,
):
    raise HTTPException(
        status_code=501,
        detail="Not Implemented: Item similarity is currently disabled."
    )


# -----------------------------------------------------------------
# GET /api/v1/recommend/user/{user_id}
# -----------------------------------------------------------------

@router.get(
    "/user/{user_id}",
    response_model=UserRecommendationResponse,
    summary="Personalized recommendations for a user (Hybrid CF + CBF)",
)
def get_recommendations_for_user(
    user_id: str,
    k: Annotated[int, Query(ge=1, le=50, description="Number of results")] = 10,
    recommender: HybridService = Depends(get_hybrid_service),
):
    _check_model_ready(recommender)

    n_profile_books = recommender.get_user_profile_size(user_id)

    try:
        book_ids = recommender.recommend_for_user(user_id=user_id, k=k)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

    return UserRecommendationResponse(
        user_id=user_id,
        k=len(book_ids),
        n_profile_books=n_profile_books,
        book_ids=book_ids,
    )


# -----------------------------------------------------------------
# Helper
# -----------------------------------------------------------------

def _check_model_ready(recommender: HybridService) -> None:
    if not recommender.is_loaded:
        raise HTTPException(
            status_code=503,
            detail="Model not ready. Please try again later or POST /api/v1/model/reload.",
        )
