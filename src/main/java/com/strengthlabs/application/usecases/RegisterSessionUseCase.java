package com.strengthlabs.application.usecases;

import com.strengthlabs.application.dtos.FatigueResultDTO;
import com.strengthlabs.application.dtos.SessionInputDTO;
import com.strengthlabs.application.ports.ComputeEnginePort;
import com.strengthlabs.domain.entities.WorkoutSession;
import com.strengthlabs.domain.repositories.SessionRepository;
import com.strengthlabs.domain.valueobjects.FatigueScore;
import com.strengthlabs.domain.valueobjects.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RegisterSessionUseCase {

    private final SessionRepository sessionRepository;
    private final ComputeEnginePort computeEngine;

    public RegisterSessionUseCase(SessionRepository sessionRepository, ComputeEnginePort computeEngine) {
        this.sessionRepository = sessionRepository;
        this.computeEngine = computeEngine;
    }

    public WorkoutSession execute(UUID userId, SessionInputDTO input) {
        WorkoutSession session = new WorkoutSession(
                UUID.randomUUID(),
                userId,
                input.getDate(),
                input.getMuscleGroup(),
                input.getSets(),
                input.getRepsPerSet(),
                input.getWeightKg(),
                input.getRirAverage()
        );

        List<WorkoutSession> history = sessionRepository.findRecentByUserId(userId, 28);
        List<Map<String, Object>> sessionPayload = buildPayload(userId, history, session);

        FatigueResultDTO fatigue = computeEngine.computeFatigue(sessionPayload);
        RiskLevel risk = computeEngine.computeRisk(fatigue.getAcwr(), fatigue.getTsb());

        session.applyMetrics(FatigueScore.of(fatigue.getAtl()), risk,
                fatigue.getAtl(), fatigue.getCtl(), fatigue.getAcwr(), fatigue.getTsb());
        return sessionRepository.save(session);
    }

    private List<Map<String, Object>> buildPayload(UUID userId,
                                                    List<WorkoutSession> history,
                                                    WorkoutSession current) {
        List<Map<String, Object>> sessions = history.stream()
                .map(s -> Map.<String, Object>of(
                        "user_id", 1,
                        "date", s.getDate().toString(),
                        "duration_minutes", s.getSets() * s.getRepsPerSet(),
                        "rpe", (double) (10 - s.getRirAverage())
                ))
                .collect(Collectors.toList());

        sessions.add(Map.of(
                "user_id", 1,
                "date", current.getDate().toString(),
                "duration_minutes", current.getSets() * current.getRepsPerSet(),
                "rpe", (double) (10 - current.getRirAverage())
        ));
        return sessions;
    }
}
