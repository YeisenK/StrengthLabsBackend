package com.strengthlabs.presentation.controllers;

import com.strengthlabs.application.usecases.GetFatigueSummaryUseCase;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/fatigue")
public class FatigueController {

    private final GetFatigueSummaryUseCase getFatigueSummaryUseCase;
    private final WorkoutJpaRepository workoutRepo;

    public FatigueController(GetFatigueSummaryUseCase getFatigueSummaryUseCase,
                              WorkoutJpaRepository workoutRepo) {
        this.getFatigueSummaryUseCase = getFatigueSummaryUseCase;
        this.workoutRepo = workoutRepo;
    }

    // ── GET /fatigue/summary ───────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(Authentication auth, Locale locale) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(getFatigueSummaryUseCase.execute(userId, locale));
    }

    // ── GET /fatigue/weekly ────────────────────────────────────────────────────

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeekly(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        Instant since7 = LocalDate.now(ZoneOffset.UTC)
                .minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
        java.util.List<WorkoutJpaEntity> recent = workoutRepo
                .findByUserIdAndDateAfterOrderByDateDesc(userId, since7);
        return ResponseEntity.ok(Map.of("weekly_volume",
                getFatigueSummaryUseCase.computeWeeklyVolume(recent)));
    }
}
