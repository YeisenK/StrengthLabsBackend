from fastapi import APIRouter
from pydantic import BaseModel

from domain.periodization_model import generate_plan

router = APIRouter(prefix="/compute/plan", tags=["plan"])


class PlanRequest(BaseModel):
    tsb: float


class PlanResponse(BaseModel):
    recommendation: str
    message: str


@router.post("", response_model=PlanResponse)
def compute_plan(payload: PlanRequest) -> PlanResponse:
    result = generate_plan(payload.tsb)
    return PlanResponse(**result)
