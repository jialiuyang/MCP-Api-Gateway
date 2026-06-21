package com.mcpg.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Persistent governance policy.
 *
 * <p>Each row is a distinct "policy card" that an operator can toggle in
 * the console. The evaluation engine (B6+) consumes these rows; for now
 * the row carries enough metadata to render a meaningful card.</p>
 *
 * <p>{@link #configJson} is a free-form payload; each policy {@link #policyKey}
 * defines its own schema. Storing JSON instead of typed columns lets new
 * policies land without DDL changes.</p>
 */
@Entity
@Table(name = "mcpg_policy", uniqueConstraints = {
        @UniqueConstraint(name = "uk_policy_key", columnNames = {"policy_key"})
})
@Getter
@Setter
public class PolicyEntity {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable identifier such as {@code rate-limit.global}. */
    @Column(name = "policy_key", nullable = false, length = 64)
    private String policyKey;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    /** Logical group used by the UI to lay cards out. */
    @Column(nullable = false, length = 64)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity = Severity.MEDIUM;

    @Column(nullable = false)
    private boolean enabled;

    /** Free-form JSON; each policy_key defines its own shape. */
    @Lob
    @Column(name = "config_json")
    private String configJson;

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
