package com.strengthlabs.api;

import com.strengthlabs.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Boots the full application context against the testcontainers Postgres to
 * smoke-test that all beans wire up and the schema validates after Flyway.
 */
class StrengthLabsApiApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}
}
