package com.mcpg.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Persisted copy of the most-recent OpenAPI / Swagger document for a service.
 *
 * <p>Kept in a dedicated table so that:</p>
 * <ul>
 *   <li>The (potentially large) raw text does not bloat queries on
 *       {@code mcpg_service}.</li>
 *   <li>We can keep the document for audit even if the user later deletes
 *       the parent service (see {@code ON DELETE CASCADE}).</li>
 *   <li>The daily refresh job can write a new spec row and atomically swap
 *       the {@code current} pointer; useful for diffing in B4.</li>
 * </ul>
 */
@Entity
@Table(name = "mcpg_service_spec", indexes = {
        @Index(name = "idx_service_spec_service", columnList = "service_id")
})
@Getter
@Setter
public class ServiceSpecEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "spec_version", length = 32)
    private String specVersion;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "api_version", length = 64)
    private String apiVersion;

    /**
     * Raw OpenAPI / Swagger document. {@code @Lob} + LONGTEXT/LONGVARCHAR
     * depending on dialect.
     */
    @Lob
    @Column(name = "raw_content", nullable = false)
    private String rawContent;

    @Column(name = "operation_count", nullable = false)
    private int operationCount;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @PrePersist
    void onCreate() {
        if (fetchedAt == null) fetchedAt = Instant.now();
    }
}
