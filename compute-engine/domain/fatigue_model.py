from __future__ import annotations

import math
import statistics
from datetime import date, timedelta
from typing import Dict, List, Optional


TAU_ATL: float = 7.0
TAU_CTL: float = 42.0

# λ = 1 - e^(-1/τ)
LAMBDA_ATL: float = 1.0 - math.exp(-1.0 / TAU_ATL)
LAMBDA_CTL: float = 1.0 - math.exp(-1.0 / TAU_CTL)

ACWR_SWEET_SPOT = (0.8, 1.3)
ACWR_HIGH_RISK  = 1.5
MONOTONY_HIGH_RISK = 2.0
RAMP_RATE_CAUTION  = 5.0
RAMP_RATE_HIGH     = 8.0

TRIMP_BETA_MALE   = 1.92
TRIMP_BETA_FEMALE = 1.67


def session_trimp(
    duration_minutes: float,
    hr_avg: float,
    hr_rest: float,
    hr_max: float,
    sex: str = "male",
) -> float:
    # TRIMP = duración × HR_ratio × e^(β × HR_ratio),  HR_ratio = (HR_avg - HR_rest) / (HR_max - HR_rest)
    if hr_max <= hr_rest:
        raise ValueError("hr_max must be greater than hr_rest")

    hr_ratio = (hr_avg - hr_rest) / (hr_max - hr_rest)
    hr_ratio = max(0.0, min(1.0, hr_ratio))

    beta = TRIMP_BETA_MALE if sex.lower() == "male" else TRIMP_BETA_FEMALE
    return duration_minutes * hr_ratio * math.exp(beta * hr_ratio)


def session_rpe_load(duration_minutes: float, rpe: float) -> float:
    if not (0.0 <= rpe <= 10.0):
        raise ValueError("RPE must be between 0 and 10")
    return duration_minutes * rpe


def compute_session_load(session: dict) -> float:
    dm  = float(session["duration_minutes"])
    rpe = float(session["rpe"])

    hr_avg  = session.get("hr_avg")
    hr_rest = session.get("hr_rest")
    hr_max  = session.get("hr_max")

    if hr_avg is not None and hr_rest is not None and hr_max is not None:
        sex = session.get("sex", "male")
        return session_trimp(dm, float(hr_avg), float(hr_rest), float(hr_max), sex)

    return session_rpe_load(dm, rpe)


def build_daily_loads(sessions: List[dict]) -> Dict[date, float]:
    daily: Dict[date, float] = {}
    for s in sessions:
        d = s["date"]
        daily[d] = daily.get(d, 0.0) + compute_session_load(s)
    return daily


def build_ewma_series(
    daily_loads: Dict[date, float],
    start: date,
    end: date,
    lam: float,
    seed: float = 0.0,
) -> Dict[date, float]:
    # S_t = S_{t-1} + λ × (carga_t - S_{t-1})
    series: Dict[date, float] = {}
    ewma = seed
    current = start
    while current <= end:
        load = daily_loads.get(current, 0.0)
        ewma = ewma + lam * (load - ewma)
        series[current] = ewma
        current += timedelta(days=1)
    return series


def calculate_fatigue_metrics(
    sessions: List[dict],
    today: Optional[date] = None,
    seed_atl: float = 0.0,
    seed_ctl: float = 0.0,
    history_days: int = 90,
) -> dict:
    if today is None:
        today = date.today()

    start = today - timedelta(days=history_days)

    daily_loads = build_daily_loads(sessions)
    atl_series  = build_ewma_series(daily_loads, start, today, LAMBDA_ATL, seed_atl)
    ctl_series  = build_ewma_series(daily_loads, start, today, LAMBDA_CTL, seed_ctl)

    atl  = atl_series[today]
    ctl  = ctl_series[today]
    tsb  = ctl - atl
    acwr = 0.0 if ctl == 0 else atl / ctl

    last_7_loads = [daily_loads.get(today - timedelta(days=i), 0.0) for i in range(7)]
    mean_7 = statistics.mean(last_7_loads)
    try:
        sd_7 = statistics.stdev(last_7_loads)
    except statistics.StatisticsError:
        sd_7 = 0.0

    monotony = (mean_7 / sd_7) if sd_7 > 0 else 0.0
    strain   = sum(last_7_loads) * monotony

    ctl_7_days_ago = ctl_series.get(today - timedelta(days=7), 0.0)
    ramp_rate = ctl - ctl_7_days_ago

    risk_flags: List[str] = []

    if acwr > ACWR_HIGH_RISK:
        risk_flags.append(f"HIGH INJURY RISK: ACWR {acwr:.2f} > {ACWR_HIGH_RISK}")
    elif acwr > ACWR_SWEET_SPOT[1]:
        risk_flags.append(f"CAUTION: ACWR {acwr:.2f} above sweet spot (0.8–1.3)")
    elif 0 < acwr < ACWR_SWEET_SPOT[0]:
        risk_flags.append(f"UNDERTRAINING: ACWR {acwr:.2f} below sweet spot (0.8–1.3)")

    if monotony > MONOTONY_HIGH_RISK:
        risk_flags.append(f"HIGH MONOTONY: {monotony:.2f} > {MONOTONY_HIGH_RISK} (vary training)")

    if ramp_rate > RAMP_RATE_HIGH:
        risk_flags.append(f"HIGH RAMP RATE: +{ramp_rate:.1f} CTL units/week (limit: {RAMP_RATE_HIGH})")
    elif ramp_rate > RAMP_RATE_CAUTION:
        risk_flags.append(f"ELEVATED RAMP RATE: +{ramp_rate:.1f} CTL units/week")

    if tsb < -30:
        risk_flags.append(f"DEEP FATIGUE: TSB {tsb:.1f} (significant accumulated fatigue)")

    readiness_score = _compute_readiness(acwr, tsb, monotony, ramp_rate)

    return {
        "atl":             round(atl, 2),
        "ctl":             round(ctl, 2),
        "tsb":             round(tsb, 2),
        "acwr":            round(acwr, 2),
        "monotony":        round(monotony, 2),
        "strain":          round(strain, 2),
        "ramp_rate":       round(ramp_rate, 2),
        "readiness_score": round(readiness_score, 1),
        "risk_flags":      risk_flags,
        "daily_loads":     {str(k): round(v, 2) for k, v in daily_loads.items()},
        "atl_series":      {str(k): round(v, 2) for k, v in atl_series.items()},
        "ctl_series":      {str(k): round(v, 2) for k, v in ctl_series.items()},
    }


