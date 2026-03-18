package com.strengthlabs.application.usecases;

import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.domain.valueobjects.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class CalculateRiskUseCase {

    private final ComputeEnginePort computeEngine;

    public CalculateRiskUseCase(ComputeEnginePort computeEngine) {
        this.computeEngine = computeEngine;
    }

    public RiskLevel execute(double acwr, double tsb) {
        return computeEngine.computeRisk(acwr, tsb);
    }
}
