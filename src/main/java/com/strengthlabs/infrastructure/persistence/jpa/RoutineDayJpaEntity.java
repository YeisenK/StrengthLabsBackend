package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routine_days")
public class RoutineDayJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private RoutineJpaEntity routine;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_es")
    private String nameEs;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "routineDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<RoutineExerciseJpaEntity> exercises = new ArrayList<>();

    protected RoutineDayJpaEntity() {}

    public RoutineDayJpaEntity(UUID id, RoutineJpaEntity routine, String name, String nameEs, int orderIndex) {
        this.id = id;
        this.routine = routine;
        this.name = name;
        this.nameEs = nameEs;
        this.orderIndex = orderIndex;
    }

    public UUID getId() { return id; }
    public RoutineJpaEntity getRoutine() { return routine; }
    public String getName() { return name; }
    public String getNameEs() { return nameEs; }
    public int getOrderIndex() { return orderIndex; }
    public List<RoutineExerciseJpaEntity> getExercises() { return exercises; }
    public void addExercise(RoutineExerciseJpaEntity exercise) { exercises.add(exercise); }
}
