package com.strengthlabs.presentation.controllers;

import com.strengthlabs.application.dtos.SessionInputDTO;
import com.strengthlabs.application.usecases.RegisterSessionUseCase;
import com.strengthlabs.domain.entities.WorkoutSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final RegisterSessionUseCase registerSession;

    public SessionController(RegisterSessionUseCase registerSession) {
        this.registerSession = registerSession;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerSession(@Valid @RequestBody SessionInputDTO input,
                                                               Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        WorkoutSession session = registerSession.execute(userId, input);

        Map<String, Object> response = Map.of(
                "id", session.getId(),
                "date", session.getDate(),
                "muscleGroup", session.getMuscleGroup(),
                "fatigueScore", session.getFatigueScore() != null ? session.getFatigueScore().getValue() : null,
                "riskZone", session.getRiskLevel() != null ? session.getRiskLevel().getZone() : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
