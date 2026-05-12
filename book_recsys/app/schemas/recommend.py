from pydantic import BaseModel, Field
from typing import List, Optional


# -----------------------------------------------------------------
# Item-based recommendation (legacy, currently disabled)
# -----------------------------------------------------------------

class SimilarBooksRequest(BaseModel):
    k: int = Field(default=10, ge=1, le=50, description="Number of recommendations (1-50)")


class SimilarBooksResponse(BaseModel):
    query_book_id: str
    k: int
    results: List[str]


# -----------------------------------------------------------------
# User-based recommendation
# -----------------------------------------------------------------

class UserRecommendationResponse(BaseModel):
    user_id: str
    k: int
    n_profile_books: int = Field(
        description="Number of user's read books used to build the profile vector"
    )
    book_ids: List[str] = Field(description="Ranked list of recommended book IDs")


# -----------------------------------------------------------------
# Generic error
# -----------------------------------------------------------------

class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None
