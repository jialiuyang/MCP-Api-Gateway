package com.mcpg.web.dto;

import com.mcpg.web.entity.PolicyEntity;

import java.time.Instant;

public record PolicyDto(
        Long id,
        String policyKey,
        String name,
        String description,
        String category,
        PolicyEntity.Severity severity,
        boolean enabled,
        String configJson,
        String updatedBy,
        Instant updatedAt
) {
    public static PolicyDto from(PolicyEntity e) {
        return new PolicyDto(
                e.getId(),
                e.getPolicyKey(),
                e.getName(),
                e.getDescription(),
                e.getCategory(),
                e.getSeverity(),
                e.isEnabled(),
                e.getConfigJson(),
                e.getUpdatedBy(),
                e.getUpdatedAt());
    }
}
