package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseJpaRepository extends JpaRepository<ExerciseJpaEntity, UUID> {
    List<ExerciseJpaEntity> findByIsCustomFalse();
    List<ExerciseJpaEntity> findByCreatedBy(UUID userId);
    Optional<ExerciseJpaEntity> findByNameAndIsCustomFalse(String name);
}
