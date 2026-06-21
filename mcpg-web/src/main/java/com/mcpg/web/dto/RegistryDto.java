package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.RegistryEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Transport representation of a {@link RegistryEntity}.
 *
 * <p>The plain-text password is deliberately NOT exposed by this DTO so that
 * GET responses do not leak credentials into browser history or HAR exports.
 * Updates that need to change the password use a separate field on the
 * update request DTO.</p>
 */
@Data
@Builder
public class RegistryDto {

    private Long id;
    private String name;
    private String displayName;
    private String type;
    private String endpoint;
    private String username;
    private Environment environment;
    private String namespace;
    private String groupName;
    private String extra;
    private boolean enabled;
    private RegistryEntity.Status status;
    private String lastError;
    private Integer lastServiceCount;
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static RegistryDto from(RegistryEntity e) {
        return RegistryDto.builder()
                .id(e.getId())
                .name(e.getName())
                .displayName(e.getDisplayName())
                .type(e.getType())
                .endpoint(e.getEndpoint())
                .username(e.getUsername())
                .environment(e.getEnvironment())
                .namespace(e.getNamespace())
                .groupName(e.getGroupName())
                .extra(e.getExtra())
                .enabled(e.isEnabled())
                .status(e.getStatus())
                .lastError(e.getLastError())
                .lastServiceCount(e.getLastServiceCount())
                .lastSyncedAt(e.getLastSyncedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
