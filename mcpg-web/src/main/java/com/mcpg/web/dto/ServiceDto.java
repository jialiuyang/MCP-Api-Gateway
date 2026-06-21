package com.mcpg.web.dto;

import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.ServiceEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ServiceDto {
    private Long id;
    private String name;
    private String displayName;
    private Environment environment;
    private String sourceType;
    private String sourceRef;
    private String baseUrl;
    private String specUrl;
    private String status;
    private String lastError;
    private int toolCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSyncedAt;

    public static ServiceDto from(ServiceEntity entity) {
        return ServiceDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .displayName(entity.getDisplayName())
                .environment(entity.getEnvironment())
                .sourceType(entity.getSourceType() == null ? null : entity.getSourceType().name())
                .sourceRef(entity.getSourceRef())
                .baseUrl(entity.getBaseUrl())
                .specUrl(entity.getSpecUrl())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .lastError(entity.getLastError())
                .toolCount(entity.getToolCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastSyncedAt(entity.getLastSyncedAt())
                .build();
    }
}
