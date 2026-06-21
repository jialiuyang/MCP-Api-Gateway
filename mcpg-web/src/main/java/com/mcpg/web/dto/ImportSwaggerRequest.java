package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.ServiceEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for the manual Swagger import endpoint.
 *
 * <p>{@code name} is intentionally required and not derived from the spec so
 * that operators can pick a stable identifier even when the spec's
 * {@code info.title} is verbose or non-ASCII.</p>
 */
@Data
public class ImportSwaggerRequest {

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 256)
    private String displayName;

    @NotBlank
    @Size(max = 1024)
    private String url;

    /**
     * Optional override of the base URL used for actual HTTP invocation. If
     * blank, the value declared in the spec (servers[0].url or schemes://host)
     * is used; if that is also missing, the host portion of {@code url} is
     * used as a last-resort fallback.
     */
    @Size(max = 512)
    private String baseUrl;

    private Environment environment = Environment.DEV;

    /**
     * Origin classifier. When omitted the import is treated as
     * {@link ServiceEntity.SourceType#MANUAL}. Registry-driven discovery
     * fills this in so the {@code mcpg_service.source_type} column matches
     * the upstream registry adapter (e.g. {@code NACOS}, {@code EUREKA}).
     */
    private ServiceEntity.SourceType sourceType;

    /**
     * Identifier within the source system (e.g. Nacos {@code group:service}).
     * Persisted onto the service row for traceability and used by the
     * discovery scheduler to decide whether a row should be deleted when a
     * service disappears from its registry.
     */
    @Size(max = 1024)
    private String sourceRef;
}
