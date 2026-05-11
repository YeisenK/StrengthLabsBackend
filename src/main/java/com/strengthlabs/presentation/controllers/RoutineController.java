package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.RoutineExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.RoutineJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.RoutineJpaRepository;
import com.strengthlabs.presentation.middleware.LocalizedStatusException;
import com.strengthlabs.presentation.util.I18nHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/routines")
public class RoutineController {

    private final RoutineJpaRepository routineRepo;

    public RoutineController(RoutineJpaRepository routineRepo) {
        this.routineRepo = routineRepo;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRoutines(
            @RequestParam(required = false) String level,
            Locale locale) {
        List<RoutineJpaEntity> routines = level == null
                ? routineRepo.findAll()
                : routineRepo.findByLevel(level);
        return ResponseEntity.ok(Map.of("items",
                routines.stream().map(r -> toSummary(r, locale)).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRoutine(@PathVariable String id, Locale locale) {
        return routineRepo.findById(id)
                .map(r -> ResponseEntity.ok(toDetail(r, locale)))
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.routine.not.found"));
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    private Map<String, Object> toSummary(RoutineJpaEntity r, Locale locale) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", I18nHelper.pick(r.getName(), r.getNameEs(), locale));
        m.put("level", r.getLevel());
        m.put("goal", r.getGoal());
        m.put("days_per_week", r.getDaysPerWeek());
        m.put("description", I18nHelper.pick(r.getDescription(), r.getDescriptionEs(), locale));
        return m;
    }

    private Map<String, Object> toDetail(RoutineJpaEntity r, Locale locale) {
        Map<String, Object> m = toSummary(r, locale);
        m.put("days", r.getDays().stream().map(day -> {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("name", I18nHelper.pick(day.getName(), day.getNameEs(), locale));
            d.put("exercises", day.getExercises().stream()
                    .map(e -> toExercise(e, locale)).toList());
            return d;
        }).toList());
        return m;
    }

    private Map<String, Object> toExercise(RoutineExerciseJpaEntity e, Locale locale) {
        Map<String, Object> exercise = Map.of(
                "id", "ex-" + e.getExerciseName().toLowerCase().replace(" ", "-"),
                "name", I18nHelper.pick(e.getExerciseName(), e.getExerciseNameEs(), locale),
                "muscle_group", e.getMuscleGroup(),
                "is_custom", false
        );
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exercise", exercise);
        m.put("sets", e.getSets());
        m.put("reps_scheme", e.getRepsScheme());
        m.put("notes", e.getNotes());
        return m;
    }
}
