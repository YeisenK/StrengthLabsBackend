package com.strengthlabs.infrastructure.compute;

import com.strengthlabs.application.dtos.ComputeSessionDTO;
import com.strengthlabs.application.dtos.FatigueComputeResult;
import com.strengthlabs.application.dtos.RiskComputeResult;
import com.strengthlabs.application.ports.ComputeEnginePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PythonComputeAdapter implements ComputeEnginePort {

    private static final Logger log = LoggerFactory.getLogger(PythonComputeAdapter.class);

    private final RestTemplate restTemplate;
    private final String computeUrl;

    public PythonComputeAdapter(RestTemplate restTemplate,
                                 @Value("${compute-engine.url}") String computeUrl) {
        this.restTemplate = restTemplate;
        this.computeUrl = computeUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FatigueComputeResult computeFatigue(List<ComputeSessionDTO> sessions) {
        try {
            List<Map<String, Object>> payload = sessions.stream()
                    .map(s -> Map.<String, Object>of(
                            "user_id", s.userId(),
                            "date", s.date(),
                            "duration_minutes", s.durationMinutes(),
                            "rpe", s.rpe()
                    ))
                    .toList();

            Map<String, Object> raw = restTemplate.postForObject(
                    computeUrl + "/compute/fatigue", Map.of("sessions", payload), Map.class);
            if (raw == null) return FatigueComputeResult.unavailable();

            return new FatigueComputeResult(
                    toDouble(raw.get("atl")),
                    toDouble(raw.get("ctl")),
                    toDouble(raw.get("tsb")),
                    toDouble(raw.get("acwr")),
                    toDouble(raw.get("monotony")),
                    toDouble(raw.get("strain")),
                    toDouble(raw.get("ramp_rate")),
                    toDouble(raw.get("readiness_score")),
                    toMapList(raw.get("risk_flags")),
                    toDoubleMap(raw.get("atl_series")),
                    true
            );
        } catch (Exception e) {
            log.error("Compute engine unavailable at {}/compute/fatigue — returning zero fallback. Cause: {}",
                    computeUrl, e.getMessage());
            return FatigueComputeResult.unavailable();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public RiskComputeResult computeRisk(double acwr, double tsb, double rampRate, double monotony) {
        try {
            Map<String, Object> payload = Map.of(
                    "acwr", acwr, "tsb", tsb, "ramp_rate", rampRate, "monotony", monotony
            );
            Map<String, Object> raw = restTemplate.postForObject(
                    computeUrl + "/compute/risk", payload, Map.class);
            if (raw == null) return RiskComputeResult.unavailable();

            return new RiskComputeResult(
                    toDouble(raw.get("injury_risk_score")),
                    toDouble(raw.get("overtraining_risk_score")),
                    toDouble(raw.get("composite_risk_score")),
                    (String) raw.getOrDefault("risk_level", "low"),
                    (String) raw.getOrDefault("dominant_factor", ""),
                    toMapList(raw.get("recommendations")),
                    true
            );
        } catch (Exception e) {
            log.error("Compute engine unavailable at {}/compute/risk — returning zero fallback. Cause: {}",
                    computeUrl, e.getMessage());
            return RiskComputeResult.unavailable();
        }
    }

    // ── Conversion helpers ─────────────────────────────────────────────────────

    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(Object v) {
        if (v instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> toDoubleMap(Object v) {
        if (v instanceof Map<?, ?> map) {
            return map.entrySet().stream().collect(Collectors.toMap(
                    e -> e.getKey().toString(),
                    e -> toDouble(e.getValue())
            ));
        }
        return Map.of();
    }
}
