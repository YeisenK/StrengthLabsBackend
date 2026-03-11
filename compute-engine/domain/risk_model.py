def calculate_risk(acwr: float, tsb: float) -> dict:
    if acwr > 1.5 or tsb < -30:
        level = "high"
        score = 0.85
    elif acwr > 1.3 or tsb < -10:
        level = "moderate"
        score = 0.55
    else:
        level = "low"
        score = 0.2

    return {
        "risk_score": round(score, 2),
        "risk_level": level,
    }
