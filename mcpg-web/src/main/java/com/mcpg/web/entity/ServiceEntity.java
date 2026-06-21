package com.mcpg.web.entity;

import com.mcpg.core.model.Environment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Persistent representation of a service that the gateway has knowledge of.
 *
 * <p>A row can be created either by:</p>
 * <ul>
 *   <li>Manual import (a human pastes a Swagger URL in the UI) -
 *       {@link #sourceType} = {@code MANUAL}</li>
 *   <li>Registry-driven auto discovery (B3) - {@link #sourceType} = the
 *       registry adapter type identifier ({@code NACOS} / {@code EUREKA}).</li>
 * </ul>
 *
 * <p>The unique constraint on ({@link #name}, {@link #environment}) ensures
 * that the same logical service in different environments is represented as
 * distinct rows. This keeps the governance model (environment isolation)
 * simple to enforce.</p>
 */
@Entity
@Table(name = "mcpg_service", uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_name_env", columnNames = {"name", "environment"})
})
@Getter
@Setter
public class ServiceEntity {

    public enum SourceType { MANUAL, NACOS, EUREKA, CONSUL, POLARIS, K8S, ZOOKEEPER }

    public enum Status { ACTIVE, ERROR, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "display_name", length = 256)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Environment environment = Environment.DEV;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType = SourceType.MANUAL;

    /**
     * Identifier within the source system. For manual imports this is the
     * Swagger URL; for Nacos it is namespace+group+service; etc.
     */
    @Column(name = "source_ref", length = 1024)
    private String sourceRef;

    /**
     * Resolved HTTP base URL where the operations are reachable. May be
     * different from {@link #sourceRef} when the spec carries its own
     * {@code servers[]} block.
     */
    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    /**
     * URL used to fetch the OpenAPI / Swagger document. Persisted so the
     * daily refresh job can re-pull without consulting the registry.
     */
    @Column(name = "spec_url", length = 1024)
    private String specUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Column(name = "last_error", length = 2048)
    private String lastError;

    @Column(name = "tool_count", nullable = false)
    private int toolCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

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
