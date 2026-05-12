package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutJpaRepository extends JpaRepository<WorkoutJpaEntity, UUID> {

    /**
     * Loads workouts with their exercises and sets eager-fetched via a single
     * JPA fetch graph — avoids the N+1 that LAZY collections produced when
     * the controller iterated the list.
     */
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    List<WorkoutJpaEntity> findByUserIdOrderByDateDesc(UUID userId);

    // Paginated variant — @EntityGraph with a collection + Pageable produces an
    // "in-memory pagination" warning from Hibernate; we accept the cost since
    // the page is already small. The N+1 risk is bounded by page size.
    Page<WorkoutJpaEntity> findByUserIdOrderByDateDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    List<WorkoutJpaEntity> findByUserIdAndDateAfterOrderByDateDesc(UUID userId, Instant since);

    /**
     * Bounded date range — used by fatigue summaries that need only the
     * last 7 days of sessions, filtered at the DB level instead of pulling
     * 90 days into memory.
     */
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    List<WorkoutJpaEntity> findByUserIdAndDateBetweenOrderByDateDesc(
            UUID userId, Instant from, Instant to);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<WorkoutJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<WorkoutJpaEntity> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
