package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutJpaRepository extends JpaRepository<WorkoutJpaEntity, UUID> {
    List<WorkoutJpaEntity> findByUserIdOrderByDateDesc(UUID userId);
    List<WorkoutJpaEntity> findByUserIdAndDateAfterOrderByDateDesc(UUID userId, Instant since);
    Optional<WorkoutJpaEntity> findByIdAndUserId(UUID id, UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
