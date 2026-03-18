package com.strengthlabs.domain.entities;

import com.strengthlabs.domain.valueobjects.FatigueScore;
import com.strengthlabs.domain.valueobjects.RiskLevel;

import java.time.LocalDate;
import java.util.UUID;

public class WorkoutSession {
    private UUID id;
    private UUID userId;
    private LocalDate date;
    private String muscleGroup;
    private int sets;
    private int repsPerSet;
    private double weightKg;
    private int rirAverage;
    private FatigueScore fatigueScore;
    private RiskLevel riskLevel;
    private Double atl;
    private Double ctl;
    private Double acwr;
    private Double tsb;

    public WorkoutSession(UUID id, UUID userId, LocalDate date, String muscleGroup,
                          int sets, int repsPerSet, double weightKg, int rirAverage) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.muscleGroup = muscleGroup;
        this.sets = sets;
        this.repsPerSet = repsPerSet;
        this.weightKg = weightKg;
        this.rirAverage = rirAverage;
    }

    public void applyMetrics(FatigueScore fatigueScore, RiskLevel riskLevel,
                             Double atl, Double ctl, Double acwr, Double tsb) {
        this.fatigueScore = fatigueScore;
        this.riskLevel = riskLevel;
        this.atl = atl;
        this.ctl = ctl;
        this.acwr = acwr;
        this.tsb = tsb;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public String getMuscleGroup() { return muscleGroup; }
    public int getSets() { return sets; }
    public int getRepsPerSet() { return repsPerSet; }
    public double getWeightKg() { return weightKg; }
    public int getRirAverage() { return rirAverage; }
    public FatigueScore getFatigueScore() { return fatigueScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public Double getAtl() { return atl; }
    public Double getCtl() { return ctl; }
    public Double getAcwr() { return acwr; }
    public Double getTsb() { return tsb; }
}
