package com.mcpg.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal system endpoint used to verify the application is up. The "real"
 * health information is provided by Spring Boot Actuator at /actuator/health.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${spring.application.name:mcpg}")
    private String applicationName;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String version;

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", applicationName);
        body.put("version", version);
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", "UP");
        return body;
    }
}
