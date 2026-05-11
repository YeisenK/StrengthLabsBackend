package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineJpaRepository extends JpaRepository<RoutineJpaEntity, String> {
    List<RoutineJpaEntity> findByLevel(String level);
}
