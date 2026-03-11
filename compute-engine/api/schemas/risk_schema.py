from pydantic import BaseModel, Field


class RiskRequest(BaseModel):
    acwr: float = Field(..., ge=0)
    tsb: float


class RiskResponse(BaseModel):
    risk_score: float
    risk_level: str
