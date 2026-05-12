package com.strengthlabs.infrastructure.persistence;

import com.strengthlabs.domain.entities.User;
import com.strengthlabs.domain.repositories.UserRepository;
import com.strengthlabs.infrastructure.persistence.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Populates the demo user with 30 days of realistic workouts.
 * Activated by running with the {@code demo} profile:
 * {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo}.
 *
 * Idempotent — only runs once even across restarts (checks for existing demo user).
 */
@Component
@Profile("demo")
@Order(3)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_EMAIL = "demo@strengthlabs.com";
    private static final String DEMO_PASSWORD = "Demo1234";
    private static final String DEMO_NAME = "Demo User";
    private static final int DAYS_OF_HISTORY = 30;
    private static final long SEED = 42L;

    private final UserRepository userRepo;
    private final WorkoutJpaRepository workoutRepo;
    private final ExerciseJpaRepository exerciseRepo;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepo,
                           WorkoutJpaRepository workoutRepo,
                           ExerciseJpaRepository exerciseRepo,
                           PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.workoutRepo = workoutRepo;
        this.exerciseRepo = exerciseRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Optional<User> existing = userRepo.findByEmail(DEMO_EMAIL);
        if (existing.isPresent()) {
            log.info("Demo user already exists ({}), skipping seed.", DEMO_EMAIL);
            return;
        }

        log.info("Seeding demo user with {} days of workouts...", DAYS_OF_HISTORY);

        UUID demoUserId = UUID.randomUUID();
        User user = new User(demoUserId, DEMO_NAME, DEMO_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD), User.Role.USER);
        userRepo.save(user);

        Map<String, List<ExerciseJpaEntity>> byMuscleGroup = exerciseRepo.findByIsCustomFalse().stream()
                .collect(Collectors.groupingBy(e -> e.getMuscleGroup().toLowerCase()));

        if (byMuscleGroup.isEmpty()) {
            log.warn("Exercise catalogue is empty — DemoDataSeeder cannot create workouts. " +
                    "Make sure ExerciseDataSeeder runs first.");
            return;
        }

        Random rng = new Random(SEED);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // 4 sessions/week pattern: Mon push, Tue pull, Thu legs, Fri full
        // We walk back 30 days and create workouts on roughly 4 days a week.
        int created = 0;
        for (int daysAgo = DAYS_OF_HISTORY - 1; daysAgo >= 0; daysAgo--) {
            LocalDate day = today.minusDays(daysAgo);
            int dayOfWeek = day.getDayOfWeek().getValue();
            DayPlan plan = planForDayOfWeek(dayOfWeek);
            if (plan == null) continue;

            WorkoutJpaEntity workout = buildWorkout(demoUserId, day, plan, byMuscleGroup, rng);
            if (workout != null) {
                workoutRepo.save(workout);
                created++;
            }
        }

        log.info("Demo user seeded: {} ({}). {} workouts created. Login with {}/{}",
                DEMO_NAME, DEMO_EMAIL, created, DEMO_EMAIL, DEMO_PASSWORD);
    }

    // ── Plan templates ────────────────────────────────────────────────────────

    private DayPlan planForDayOfWeek(int dow) {
        return switch (dow) {
            case 1 -> new DayPlan("Push Day",
                    List.of("chest", "chest", "shoulders", "shoulders", "triceps", "triceps"));
            case 2 -> new DayPlan("Pull Day",
                    List.of("back", "back", "back", "biceps", "biceps", "shoulders"));
            case 4 -> new DayPlan("Leg Day",
                    List.of("legs", "legs", "legs", "glutes", "calves", "core"));
            case 5 -> new DayPlan("Upper Body Volume",
                    List.of("chest", "back", "shoulders", "biceps", "triceps", "core"));
            default -> null;
        };
    }

    private WorkoutJpaEntity buildWorkout(UUID userId, LocalDate day, DayPlan plan,
                                           Map<String, List<ExerciseJpaEntity>> byMuscleGroup,
                                           Random rng) {
        Instant date = day.atTime(18, 30).toInstant(ZoneOffset.UTC);
        int durationSeconds = (45 + rng.nextInt(31)) * 60; // 45-75 min

        WorkoutJpaEntity workout = new WorkoutJpaEntity(
                UUID.randomUUID(), userId, plan.name(), date, durationSeconds, null);

        // Pick distinct exercises per muscle group while respecting plan order
        Set<UUID> usedExercises = new HashSet<>();
        int orderIdx = 0;
        for (String mg : plan.muscleGroupOrder()) {
            ExerciseJpaEntity exercise = pickExercise(byMuscleGroup, mg, usedExercises, rng);
            if (exercise == null) continue;
            usedExercises.add(exercise.getId());

            WorkoutExerciseJpaEntity we = new WorkoutExerciseJpaEntity(
                    UUID.randomUUID(), workout, exercise, orderIdx++);

            int setCount = 3 + rng.nextInt(2); // 3 or 4 sets
            double baseWeight = baseWeightForMuscleGroup(mg, rng);
            int baseReps = 5 + rng.nextInt(7); // 5-11
            for (int s = 0; s < setCount; s++) {
                double weight = round(baseWeight * (1.0 + (s - 1) * 0.025), 2.5);
                int reps = Math.max(3, baseReps - s);
                double rpe = 6.5 + rng.nextDouble() * 2.5; // 6.5 - 9.0
                we.addSet(new WorkoutSetJpaEntity(
                        UUID.randomUUID(), we, weight, reps, round(rpe, 0.5), s));
            }
            workout.addExercise(we);
        }

        return workout.getExercises().isEmpty() ? null : workout;
    }

    private ExerciseJpaEntity pickExercise(Map<String, List<ExerciseJpaEntity>> byMG,
                                            String mg, Set<UUID> used, Random rng) {
        List<ExerciseJpaEntity> candidates = byMG.getOrDefault(mg, List.of()).stream()
                .filter(e -> !used.contains(e.getId()))
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(rng.nextInt(candidates.size()));
    }

    private double baseWeightForMuscleGroup(String mg, Random rng) {
        return switch (mg) {
            case "legs" -> 100.0 + rng.nextInt(40);
            case "back" -> 70.0 + rng.nextInt(30);
            case "chest" -> 60.0 + rng.nextInt(25);
            case "shoulders", "triceps" -> 30.0 + rng.nextInt(15);
            case "biceps" -> 15.0 + rng.nextInt(15);
            case "glutes" -> 80.0 + rng.nextInt(40);
            case "calves" -> 60.0 + rng.nextInt(30);
            case "core", "cardio" -> 0.0;
            default -> 20.0 + rng.nextInt(20);
        };
    }

    private static double round(double value, double step) {
        return Math.round(value / step) * step;
    }

    private record DayPlan(String name, List<String> muscleGroupOrder) {}
}
