from datetime import date
from typing import List

from pydantic import BaseModel, Field


class TrainingSessionInput(BaseModel):
    user_id: int = Field(..., gt=0)
    date: date
    duration_minutes: int = Field(..., ge=0)
    rpe: float = Field(..., ge=0, le=10)


class FatigueRequest(BaseModel):
    sessions: List[TrainingSessionInput]


class FatigueResponse(BaseModel):
    atl: float
    ctl: float
    acwr: float
    tsb: float
