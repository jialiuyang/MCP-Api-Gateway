package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for {@code POST /api/registries}. */
@Data
public class CreateRegistryRequest {

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 256)
    private String displayName;

    @NotBlank
    @Size(max = 32)
    private String type;

    @NotBlank
    @Size(max = 512)
    private String endpoint;

    @Size(max = 128)
    private String username;

    @Size(max = 256)
    private String password;

    private Environment environment = Environment.DEV;

    @Size(max = 128)
    private String namespace;

    @Size(max = 128)
    private String groupName;

    @Size(max = 1024)
    private String extra;

    /** When omitted, defaults to {@code true}. */
    private Boolean enabled;
}
