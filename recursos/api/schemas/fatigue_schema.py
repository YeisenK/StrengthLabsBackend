from datetime import date
from typing import Any, Dict, List

from pydantic import BaseModel, Field


class TrainingSessionInput(BaseModel):
    user_id: str
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
    monotony: float
    strain: float
    ramp_rate: float
    readiness_score: float
    risk_flags: List[Dict[str, Any]]
    atl_series: Dict[str, float]
