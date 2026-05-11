package com.strengthlabs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TrainingMetricsJpaRepository extends JpaRepository<TrainingMetricsJpaEntity, UUID> {
    Optional<TrainingMetricsJpaEntity> findByUserIdAndMetricDate(UUID userId, LocalDate metricDate);
}
