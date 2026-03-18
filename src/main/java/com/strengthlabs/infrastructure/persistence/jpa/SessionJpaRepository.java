package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {
    List<SessionJpaEntity> findByUserId(UUID userId);
    List<SessionJpaEntity> findByUserIdOrderByDateDesc(UUID userId, Pageable pageable);
}
