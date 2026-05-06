from domain.risk_model import calculate_risk


def test_calculate_risk_high():
    result = calculate_risk(acwr=1.6, tsb=-35)

    assert result["risk_level"] == "high"
    assert result["composite_risk_score"] == 0.69
    assert result["injury_risk_score"] == 0.48
    assert result["overtraining_risk_score"] == 0.77
    assert result["dominant_factor"] == "tsb"
    assert len(result["recommendations"]) > 0


def test_calculate_risk_low():
    result = calculate_risk(acwr=1.0, tsb=5)

    assert result["risk_level"] == "low"
    assert result["composite_risk_score"] == 0.08
    assert result["injury_risk_score"] == 0.04
    assert result["overtraining_risk_score"] == 0.09
    assert "well-managed" in result["recommendations"][0]
