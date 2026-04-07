package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    private final WorkoutJpaRepository workoutRepo;
    private final ExerciseJpaRepository exerciseRepo;

    public WorkoutController(WorkoutJpaRepository workoutRepo,
                              ExerciseJpaRepository exerciseRepo) {
        this.workoutRepo = workoutRepo;
        this.exerciseRepo = exerciseRepo;
    }

    // ── Request records ────────────────────────────────────────────────────────

    public record SetInput(Double weight, @NotNull Integer reps, Double rpe, int order) {}

    public record ExerciseInput(@NotNull UUID exercise_id,
                                 @NotNull List<SetInput> sets,
                                 int order) {}

    public record WorkoutInput(@NotBlank String name,
                                @NotBlank String date,
                                int duration_seconds,
                                String notes,
                                @NotNull List<ExerciseInput> exercises) {}

    public record UpdateInput(String name, String notes) {}

    // ── GET /workouts ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWorkouts(Authentication auth) {
        UUID userId = userId(auth);
        List<Map<String, Object>> items = workoutRepo.findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(Map.of("items", items));
    }

    // ── POST /workouts ─────────────────────────────────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createWorkout(
            @Valid @RequestBody WorkoutInput input,
            Authentication auth) {

        UUID userId = userId(auth);
        Instant date = Instant.parse(normalizeDate(input.date()));

        WorkoutJpaEntity workout = new WorkoutJpaEntity(
                UUID.randomUUID(), userId, input.name(), date,
                input.duration_seconds(), input.notes());

        for (ExerciseInput ex : input.exercises()) {
            ExerciseJpaEntity exercise = exerciseRepo.findById(ex.exercise_id())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Exercise not found: " + ex.exercise_id()));

            WorkoutExerciseJpaEntity we = new WorkoutExerciseJpaEntity(
                    UUID.randomUUID(), workout, exercise, ex.order());

            for (SetInput s : ex.sets()) {
                we.addSet(new WorkoutSetJpaEntity(
                        UUID.randomUUID(), we, s.weight(), s.reps(), s.rpe(), s.order()));
            }
            workout.addExercise(we);
        }

        WorkoutJpaEntity saved = workoutRepo.save(workout);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved));
    }

    // ── GET /workouts/{id} ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWorkout(@PathVariable UUID id,
                                                           Authentication auth) {
        WorkoutJpaEntity workout = workoutRepo.findByIdAndUserId(id, userId(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));
        return ResponseEntity.ok(toMap(workout));
    }

    // ── PUT /workouts/{id} ─────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateWorkout(@PathVariable UUID id,
                                                              @RequestBody UpdateInput input,
                                                              Authentication auth) {
        WorkoutJpaEntity workout = workoutRepo.findByIdAndUserId(id, userId(auth))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        if (input.name() != null) workout.setName(input.name());
        if (input.notes() != null) workout.setNotes(input.notes());

        return ResponseEntity.ok(toMap(workoutRepo.save(workout)));
    }

    // ── DELETE /workouts/{id} ──────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteWorkout(@PathVariable UUID id, Authentication auth) {
        UUID userId = userId(auth);
        if (workoutRepo.findByIdAndUserId(id, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
        }
        workoutRepo.deleteByIdAndUserId(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    private Map<String, Object> toMap(WorkoutJpaEntity w) {
        List<Map<String, Object>> exercises = w.getExercises().stream()
                .map(this::exerciseToMap)
                .toList();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", w.getId().toString());
        map.put("name", w.getName());
        map.put("date", w.getDate().toString());
        map.put("duration_seconds", w.getDurationSeconds());
        map.put("notes", w.getNotes());
        map.put("exercises", exercises);
        return map;
    }

    private Map<String, Object> exerciseToMap(WorkoutExerciseJpaEntity we) {
        ExerciseJpaEntity ex = we.getExercise();

        Map<String, Object> exerciseMap = new LinkedHashMap<>();
        exerciseMap.put("id", ex.getId().toString());
        exerciseMap.put("name", ex.getName());
        exerciseMap.put("muscle_group", ex.getMuscleGroup());
        exerciseMap.put("is_custom", ex.isCustom());

        List<Map<String, Object>> sets = we.getSets().stream()
                .map(s -> {
                    Map<String, Object> sm = new LinkedHashMap<>();
                    sm.put("id", s.getId().toString());
                    sm.put("weight", s.getWeightKg());
                    sm.put("reps", s.getReps());
                    sm.put("rpe", s.getRpe());
                    return sm;
                })
                .toList();

        return Map.of("exercise", exerciseMap, "sets", sets);
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    /** Handle dates with or without trailing 'Z'. */
    private String normalizeDate(String date) {
        return date.endsWith("Z") ? date : date + "Z";
    }
}
