package com.mcpg.web.entity;

import com.mcpg.core.model.Environment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Single-row table holding site-wide preferences that operators tweak from
 * the Settings page.
 *
 * <p>Like {@link ExposureSettingsEntity} this is a degenerate singleton
 * ({@code id = 1}). Persisting it (rather than relying on
 * {@code application.yml}) means an operator's choices survive restarts and
 * are visible to every node in a cluster.</p>
 */
@Entity
@Table(name = "mcpg_site_settings")
@Getter
@Setter
public class SiteSettingsEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "site_name", nullable = false, length = 128)
    private String siteName = "MCP Gateway Enterprise";

    @Enumerated(EnumType.STRING)
    @Column(name = "default_environment", nullable = false, length = 16)
    private Environment defaultEnvironment = Environment.DEV;

    /**
     * Cron expression for the swagger refresh scheduler. Spring's cron format
     * (six fields). Default = 03:00 every day, server timezone.
     */
    @Column(name = "refresh_cron", nullable = false, length = 64)
    private String refreshCron = "0 0 3 * * *";

    /**
     * Hard-cap on the number of tools any single service is allowed to
     * synthesize. Protects the gateway against pathological specs that
     * declare thousands of operations.
     */
    @Column(name = "max_tools_per_service", nullable = false)
    private int maxToolsPerService = 500;

    @Column(name = "sso_enabled", nullable = false)
    private boolean ssoEnabled = false;

    @Column(name = "audit_retention_days", nullable = false)
    private int auditRetentionDays = 90;

    @Column(name = "demo_mode", nullable = false)
    private boolean demoMode = true;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
