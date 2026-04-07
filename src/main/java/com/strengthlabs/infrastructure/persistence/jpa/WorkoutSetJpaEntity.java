package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "workout_sets")
public class WorkoutSetJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExerciseJpaEntity workoutExercise;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(nullable = false)
    private int reps;

    private Double rpe;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected WorkoutSetJpaEntity() {}

    public WorkoutSetJpaEntity(UUID id, WorkoutExerciseJpaEntity workoutExercise,
                                Double weightKg, int reps, Double rpe, int orderIndex) {
        this.id = id;
        this.workoutExercise = workoutExercise;
        this.weightKg = weightKg;
        this.reps = reps;
        this.rpe = rpe;
        this.orderIndex = orderIndex;
    }

    public UUID getId() { return id; }
    public Double getWeightKg() { return weightKg; }
    public int getReps() { return reps; }
    public Double getRpe() { return rpe; }
    public int getOrderIndex() { return orderIndex; }
}
