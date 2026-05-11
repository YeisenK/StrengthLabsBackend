package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkoutSetJpaRepository extends JpaRepository<WorkoutSetJpaEntity, UUID> {

    /**
     * Most recent set logged by {@code userId} for {@code exerciseId}.
     * Ordered by the workout's date desc, then the set's order_index desc
     * (i.e. the very last set of the most recent session).
     * Use {@code Pageable.ofSize(1)} to limit.
     */
    @Query("""
            SELECT ws FROM WorkoutSetJpaEntity ws
            JOIN ws.workoutExercise we
            JOIN we.workout w
            WHERE w.userId = :userId
              AND we.exercise.id = :exerciseId
            ORDER BY w.date DESC, ws.orderIndex DESC
            """)
    List<WorkoutSetJpaEntity> findMostRecentForUserAndExercise(
            @Param("userId") UUID userId,
            @Param("exerciseId") UUID exerciseId,
            Pageable pageable);
}
