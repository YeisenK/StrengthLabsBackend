package com.strengthlabs.infrastructure.persistence;

import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds the global exercise catalogue on every startup.
 * Only inserts exercises that don't already exist (idempotent).
 */
@Component
public class ExerciseDataSeeder implements ApplicationRunner {

    private final ExerciseJpaRepository exerciseRepo;

    public ExerciseDataSeeder(ExerciseJpaRepository exerciseRepo) {
        this.exerciseRepo = exerciseRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!exerciseRepo.findByIsCustomFalse().isEmpty()) return; // Already seeded

        List<Object[]> exercises = List.of(
            // Chest
            new Object[]{"Barbell Bench Press", "chest"},
            new Object[]{"Incline Barbell Press", "chest"},
            new Object[]{"Incline Dumbbell Press", "chest"},
            new Object[]{"Dumbbell Bench Press", "chest"},
            new Object[]{"Cable Fly", "chest"},
            new Object[]{"Push-up", "chest"},
            new Object[]{"Dip", "chest"},
            // Back
            new Object[]{"Barbell Deadlift", "back"},
            new Object[]{"Barbell Row", "back"},
            new Object[]{"Pendlay Row", "back"},
            new Object[]{"Dumbbell Row", "back"},
            new Object[]{"Lat Pulldown", "back"},
            new Object[]{"Seated Cable Row", "back"},
            new Object[]{"Pull-up", "back"},
            new Object[]{"Weighted Pull-up", "back"},
            new Object[]{"T-Bar Row", "back"},
            new Object[]{"Romanian Deadlift", "back"},
            // Shoulders
            new Object[]{"Overhead Press", "shoulders"},
            new Object[]{"Dumbbell Shoulder Press", "shoulders"},
            new Object[]{"Arnold Press", "shoulders"},
            new Object[]{"Lateral Raise", "shoulders"},
            new Object[]{"Front Raise", "shoulders"},
            new Object[]{"Face Pull", "shoulders"},
            new Object[]{"Rear Delt Fly", "shoulders"},
            // Biceps
            new Object[]{"Barbell Curl", "biceps"},
            new Object[]{"Dumbbell Curl", "biceps"},
            new Object[]{"EZ Bar Curl", "biceps"},
            new Object[]{"Hammer Curl", "biceps"},
            new Object[]{"Preacher Curl", "biceps"},
            new Object[]{"Incline Dumbbell Curl", "biceps"},
            // Triceps
            new Object[]{"Tricep Dip", "triceps"},
            new Object[]{"Tricep Pushdown", "triceps"},
            new Object[]{"Skull Crusher", "triceps"},
            new Object[]{"Close-Grip Bench Press", "triceps"},
            new Object[]{"Overhead Tricep Extension", "triceps"},
            // Legs
            new Object[]{"Barbell Back Squat", "legs"},
            new Object[]{"Front Squat", "legs"},
            new Object[]{"Hack Squat", "legs"},
            new Object[]{"Leg Press", "legs"},
            new Object[]{"Bulgarian Split Squat", "legs"},
            new Object[]{"Walking Lunge", "legs"},
            new Object[]{"Leg Extension", "legs"},
            new Object[]{"Leg Curl", "legs"},
            new Object[]{"Goblet Squat", "legs"},
            // Glutes
            new Object[]{"Hip Thrust", "glutes"},
            new Object[]{"Glute Bridge", "glutes"},
            new Object[]{"Cable Kickback", "glutes"},
            // Calves
            new Object[]{"Calf Raise", "calves"},
            new Object[]{"Standing Calf Raise", "calves"},
            new Object[]{"Seated Calf Raise", "calves"},
            // Core
            new Object[]{"Plank", "core"},
            new Object[]{"Ab Wheel Rollout", "core"},
            new Object[]{"Hanging Leg Raise", "core"},
            new Object[]{"Cable Crunch", "core"},
            new Object[]{"Crunch", "core"},
            // Forearms
            new Object[]{"Wrist Curl", "forearms"},
            new Object[]{"Farmer's Walk", "forearms"},
            // Cardio
            new Object[]{"Treadmill Walk/Jog", "cardio"},
            new Object[]{"Bike Sprint", "cardio"},
            new Object[]{"Rowing Machine", "cardio"},
            new Object[]{"Run or Bike", "cardio"},
            new Object[]{"Jump Rope", "cardio"}
        );

        exercises.forEach(e -> exerciseRepo.save(
                new ExerciseJpaEntity(UUID.randomUUID(), (String) e[0], (String) e[1], false, null)));
    }
}
