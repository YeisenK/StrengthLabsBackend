package com.strengthlabs.infrastructure.persistence.jpa;

import com.strengthlabs.domain.entities.WorkoutSession;
import com.strengthlabs.domain.valueobjects.FatigueScore;
import com.strengthlabs.domain.valueobjects.RiskLevel;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions")
public class SessionJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    private int sets;

    @Column(name = "reps_per_set")
    private int repsPerSet;

    @Column(name = "weight_kg")
    private double weightKg;

    @Column(name = "rir_average")
    private int rirAverage;

    @Column(name = "fatigue_score")
    private Double fatigueScoreValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_zone")
    private RiskLevel.Zone riskZone;

    @Column(name = "risk_reason")
    private String riskReason;

    private Double atl;
    private Double ctl;
    private Double acwr;
    private Double tsb;

    public static SessionJpaEntity fromDomain(WorkoutSession session) {
        SessionJpaEntity entity = new SessionJpaEntity();
        entity.id = session.getId();
        entity.userId = session.getUserId();
        entity.date = session.getDate();
        entity.muscleGroup = session.getMuscleGroup();
        entity.sets = session.getSets();
        entity.repsPerSet = session.getRepsPerSet();
        entity.weightKg = session.getWeightKg();
        entity.rirAverage = session.getRirAverage();
        if (session.getFatigueScore() != null) {
            entity.fatigueScoreValue = session.getFatigueScore().getValue();
        }
        if (session.getRiskLevel() != null) {
            entity.riskZone = session.getRiskLevel().getZone();
            entity.riskReason = session.getRiskLevel().getReason();
        }
        entity.atl = session.getAtl();
        entity.ctl = session.getCtl();
        entity.acwr = session.getAcwr();
        entity.tsb = session.getTsb();
        return entity;
    }

    public WorkoutSession toDomain() {
        WorkoutSession session = new WorkoutSession(id, userId, date, muscleGroup,
                sets, repsPerSet, weightKg, rirAverage);
        if (fatigueScoreValue != null && riskZone != null) {
            FatigueScore fatigue = FatigueScore.of(fatigueScoreValue);
            RiskLevel risk = switch (riskZone) {
                case GREEN -> RiskLevel.green();
                case YELLOW -> RiskLevel.yellow(riskReason);
                case RED -> RiskLevel.red(riskReason);
            };
            session.applyMetrics(fatigue, risk, atl, ctl, acwr, tsb);
        }
        return session;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public Double getAtl() { return atl; }
    public Double getCtl() { return ctl; }
    public Double getAcwr() { return acwr; }
    public Double getTsb() { return tsb; }
}
