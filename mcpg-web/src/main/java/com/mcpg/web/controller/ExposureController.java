package com.mcpg.web.controller;

import com.mcpg.web.dto.ExposureSettingsDto;
import com.mcpg.web.dto.UpdateExposureRequest;
import com.mcpg.web.service.ExposureSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the gateway-wide MCP exposure strategy.
 *
 * <p>This is intentionally a small controller: a GET that returns the
 * current setting along with live tool counters, and a PUT that updates the
 * setting and fires {@code ToolsChangedEvent} so connected MCP clients
 * refresh their tool list without restart.</p>
 */
@RestController
@RequestMapping("/api/exposure")
public class ExposureController {

    private final ExposureSettingsService service;

    public ExposureController(ExposureSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ExposureSettingsDto get() {
        return service.get();
    }

    @PutMapping
    public ExposureSettingsDto update(@Valid @RequestBody UpdateExposureRequest req) {
        return service.update(req);
    }
}
