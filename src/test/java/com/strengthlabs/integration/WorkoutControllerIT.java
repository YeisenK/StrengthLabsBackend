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
    @DisplayName("POST /workouts without auth is rejected")
    void createWorkoutRequiresAuth() throws Exception {
        HttpResponse r = doPost("/workouts",
                Map.of("name", "x", "date", "2026-05-10T10:00:00Z",
                        "duration_seconds", 0, "exercises", List.of()));
        // Spring Security on a stateless API responds with 403 when there's
        // no Authorization header (no auth challenge is meaningful). Either
        // is "you can't do that" from the client's perspective.
        assertThat(r.status()).isIn(401, 403);
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

    @Test
    @DisplayName("POST /workouts with the same client_request_id is idempotent")
    void idempotentCreate() throws Exception {
        String token = registerAndGetToken("idem@test.com");
        UUID clientRequestId = UUID.randomUUID();

        Map<String, Object> body = new java.util.LinkedHashMap<>(workoutBody("First send"));
        body.put("client_request_id", clientRequestId.toString());

        HttpResponse first = doPost("/workouts", body, token);
        assertThat(first.status()).isEqualTo(201);
        String firstId = (String) first.body().get("id");

        // Retry with the same key but a different name — server must return
        // the original row, not create a new one.
        Map<String, Object> retry = new java.util.LinkedHashMap<>(workoutBody("Retry attempt"));
        retry.put("client_request_id", clientRequestId.toString());

        HttpResponse second = doPost("/workouts", retry, token);
        assertThat(second.status()).isEqualTo(200);
        assertThat(second.body().get("id")).isEqualTo(firstId);

        // And the DB really only has one row for this user.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>)
                doGet("/workouts", token).body().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("client_request_id")).isEqualTo(clientRequestId.toString());
    }

    @Test
    @DisplayName("POST /workouts with no client_request_id creates a normal row each call")
    void nonIdempotentLegacy() throws Exception {
        String token = registerAndGetToken("legacy@test.com");
        doPost("/workouts", workoutBody("A"), token);
        doPost("/workouts", workoutBody("B"), token);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>)
                doGet("/workouts", token).body().get("items");
        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("PUT /workouts/{id} with stale If-Match returns 409")
    void putRejectsStaleVersion() throws Exception {
        String token = registerAndGetToken("ifmatch@test.com");
        HttpResponse created = doPost("/workouts", workoutBody("Original"), token);
        String workoutId = (String) created.body().get("id");

        // First update succeeds and bumps version 0 → 1.
        HttpResponse v1 = doPutWithHeader("/workouts/" + workoutId,
                Map.of("name", "Updated"), token, "If-Match", "0");
        assertThat(v1.status()).isEqualTo(200);

        // The response must already reflect the bumped version — clients use
        // this value for the next If-Match. If we serialise before the flush
        // they get stuck on version=0 and every subsequent PUT 409s.
        assertThat(((Number) v1.body().get("version")).intValue()).isEqualTo(1);

        // Second update with the now-stale version 0 must be rejected.
        HttpResponse v2 = doPutWithHeader("/workouts/" + workoutId,
                Map.of("name", "Should fail"), token, "If-Match", "0");
        assertThat(v2.status()).isEqualTo(409);

        // A subsequent PUT with the fresh version succeeds and bumps to 2.
        HttpResponse v3 = doPutWithHeader("/workouts/" + workoutId,
                Map.of("name", "Bumped again"), token, "If-Match", "1");
        assertThat(v3.status()).isEqualTo(200);
        assertThat(((Number) v3.body().get("version")).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /workouts?page=&size= returns a page object with metadata")
    void getWorkoutsPaginated() throws Exception {
        String token = registerAndGetToken("page@test.com");
        for (int i = 0; i < 5; i++) {
            doPost("/workouts", workoutBody("W" + i), token);
        }

        HttpResponse page0 = doGet("/workouts?page=0&size=2", token);
        assertThat(page0.status()).isEqualTo(200);
        assertThat(page0.body().get("page")).isEqualTo(0);
        assertThat(page0.body().get("size")).isEqualTo(2);
        assertThat(((Number) page0.body().get("total")).intValue()).isEqualTo(5);
        assertThat(page0.body().get("has_more")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) page0.body().get("items");
        assertThat(items).hasSize(2);

        HttpResponse page2 = doGet("/workouts?page=2&size=2", token);
        assertThat(page2.body().get("has_more")).isEqualTo(false);
    }

    @Test
    @DisplayName("GET /workouts response includes version and client_request_id")
    @SuppressWarnings("unchecked")
    void responseIncludesVersion() throws Exception {
        String token = registerAndGetToken("ver@test.com");
        UUID clientRequestId = UUID.randomUUID();
        Map<String, Object> body = new java.util.LinkedHashMap<>(workoutBody("With version"));
        body.put("client_request_id", clientRequestId.toString());

        HttpResponse created = doPost("/workouts", body, token);
        assertThat(created.body().get("version")).isEqualTo(0);
        assertThat(created.body().get("client_request_id")).isEqualTo(clientRequestId.toString());
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
