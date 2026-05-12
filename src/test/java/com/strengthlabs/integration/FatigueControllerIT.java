package com.strengthlabs.integration;

import com.strengthlabs.application.dtos.FatigueComputeResult;
import com.strengthlabs.application.dtos.RiskComputeResult;
import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.infrastructure.persistence.jpa.TrainingMetricsJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("FatigueController integration")
class FatigueControllerIT extends AbstractIntegrationTest {

    @Autowired private UserJpaRepository userRepo;
    @Autowired private TrainingMetricsJpaRepository metricsRepo;
    @MockitoBean private ComputeEnginePort computeEngine;

    @BeforeEach
    void cleanDb() {
        metricsRepo.deleteAll();
        userRepo.deleteAll();
        reset(computeEngine);

        when(computeEngine.computeFatigue(anyList())).thenReturn(new FatigueComputeResult(
                10.5, 12.3, 1.8, 1.05, 1.5, 18.4, 2.1, 80.0,
                List.of(), Map.of(), true));
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RiskComputeResult(
                        25.0, 30.0, 27.5, "low", "acwr",
                        List.of(Map.of("code", "ALL_GOOD")), true));
    }

    @Test
    @DisplayName("GET /fatigue/summary returns all 20 fields with mocked compute engine")
    void summaryReturnsAllFields() throws Exception {
        String token = registerAndGetToken("fat@test.com");

        HttpResponse r = doGet("/fatigue/summary", token);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).containsKeys(
                "overall_index", "is_overtraining", "weekly_volume", "trend",
                "atl", "ctl", "tsb", "acwr", "monotony", "strain", "ramp_rate",
                "readiness_score", "risk_flags",
                "injury_risk_score", "overtraining_risk_score", "composite_risk_score",
                "risk_level", "dominant_factor", "recommendations", "compute_available"
        );
        assertThat(r.body().get("compute_available")).isEqualTo(true);
    }

    @Test
    @DisplayName("GET /fatigue/summary without auth returns 401")
    void summaryRequiresAuth() throws Exception {
        assertThat(doGet("/fatigue/summary").status()).isEqualTo(401);
    }

    @Test
    @DisplayName("second call same day uses cache, compute engine not called again")
    void secondCallHitsCache() throws Exception {
        String token = registerAndGetToken("cache@test.com");

        doGet("/fatigue/summary", token);
        doGet("/fatigue/summary", token);

        verify(computeEngine, times(1)).computeFatigue(anyList());
        verify(computeEngine, times(1)).computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("compute_available=false when compute engine returns unavailable")
    void engineUnavailableSurfacesFlag() throws Exception {
        when(computeEngine.computeFatigue(anyList())).thenReturn(FatigueComputeResult.unavailable());
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(RiskComputeResult.unavailable());

        String token = registerAndGetToken("down@test.com");
        HttpResponse r = doGet("/fatigue/summary", token);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("compute_available")).isEqualTo(false);
    }

    @Test
    @DisplayName("GET /fatigue/weekly returns volume map with muscle group keys")
    @SuppressWarnings("unchecked")
    void weeklyReturnsVolumeMap() throws Exception {
        String token = registerAndGetToken("weekly@test.com");

        HttpResponse r = doGet("/fatigue/weekly", token);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).containsKey("weekly_volume");
        Map<String, Object> volume = (Map<String, Object>) r.body().get("weekly_volume");
        assertThat(volume).containsKeys("chest", "back", "legs", "shoulders");
    }
}
