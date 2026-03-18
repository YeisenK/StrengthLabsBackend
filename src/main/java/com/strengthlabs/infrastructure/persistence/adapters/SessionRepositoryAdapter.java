package com.strengthlabs.infrastructure.persistence.adapters;

import com.strengthlabs.domain.entities.WorkoutSession;
import com.strengthlabs.domain.repositories.SessionRepository;
import com.strengthlabs.infrastructure.persistence.jpa.SessionJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.SessionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository jpaRepository;

    public SessionRepositoryAdapter(SessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WorkoutSession save(WorkoutSession session) {
        SessionJpaEntity saved = jpaRepository.save(SessionJpaEntity.fromDomain(session));
        return saved.toDomain();
    }

    @Override
    public Optional<WorkoutSession> findById(UUID id) {
        return jpaRepository.findById(id).map(SessionJpaEntity::toDomain);
    }

    @Override
    public List<WorkoutSession> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(SessionJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkoutSession> findRecentByUserId(UUID userId, int limit) {
        return jpaRepository.findByUserIdOrderByDateDesc(userId, PageRequest.of(0, limit)).stream()
                .map(SessionJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
