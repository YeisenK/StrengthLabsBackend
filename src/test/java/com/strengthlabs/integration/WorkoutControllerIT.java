package com.strengthlabs.integration;

import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.ExerciseJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.UserJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkoutController integration")
class WorkoutControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private WorkoutJpaRepository workoutRepo;

    @Autowired
    private ExerciseJpaRepository exerciseRepo;

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
    void createWorkout() {
        String token = registerAndGetToken("user1@test.com");

        Map<String, Object> body = Map.of(
                "name", "Push Day",
                "date", "2026-05-10T10:00:00Z",
                "duration_seconds", 3600,
                "notes", "Felt strong",
                "exercises", List.of(Map.of(
                        "exercise_id", exerciseId.toString(),
                        "order", 0,
                        "sets", List.of(
                                Map.of("weight", 100.0, "reps", 5, "rpe", 8.0, "order", 0),
                                Map.of("weight", 100.0, "reps", 5, "rpe", 8.5, "order", 1)
                        )
                ))
        );

        ResponseEntity<Map> response = authedPost("/workouts", body, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("name")).isEqualTo("Push Day");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) response.getBody().get("exercises");
        assertThat(exercises).hasSize(1);
    }

    @Test
    @DisplayName("POST /workouts without auth returns 401")
    void createWorkoutRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/workouts", Map.of("name", "x", "date", "2026-05-10T10:00:00Z",
                        "duration_seconds", 0, "exercises", List.of()),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /workouts with unknown exercise_id returns 400")
    void createWorkoutUnknownExercise() {
        String token = registerAndGetToken("user-unk@test.com");

        Map<String, Object> body = Map.of(
                "name", "Push Day",
                "date", "2026-05-10T10:00:00Z",
                "duration_seconds", 3600,
                "exercises", List.of(Map.of(
                        "exercise_id", UUID.randomUUID().toString(),
                        "order", 0,
                        "sets", List.of(Map.of("weight", 80.0, "reps", 5, "order", 0))
                ))
        );

        ResponseEntity<Map> response = authedPost("/workouts", body, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /workouts returns only the authenticated user's workouts")
    void getWorkoutsScopedToUser() {
        String tokenA = registerAndGetToken("alice@test.com");
        String tokenB = registerAndGetToken("bob@test.com");

        createSimpleWorkout(tokenA, "Alice Push");
        createSimpleWorkout(tokenA, "Alice Pull");
        createSimpleWorkout(tokenB, "Bob Legs");

        ResponseEntity<Map> aliceResp = authedGet("/workouts", tokenA);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aliceItems = (List<Map<String, Object>>) aliceResp.getBody().get("items");
        assertThat(aliceItems).hasSize(2);
        assertThat(aliceItems).extracting(m -> m.get("name"))
                .containsExactlyInAnyOrder("Alice Push", "Alice Pull");

        ResponseEntity<Map> bobResp = authedGet("/workouts", tokenB);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bobItems = (List<Map<String, Object>>) bobResp.getBody().get("items");
        assertThat(bobItems).hasSize(1);
        assertThat(bobItems.get(0).get("name")).isEqualTo("Bob Legs");
    }

    @Test
    @DisplayName("GET /workouts/{id} returns 404 when workout belongs to another user")
    void getWorkoutFromAnotherUserReturns404() {
        String tokenA = registerAndGetToken("a@test.com");
        String tokenB = registerAndGetToken("b@test.com");

        ResponseEntity<Map> created = createSimpleWorkout(tokenA, "Alice Workout");
        String workoutId = (String) created.getBody().get("id");

        ResponseEntity<Map> response = authedGet("/workouts/" + workoutId, tokenB);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /workouts/{id} removes the workout")
    void deleteWorkout() {
        String token = registerAndGetToken("del@test.com");
        ResponseEntity<Map> created = createSimpleWorkout(token, "To Delete");
        String workoutId = (String) created.getBody().get("id");

        ResponseEntity<Void> response = authedDelete("/workouts/" + workoutId, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> getResp = authedGet("/workouts/" + workoutId, token);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /workouts/{id} on another user's workout returns 404")
    void deleteAnotherUsersWorkout() {
        String tokenA = registerAndGetToken("ax@test.com");
        String tokenB = registerAndGetToken("bx@test.com");

        ResponseEntity<Map> created = createSimpleWorkout(tokenA, "Alice");
        String workoutId = (String) created.getBody().get("id");

        ResponseEntity<Void> response = authedDelete("/workouts/" + workoutId, tokenB);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /workouts/{id} updates name and notes")
    void updateWorkout() {
        String token = registerAndGetToken("upd@test.com");
        ResponseEntity<Map> created = createSimpleWorkout(token, "Original");
        String workoutId = (String) created.getBody().get("id");

        ResponseEntity<Map> updated = authedPut(
                "/workouts/" + workoutId,
                Map.of("name", "Updated Name", "notes", "New notes"),
                token);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("name")).isEqualTo("Updated Name");
        assertThat(updated.getBody().get("notes")).isEqualTo("New notes");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "User", "email", email, "password", "Password1"),
                Map.class);
        return (String) response.getBody().get("access_token");
    }

    private ResponseEntity<Map> createSimpleWorkout(String token, String name) {
        Map<String, Object> body = Map.of(
                "name", name,
                "date", "2026-05-10T10:00:00Z",
                "duration_seconds", 1800,
                "exercises", List.of(Map.of(
                        "exercise_id", exerciseId.toString(),
                        "order", 0,
                        "sets", List.of(Map.of("weight", 80.0, "reps", 5, "order", 0))
                ))
        );
        return authedPost("/workouts", body, token);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @SuppressWarnings({"rawtypes"})
    private ResponseEntity<Map> authedPost(String path, Object body, String token) {
        return restTemplate.exchange(
                path, HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @SuppressWarnings({"rawtypes"})
    private ResponseEntity<Map> authedGet(String path, String token) {
        return restTemplate.exchange(
                path, HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);
    }

    @SuppressWarnings({"rawtypes"})
    private ResponseEntity<Map> authedPut(String path, Object body, String token) {
        return restTemplate.exchange(
                path, HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    private ResponseEntity<Void> authedDelete(String path, String token) {
        return restTemplate.exchange(
                path, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), Void.class);
    }
}
