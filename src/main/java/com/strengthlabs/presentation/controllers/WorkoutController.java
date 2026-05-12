package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.strengthlabs.presentation.middleware.LocalizedStatusException;
import com.strengthlabs.presentation.util.I18nHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import java.time.Instant;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    /** Cap to avoid pathological clients asking for too much in one page. */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

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
                                UUID client_request_id,
                                @NotNull List<ExerciseInput> exercises) {}

    public record UpdateInput(String name, String notes) {}

    // ── GET /workouts ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWorkouts(
            Authentication auth,
            Locale locale,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        UUID userId = userId(auth);

        if (page == null && size == null) {
            // Backwards-compatible non-paginated path — used by older clients
            // and small accounts. Capped implicitly by typical workout counts.
            List<Map<String, Object>> items = workoutRepo.findByUserIdOrderByDateDesc(userId)
                    .stream()
                    .map(w -> toMap(w, locale))
                    .toList();
            return ResponseEntity.ok(Map.of("items", items));
        }

        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? DEFAULT_PAGE_SIZE : Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(p, s);
        Page<WorkoutJpaEntity> result = workoutRepo.findByUserIdOrderByDateDesc(userId, pageable);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", result.getContent().stream().map(w -> toMap(w, locale)).toList());
        body.put("page", p);
        body.put("size", s);
        body.put("total", result.getTotalElements());
        body.put("has_more", result.hasNext());
        return ResponseEntity.ok(body);
    }

    // ── POST /workouts ─────────────────────────────────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createWorkout(
            @Valid @RequestBody WorkoutInput input,
            Authentication auth,
            Locale locale) {

        UUID userId = userId(auth);

        // Idempotency: if the client retried with the same UUID, return the
        // existing workout instead of creating a duplicate.
        if (input.client_request_id() != null) {
            var existing = workoutRepo.findByUserIdAndClientRequestId(
                    userId, input.client_request_id());
            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(toMap(existing.get(), locale));
            }
        }

        Instant date = Instant.parse(normalizeDate(input.date()));

        WorkoutJpaEntity workout = new WorkoutJpaEntity(
                UUID.randomUUID(), userId, input.name(), date,
                input.duration_seconds(), input.notes(), input.client_request_id());

        for (ExerciseInput ex : input.exercises()) {
            ExerciseJpaEntity exercise = exerciseRepo.findById(ex.exercise_id())
                    .orElseThrow(() -> new LocalizedStatusException(
                            HttpStatus.BAD_REQUEST, "error.exercise.not.found"));

            WorkoutExerciseJpaEntity we = new WorkoutExerciseJpaEntity(
                    UUID.randomUUID(), workout, exercise, ex.order());

            for (SetInput s : ex.sets()) {
                we.addSet(new WorkoutSetJpaEntity(
                        UUID.randomUUID(), we, s.weight(), s.reps(), s.rpe(), s.order()));
            }
            workout.addExercise(we);
        }

        WorkoutJpaEntity saved = workoutRepo.save(workout);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved, locale));
    }

    // ── GET /workouts/{id} ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWorkout(@PathVariable UUID id,
                                                           Authentication auth,
                                                           Locale locale) {
        WorkoutJpaEntity workout = workoutRepo.findByIdAndUserId(id, userId(auth))
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.workout.not.found"));
        return ResponseEntity.ok(toMap(workout, locale));
    }

    // ── PUT /workouts/{id} ─────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateWorkout(@PathVariable UUID id,
                                                              @RequestBody UpdateInput input,
                                                              @RequestHeader(value = "If-Match", required = false) Long ifMatchVersion,
                                                              Authentication auth,
                                                              Locale locale) {
        WorkoutJpaEntity workout = workoutRepo.findByIdAndUserId(id, userId(auth))
                .orElseThrow(() -> new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.workout.not.found"));

        // Conditional update — if the client supplied a version, reject when
        // the server is ahead. Without If-Match, JPA's @Version still protects
        // against truly concurrent in-flight transactions.
        if (ifMatchVersion != null && !ifMatchVersion.equals(workout.getVersion())) {
            throw new LocalizedStatusException(HttpStatus.CONFLICT, "error.workout.conflict");
        }

        if (input.name() != null) workout.setName(input.name());
        if (input.notes() != null) workout.setNotes(input.notes());

        return ResponseEntity.ok(toMap(workoutRepo.save(workout), locale));
    }

    // ── DELETE /workouts/{id} ──────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteWorkout(@PathVariable UUID id, Authentication auth) {
        UUID userId = userId(auth);
        if (workoutRepo.findByIdAndUserId(id, userId).isEmpty()) {
            throw new LocalizedStatusException(HttpStatus.NOT_FOUND, "error.workout.not.found");
        }
        workoutRepo.deleteByIdAndUserId(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    private Map<String, Object> toMap(WorkoutJpaEntity w, Locale locale) {
        List<Map<String, Object>> exercises = w.getExercises().stream()
                .map(we -> exerciseToMap(we, locale))
                .toList();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", w.getId().toString());
        map.put("name", w.getName());
        map.put("date", w.getDate().toString());
        map.put("duration_seconds", w.getDurationSeconds());
        map.put("notes", w.getNotes());
        map.put("client_request_id",
                w.getClientRequestId() != null ? w.getClientRequestId().toString() : null);
        map.put("version", w.getVersion());
        map.put("exercises", exercises);
        return map;
    }

    private Map<String, Object> exerciseToMap(WorkoutExerciseJpaEntity we, Locale locale) {
        ExerciseJpaEntity ex = we.getExercise();

        Map<String, Object> exerciseMap = new LinkedHashMap<>();
        exerciseMap.put("id", ex.getId().toString());
        exerciseMap.put("name", I18nHelper.pick(ex.getName(), ex.getNameEs(), locale));
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
