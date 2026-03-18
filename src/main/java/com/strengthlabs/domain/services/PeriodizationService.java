package com.strengthlabs.domain.services;

import com.strengthlabs.domain.valueobjects.FatigueScore;
import com.strengthlabs.domain.valueobjects.RiskLevel;

public class PeriodizationService {

    public String recommendPhase(FatigueScore fatigueScore, RiskLevel riskLevel) {
        if (riskLevel.isTrainingBlocked()) {
            return "DELOAD";
        }
        if (fatigueScore.isCritical()) {
            return "RECOVERY";
        }
        if (fatigueScore.isModerate()) {
            return "MAINTENANCE";
        }
        return "ACCUMULATION";
    }
}
