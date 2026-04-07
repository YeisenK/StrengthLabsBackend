package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutSetJpaEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fatigue")
public class FatigueController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // All muscle groups the Flutter app knows about
    private static final List<String> ALL_MUSCLE_GROUPS = List.of(
            "chest", "back", "shoulders", "biceps", "triceps",
            "legs", "core", "glutes", "calves", "forearms", "cardio", "other"
    );

    // Sets per muscle group per week considered 100%
    private static final double MAX_SETS_PER_WEEK = 20.0;

    private final WorkoutJpaRepository workoutRepo;
    private final RestTemplate restTemplate;
    private final String computeUrl;

    public FatigueController(WorkoutJpaRepository workoutRepo,
                              RestTemplate restTemplate,
                              @Value("${compute-engine.url}") String computeUrl) {
        this.workoutRepo = workoutRepo;
        this.restTemplate = restTemplate;
        this.computeUrl = computeUrl;
    }

    // ── GET /fatigue/summary ───────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant since90 = today.minusDays(90).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<WorkoutJpaEntity> workouts = workoutRepo.findByUserIdAndDateAfterOrderByDateDesc(userId, since90);

        // Build session payload for compute engine
        List<Map<String, Object>> sessions = buildSessionPayload(workouts);

        // Call compute/fatigue
        Map<String, Object> fatigueResult = callComputeFatigue(sessions);
        // Call compute/risk with all available metrics
        Map<String, Object> riskResult = callComputeRisk(fatigueResult);

        // Weekly volume by muscle group (last 7 days)
        Instant since7 = today.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<WorkoutJpaEntity> recentWorkouts = workouts.stream()
                .filter(w -> !w.getDate().isBefore(since7))
                .toList();

        Map<String, Double> weeklyVolume = computeWeeklyVolume(recentWorkouts);

        // 7-day trend from ATL series
        @SuppressWarnings("unchecked")
        Map<String, Object> atlSeriesRaw = (Map<String, Object>) fatigueResult.getOrDefault("atl_series", Map.of());
        List<Map<String, Object>> trend = buildTrend(atlSeriesRaw, today);

        // Assemble full response
        double readinessScore = toDouble(fatigueResult.getOrDefault("readiness_score", 0.0));
        String riskLevel = (String) riskResult.getOrDefault("risk_level", "low");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overall_index", readinessScore);
        response.put("is_overtraining", "critical".equals(riskLevel));
        response.put("weekly_volume", weeklyVolume);
        response.put("trend", trend);
        // Extended metrics for the Plan and Fatigue tabs
        response.put("atl", fatigueResult.getOrDefault("atl", 0.0));
        response.put("ctl", fatigueResult.getOrDefault("ctl", 0.0));
        response.put("tsb", fatigueResult.getOrDefault("tsb", 0.0));
        response.put("acwr", fatigueResult.getOrDefault("acwr", 0.0));
        response.put("monotony", fatigueResult.getOrDefault("monotony", 0.0));
        response.put("strain", fatigueResult.getOrDefault("strain", 0.0));
        response.put("ramp_rate", fatigueResult.getOrDefault("ramp_rate", 0.0));
        response.put("readiness_score", readinessScore);
        response.put("risk_flags", fatigueResult.getOrDefault("risk_flags", List.of()));
        response.put("injury_risk_score", riskResult.getOrDefault("injury_risk_score", 0.0));
        response.put("overtraining_risk_score", riskResult.getOrDefault("overtraining_risk_score", 0.0));
        response.put("composite_risk_score", riskResult.getOrDefault("composite_risk_score", 0.0));
        response.put("risk_level", riskLevel);
        response.put("dominant_factor", riskResult.getOrDefault("dominant_factor", ""));
        response.put("recommendations", riskResult.getOrDefault("recommendations", List.of()));

        return ResponseEntity.ok(response);
    }

    // ── GET /fatigue/weekly ────────────────────────────────────────────────────

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeekly(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant since7 = today.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<WorkoutJpaEntity> recent = workoutRepo
                .findByUserIdAndDateAfterOrderByDateDesc(userId, since7);

        return ResponseEntity.ok(Map.of("weekly_volume", computeWeeklyVolume(recent)));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildSessionPayload(List<WorkoutJpaEntity> workouts) {
        return workouts.stream().map(w -> {
            List<WorkoutSetJpaEntity> allSets = w.getExercises().stream()
                    .flatMap(we -> we.getSets().stream())
                    .toList();
            double avgRpe = allSets.stream()
                    .mapToDouble(s -> s.getRpe() != null ? s.getRpe() : 7.0)
                    .average()
                    .orElse(7.0);
            int durationMinutes = Math.max(1, w.getDurationSeconds() / 60);

            Map<String, Object> session = new LinkedHashMap<>();
            session.put("user_id", 1);
            session.put("date", DATE_FMT.format(w.getDate().atZone(ZoneOffset.UTC).toLocalDate()));
            session.put("duration_minutes", durationMinutes);
            session.put("rpe", avgRpe);
            return session;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callComputeFatigue(List<Map<String, Object>> sessions) {
        try {
            Map<String, Object> payload = Map.of("sessions", sessions);
            Map<String, Object> result = restTemplate.postForObject(
                    computeUrl + "/compute/fatigue", payload, Map.class);
            return result != null ? result : Map.of();
        } catch (Exception e) {
            return Map.of("atl", 0.0, "ctl", 0.0, "tsb", 0.0, "acwr", 0.0,
                    "monotony", 0.0, "strain", 0.0, "ramp_rate", 0.0,
                    "readiness_score", 0.0, "risk_flags", List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callComputeRisk(Map<String, Object> fatigueResult) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("acwr", fatigueResult.getOrDefault("acwr", 0.0));
            payload.put("tsb", fatigueResult.getOrDefault("tsb", 0.0));
            payload.put("ramp_rate", fatigueResult.getOrDefault("ramp_rate", 0.0));
            payload.put("monotony", fatigueResult.getOrDefault("monotony", 0.0));
            Map<String, Object> result = restTemplate.postForObject(
                    computeUrl + "/compute/risk", payload, Map.class);
            return result != null ? result : Map.of();
        } catch (Exception e) {
            return Map.of("injury_risk_score", 0.0, "overtraining_risk_score", 0.0,
                    "composite_risk_score", 0.0, "risk_level", "low",
                    "dominant_factor", "", "recommendations", List.of());
        }
    }

    private Map<String, Double> computeWeeklyVolume(List<WorkoutJpaEntity> workouts) {
        Map<String, Double> setCounts = new LinkedHashMap<>();
        ALL_MUSCLE_GROUPS.forEach(mg -> setCounts.put(mg, 0.0));

        for (WorkoutJpaEntity w : workouts) {
            for (var we : w.getExercises()) {
                String mg = we.getExercise().getMuscleGroup().toLowerCase();
                setCounts.merge(mg, (double) we.getSets().size(), Double::sum);
            }
        }

        // Normalize to 0–100 %
        Map<String, Double> result = new LinkedHashMap<>();
        setCounts.forEach((mg, count) ->
                result.put(mg, Math.min(100.0, count / MAX_SETS_PER_WEEK * 100.0)));
        return result;
    }

    private List<Map<String, Object>> buildTrend(Map<String, Object> atlSeries, LocalDate today) {
        // Extract last 7 days of ATL and present as trend index (normalised 0-100)
        // ATL values are in training load units; we scale relative to max in the series
        double maxAtl = atlSeries.values().stream()
                .mapToDouble(v -> toDouble(v))
                .max()
                .orElse(1.0);
        if (maxAtl == 0) maxAtl = 1.0;

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = DATE_FMT.format(day);
            double atl = toDouble(atlSeries.getOrDefault(key, 0.0));
            double index = Math.min(100.0, atl / maxAtl * 100.0);
            trend.add(Map.of("date", key, "index", Math.round(index * 10.0) / 10.0));
        }
        return trend;
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
