from datetime import date

from domain.fatigue_model import calculate_fatigue_metrics


def test_calculate_fatigue_metrics():
    today = date(2026, 3, 10)
    sessions = [
        {"user_id": 1, "date": date(2026, 3, 10), "duration_minutes": 60, "rpe": 7},
        {"user_id": 1, "date": date(2026, 3, 9),  "duration_minutes": 50, "rpe": 8},
        {"user_id": 1, "date": date(2026, 3, 8),  "duration_minutes": 70, "rpe": 6},
        {"user_id": 1, "date": date(2026, 3, 6),  "duration_minutes": 80, "rpe": 7},
        {"user_id": 1, "date": date(2026, 3, 5),  "duration_minutes": 45, "rpe": 7},
        {"user_id": 1, "date": date(2026, 3, 4),  "duration_minutes": 60, "rpe": 8},
    ]

    result = calculate_fatigue_metrics(sessions, today=today)

    assert result["atl"] == 233.83
    assert result["ctl"] == 56.84
    assert result["tsb"] == -176.99
    assert result["acwr"] == 4.11
    assert result["monotony"] == 2.06
    assert result["readiness_score"] == 8.6
    assert "HIGH INJURY RISK" in result["risk_flags"][0]
    assert "atl_series" in result
    assert str(today) in result["atl_series"]


def test_calculate_fatigue_metrics_empty_sessions():
    today = date(2026, 3, 10)

    result = calculate_fatigue_metrics([], today=today)

    assert result["atl"] == 0.0
    assert result["ctl"] == 0.0
    assert result["acwr"] == 0.0
    assert result["tsb"] == 0.0
    assert result["risk_flags"] == []
    assert "atl_series" in result
