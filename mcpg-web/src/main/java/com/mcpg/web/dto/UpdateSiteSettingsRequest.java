package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Patch body for {@code PUT /api/settings}. All fields optional - {@code null}
 * means "leave as-is".
 */
public class UpdateSiteSettingsRequest {

    private String siteName;
    private Environment defaultEnvironment;
    private String refreshCron;

    @Min(value = 10, message = "max tools per service must be >= 10")
    @Max(value = 10000, message = "max tools per service must be <= 10000")
    private Integer maxToolsPerService;

    private Boolean ssoEnabled;

    @Min(value = 1, message = "audit retention days must be >= 1")
    @Max(value = 3650, message = "audit retention days must be <= 3650")
    private Integer auditRetentionDays;

    private Boolean demoMode;

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public Environment getDefaultEnvironment() { return defaultEnvironment; }
    public void setDefaultEnvironment(Environment defaultEnvironment) { this.defaultEnvironment = defaultEnvironment; }
    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }
    public Integer getMaxToolsPerService() { return maxToolsPerService; }
    public void setMaxToolsPerService(Integer maxToolsPerService) { this.maxToolsPerService = maxToolsPerService; }
    public Boolean getSsoEnabled() { return ssoEnabled; }
    public void setSsoEnabled(Boolean ssoEnabled) { this.ssoEnabled = ssoEnabled; }
    public Integer getAuditRetentionDays() { return auditRetentionDays; }
    public void setAuditRetentionDays(Integer auditRetentionDays) { this.auditRetentionDays = auditRetentionDays; }
    public Boolean getDemoMode() { return demoMode; }
    public void setDemoMode(Boolean demoMode) { this.demoMode = demoMode; }
}
