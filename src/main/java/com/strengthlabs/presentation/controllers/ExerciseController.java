package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutSetJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutSetJpaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.strengthlabs.presentation.middleware.LocalizedStatusException;
import com.strengthlabs.presentation.util.I18nHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

import java.util.*;

@RestController
@RequestMapping("/exercises")
public class ExerciseController {

    private static final Set<String> VALID_MUSCLE_GROUPS = Set.of(
            "chest", "back", "shoulders", "biceps", "triceps",
            "legs", "core", "glutes", "calves", "forearms", "cardio", "other"
    );

    private final ExerciseJpaRepository exerciseRepo;
    private final WorkoutSetJpaRepository workoutSetRepo;

    public ExerciseController(ExerciseJpaRepository exerciseRepo,
                               WorkoutSetJpaRepository workoutSetRepo) {
        this.exerciseRepo = exerciseRepo;
        this.workoutSetRepo = workoutSetRepo;
    }

    public record ExerciseInput(@NotBlank String name, @NotBlank String muscle_group) {}

    /** Returns global exercises + user's custom exercises. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getExercises(Authentication auth, Locale locale) {
        UUID userId = UUID.fromString(auth.getName());

        List<ExerciseJpaEntity> global = exerciseRepo.findByIsCustomFalse();
        List<ExerciseJpaEntity> custom = exerciseRepo.findByCreatedBy(userId);

        List<Map<String, Object>> all = new ArrayList<>();
        global.stream().map(e -> toMap(e, locale)).forEach(all::add);
        custom.stream().map(e -> toMap(e, locale)).forEach(all::add);

        return ResponseEntity.ok(all);
    }

    /** Creates a custom exercise for the authenticated user. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createExercise(
            @Valid @RequestBody ExerciseInput input,
            Authentication auth,
            Locale locale) {
        String muscleGroup = input.muscle_group().toLowerCase();
        if (!VALID_MUSCLE_GROUPS.contains(muscleGroup)) {
            throw new LocalizedStatusException(HttpStatus.BAD_REQUEST, "error.muscle_group.invalid");
        }
        UUID userId = UUID.fromString(auth.getName());
        ExerciseJpaEntity entity = new ExerciseJpaEntity(
                UUID.randomUUID(), input.name(), muscleGroup, true, userId);
        ExerciseJpaEntity saved = exerciseRepo.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved, locale));
    }

    /**
     * Returns the most recent set the authenticated user logged for this exercise,
     * so the UI can pre-fill weight/reps/RPE when adding a new set.
     * Returns an empty body (200 OK) if the user has never used this exercise.
     */
    @GetMapping("/{id}/last-set")
    public ResponseEntity<Map<String, Object>> getLastSet(@PathVariable UUID id,
                                                            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        List<WorkoutSetJpaEntity> hits = workoutSetRepo.findMostRecentForUserAndExercise(
                userId, id, PageRequest.of(0, 1));
        if (hits.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }
        WorkoutSetJpaEntity last = hits.get(0);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("weight", last.getWeightKg());
        body.put("reps", last.getReps());
        body.put("rpe", last.getRpe());
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toMap(ExerciseJpaEntity e, Locale locale) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId().toString());
        m.put("name", I18nHelper.pick(e.getName(), e.getNameEs(), locale));
        m.put("muscle_group", e.getMuscleGroup());
        m.put("is_custom", e.isCustom());
        return m;
    }
}
