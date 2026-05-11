package com.strengthlabs.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strengthlabs.application.dtos.ComputeSessionDTO;
import com.strengthlabs.application.dtos.FatigueComputeResult;
import com.strengthlabs.application.dtos.RiskComputeResult;
import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.infrastructure.persistence.jpa.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetFatigueSummaryUseCase")
class GetFatigueSummaryUseCaseTest {

    @Mock
    private WorkoutJpaRepository workoutRepo;

    @Mock
    private ComputeEnginePort computeEngine;

    @Mock
    private TrainingMetricsJpaRepository metricsRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GetFatigueSummaryUseCase useCase;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        useCase = new GetFatigueSummaryUseCase(workoutRepo, computeEngine, metricsRepo, objectMapper);
    }

    // ── Cache behavior ────────────────────────────────────────────────────────

    @Test
    @DisplayName("cache hit: returns cached metrics without calling compute engine")
    void cacheHit_doesNotCallComputeEngine() {
        TrainingMetricsJpaEntity cached = sampleCached();
        when(metricsRepo.findByUserIdAndMetricDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.of(cached));

        Map<String, Object> response = useCase.execute(userId);

        assertEquals(72.5, response.get("readiness_score"));
        assertEquals("low", response.get("risk_level"));
        verifyNoInteractions(computeEngine);
        verify(workoutRepo, never()).findByUserIdAndDateAfterOrderByDateDesc(any(), any());
    }

    @Test
    @DisplayName("cache miss: invokes compute engine and persists snapshot")
    void cacheMiss_invokesComputeEngineAndPersists() {
        when(metricsRepo.findByUserIdAndMetricDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(workoutRepo.findByUserIdAndDateAfterOrderByDateDesc(eq(userId), any(Instant.class)))
                .thenReturn(List.of());
        when(computeEngine.computeFatigue(anyList())).thenReturn(sampleFatigue());
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(sampleRisk());

        Map<String, Object> response = useCase.execute(userId);

        ArgumentCaptor<TrainingMetricsJpaEntity> captor = ArgumentCaptor.forClass(TrainingMetricsJpaEntity.class);
        verify(metricsRepo).save(captor.capture());

        TrainingMetricsJpaEntity saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(80.0, saved.getReadinessScore());
        assertEquals("low", saved.getRiskLevel());
        assertThat(saved.isComputeAvailable()).isTrue();
        assertEquals(80.0, response.get("readiness_score"));
    }

    @Test
    @DisplayName("compute engine unavailable: response flagged compute_available=false")
    void computeEngineDown_flagsResponseAndDoesNotPersistTrustedCache() {
        when(metricsRepo.findByUserIdAndMetricDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(workoutRepo.findByUserIdAndDateAfterOrderByDateDesc(eq(userId), any(Instant.class)))
                .thenReturn(List.of());
        when(computeEngine.computeFatigue(anyList())).thenReturn(FatigueComputeResult.unavailable());
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(RiskComputeResult.unavailable());

        Map<String, Object> response = useCase.execute(userId);

        assertEquals(false, response.get("compute_available"));
        ArgumentCaptor<TrainingMetricsJpaEntity> captor = ArgumentCaptor.forClass(TrainingMetricsJpaEntity.class);
        verify(metricsRepo).save(captor.capture());
        assertThat(captor.getValue().isComputeAvailable()).isFalse();
    }

    // ── Response shape ────────────────────────────────────────────────────────

    @Test
    @DisplayName("response contains all 19 documented fields")
    void responseContainsAllExpectedFields() {
        when(metricsRepo.findByUserIdAndMetricDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(workoutRepo.findByUserIdAndDateAfterOrderByDateDesc(eq(userId), any(Instant.class)))
                .thenReturn(List.of());
        when(computeEngine.computeFatigue(anyList())).thenReturn(sampleFatigue());
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(sampleRisk());

        Map<String, Object> response = useCase.execute(userId);

        assertThat(response).containsKeys(
                "overall_index", "is_overtraining", "weekly_volume", "trend",
                "atl", "ctl", "tsb", "acwr", "monotony", "strain", "ramp_rate",
                "readiness_score", "risk_flags",
                "injury_risk_score", "overtraining_risk_score", "composite_risk_score",
                "risk_level", "dominant_factor", "recommendations", "compute_available"
        );
    }

    @Test
    @DisplayName("is_overtraining is true when risk_level is critical")
    void isOvertrainingTrueWhenCritical() {
        when(metricsRepo.findByUserIdAndMetricDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(workoutRepo.findByUserIdAndDateAfterOrderByDateDesc(eq(userId), any(Instant.class)))
                .thenReturn(List.of());
        when(computeEngine.computeFatigue(anyList())).thenReturn(sampleFatigue());
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RiskComputeResult(80, 90, 85, "critical", "monotony",
                        List.of("Reduce volume"), true));

        Map<String, Object> response = useCase.execute(userId);

        assertEquals(true, response.get("is_overtraining"));
    }

    // ── computeWeeklyVolume ───────────────────────────────────────────────────

    @Test
    @DisplayName("computeWeeklyVolume: empty workouts → all groups at 0%")
    void weeklyVolumeEmpty() {
        Map<String, Double> volume = useCase.computeWeeklyVolume(List.of());
        assertThat(volume).hasSize(12);
        assertThat(volume.values()).allMatch(v -> v == 0.0);
    }

    @Test
    @DisplayName("computeWeeklyVolume: caps at 100% when sets exceed weekly max")
    void weeklyVolumeCapsAt100() {
        WorkoutJpaEntity workout = workoutWithSets("legs", 50);
        Map<String, Double> volume = useCase.computeWeeklyVolume(List.of(workout));
        assertEquals(100.0, volume.get("legs"));
    }

    @Test
    @DisplayName("computeWeeklyVolume: 10 sets of legs maps to 50%")
    void weeklyVolumePartial() {
        WorkoutJpaEntity workout = workoutWithSets("legs", 10);
        Map<String, Double> volume = useCase.computeWeeklyVolume(List.of(workout));
        assertEquals(50.0, volume.get("legs"));
        assertEquals(0.0, volume.get("chest"));
    }

    @Test
    @DisplayName("computeWeeklyVolume: lowercases muscle group keys")
    void weeklyVolumeNormalizesMuscleGroup() {
        WorkoutJpaEntity workout = workoutWithSets("CHEST", 5);
        Map<String, Double> volume = useCase.computeWeeklyVolume(List.of(workout));
        assertEquals(25.0, volume.get("chest"));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private FatigueComputeResult sampleFatigue() {
        return new FatigueComputeResult(
                10.5, 12.3, 1.8, 1.05, 1.5, 18.4, 2.1, 80.0,
                List.of(), Map.of("2026-05-10", 10.5), true);
    }

    private RiskComputeResult sampleRisk() {
        return new RiskComputeResult(
                25.0, 30.0, 27.5, "low", "acwr",
                List.of("Maintain current load"), true);
    }

    private TrainingMetricsJpaEntity sampleCached() {
        return new TrainingMetricsJpaEntity(
                UUID.randomUUID(), userId, LocalDate.now(ZoneOffset.UTC),
                10.0, 12.0, 2.0, 1.0, 1.4, 16.8, 1.9, 72.5,
                20.0, 25.0, 22.5, "low", "acwr",
                "[]", "[]", "{}", "[]", true);
    }

    private WorkoutJpaEntity workoutWithSets(String muscleGroup, int setCount) {
        UUID workoutId = UUID.randomUUID();
        WorkoutJpaEntity workout = new WorkoutJpaEntity(
                workoutId, userId, "Test Workout", Instant.now(), 3600, null);
        ExerciseJpaEntity exercise = new ExerciseJpaEntity(
                UUID.randomUUID(), "Test Exercise", muscleGroup, false, null);
        WorkoutExerciseJpaEntity we = new WorkoutExerciseJpaEntity(
                UUID.randomUUID(), workout, exercise, 0);
        for (int i = 0; i < setCount; i++) {
            we.addSet(new WorkoutSetJpaEntity(
                    UUID.randomUUID(), we, 80.0, 8, 7.0, i));
        }
        workout.addExercise(we);
        return workout;
    }
}
