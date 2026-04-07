package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workouts")
public class WorkoutJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant date;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<WorkoutExerciseJpaEntity> exercises = new ArrayList<>();

    protected WorkoutJpaEntity() {}

    public WorkoutJpaEntity(UUID id, UUID userId, String name, Instant date,
                             int durationSeconds, String notes) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.date = date;
        this.durationSeconds = durationSeconds;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getDate() { return date; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<WorkoutExerciseJpaEntity> getExercises() { return exercises; }
    public void addExercise(WorkoutExerciseJpaEntity exercise) { exercises.add(exercise); }
}
