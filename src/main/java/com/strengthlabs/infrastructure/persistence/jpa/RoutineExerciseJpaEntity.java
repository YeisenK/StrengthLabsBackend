package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "routine_exercises")
public class RoutineExerciseJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_day_id", nullable = false)
    private RoutineDayJpaEntity routineDay;

    @Column(name = "exercise_name", nullable = false)
    private String exerciseName;

    @Column(name = "exercise_name_es")
    private String exerciseNameEs;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    @Column(nullable = false)
    private int sets;

    @Column(name = "reps_scheme", nullable = false)
    private String repsScheme;

    @Column(length = 500)
    private String notes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected RoutineExerciseJpaEntity() {}

    public RoutineExerciseJpaEntity(UUID id, RoutineDayJpaEntity routineDay,
                                     String exerciseName, String exerciseNameEs,
                                     String muscleGroup, int sets, String repsScheme,
                                     String notes, int orderIndex) {
        this.id = id;
        this.routineDay = routineDay;
        this.exerciseName = exerciseName;
        this.exerciseNameEs = exerciseNameEs;
        this.muscleGroup = muscleGroup;
        this.sets = sets;
        this.repsScheme = repsScheme;
        this.notes = notes;
        this.orderIndex = orderIndex;
    }

    public UUID getId() { return id; }
    public String getExerciseName() { return exerciseName; }
    public String getExerciseNameEs() { return exerciseNameEs; }
    public String getMuscleGroup() { return muscleGroup; }
    public int getSets() { return sets; }
    public String getRepsScheme() { return repsScheme; }
    public String getNotes() { return notes; }
    public int getOrderIndex() { return orderIndex; }
}
