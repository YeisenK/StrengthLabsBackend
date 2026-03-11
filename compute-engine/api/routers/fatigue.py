from fastapi import APIRouter

from api.schemas.fatigue_schema import FatigueRequest, FatigueResponse
from domain.fatigue_model import calculate_fatigue_metrics

router = APIRouter(prefix="/compute/fatigue", tags=["fatigue"])


@router.post("", response_model=FatigueResponse)
def compute_fatigue(payload: FatigueRequest) -> FatigueResponse:
    sessions = [
        {
            "user_id": session.user_id,
            "date": session.date,
            "duration_minutes": session.duration_minutes,
            "rpe": session.rpe,
        }
        for session in payload.sessions
    ]

    result = calculate_fatigue_metrics(sessions)
    return FatigueResponse(**result)
