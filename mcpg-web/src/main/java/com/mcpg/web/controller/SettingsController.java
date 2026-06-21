package com.mcpg.web.controller;

import com.mcpg.web.dto.SiteSettingsDto;
import com.mcpg.web.dto.UpdateSiteSettingsRequest;
import com.mcpg.web.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SiteSettingsService settingsService;

    public SettingsController(SiteSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SiteSettingsDto get() {
        return settingsService.get();
    }

    @PutMapping
    public SiteSettingsDto update(@Valid @RequestBody UpdateSiteSettingsRequest req) {
        return settingsService.update(req);
    }
}
