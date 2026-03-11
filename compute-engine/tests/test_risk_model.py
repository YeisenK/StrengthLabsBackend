from domain.risk_model import calculate_risk


def test_calculate_risk_high():
    result = calculate_risk(1.6, -35)

    assert result["risk_level"] == "high"
    assert result["risk_score"] == 0.85


def test_calculate_risk_low():
    result = calculate_risk(1.0, 5)

    assert result["risk_level"] == "low"
    assert result["risk_score"] == 0.2
