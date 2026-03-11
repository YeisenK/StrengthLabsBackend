from datetime import date, timedelta
from typing import Dict, List


def build_daily_loads(sessions: List[dict]) -> Dict[date, float]:
    daily_loads: Dict[date, float] = {}

    for session in sessions:
        session_date = session["date"]
        duration_minutes = session["duration_minutes"]
        rpe = session["rpe"]

        load = duration_minutes * rpe
        daily_loads[session_date] = daily_loads.get(session_date, 0.0) + load

    return daily_loads


def average_last_n_days(daily_loads: Dict[date, float], today: date, days: int) -> float:
    total = 0.0

    for i in range(days):
        target_date = today - timedelta(days=i)
        total += daily_loads.get(target_date, 0.0)

    return total / days


def calculate_fatigue_metrics(sessions: List[dict], today: date | None = None) -> dict:
    if today is None:
        today = date.today()

    daily_loads = build_daily_loads(sessions)

    atl = average_last_n_days(daily_loads, today, 7)
    ctl = average_last_n_days(daily_loads, today, 28)
    acwr = 0.0 if ctl == 0 else atl / ctl
    tsb = ctl - atl

    return {
        "atl": round(atl, 2),
        "ctl": round(ctl, 2),
        "acwr": round(acwr, 2),
        "tsb": round(tsb, 2),
    }
