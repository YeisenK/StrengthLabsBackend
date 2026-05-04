from fastapi import APIRouter

from api.schemas.plan_schema import PlanRequest, PlanResponse
from domain.periodization_model import generate_plan

router = APIRouter(prefix="/compute/plan", tags=["plan"])


@router.post("", response_model=PlanResponse)
def compute_plan(payload: PlanRequest) -> PlanResponse:
    result = generate_plan(
        ctl=payload.ctl,
        atl=payload.atl,
        tsb=payload.tsb,
        acwr=payload.acwr,
        monotony=payload.monotony,
        ramp_rate=payload.ramp_rate,
        readiness_score=payload.readiness_score,
        composite_risk=payload.composite_risk,
        injury_risk_score=payload.injury_risk_score,
        overtraining_risk_score=payload.overtraining_risk_score,
        days_to_event=payload.days_to_event,
        weeks_in_phase=payload.weeks_in_phase,
    )
    return PlanResponse(**result)
