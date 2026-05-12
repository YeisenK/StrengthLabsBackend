package com.strengthlabs.application.usecases;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strengthlabs.application.dtos.ComputeSessionDTO;
import com.strengthlabs.application.dtos.FatigueComputeResult;
import com.strengthlabs.application.dtos.RiskComputeResult;
import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.infrastructure.persistence.jpa.TrainingMetricsJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.TrainingMetricsJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutSetJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GetFatigueSummaryUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetFatigueSummaryUseCase.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> ALL_MUSCLE_GROUPS = List.of(
            "chest", "back", "shoulders", "biceps", "triceps",
            "legs", "core", "glutes", "calves", "forearms", "cardio", "other"
    );
    private static final double MAX_SETS_PER_WEEK = 20.0;

    private final WorkoutJpaRepository workoutRepo;
    private final ComputeEnginePort computeEngine;
    private final TrainingMetricsJpaRepository metricsRepo;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public GetFatigueSummaryUseCase(WorkoutJpaRepository workoutRepo,
                                     ComputeEnginePort computeEngine,
                                     TrainingMetricsJpaRepository metricsRepo,
                                     ObjectMapper objectMapper,
                                     MessageSource messageSource) {
        this.workoutRepo = workoutRepo;
        this.computeEngine = computeEngine;
        this.metricsRepo = metricsRepo;
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    public Map<String, Object> execute(UUID userId, Locale locale) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Optional<TrainingMetricsJpaEntity> cached = metricsRepo.findByUserIdAndMetricDate(userId, today);
        if (cached.isPresent()) {
            return fromCache(cached.get(), locale);
        }

        Instant since90 = today.minusDays(90).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant since7 = today.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<WorkoutJpaEntity> workouts = workoutRepo.findByUserIdAndDateAfterOrderByDateDesc(userId, since90);

        FatigueComputeResult fatigue = computeEngine.computeFatigue(buildSessions(workouts, userId));
        RiskComputeResult risk = computeEngine.computeRisk(
                fatigue.acwr(), fatigue.tsb(), fatigue.rampRate(), fatigue.monotony());

        List<WorkoutJpaEntity> recentWorkouts = workouts.stream()
                .filter(w -> !w.getDate().isBefore(since7))
                .toList();

        Map<String, Double> weeklyVolume = computeWeeklyVolume(recentWorkouts);
        List<Map<String, Object>> trend = buildTrend(fatigue.atlSeries(), today);

        Map<String, Object> response = buildResponse(fatigue, risk, weeklyVolume, trend, locale);

        persistMetrics(userId, today, fatigue, risk, weeklyVolume, trend);

        return response;
    }

    public Map<String, Double> computeWeeklyVolume(List<WorkoutJpaEntity> workouts) {
        Map<String, Double> setCounts = new LinkedHashMap<>();
        ALL_MUSCLE_GROUPS.forEach(mg -> setCounts.put(mg, 0.0));
        for (WorkoutJpaEntity w : workouts) {
            for (var we : w.getExercises()) {
                String mg = we.getExercise().getMuscleGroup().toLowerCase();
                setCounts.merge(mg, (double) we.getSets().size(), Double::sum);
            }
        }
        Map<String, Double> result = new LinkedHashMap<>();
        setCounts.forEach((mg, count) ->
                result.put(mg, Math.min(100.0, count / MAX_SETS_PER_WEEK * 100.0)));
        return result;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<ComputeSessionDTO> buildSessions(List<WorkoutJpaEntity> workouts, UUID userId) {
        return workouts.stream().map(w -> {
            List<WorkoutSetJpaEntity> allSets = w.getExercises().stream()
                    .flatMap(we -> we.getSets().stream())
                    .toList();
            double avgRpe = allSets.stream()
                    .mapToDouble(s -> s.getRpe() != null ? s.getRpe() : 7.0)
                    .average()
                    .orElse(7.0);
            int durationMinutes = Math.max(1, w.getDurationSeconds() / 60);
            String date = DATE_FMT.format(w.getDate().atZone(ZoneOffset.UTC).toLocalDate());
            return new ComputeSessionDTO(userId.toString(), date, durationMinutes, avgRpe);
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTrend(Map<String, Double> atlSeries, LocalDate today) {
        double maxAtl = atlSeries.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxAtl == 0) maxAtl = 1.0;
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = DATE_FMT.format(day);
            double atl = atlSeries.getOrDefault(key, 0.0);
            double index = Math.min(100.0, atl / maxAtl * 100.0);
            trend.add(Map.of("date", key, "index", Math.round(index * 10.0) / 10.0));
        }
        return trend;
    }

    private Map<String, Object> buildResponse(FatigueComputeResult fatigue, RiskComputeResult risk,
                                               Map<String, Double> weeklyVolume,
                                               List<Map<String, Object>> trend,
                                               Locale locale) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overall_index", fatigue.readinessScore());
        response.put("is_overtraining", "critical".equals(risk.riskLevel()));
        response.put("weekly_volume", weeklyVolume);
        response.put("trend", trend);
        response.put("atl", fatigue.atl());
        response.put("ctl", fatigue.ctl());
        response.put("tsb", fatigue.tsb());
        response.put("acwr", fatigue.acwr());
        response.put("monotony", fatigue.monotony());
        response.put("strain", fatigue.strain());
        response.put("ramp_rate", fatigue.rampRate());
        response.put("readiness_score", fatigue.readinessScore());
        response.put("risk_flags", translateCodes(fatigue.riskFlags(), "compute.risk", locale));
        response.put("injury_risk_score", risk.injuryRiskScore());
        response.put("overtraining_risk_score", risk.overtrainingRiskScore());
        response.put("composite_risk_score", risk.compositeRiskScore());
        response.put("risk_level", risk.riskLevel());
        response.put("dominant_factor", risk.dominantFactor());
        response.put("recommendations", translateCodes(risk.recommendations(), "compute.rec", locale));
        response.put("compute_available", fatigue.computeAvailable() && risk.computeAvailable());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromCache(TrainingMetricsJpaEntity e, Locale locale) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overall_index", e.getReadinessScore());
        response.put("is_overtraining", "critical".equals(e.getRiskLevel()));
        response.put("weekly_volume", readJson(e.getWeeklyVolumeJson(), Map.class));
        response.put("trend", readJson(e.getTrendJson(), List.class));
        response.put("atl", e.getAtl());
        response.put("ctl", e.getCtl());
        response.put("tsb", e.getTsb());
        response.put("acwr", e.getAcwr());
        response.put("monotony", e.getMonotony());
        response.put("strain", e.getStrain());
        response.put("ramp_rate", e.getRampRate());
        response.put("readiness_score", e.getReadinessScore());
        response.put("risk_flags", translateCodes(readRawList(e.getRiskFlagsJson()), "compute.risk", locale));
        response.put("injury_risk_score", e.getInjuryRiskScore());
        response.put("overtraining_risk_score", e.getOvertrainingRiskScore());
        response.put("composite_risk_score", e.getCompositeRiskScore());
        response.put("risk_level", e.getRiskLevel());
        response.put("dominant_factor", e.getDominantFactor());
        response.put("recommendations", translateCodes(readRawList(e.getRecommendationsJson()), "compute.rec", locale));
        response.put("compute_available", e.isComputeAvailable());
        return response;
    }

    /**
     * Translates a list of code objects ({@code {"code":"KEY"}}) to localized strings.
     * Falls back to the raw value when an element is a plain string (old cache format).
     */
    private List<String> translateCodes(List<?> raw, String prefix, Locale locale) {
        List<String> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map<?, ?> m) {
                Object codeObj = m.get("code");
                String code = codeObj != null ? codeObj.toString() : "";
                result.add(messageSource.getMessage(prefix + "." + code, null, code, locale));
            } else if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }

    private void persistMetrics(UUID userId, LocalDate today,
                                 FatigueComputeResult fatigue, RiskComputeResult risk,
                                 Map<String, Double> weeklyVolume,
                                 List<Map<String, Object>> trend) {
        // Double-check before insert — when two requests for the same user
        // arrive on the same day, only one should win and the other should
        // silently no-op (its result is equivalent).
        if (metricsRepo.findByUserIdAndMetricDate(userId, today).isPresent()) {
            return;
        }
        try {
            TrainingMetricsJpaEntity entity = new TrainingMetricsJpaEntity(
                    UUID.randomUUID(), userId, today,
                    fatigue.atl(), fatigue.ctl(), fatigue.tsb(), fatigue.acwr(),
                    fatigue.monotony(), fatigue.strain(), fatigue.rampRate(), fatigue.readinessScore(),
                    risk.injuryRiskScore(), risk.overtrainingRiskScore(), risk.compositeRiskScore(),
                    risk.riskLevel(), risk.dominantFactor(),
                    toJson(fatigue.riskFlags()), toJson(risk.recommendations()),
                    toJson(weeklyVolume), toJson(trend),
                    fatigue.computeAvailable() && risk.computeAvailable()
            );
            metricsRepo.save(entity);
        } catch (DataIntegrityViolationException dup) {
            // Lost the race against another concurrent request — the cache
            // entry already exists. This is expected, not an error.
            log.debug("Training metrics already persisted for user {} on {}", userId, today);
        } catch (Exception e) {
            log.warn("Could not persist training metrics for user {}: {}", userId, e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "null";
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            if (List.class.isAssignableFrom(type)) return (T) List.of();
            if (Map.class.isAssignableFrom(type)) return (T) Map.of();
            return null;
        }
    }

    /** Reads JSON as a raw {@code List<?>} — each element may be a String or a Map. */
    private List<?> readRawList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<?>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
