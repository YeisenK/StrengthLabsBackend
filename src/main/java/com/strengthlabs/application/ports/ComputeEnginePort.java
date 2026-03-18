package com.strengthlabs.application.ports;

import com.strengthlabs.application.dtos.FatigueResultDTO;
import com.strengthlabs.domain.valueobjects.RiskLevel;

import java.util.List;
import java.util.Map;

public interface ComputeEnginePort {
    FatigueResultDTO computeFatigue(List<Map<String, Object>> sessions);
    RiskLevel computeRisk(double acwr, double tsb);
    String computePlan(double tsb);
}
