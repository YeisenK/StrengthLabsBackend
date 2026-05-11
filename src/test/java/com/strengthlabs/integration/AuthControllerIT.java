package com.strengthlabs.integration;

import com.strengthlabs.infrastructure.persistence.jpa.UserJpaRepository;
import com.strengthlabs.infrastructure.security.LoginRateLimitFilter;
import com.strengthlabs.infrastructure.security.RevokedTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController integration")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private LoginRateLimitFilter rateLimiter;

    @Autowired
    private RevokedTokenStore revokedTokenStore;

    @BeforeEach
    void cleanDb() {
        userRepo.deleteAll();
        rateLimiter.reset();
        revokedTokenStore.clear();
    }

    @Test
    @DisplayName("POST /auth/register returns 201 + token pair")
    void registerSucceeds() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "Alice", "email", "alice@test.com", "password", "Password1"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("access_token", "refresh_token");
        assertThat(userRepo.findByEmail("alice@test.com")).isPresent();
    }

    @Test
    @DisplayName("POST /auth/register with duplicate email returns 409")
    void registerDuplicateEmail() {
        registerUser("dup@test.com", "Password1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "Other", "email", "dup@test.com", "password", "Password1"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST /auth/register with invalid email returns 400")
    void registerInvalidEmail() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "Bob", "email", "not-an-email", "password", "Password1"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials returns token pair")
    void loginSucceeds() {
        registerUser("login@test.com", "Password1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/login",
                Map.of("email", "login@test.com", "password", "Password1"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("access_token");
    }

    @Test
    @DisplayName("POST /auth/login with wrong password returns 401")
    void loginWrongPassword() {
        registerUser("login2@test.com", "Password1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/login",
                Map.of("email", "login2@test.com", "password", "WrongOne"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /auth/login with unknown email returns 401")
    void loginUnknownEmail() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/login",
                Map.of("email", "noone@test.com", "password", "Password1"),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /auth/refresh with valid refresh token returns new pair")
    void refreshSucceeds() {
        Map<String, String> tokens = registerUser("refresh@test.com", "Password1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/refresh",
                Map.of("refresh_token", tokens.get("refresh_token")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("access_token", "refresh_token");
    }

    @Test
    @DisplayName("POST /auth/refresh rejects access token (only refresh tokens accepted)")
    void refreshRejectsAccessToken() {
        Map<String, String> tokens = registerUser("refresh2@test.com", "Password1");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/refresh",
                Map.of("refresh_token", tokens.get("access_token")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /auth/me with valid bearer token returns user info")
    void meSucceeds() {
        Map<String, String> tokens = registerUser("me@test.com", "Password1");

        ResponseEntity<Map> response = authedGet("/auth/me", tokens.get("access_token"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("email")).isEqualTo("me@test.com");
        assertThat(response.getBody().get("name")).isEqualTo("Test User");
    }

    @Test
    @DisplayName("GET /auth/me without token returns 401")
    void meRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/auth/me", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /auth/me with malformed token returns 401")
    void meRejectsMalformedToken() {
        ResponseEntity<Map> response = authedGet("/auth/me", "not.a.real.token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Password policy ───────────────────────────────────────────────────────

    @Test
    @DisplayName("register rejects password shorter than 8 chars")
    void rejectsShortPassword() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "X", "email", "short@test.com", "password", "Pass1"),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("register rejects password without digits")
    void rejectsPasswordWithoutDigit() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "X", "email", "noletters@test.com", "password", "PasswordOnly"),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("register rejects password without letters")
    void rejectsPasswordWithoutLetters() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "X", "email", "nolet@test.com", "password", "12345678"),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Logout / token revocation ─────────────────────────────────────────────

    @Test
    @DisplayName("after logout, access token can no longer call /auth/me")
    void logoutInvalidatesAccessToken() {
        Map<String, String> tokens = registerUser("logout@test.com", "Password1");
        String access = tokens.get("access_token");

        ResponseEntity<Map> beforeLogout = authedGet("/auth/me", access);
        assertThat(beforeLogout.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> logoutResp = restTemplate.exchange(
                "/auth/logout", HttpMethod.POST,
                new HttpEntity<>(Map.of("refresh_token", tokens.get("refresh_token")),
                        bearerHeaders(access)),
                Void.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> afterLogout = authedGet("/auth/me", access);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("after logout, refresh token cannot mint a new access token")
    void logoutInvalidatesRefreshToken() {
        Map<String, String> tokens = registerUser("logoutR@test.com", "Password1");

        restTemplate.exchange(
                "/auth/logout", HttpMethod.POST,
                new HttpEntity<>(Map.of("refresh_token", tokens.get("refresh_token")),
                        bearerHeaders(tokens.get("access_token"))),
                Void.class);

        ResponseEntity<Map> refreshResp = restTemplate.postForEntity(
                "/auth/refresh",
                Map.of("refresh_token", tokens.get("refresh_token")),
                Map.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("refresh token rotation: original refresh token cannot be reused")
    void refreshTokenRotation() {
        Map<String, String> tokens = registerUser("rotate@test.com", "Password1");
        String original = tokens.get("refresh_token");

        ResponseEntity<Map> firstRefresh = restTemplate.postForEntity(
                "/auth/refresh", Map.of("refresh_token", original), Map.class);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> reuseAttempt = restTemplate.postForEntity(
                "/auth/refresh", Map.of("refresh_token", original), Map.class);
        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("logout always returns 204 even with no tokens")
    void logoutNoOpAlwaysReturns204() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/auth/logout", HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> registerUser(String email, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of("name", "Test User", "email", email, "password", password),
                Map.class);
        return (Map<String, String>) response.getBody();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ResponseEntity<Map> authedGet(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }
}
