package com.strengthlabs.domain.repositories;

import com.strengthlabs.domain.entities.WorkoutSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    WorkoutSession save(WorkoutSession session);
    Optional<WorkoutSession> findById(UUID id);
    List<WorkoutSession> findByUserId(UUID userId);
    List<WorkoutSession> findRecentByUserId(UUID userId, int limit);
}
