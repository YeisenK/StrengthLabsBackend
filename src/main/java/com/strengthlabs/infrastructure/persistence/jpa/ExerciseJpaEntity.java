package com.strengthlabs.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class ExerciseJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "muscle_group", nullable = false)
    private String muscleGroup;

    @Column(name = "is_custom", nullable = false)
    private boolean isCustom;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    protected ExerciseJpaEntity() {}

    public ExerciseJpaEntity(UUID id, String name, String muscleGroup, boolean isCustom, UUID createdBy) {
        this.id = id;
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.isCustom = isCustom;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getMuscleGroup() { return muscleGroup; }
    public boolean isCustom() { return isCustom; }
    public UUID getCreatedBy() { return createdBy; }
}
