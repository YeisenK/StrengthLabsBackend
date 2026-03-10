package com.strengthlabs.fatigue.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MotorFatiga {

    public FatigueMetrics calculate(List<TrainingSession> sessions, LocalDate today) {
        Map<LocalDate, Double> dailyLoads = buildDailyLoads(sessions);

        double atl = averageLastNDays(dailyLoads, today, 7);
        double ctl = averageLastNDays(dailyLoads, today, 28);
        double acwr = ctl == 0 ? 0.0 : atl / ctl;
        double tsb = ctl - atl;

        return new FatigueMetrics(
                round(atl),
                round(ctl),
                round(acwr),
                round(tsb)
        );
    }

    private Map<LocalDate, Double> buildDailyLoads(List<TrainingSession> sessions) {
        Map<LocalDate, Double> dailyLoads = new HashMap<>();

        for (TrainingSession session : sessions) {
            dailyLoads.merge(session.getDate(), session.getLoad(), Double::sum);
        }

        return dailyLoads;
    }

    private double averageLastNDays(Map<LocalDate, Double> dailyLoads, LocalDate today, int days) {
        double total = 0.0;

        for (int i = 0; i < days; i++) {
            LocalDate targetDate = today.minusDays(i);
            total += dailyLoads.getOrDefault(targetDate, 0.0);
        }

        return total / days;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
