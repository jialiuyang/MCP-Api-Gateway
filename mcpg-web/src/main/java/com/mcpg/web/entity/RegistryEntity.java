package com.mcpg.web.entity;

import com.mcpg.core.model.Environment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Persistent configuration row for one service registry connection (Nacos,
 * Eureka, etc.).
 *
 * <p>A registry row is the unit of governance for auto-discovery. The fields
 * intentionally cover the common subset across registry products; product
 * specific options (Nacos namespace, Eureka zone) live in
 * {@link #namespace} / {@link #groupName} / {@link #extra}.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Operator creates a row through the UI (Settings → Service Registry).</li>
 *   <li>A test-connection call confirms the endpoint is reachable.</li>
 *   <li>{@code enabled} flips to true, and the periodic discovery scheduler
 *       picks the row up. Discovered services are persisted as
 *       {@link ServiceEntity}s with {@code sourceType} matching this row's
 *       {@link #type}.</li>
 * </ol>
 */
@Entity
@Table(name = "mcpg_registry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_registry_name", columnNames = {"name"})
})
@Getter
@Setter
public class RegistryEntity {

    public enum Status { UNKNOWN, OK, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique, operator-defined identifier (e.g. {@code nacos-prod}). */
    @Column(nullable = false, length = 128)
    private String name;

    /** Friendly label shown in the UI. */
    @Column(name = "display_name", length = 256)
    private String displayName;

    /** Adapter type identifier - must match {@code ServiceRegistryAdapter.getType()}. */
    @Column(nullable = false, length = 32)
    private String type;

    /** Endpoint string interpreted by the adapter (e.g. {@code 127.0.0.1:8848} for Nacos). */
    @Column(nullable = false, length = 512)
    private String endpoint;

    @Column(length = 128)
    private String username;

    /**
     * Plain-text password. Encryption-at-rest is reserved for the governance
     * milestone (B5); the field is intentionally kept simple here so the
     * scheduler does not need a key-store on startup.
     */
    @Column(length = 256)
    private String password;

    /** Logical environment a discovered service is tagged with. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Environment environment = Environment.DEV;

    /** Nacos namespace / Eureka zone / K8s namespace. Adapter-specific meaning. */
    @Column(length = 128)
    private String namespace;

    /** Nacos group. Other registries ignore. */
    @Column(name = "group_name", length = 128)
    private String groupName;

    /**
     * Adapter-specific extra options in {@code k1=v1;k2=v2} form. Kept as a
     * single column so adding new registry types does not require a schema
     * migration.
     */
    @Column(length = 1024)
    private String extra;

    /**
     * When true, the discovery scheduler picks this row up on its periodic
     * tick. Disabling a registry stops the scheduler from touching it but
     * leaves previously-discovered services intact.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.UNKNOWN;

    @Column(name = "last_error", length = 2048)
    private String lastError;

    /** Number of services found at the last successful discovery run. */
    @Column(name = "last_service_count")
    private Integer lastServiceCount;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
