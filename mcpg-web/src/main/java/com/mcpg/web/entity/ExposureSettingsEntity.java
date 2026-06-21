package com.mcpg.web.entity;

import com.mcpg.core.model.ExposureMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Single-row table that stores the gateway-wide MCP exposure strategy.
 *
 * <p>The table is intentionally degenerate: there is exactly one row with
 * {@code id = 1}. Keeping it as a JPA entity (instead of an in-memory
 * property) means an operator can change the mode at runtime through the
 * REST API and have the choice survive restarts.</p>
 *
 * <p>Future milestones may grow this into a per-environment or per-service
 * setting; the row already carries {@link #updatedBy} and {@link #updatedAt}
 * so the change history can be reconstructed once an audit log lands.</p>
 */
@Entity
@Table(name = "mcpg_exposure_settings")
@Getter
@Setter
public class ExposureSettingsEntity {

    /** Singleton id; we always read/write row 1. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /**
     * Active strategy. Defaults to {@link ExposureMode#HYBRID} on first
     * boot - a balance between LLM discoverability (meta tools always
     * available) and surface area control (only promoted operations are
     * direct).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExposureMode mode = ExposureMode.HYBRID;

    /** Free-text rationale captured at change time; surfaced in future audit views. */
    @Column(length = 512)
    private String note;

    /** Operator id (populated once SSO lands; null in B4). */
    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Stamp {@link #updatedAt} on every insert/update so the audit trail
     * carries an authoritative server-side timestamp even when the service
     * layer forgets to set one.
     */
    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
