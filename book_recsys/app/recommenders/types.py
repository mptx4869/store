from __future__ import annotations

from dataclasses import dataclass, field, asdict
from typing import Any

import numpy as np
import numpy.typing as npt

@dataclass(slots=True, frozen=True)
class RecommendRequest:
    user_id: str
    seen_item_ids: set[str] = field(default_factory=set)
    user_vector: npt.NDArray[np.float32] | None = None
    user_history: dict[str, float] | None = None


@dataclass(slots=True, frozen=True)
class RecommendItem:
    item_id: str
    score: float
    rank: int

    def to_api_dict(self) -> dict[str, Any]:
        return {
            "item_id": self.item_id,
            "score": float(self.score), 
            "rank": int(self.rank)
        }