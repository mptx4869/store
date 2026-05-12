from pydantic import BaseModel
from typing import List

class RecommendationRequest(BaseModel):
    limit: int = 10

class RecommendationResponseItem(BaseModel):
    db_book_id: str
    similarity_score: float
    rs_score: float
    algorithm: str

class RecommendationResponse(BaseModel):
    user_id: str | None = None
    item_id: str | None = None
    recommendations: List[RecommendationResponseItem]
