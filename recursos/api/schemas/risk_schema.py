from typing import List

from pydantic import BaseModel, Field


class RiskRequest(BaseModel):
    acwr: float = Field(..., ge=0)
    tsb: float
    ramp_rate: float = Field(default=0.0)
    monotony: float = Field(default=0.0)


class RiskResponse(BaseModel):
    injury_risk_score: float
    overtraining_risk_score: float
    composite_risk_score: float
    risk_level: str
    dominant_factor: str
    recommendations: List[str]
