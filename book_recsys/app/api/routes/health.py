from fastapi import APIRouter

router = APIRouter()


@router.get("/", summary="Health check")
def get_health():
    """Return service status. Used by load balancers and uptime monitors."""
    return {"status": "healthy", "service": "Book Recommendation API", "version": "2.0.0"}
