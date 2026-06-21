package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Partial update for an existing service.
 *
 * <p>Used by the "Edit service" dialog on the Services page so operators can
 * fix bad metadata (typically: wrong baseUrl produced by a spec with a
 * relative {@code servers[].url}). Fields left {@code null} are ignored;
 * blank strings explicitly clear the field where applicable.</p>
 */
@Data
public class UpdateServiceRequest {

    @Size(max = 256)
    private String displayName;

    @Size(max = 512)
    private String baseUrl;

    private Environment environment;
}
