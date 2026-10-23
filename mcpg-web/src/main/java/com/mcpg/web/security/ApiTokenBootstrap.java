package com.mcpg.web.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Ensures a usable API token exists when security is enabled.
 */
@Component
public class ApiTokenBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiTokenBootstrap.class);
    private static final Path TOKEN_FILE = Path.of("data", "api-token.txt");

    private final SecurityProperties properties;

    public ApiTokenBootstrap(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            log.warn("MCPG security is DISABLED (mcpg.security.enabled=false). Do not expose this process.");
            return;
        }
        if (properties.getApiToken() != null && !properties.getApiToken().isBlank()) {
            log.info("MCPG API token loaded from configuration (MCPG_API_TOKEN / mcpg.security.api-token).");
            return;
        }
        if (Files.isRegularFile(TOKEN_FILE)) {
            String existing = Files.readString(TOKEN_FILE, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) {
                properties.setApiToken(existing);
                log.info("MCPG API token loaded from {}", TOKEN_FILE.toAbsolutePath());
                return;
            }
        }
        String generated = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Files.createDirectories(TOKEN_FILE.getParent());
        Files.writeString(TOKEN_FILE, generated + System.lineSeparator(), StandardCharsets.UTF_8);
        properties.setApiToken(generated);
        log.warn("====================================================================");
        log.warn("MCPG generated a new API token (no MCPG_API_TOKEN was set).");
        log.warn("Token file: {}", TOKEN_FILE.toAbsolutePath());
        log.warn("Send it as: Authorization: Bearer <token>  or  X-MCPG-Token: <token>");
        log.warn("UI: paste the token when prompted (stored in browser localStorage).");
        log.warn("====================================================================");
    }
}
