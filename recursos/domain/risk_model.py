from __future__ import annotations

from typing import Literal

ACWR_LOW       = 0.8
ACWR_OPTIMAL   = 1.0
ACWR_CAUTION   = 1.3
ACWR_HIGH      = 1.5
ACWR_CRITICAL  = 2.0

RAMP_SAFE      = 5.0
RAMP_CAUTION   = 8.0
RAMP_HIGH      = 12.0

TSB_FRESH      =  10.0
TSB_NEUTRAL    =   0.0
TSB_TIRED      = -10.0
TSB_FATIGUED   = -20.0
TSB_OVERTRAINED= -35.0

MONO_LOW       = 1.0
MONO_HIGH      = 2.0
MONO_CRITICAL  = 3.0

RiskLevel = Literal["low", "moderate", "high", "critical"]


def _acwr_risk(acwr: float) -> float:
    # Riesgo lineal por tramos; mínimo en zona óptima (0.8–1.3), máximo al superar 2.0
    if acwr <= 0.0:
        return 0.1
    elif acwr <= ACWR_LOW:
        return 0.1 + (1.0 - acwr / ACWR_LOW) * 0.05
    elif acwr <= ACWR_OPTIMAL:
        return _lerp(0.15, 0.05, (acwr - ACWR_LOW) / (ACWR_OPTIMAL - ACWR_LOW))
    elif acwr <= ACWR_CAUTION:
        return _lerp(0.05, 0.15, (acwr - ACWR_OPTIMAL) / (ACWR_CAUTION - ACWR_OPTIMAL))
    elif acwr <= ACWR_HIGH:
        return _lerp(0.15, 0.45, (acwr - ACWR_CAUTION) / (ACWR_HIGH - ACWR_CAUTION))
    elif acwr <= ACWR_CRITICAL:
        return _lerp(0.45, 0.85, (acwr - ACWR_HIGH) / (ACWR_CRITICAL - ACWR_HIGH))
    else:
        return min(1.0, 0.85 + (acwr - ACWR_CRITICAL) * 0.075)


def _ramp_risk(ramp_rate: float) -> float:
    if ramp_rate <= 0.0:
        return 0.0
    elif ramp_rate <= RAMP_SAFE:
        return _lerp(0.0, 0.1, ramp_rate / RAMP_SAFE)
    elif ramp_rate <= RAMP_CAUTION:
        return _lerp(0.1, 0.4, (ramp_rate - RAMP_SAFE) / (RAMP_CAUTION - RAMP_SAFE))
    elif ramp_rate <= RAMP_HIGH:
        return _lerp(0.4, 0.8, (ramp_rate - RAMP_CAUTION) / (RAMP_HIGH - RAMP_CAUTION))
    else:
        return min(1.0, 0.8 + (ramp_rate - RAMP_HIGH) * 0.02)


def injury_risk_score(acwr: float, ramp_rate: float) -> float:
    # ACWR pesa 0.65 por mayor evidencia prospectiva; ceiling evita que un componente crítico quede enmascarado
    acwr_r = _acwr_risk(acwr)
    ramp_r = _ramp_risk(ramp_rate)

    weighted = 0.65 * acwr_r + 0.35 * ramp_r
    ceiling  = max(acwr_r, ramp_r) * 0.9

    return round(min(1.0, max(weighted, ceiling)), 3)


def _tsb_overtraining_risk(tsb: float) -> float:
    if tsb >= TSB_FRESH:
        return 0.05
    elif tsb >= TSB_NEUTRAL:
        return _lerp(0.05, 0.15, (TSB_FRESH - tsb) / (TSB_FRESH - TSB_NEUTRAL))
    elif tsb >= TSB_TIRED:
        return _lerp(0.15, 0.30, (TSB_NEUTRAL - tsb) / (TSB_NEUTRAL - TSB_TIRED))
    elif tsb >= TSB_FATIGUED:
        return _lerp(0.30, 0.55, (TSB_TIRED - tsb) / (TSB_TIRED - TSB_FATIGUED))
    elif tsb >= TSB_OVERTRAINED:
        return _lerp(0.55, 0.85, (TSB_FATIGUED - tsb) / (TSB_FATIGUED - TSB_OVERTRAINED))
    else:
        return min(1.0, 0.85 + (TSB_OVERTRAINED - tsb) * 0.01)


def _monotony_overtraining_risk(monotony: float) -> float:
    if monotony <= MONO_LOW:
        return 0.0
    elif monotony <= MONO_HIGH:
        return _lerp(0.0, 0.4, (monotony - MONO_LOW) / (MONO_HIGH - MONO_LOW))
    elif monotony <= MONO_CRITICAL:
        return _lerp(0.4, 0.85, (monotony - MONO_HIGH) / (MONO_CRITICAL - MONO_HIGH))
    else:
        return min(1.0, 0.85 + (monotony - MONO_CRITICAL) * 0.075)


def overtraining_risk_score(tsb: float, monotony: float) -> float:
    # TSB pesa 0.70 como señal principal de fatiga acumulada; mismo ceiling que injury_risk
    tsb_r  = _tsb_overtraining_risk(tsb)
    mono_r = _monotony_overtraining_risk(monotony)

    weighted = 0.70 * tsb_r + 0.30 * mono_r
    ceiling  = max(tsb_r, mono_r) * 0.9

    return round(min(1.0, max(weighted, ceiling)), 3)


