package com.strengthlabs.integration;

import com.strengthlabs.infrastructure.persistence.jpa.UserJpaRepository;
import com.strengthlabs.infrastructure.security.LoginRateLimitFilter;
import com.strengthlabs.infrastructure.security.RevokedTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController integration")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired private UserJpaRepository userRepo;
    @Autowired private LoginRateLimitFilter rateLimiter;
    @Autowired private RevokedTokenStore revokedTokenStore;

    @BeforeEach
    void cleanDb() {
        userRepo.deleteAll();
        rateLimiter.reset();
        revokedTokenStore.clear();
    }

    @Test
    @DisplayName("POST /auth/register returns 201 + token pair")
    void registerSucceeds() throws Exception {
        HttpResponse r = doPost("/auth/register",
                Map.of("name", "Alice", "email", "alice@test.com", "password", "Password1"));

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.body()).containsKeys("access_token", "refresh_token");
        assertThat(userRepo.findByEmail("alice@test.com")).isPresent();
    }

    @Test
    @DisplayName("POST /auth/register with duplicate email returns 409")
    void registerDuplicateEmail() throws Exception {
        doPost("/auth/register",
                Map.of("name", "A", "email", "dup@test.com", "password", "Password1"));

        HttpResponse r = doPost("/auth/register",
                Map.of("name", "B", "email", "dup@test.com", "password", "Password1"));

        assertThat(r.status()).isEqualTo(409);
    }

    @Test
    @DisplayName("POST /auth/register with invalid email returns 400")
    void registerInvalidEmail() throws Exception {
        HttpResponse r = doPost("/auth/register",
                Map.of("name", "Bob", "email", "not-an-email", "password", "Password1"));

        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials returns token pair")
    void loginSucceeds() throws Exception {
        doPost("/auth/register",
                Map.of("name", "U", "email", "login@test.com", "password", "Password1"));

        HttpResponse r = doPost("/auth/login",
                Map.of("email", "login@test.com", "password", "Password1"));

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).containsKey("access_token");
    }

    @Test
    @DisplayName("POST /auth/login with wrong password returns 401")
    void loginWrongPassword() throws Exception {
        doPost("/auth/register",
                Map.of("name", "U", "email", "login2@test.com", "password", "Password1"));

        HttpResponse r = doPost("/auth/login",
                Map.of("email", "login2@test.com", "password", "WrongOne"));

        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("POST /auth/login with unknown email returns 401")
    void loginUnknownEmail() throws Exception {
        HttpResponse r = doPost("/auth/login",
                Map.of("email", "noone@test.com", "password", "Password1"));

        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("POST /auth/refresh with valid refresh token returns new pair")
    void refreshSucceeds() throws Exception {
        HttpResponse reg = doPost("/auth/register",
                Map.of("name", "U", "email", "refresh@test.com", "password", "Password1"));
        String refreshToken = (String) reg.body().get("refresh_token");

        HttpResponse r = doPost("/auth/refresh",
                Map.of("refresh_token", refreshToken));

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).containsKeys("access_token", "refresh_token");
    }

    @Test
    @DisplayName("POST /auth/refresh rejects access token")
    void refreshRejectsAccessToken() throws Exception {
        HttpResponse reg = doPost("/auth/register",
                Map.of("name", "U", "email", "refresh2@test.com", "password", "Password1"));
        String accessToken = (String) reg.body().get("access_token");

        HttpResponse r = doPost("/auth/refresh",
                Map.of("refresh_token", accessToken));

        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("GET /auth/me with valid bearer token returns user info")
    void meSucceeds() throws Exception {
        HttpResponse reg = doPost("/auth/register",
                Map.of("name", "Test User", "email", "me@test.com", "password", "Password1"));
        String token = (String) reg.body().get("access_token");

        HttpResponse r = doGet("/auth/me", token);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("email")).isEqualTo("me@test.com");
        assertThat(r.body().get("name")).isEqualTo("Test User");
    }

    @Test
    @DisplayName("GET /auth/me without token returns 401")
    void meRequiresAuth() throws Exception {
        assertThat(doGet("/auth/me").status()).isEqualTo(401);
    }

    @Test
    @DisplayName("GET /auth/me with malformed token returns 401")
    void meRejectsMalformedToken() throws Exception {
        assertThat(doGet("/auth/me", "not.a.real.token").status()).isEqualTo(401);
    }

    @Test
    @DisplayName("register rejects password shorter than 8 chars")
    void rejectsShortPassword() throws Exception {
        HttpResponse r = doPost("/auth/register",
                Map.of("name", "X", "email", "short@test.com", "password", "Pass1"));
        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("register rejects password without digits")
    void rejectsPasswordWithoutDigit() throws Exception {
        HttpResponse r = doPost("/auth/register",
                Map.of("name", "X", "email", "nodig@test.com", "password", "PasswordOnly"));
        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("register rejects password without letters")
    void rejectsPasswordWithoutLetters() throws Exception {
        HttpResponse r = doPost("/auth/register",
                Map.of("name", "X", "email", "nolet@test.com", "password", "12345678"));
        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("after logout, access token can no longer call /auth/me")
    void logoutInvalidatesAccessToken() throws Exception {
        HttpResponse reg = doPost("/auth/register",
                Map.of("name", "U", "email", "logout@test.com", "password", "Password1"));
        String access = (String) reg.body().get("access_token");
        String refresh = (String) reg.body().get("refresh_token");

        assertThat(doGet("/auth/me", access).status()).isEqualTo(200);

        HttpResponse logout = doPost("/auth/logout",
                Map.of("refresh_token", refresh), access);
        assertThat(logout.status()).isEqualTo(204);

        assertThat(doGet("/auth/me", access).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("after logout, refresh token cannot mint a new access token")
    void logoutInvalidatesRefreshToken() throws Exception {
        HttpResponse reg = doPost("/auth/register",
                Map.of("name", "U", "email", "logoutR@test.com", "password", "Password1"));
        String access = (String) reg.body().get("access_token");
        String refresh = (String) reg.body().get("refresh_token");

        doPost("/auth/logout", Map.of("refresh_token", refresh), access);

        HttpResponse r = doPost("/auth/refresh", Map.of("refresh_token", refresh));
        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("refresh token rotation: original token cannot be reused")
    void refreshTokenRotation() throws Exception {
        HttpResponse reg = doPost("/auth/register",
                Map.of("name", "U", "email", "rotate@test.com", "password", "Password1"));
        String original = (String) reg.body().get("refresh_token");

        assertThat(doPost("/auth/refresh", Map.of("refresh_token", original)).status())
                .isEqualTo(200);

        assertThat(doPost("/auth/refresh", Map.of("refresh_token", original)).status())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("logout always returns 204 even with no tokens")
    void logoutNoOpAlwaysReturns204() throws Exception {
        assertThat(doPost("/auth/logout", Map.of()).status()).isEqualTo(204);
    }
}
