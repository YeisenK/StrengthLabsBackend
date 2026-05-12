package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "metric_date"}))
public class TrainingMetricsJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(nullable = false)
    private double atl;

    @Column(nullable = false)
    private double ctl;

    @Column(nullable = false)
    private double tsb;

    @Column(nullable = false)
    private double acwr;

    @Column(nullable = false)
    private double monotony;

    @Column(nullable = false)
    private double strain;

    @Column(name = "ramp_rate", nullable = false)
    private double rampRate;

    @Column(name = "readiness_score", nullable = false)
    private double readinessScore;

    @Column(name = "injury_risk_score", nullable = false)
    private double injuryRiskScore;

    @Column(name = "overtraining_risk_score", nullable = false)
    private double overtrainingRiskScore;

    @Column(name = "composite_risk_score", nullable = false)
    private double compositeRiskScore;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "dominant_factor", nullable = false)
    private String dominantFactor;

    @Column(name = "risk_flags_json", nullable = false, columnDefinition = "TEXT")
    private String riskFlagsJson;

    @Column(name = "recommendations_json", nullable = false, columnDefinition = "TEXT")
    private String recommendationsJson;

    @Column(name = "weekly_volume_json", nullable = false, columnDefinition = "TEXT")
    private String weeklyVolumeJson;

    @Column(name = "trend_json", nullable = false, columnDefinition = "TEXT")
    private String trendJson;

    @Column(name = "compute_available", nullable = false)
    private boolean computeAvailable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TrainingMetricsJpaEntity() {}

    public TrainingMetricsJpaEntity(UUID id, UUID userId, LocalDate metricDate,
                                     double atl, double ctl, double tsb, double acwr,
                                     double monotony, double strain, double rampRate,
                                     double readinessScore, double injuryRiskScore,
                                     double overtrainingRiskScore, double compositeRiskScore,
                                     String riskLevel, String dominantFactor,
                                     String riskFlagsJson, String recommendationsJson,
                                     String weeklyVolumeJson, String trendJson,
                                     boolean computeAvailable) {
        this.id = id;
        this.userId = userId;
        this.metricDate = metricDate;
        this.atl = atl;
        this.ctl = ctl;
        this.tsb = tsb;
        this.acwr = acwr;
        this.monotony = monotony;
        this.strain = strain;
        this.rampRate = rampRate;
        this.readinessScore = readinessScore;
        this.injuryRiskScore = injuryRiskScore;
        this.overtrainingRiskScore = overtrainingRiskScore;
        this.compositeRiskScore = compositeRiskScore;
        this.riskLevel = riskLevel;
        this.dominantFactor = dominantFactor;
        this.riskFlagsJson = riskFlagsJson;
        this.recommendationsJson = recommendationsJson;
        this.weeklyVolumeJson = weeklyVolumeJson;
        this.trendJson = trendJson;
        this.computeAvailable = computeAvailable;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getMetricDate() { return metricDate; }
    public double getAtl() { return atl; }
    public double getCtl() { return ctl; }
    public double getTsb() { return tsb; }
    public double getAcwr() { return acwr; }
    public double getMonotony() { return monotony; }
    public double getStrain() { return strain; }
    public double getRampRate() { return rampRate; }
    public double getReadinessScore() { return readinessScore; }
    public double getInjuryRiskScore() { return injuryRiskScore; }
    public double getOvertrainingRiskScore() { return overtrainingRiskScore; }
    public double getCompositeRiskScore() { return compositeRiskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getDominantFactor() { return dominantFactor; }
    public String getRiskFlagsJson() { return riskFlagsJson; }
    public String getRecommendationsJson() { return recommendationsJson; }
    public String getWeeklyVolumeJson() { return weeklyVolumeJson; }
    public String getTrendJson() { return trendJson; }
    public boolean isComputeAvailable() { return computeAvailable; }
    public Instant getCreatedAt() { return createdAt; }
}
