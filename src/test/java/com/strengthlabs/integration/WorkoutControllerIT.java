package com.strengthlabs.integration;

import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.UserJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkoutController integration")
class WorkoutControllerIT extends AbstractIntegrationTest {

    @Autowired private UserJpaRepository userRepo;
    @Autowired private WorkoutJpaRepository workoutRepo;
    @Autowired private ExerciseJpaRepository exerciseRepo;

    private UUID exerciseId;

    @BeforeEach
    void cleanDb() {
        workoutRepo.deleteAll();
        userRepo.deleteAll();
        exerciseRepo.deleteAll();

        ExerciseJpaEntity exercise = new ExerciseJpaEntity(
                UUID.randomUUID(), "Test Squat", "legs", false, null);
        exerciseRepo.save(exercise);
        exerciseId = exercise.getId();
    }

    @Test
    @DisplayName("POST /workouts creates workout with exercises and sets")
    @SuppressWarnings("unchecked")
    void createWorkout() throws Exception {
        String token = registerAndGetToken("user1@test.com");

        HttpResponse r = doPost("/workouts", workoutBody("Push Day"), token);

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.body().get("name")).isEqualTo("Push Day");
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) r.body().get("exercises");
        assertThat(exercises).hasSize(1);
    }

    @Test
    @DisplayName("POST /workouts without auth returns 401")
    void createWorkoutRequiresAuth() throws Exception {
        HttpResponse r = doPost("/workouts",
                Map.of("name", "x", "date", "2026-05-10T10:00:00Z",
                        "duration_seconds", 0, "exercises", List.of()));
        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("POST /workouts with unknown exercise_id returns 400")
    void createWorkoutUnknownExercise() throws Exception {
        String token = registerAndGetToken("user-unk@test.com");

        Map<String, Object> body = Map.of(
                "name", "Push Day", "date", "2026-05-10T10:00:00Z",
                "duration_seconds", 3600,
                "exercises", List.of(Map.of(
                        "exercise_id", UUID.randomUUID().toString(),
                        "order", 0,
                        "sets", List.of(Map.of("weight", 80.0, "reps", 5, "order", 0))
                ))
        );

        assertThat(doPost("/workouts", body, token).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("GET /workouts returns only the authenticated user's workouts")
    @SuppressWarnings("unchecked")
    void getWorkoutsScopedToUser() throws Exception {
        String tokenA = registerAndGetToken("alice@test.com");
        String tokenB = registerAndGetToken("bob@test.com");

        doPost("/workouts", workoutBody("Alice Push"), tokenA);
        doPost("/workouts", workoutBody("Alice Pull"), tokenA);
        doPost("/workouts", workoutBody("Bob Legs"), tokenB);

        List<Map<String, Object>> aliceItems =
                (List<Map<String, Object>>) doGet("/workouts", tokenA).body().get("items");
        assertThat(aliceItems).hasSize(2);
        assertThat(aliceItems).extracting(m -> m.get("name"))
                .containsExactlyInAnyOrder("Alice Push", "Alice Pull");

        List<Map<String, Object>> bobItems =
                (List<Map<String, Object>>) doGet("/workouts", tokenB).body().get("items");
        assertThat(bobItems).hasSize(1);
        assertThat(bobItems.get(0).get("name")).isEqualTo("Bob Legs");
    }

    @Test
    @DisplayName("GET /workouts/{id} returns 404 when workout belongs to another user")
    void getWorkoutFromAnotherUserReturns404() throws Exception {
        String tokenA = registerAndGetToken("a@test.com");
        String tokenB = registerAndGetToken("b@test.com");

        HttpResponse created = doPost("/workouts", workoutBody("Alice Workout"), tokenA);
        String workoutId = (String) created.body().get("id");

        assertThat(doGet("/workouts/" + workoutId, tokenB).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("DELETE /workouts/{id} removes the workout")
    void deleteWorkout() throws Exception {
        String token = registerAndGetToken("del@test.com");
        HttpResponse created = doPost("/workouts", workoutBody("To Delete"), token);
        String workoutId = (String) created.body().get("id");

        assertThat(doDelete("/workouts/" + workoutId, token).status()).isEqualTo(204);
        assertThat(doGet("/workouts/" + workoutId, token).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("DELETE /workouts/{id} on another user's workout returns 404")
    void deleteAnotherUsersWorkout() throws Exception {
        String tokenA = registerAndGetToken("ax@test.com");
        String tokenB = registerAndGetToken("bx@test.com");

        HttpResponse created = doPost("/workouts", workoutBody("Alice"), tokenA);
        String workoutId = (String) created.body().get("id");

        assertThat(doDelete("/workouts/" + workoutId, tokenB).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("PUT /workouts/{id} updates name and notes")
    void updateWorkout() throws Exception {
        String token = registerAndGetToken("upd@test.com");
        HttpResponse created = doPost("/workouts", workoutBody("Original"), token);
        String workoutId = (String) created.body().get("id");

        HttpResponse updated = doPut("/workouts/" + workoutId,
                Map.of("name", "Updated Name", "notes", "New notes"), token);

        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.body().get("name")).isEqualTo("Updated Name");
        assertThat(updated.body().get("notes")).isEqualTo("New notes");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Map<String, Object> workoutBody(String name) {
        return Map.of(
                "name", name,
                "date", "2026-05-10T10:00:00Z",
                "duration_seconds", 3600,
                "notes", "",
                "exercises", List.of(Map.of(
                        "exercise_id", exerciseId.toString(),
                        "order", 0,
                        "sets", List.of(
                                Map.of("weight", 100.0, "reps", 5, "rpe", 8.0, "order", 0),
                                Map.of("weight", 100.0, "reps", 5, "rpe", 8.5, "order", 1)
                        )
                ))
        );
    }
}
