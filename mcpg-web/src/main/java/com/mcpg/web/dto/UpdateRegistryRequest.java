package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for {@code PUT /api/registries/{id}}.
 *
 * <p>All fields are optional - {@code null} means "leave as-is" so the
 * caller does not have to send every property on a partial edit. The one
 * exception is {@link #password}: an empty string explicitly clears the
 * stored password, while {@code null} keeps the existing value.</p>
 */
@Data
public class UpdateRegistryRequest {

    @Size(max = 256)
    private String displayName;

    @Size(max = 512)
    private String endpoint;

    @Size(max = 128)
    private String username;

    /** {@code null} = keep current; {@code ""} = clear; otherwise replace. */
    @Size(max = 256)
    private String password;

    private Environment environment;

    @Size(max = 128)
    private String namespace;

    @Size(max = 128)
    private String groupName;

    @Size(max = 1024)
    private String extra;

    private Boolean enabled;
}
