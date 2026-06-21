package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.SiteSettingsEntity;

import java.time.Instant;

public record SiteSettingsDto(
        String siteName,
        Environment defaultEnvironment,
        String refreshCron,
        int maxToolsPerService,
        boolean ssoEnabled,
        int auditRetentionDays,
        boolean demoMode,
        String updatedBy,
        Instant updatedAt
) {
    public static SiteSettingsDto from(SiteSettingsEntity e) {
        return new SiteSettingsDto(
                e.getSiteName(),
                e.getDefaultEnvironment(),
                e.getRefreshCron(),
                e.getMaxToolsPerService(),
                e.isSsoEnabled(),
                e.getAuditRetentionDays(),
                e.isDemoMode(),
                e.getUpdatedBy(),
                e.getUpdatedAt());
    }
}
