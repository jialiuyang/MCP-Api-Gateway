package com.mcpg.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the Spring application context starts cleanly
 * with the {@code dev} profile (H2 in-memory database).
 */
@SpringBootTest
@ActiveProfiles("dev")
class McpgApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: the assertion is that the context was built
        // without any auto-configuration error.
    }
}
