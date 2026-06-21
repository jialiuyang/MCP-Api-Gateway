package com.mcpg.web.entity;

import com.mcpg.core.model.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per generated MCP tool. The table is the source of truth that the
 * meta tools query at runtime, so its shape is optimized for fast lookup by
 * either {@code tool_name} (call_api) or full-text search on summary /
 * description (search_api).
 */
@Entity
@Table(name = "mcpg_tool",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tool_name", columnNames = {"tool_name"})
        },
        indexes = {
                @Index(name = "idx_tool_service", columnList = "service_id"),
                @Index(name = "idx_tool_promoted", columnList = "promoted")
        })
@Getter
@Setter
public class ToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    /** Generated stable name e.g. {@code order_service__getOrder}. */
    @Column(name = "tool_name", nullable = false, length = 256)
    private String toolName;

    @Column(name = "operation_id", nullable = false, length = 128)
    private String operationId;

    @Column(name = "http_method", nullable = false, length = 8)
    private String httpMethod;

    @Column(name = "path", nullable = false, length = 512)
    private String path;

    @Column(length = 512)
    private String summary;

    @Lob
    @Column(name = "description")
    private String description;

    /**
     * Comma-separated tags pulled from {@code OperationObject.tags}. Used by
     * search_api and grouping in the UI.
     */
    @Column(name = "tags", length = 512)
    private String tags;

    /**
     * Full JSON Schema of the input (mirrors MCP tool {@code inputSchema}).
     * Stored verbatim so that the meta tools do not need to re-derive it on
     * each call.
     */
    @Lob
    @Column(name = "input_schema_json", nullable = false)
    private String inputSchemaJson;

    @Lob
    @Column(name = "output_schema_json")
    private String outputSchemaJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel = RiskLevel.READ;

    /**
     * Whether this tool has been "promoted" to a first-class direct tool in
     * {@link com.mcpg.core.model.ExposureMode#HYBRID} mode (see B4).
     */
    @Column(nullable = false)
    private boolean promoted;

    @Column(nullable = false)
    private boolean deprecated;

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
