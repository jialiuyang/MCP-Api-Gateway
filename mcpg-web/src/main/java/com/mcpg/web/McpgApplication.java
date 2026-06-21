package com.mcpg.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the MCP Gateway Enterprise application.
 *
 * <p>{@link EnableScheduling} is enabled here because the gateway runs a
 * daily Swagger refresh task; see
 * {@code com.mcpg.web.scheduler.SwaggerRefreshScheduler}.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.mcpg.web",
        "com.mcpg.parser",
        "com.mcpg.registry",
        "com.mcpg.server"
})
@EnableScheduling
@EnableAsync
public class McpgApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpgApplication.class, args);
    }
}
