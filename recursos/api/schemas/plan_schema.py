from typing import List, Optional

from pydantic import BaseModel, Field


class PlanRequest(BaseModel):
    ctl: float = Field(..., ge=0)
    atl: float = Field(..., ge=0)
    tsb: float
    acwr: float = Field(..., ge=0)
    monotony: float = Field(..., ge=0)
    ramp_rate: float
    readiness_score: float = Field(..., ge=0, le=100)
    composite_risk: float = Field(..., ge=0, le=1)
    injury_risk_score: float = Field(..., ge=0, le=1)
    overtraining_risk_score: float = Field(..., ge=0, le=1)
    days_to_event: Optional[int] = None
    weeks_in_phase: int = 0


class SessionDay(BaseModel):
    day: int
    day_name: str
    session_type: str
    intensity_zone: str
    duration_minutes: int
    rpe_target: float
    description: str
    is_rest: bool
    key_session: bool


class PlanResponse(BaseModel):
    phase: str
    microcycle_type: str
    week_objective: str
    target_weekly_load: float
    target_acwr: float
    projected_tsb_delta: float
    load_adjustment_pct: float
    sessions: List[SessionDay]
    coach_notes: List[str]
    periodization_rationale: str
