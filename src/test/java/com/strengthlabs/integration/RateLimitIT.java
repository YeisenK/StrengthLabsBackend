package com.strengthlabs.integration;

import com.strengthlabs.infrastructure.security.LoginRateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rate limit on /auth/login")
@TestPropertySource(properties = {
        "security.rate-limit.max-attempts=3",
        "security.rate-limit.window-minutes=15"
})
class RateLimitIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoginRateLimitFilter rateLimiter;

    @BeforeEach
    void resetLimiter() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("4th login attempt within window returns 429 with Retry-After")
    void exceedsLimit() {
        for (int i = 0; i < 3; i++) {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/auth/login",
                    Map.of("email", "ghost@test.com", "password", "WhatEver1"),
                    Map.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<Map> blocked = restTemplate.postForEntity(
                "/auth/login",
                Map.of("email", "ghost@test.com", "password", "WhatEver1"),
                Map.class);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().get("Retry-After")).isNotNull();
    }
}
