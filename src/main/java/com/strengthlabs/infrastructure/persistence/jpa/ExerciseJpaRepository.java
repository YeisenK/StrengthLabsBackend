package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciseJpaRepository extends JpaRepository<ExerciseJpaEntity, UUID> {
    List<ExerciseJpaEntity> findByIsCustomFalse();
    List<ExerciseJpaEntity> findByCreatedBy(UUID userId);
}
