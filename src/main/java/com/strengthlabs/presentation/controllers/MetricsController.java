package com.strengthlabs.presentation.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetricsController {

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return Map.of(
            "atl", 74.2,
            "ctl", 68.9,
            "acwr", 1.08,
            "tsb", -5.3,
            "riskZone", "OPTIMAL",
            "recentSessions", List.of(
                Map.of(
                    "id", "1",
                    "title", "Upper Body Strength",
                    "date", "2026-03-16T18:30:00",
                    "durationMinutes", 75,
                    "rpe", 8.0,
                    "load", 600.0
                ),
                Map.of(
                    "id", "2",
                    "title", "Lower Body Power",
                    "date", "2026-03-15T17:00:00",
                    "durationMinutes", 60,
                    "rpe", 7.5,
                    "load", 450.0
                )
            ),
            "history", List.of(
                Map.of("date", "2026-03-10", "atl", 63.4, "ctl", 61.1, "acwr", 1.03, "tsb", -2.3),
                Map.of("date", "2026-03-11", "atl", 65.0, "ctl", 61.8, "acwr", 1.05, "tsb", -3.2),
                Map.of("date", "2026-03-12", "atl", 67.8, "ctl", 62.9, "acwr", 1.08, "tsb", -4.9),
                Map.of("date", "2026-03-13", "atl", 69.5, "ctl", 64.0, "acwr", 1.09, "tsb", -5.5),
                Map.of("date", "2026-03-14", "atl", 71.0, "ctl", 65.2, "acwr", 1.09, "tsb", -5.8),
                Map.of("date", "2026-03-15", "atl", 72.8, "ctl", 67.0, "acwr", 1.09, "tsb", -5.8),
                Map.of("date", "2026-03-16", "atl", 74.2, "ctl", 68.9, "acwr", 1.08, "tsb", -5.3)
            )
        );
    }
}
