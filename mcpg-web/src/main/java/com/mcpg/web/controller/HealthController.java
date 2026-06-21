package com.mcpg.web.controller;

import com.mcpg.web.dto.HealthOverviewDto;
import com.mcpg.web.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tool health surface.
 *
 * <p>Note the path is {@code /api/tool-health} (not {@code /api/health}) so
 * Spring Boot Actuator's {@code /actuator/health} stays the canonical health
 * probe and is not shadowed.</p>
 */
@RestController
@RequestMapping("/api/tool-health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/overview")
    public HealthOverviewDto overview() {
        return healthService.overview();
    }
}
