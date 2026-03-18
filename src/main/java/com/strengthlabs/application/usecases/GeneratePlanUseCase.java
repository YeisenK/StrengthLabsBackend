package com.strengthlabs.application.usecases;

import com.strengthlabs.application.dtos.FatigueResultDTO;
import com.strengthlabs.application.ports.ComputeEnginePort;
import org.springframework.stereotype.Service;

@Service
public class GeneratePlanUseCase {

    private final CalculateFatigueUseCase calculateFatigue;
    private final ComputeEnginePort computeEngine;

    public GeneratePlanUseCase(CalculateFatigueUseCase calculateFatigue, ComputeEnginePort computeEngine) {
        this.calculateFatigue = calculateFatigue;
        this.computeEngine = computeEngine;
    }

    public String execute(java.util.UUID userId) {
        FatigueResultDTO fatigue = calculateFatigue.execute(userId);
        return computeEngine.computePlan(fatigue.getTsb());
    }
}
