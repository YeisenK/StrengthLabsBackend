from fastapi import APIRouter

from api.schemas.risk_schema import RiskRequest, RiskResponse
from domain.risk_model import calculate_risk

router = APIRouter(prefix="/compute/risk", tags=["risk"])


@router.post("", response_model=RiskResponse)
def compute_risk(payload: RiskRequest) -> RiskResponse: 
    result = calculate_risk(payload.acwr, payload.tsb)
    return RiskResponse(**result)
