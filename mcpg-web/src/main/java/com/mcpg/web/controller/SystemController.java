package com.mcpg.web.controller;

import com.mcpg.web.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal system endpoints used by the console boot sequence.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${spring.application.name:mcpg}")
    private String applicationName;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String version;

    private final SecurityProperties securityProperties;

    public SystemController(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Public (no token required) probe so the SPA knows whether to ask for a token.
     */
    @GetMapping("/auth-config")
    public Map<String, Object> authConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("securityEnabled", securityProperties.isEnabled());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_OPERATOR".equals(a.getAuthority()));
        body.put("authenticated", authenticated);
        return body;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", applicationName);
        body.put("version", version);
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", "UP");
        body.put("securityEnabled", securityProperties.isEnabled());
        return body;
    }
}
