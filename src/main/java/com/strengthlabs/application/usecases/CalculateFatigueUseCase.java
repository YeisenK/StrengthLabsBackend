package com.strengthlabs.application.usecases;

import com.strengthlabs.application.dtos.FatigueResultDTO;
import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.domain.entities.WorkoutSession;
import com.strengthlabs.domain.repositories.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CalculateFatigueUseCase {

    private final SessionRepository sessionRepository;
    private final ComputeEnginePort computeEngine;

    public CalculateFatigueUseCase(SessionRepository sessionRepository, ComputeEnginePort computeEngine) {
        this.sessionRepository = sessionRepository;
        this.computeEngine = computeEngine;
    }

    public FatigueResultDTO execute(UUID userId) {
        List<WorkoutSession> sessions = sessionRepository.findRecentByUserId(userId, 28);

        List<Map<String, Object>> payload = sessions.stream()
                .map(s -> Map.<String, Object>of(
                        "user_id", 1,
                        "date", s.getDate().toString(),
                        "duration_minutes", s.getSets() * s.getRepsPerSet(),
                        "rpe", (double) (10 - s.getRirAverage())
                ))
                .collect(Collectors.toList());

        return computeEngine.computeFatigue(payload);
    }
}