def _compute_readiness(acwr: float, tsb: float, monotony: float, ramp_rate: float) -> float:
    # Pesos: ACWR 40%, TSB 35%, monotony 15%, ramp 10%
    if acwr == 0.0:
        acwr_score = 50.0
    elif acwr <= 0.8:
        acwr_score = (acwr / 0.8) * 70.0
    elif acwr <= 1.0:
        acwr_score = 70.0 + ((acwr - 0.8) / 0.2) * 30.0
    elif acwr <= 1.3:
        acwr_score = 100.0 - ((acwr - 1.0) / 0.3) * 20.0
    elif acwr <= 1.5:
        acwr_score = 80.0 - ((acwr - 1.3) / 0.2) * 50.0
    else:
        acwr_score = max(0.0, 30.0 - (acwr - 1.5) * 60.0)

    if tsb >= 20:
        tsb_score = 100.0
    elif tsb >= 0:
        tsb_score = 80.0 + (tsb / 20.0) * 20.0
    elif tsb >= -20:
        tsb_score = 80.0 + (tsb / 20.0) * 30.0
    elif tsb >= -40:
        tsb_score = 50.0 + ((tsb + 20.0) / 20.0) * 30.0
    else:
        tsb_score = max(0.0, 20.0 + (tsb + 40.0) * 1.0)

    mono_score = max(0.0, 100.0 - max(0.0, monotony - 1.0) * 40.0)

    if ramp_rate <= RAMP_RATE_CAUTION:
        ramp_score = 100.0
    elif ramp_rate <= RAMP_RATE_HIGH:
        ramp_score = 100.0 - ((ramp_rate - RAMP_RATE_CAUTION) / (RAMP_RATE_HIGH - RAMP_RATE_CAUTION)) * 40.0
    else:
        ramp_score = max(0.0, 60.0 - (ramp_rate - RAMP_RATE_HIGH) * 10.0)

    composite = (
        0.40 * acwr_score
        + 0.35 * tsb_score
        + 0.15 * mono_score
        + 0.10 * ramp_score
    )
    return max(0.0, min(100.0, composite))


def weekly_summary(sessions: List[dict], today: Optional[date] = None) -> str:
    if today is None:
        today = date.today()

    m = calculate_fatigue_metrics(sessions, today)

    lines = [
        f"=== Training Load Summary  ({today}) ===",
        f"  ATL  (acute,  7d EWMA) : {m['atl']:>8.1f}",
        f"  CTL  (chronic,42d EWMA): {m['ctl']:>8.1f}",
        f"  TSB  (form/freshness)  : {m['tsb']:>+8.1f}",
        f"  ACWR                   : {m['acwr']:>8.2f}",
        f"  Monotony               : {m['monotony']:>8.2f}",
        f"  Strain                 : {m['strain']:>8.1f}",
        f"  Ramp Rate (CTL/wk)     : {m['ramp_rate']:>+8.1f}",
        f"  Readiness Score        : {m['readiness_score']:>8.1f} / 100",
    ]

    if m["risk_flags"]:
        lines.append("\n  ⚠  Risk Flags:")
        for flag in m["risk_flags"]:
            lines.append(f"     • {flag}")
    else:
        lines.append("\n  ✓  No risk flags.")

    return "\n".join(lines)


if __name__ == "__main__":
    from datetime import date, timedelta

    today = date.today()

    sessions = [
        {"date": today - timedelta(days=6),  "duration_minutes": 75, "rpe": 7, "hr_avg": 155, "hr_rest": 52, "hr_max": 188, "sex": "male"},
        {"date": today - timedelta(days=5),  "duration_minutes": 60, "rpe": 6, "hr_avg": 148, "hr_rest": 52, "hr_max": 188, "sex": "male"},
        {"date": today - timedelta(days=3),  "duration_minutes": 90, "rpe": 8, "hr_avg": 162, "hr_rest": 52, "hr_max": 188, "sex": "male"},
        {"date": today - timedelta(days=1),  "duration_minutes": 45, "rpe": 5, "hr_avg": 140, "hr_rest": 52, "hr_max": 188, "sex": "male"},
        {"date": today - timedelta(days=10), "duration_minutes": 60, "rpe": 6},
        {"date": today - timedelta(days=12), "duration_minutes": 50, "rpe": 5},
        {"date": today - timedelta(days=14), "duration_minutes": 80, "rpe": 7},
    ]

    print(weekly_summary(sessions, today))