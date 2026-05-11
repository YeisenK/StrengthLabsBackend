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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("FatigueController integration")
class FatigueControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private TrainingMetricsJpaRepository metricsRepo;

    @MockitoBean
    private ComputeEnginePort computeEngine;

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
                        List.of("Maintain current load"), true));
    }

    @Test
    @DisplayName("GET /fatigue/summary returns 19 fields with mocked compute engine")
    void summaryReturnsAllFields() {
        String token = registerAndGetToken("fat@test.com");

        ResponseEntity<Map> response = authedGet("/fatigue/summary", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys(
                "overall_index", "is_overtraining", "weekly_volume", "trend",
                "atl", "ctl", "tsb", "acwr", "monotony", "strain", "ramp_rate",
                "readiness_score", "risk_flags",
                "injury_risk_score", "overtraining_risk_score", "composite_risk_score",
                "risk_level", "dominant_factor", "recommendations", "compute_available"
        );
        assertThat(response.getBody().get("compute_available")).isEqualTo(true);
    }

    @Test
    @DisplayName("GET /fatigue/summary without auth returns 401")
    void summaryRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/fatigue/summary", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Second call same day uses cache and does not invoke compute engine")
    void secondCallHitsCache() {
        String token = registerAndGetToken("cache@test.com");

        authedGet("/fatigue/summary", token);
        authedGet("/fatigue/summary", token);

        verify(computeEngine, times(1)).computeFatigue(anyList());
        verify(computeEngine, times(1)).computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("compute_available=false when compute engine returns unavailable result")
    void engineUnavailableSurfacesFlag() {
        when(computeEngine.computeFatigue(anyList())).thenReturn(FatigueComputeResult.unavailable());
        when(computeEngine.computeRisk(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(RiskComputeResult.unavailable());

        String token = registerAndGetToken("down@test.com");
        ResponseEntity<Map> response = authedGet("/fatigue/summary", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("compute_available")).isEqualTo(false);
    }

    @Test
    @DisplayName("GET /fatigue/weekly returns volume map for last 7 days")
    void weeklyReturnsVolumeMap() {
        String token = registerAndGetToken("weekly@test.com");

        ResponseEntity<Map> response = authedGet("/fatigue/weekly", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("weekly_volume");
        @SuppressWarnings("unchecked")
        Map<String, Object> volume = (Map<String, Object>) response.getBody().get("weekly_volume");
        assertThat(volume).containsKeys("chest", "back", "legs", "shoulders");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "User", "email", email, "password", "Password1"),
                Map.class);
        return (String) response.getBody().get("access_token");
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> authedGet(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }
}