def _score_to_level(score: float) -> RiskLevel:
    if score >= 0.70:
        return "critical"
    elif score >= 0.45:
        return "high"
    elif score >= 0.20:
        return "moderate"
    else:
        return "low"


def calculate_risk(
    acwr: float,
    tsb: float,
    ramp_rate: float = 0.0,
    monotony: float = 0.0,
) -> dict:
    inj  = injury_risk_score(acwr, ramp_rate)
    over = overtraining_risk_score(tsb, monotony)

    # Peor dimensión domina; la menor aporta señal secundaria sin enmascarar la crítica
    composite = max(inj, over) * 0.75 + min(inj, over) * 0.25

    level = _score_to_level(composite)

    components = {
        "acwr":      _acwr_risk(acwr),
        "ramp_rate": _ramp_risk(ramp_rate),
        "tsb":       _tsb_overtraining_risk(tsb),
        "monotony":  _monotony_overtraining_risk(monotony),
    }
    dominant_factor = max(components, key=components.__getitem__)
    recommendations = _build_recommendations(acwr, tsb, ramp_rate, monotony, level)

    return {
        "injury_risk_score":       round(inj, 2),
        "overtraining_risk_score": round(over, 2),
        "composite_risk_score":    round(composite, 2),
        "risk_level":              level,
        "dominant_factor":         dominant_factor,
        "component_scores":        {k: round(v, 2) for k, v in components.items()},
        "recommendations":         recommendations,
    }


def _build_recommendations(
    acwr: float,
    tsb: float,
    ramp_rate: float,
    monotony: float,
    level: RiskLevel,
) -> list[str]:
    recs = []

    if acwr > ACWR_CRITICAL:
        recs.append("Reduce training load immediately — ACWR is severely elevated. Take 2–3 easy days before any quality work.")
    elif acwr > ACWR_HIGH:
        recs.append("Scale back intensity and/or volume this week. Target ACWR below 1.5 before resuming hard sessions.")
    elif acwr > ACWR_CAUTION:
        recs.append("Monitor closely. Avoid adding new load this week; maintain current level.")
    elif acwr < ACWR_LOW and acwr > 0:
        recs.append("Load is below the optimal range. A gradual increase (≤5 ATU/week) would support fitness development.")

    if ramp_rate > RAMP_HIGH:
        recs.append("Weekly load increase is too rapid. Connective tissue adapts slower than muscle — cap increases at 8 ATU/week.")
    elif ramp_rate > RAMP_CAUTION:
        recs.append("Ramp rate is elevated. Consider an easy week to allow structural adaptation to catch up.")

    if tsb < TSB_OVERTRAINED:
        recs.append("Accumulated fatigue is at a clinical concern level (TSB < −35). A full recovery week (or more) is warranted.")
    elif tsb < TSB_FATIGUED:
        recs.append("Significant fatigue accumulated. Prioritise sleep, nutrition, and reduce intensity before the next hard block.")
    elif tsb < TSB_TIRED:
        recs.append("Mild fatigue present. Normal within a training block; monitor for progression.")
    elif tsb > 20:
        recs.append("Athlete is very fresh. Good window for a peak performance effort or race.")

    if monotony > MONO_CRITICAL:
        recs.append("Training is highly repetitive. Introduce variation in session type, intensity, or duration to reduce illness risk.")
    elif monotony > MONO_HIGH:
        recs.append("Moderate monotony. Add at least one session this week that differs significantly in format or intensity.")

    if not recs:
        recs.append("Training load is well-managed. Continue current plan.")

    return recs


def _lerp(a: float, b: float, t: float) -> float:
    """Interpolación lineal: a + (b - a) × t,  t ∈ [0, 1]."""
    return a + (b - a) * max(0.0, min(1.0, t))


if __name__ == "__main__":
    scenarios = [
        ("Well managed",      {"acwr": 1.05, "tsb": -8,  "ramp_rate": 3.0,  "monotony": 1.1}),
        ("Pushing hard",      {"acwr": 1.35, "tsb": -18, "ramp_rate": 6.5,  "monotony": 1.6}),
        ("Overreaching",      {"acwr": 1.65, "tsb": -32, "ramp_rate": 11.0, "monotony": 2.4}),
        ("Tapering/fresh",    {"acwr": 0.75, "tsb":  18, "ramp_rate": -3.0, "monotony": 0.8}),
        ("Monotony trap",     {"acwr": 1.05, "tsb": -12, "ramp_rate": 2.0,  "monotony": 2.8}),
    ]

    for label, inputs in scenarios:
        result = calculate_risk(**inputs)
        print(f"\n{'─'*55}")
        print(f"  Scenario        : {label}")
        print(f"  Inputs          : ACWR={inputs['acwr']}, TSB={inputs['tsb']}, "
              f"Ramp={inputs['ramp_rate']}, Mono={inputs['monotony']}")
        print(f"  Injury risk     : {result['injury_risk_score']:.2f}")
        print(f"  Overtraining    : {result['overtraining_risk_score']:.2f}")
        print(f"  Composite       : {result['composite_risk_score']:.2f}  [{result['risk_level'].upper()}]")
        print(f"  Dominant factor : {result['dominant_factor']}")
        print(f"  Recommendations :")
        for r in result["recommendations"]:
            print(f"    • {r}")