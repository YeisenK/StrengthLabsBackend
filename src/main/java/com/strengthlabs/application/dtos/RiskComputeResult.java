package com.strengthlabs.application.dtos;

import java.util.List;
import java.util.Map;

public record RiskComputeResult(
        double injuryRiskScore,
        double overtrainingRiskScore,
        double compositeRiskScore,
        String riskLevel,
        String dominantFactor,
        List<Map<String, Object>> recommendations,
        boolean computeAvailable
) {
    public static RiskComputeResult unavailable() {
        return new RiskComputeResult(0, 0, 0, "low", "", List.of(), false);
    }
}
