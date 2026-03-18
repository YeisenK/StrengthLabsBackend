package com.strengthlabs.infrastructure.compute;

import com.strengthlabs.application.dtos.FatigueResultDTO;
import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.domain.valueobjects.RiskLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class PythonComputeAdapter implements ComputeEnginePort {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PythonComputeAdapter(RestTemplate restTemplate,
                                @Value("${compute-engine.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public FatigueResultDTO computeFatigue(List<Map<String, Object>> sessions) {
        Map<String, Object> payload = Map.of("sessions", sessions);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                baseUrl + "/compute/fatigue", payload, Map.class);

        return new FatigueResultDTO(
                toDouble(response.get("atl")),
                toDouble(response.get("ctl")),
                toDouble(response.get("acwr")),
                toDouble(response.get("tsb"))
        );
    }

    @Override
    public RiskLevel computeRisk(double acwr, double tsb) {
        Map<String, Object> payload = Map.of("acwr", acwr, "tsb", tsb);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                baseUrl + "/compute/risk", payload, Map.class);

        String level = (String) response.get("risk_level");
        return switch (level) {
            case "HIGH" -> RiskLevel.red("High injury risk: ACWR=" + acwr);
            case "MODERATE" -> RiskLevel.yellow("Moderate risk: monitor load carefully");
            default -> RiskLevel.green();
        };
    }

    @Override
    public String computePlan(double tsb) {
        Map<String, Object> payload = Map.of("tsb", tsb);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                baseUrl + "/compute/plan", payload, Map.class);
        return (String) response.get("recommendation");
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
