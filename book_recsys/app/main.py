from fastapi import FastAPI
from contextlib import asynccontextmanager
import asyncio
from app.api.routes import health, recommend, model
from app.core.dependencies import get_hybrid_service

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Eager-load
    print("[Startup] Pre-loading Hybrid Service...")
    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, get_hybrid_service)
    print("[Startup] Hybrid Service ready.")
    
    yield

app = FastAPI(
    title="Book Recommendation API",
    description=(
        "Hybrid book recommendation system for user-based personalized recommendations. "
    ),
    version="2.0.0",
    lifespan=lifespan,
)

app.include_router(health.router,    prefix="/health",              tags=["System"])
app.include_router(recommend.router, prefix="/api/v1/recommend",    tags=["Recommendations"])
app.include_router(model.router,     prefix="/api/v1/model",        tags=["Model Management"])


@app.get("/")
def root():
    return {
        "message": "Book Recommendation API v2. Visit /docs for Swagger UI.",
        "endpoints": {
            "item_similar":      "GET /api/v1/recommend/item/{book_id}/similar",
            "user_recommend":    "GET /api/v1/recommend/user/{user_id}",
            "model_status":      "GET /api/v1/model/status",
            "model_reload":      "POST /api/v1/model/reload",
        }
    }
