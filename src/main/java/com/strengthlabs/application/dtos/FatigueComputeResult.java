package com.strengthlabs.application.dtos;

import java.util.List;
import java.util.Map;

public record FatigueComputeResult(
        double atl,
        double ctl,
        double tsb,
        double acwr,
        double monotony,
        double strain,
        double rampRate,
        double readinessScore,
        List<Map<String, Object>> riskFlags,
        Map<String, Double> atlSeries,
        boolean computeAvailable
) {
    public static FatigueComputeResult unavailable() {
        return new FatigueComputeResult(0, 0, 0, 0, 0, 0, 0, 0, List.of(), Map.of(), false);
    }
}
