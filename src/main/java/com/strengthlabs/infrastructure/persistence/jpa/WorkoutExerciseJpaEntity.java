package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_exercises")
public class WorkoutExerciseJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private WorkoutJpaEntity workout;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseJpaEntity exercise;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "workoutExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<WorkoutSetJpaEntity> sets = new ArrayList<>();

    protected WorkoutExerciseJpaEntity() {}

    public WorkoutExerciseJpaEntity(UUID id, WorkoutJpaEntity workout,
                                     ExerciseJpaEntity exercise, int orderIndex) {
        this.id = id;
        this.workout = workout;
        this.exercise = exercise;
        this.orderIndex = orderIndex;
    }

    public UUID getId() { return id; }
    public ExerciseJpaEntity getExercise() { return exercise; }
    public int getOrderIndex() { return orderIndex; }
    public List<WorkoutSetJpaEntity> getSets() { return sets; }
    public void addSet(WorkoutSetJpaEntity set) { sets.add(set); }
}
