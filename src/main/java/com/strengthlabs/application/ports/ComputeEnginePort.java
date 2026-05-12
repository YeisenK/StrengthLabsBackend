package com.strengthlabs.application.ports;

import com.strengthlabs.application.dtos.ComputeSessionDTO;
import com.strengthlabs.application.dtos.FatigueComputeResult;
import com.strengthlabs.application.dtos.RiskComputeResult;

import java.util.List;

public interface ComputeEnginePort {

    FatigueComputeResult computeFatigue(List<ComputeSessionDTO> sessions);

    RiskComputeResult computeRisk(double acwr, double tsb, double rampRate, double monotony);
}
