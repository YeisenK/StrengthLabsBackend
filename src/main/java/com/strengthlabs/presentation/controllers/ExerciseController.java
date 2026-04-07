package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseJpaRepository exerciseRepo;

    public ExerciseController(ExerciseJpaRepository exerciseRepo) {
        this.exerciseRepo = exerciseRepo;
    }

    public record ExerciseInput(@NotBlank String name, @NotBlank String muscle_group) {}

    /** Returns global exercises + user's custom exercises. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getExercises(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());

        List<ExerciseJpaEntity> global = exerciseRepo.findByIsCustomFalse();
        List<ExerciseJpaEntity> custom = exerciseRepo.findByCreatedBy(userId);

        List<Map<String, Object>> all = new ArrayList<>();
        global.stream().map(this::toMap).forEach(all::add);
        custom.stream().map(this::toMap).forEach(all::add);

        return ResponseEntity.ok(all);
    }

    /** Creates a custom exercise for the authenticated user. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createExercise(
            @Valid @RequestBody ExerciseInput input,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        ExerciseJpaEntity entity = new ExerciseJpaEntity(
                UUID.randomUUID(), input.name(), input.muscle_group(), true, userId);
        ExerciseJpaEntity saved = exerciseRepo.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(saved));
    }

    private Map<String, Object> toMap(ExerciseJpaEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId().toString());
        m.put("name", e.getName());
        m.put("muscle_group", e.getMuscleGroup());
        m.put("is_custom", e.isCustom());
        return m;
    }
}
