package com.strengthlabs.integration;

import com.strengthlabs.infrastructure.security.LoginRateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rate limit on /auth/login")
@TestPropertySource(properties = {
        "security.rate-limit.max-attempts=3",
        "security.rate-limit.window-minutes=15"
})
class RateLimitIT extends AbstractIntegrationTest {

    @Autowired private LoginRateLimitFilter rateLimiter;

    @BeforeEach
    void resetLimiter() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("4th login attempt within window returns 429 with Retry-After header")
    void exceedsLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            HttpResponse r = doPost("/auth/login",
                    Map.of("email", "ghost@test.com", "password", "WhatEver1"));
            assertThat(r.status()).isEqualTo(401);
        }

        HttpResponse blocked = doPost("/auth/login",
                Map.of("email", "ghost@test.com", "password", "WhatEver1"));

        assertThat(blocked.status()).isEqualTo(429);
        assertThat(blocked.headers().get("Retry-After")).isNotNull();
    }
}
