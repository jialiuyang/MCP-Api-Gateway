package com.mcpg.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpg.core.model.RiskLevel;
import com.mcpg.web.entity.ToolEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ToolDto {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
    private Long serviceId;
    private String toolName;
    private String operationId;
    private String httpMethod;
    private String path;
    private String summary;
    private String description;
    private List<String> tags;
    private JsonNode inputSchema;
    private JsonNode outputSchema;
    private RiskLevel riskLevel;
    private boolean promoted;
    private boolean deprecated;
    private Instant updatedAt;

    public static ToolDto from(ToolEntity entity, boolean includeSchemas) {
        ToolDtoBuilder builder = ToolDto.builder()
                .id(entity.getId())
                .serviceId(entity.getServiceId())
                .toolName(entity.getToolName())
                .operationId(entity.getOperationId())
                .httpMethod(entity.getHttpMethod())
                .path(entity.getPath())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .tags(entity.getTags() == null || entity.getTags().isBlank()
                        ? List.of()
                        : List.of(entity.getTags().split(",")))
                .riskLevel(entity.getRiskLevel())
                .promoted(entity.isPromoted())
                .deprecated(entity.isDeprecated())
                .updatedAt(entity.getUpdatedAt());

        if (includeSchemas) {
            builder.inputSchema(parseOrNull(entity.getInputSchemaJson()))
                    .outputSchema(parseOrNull(entity.getOutputSchemaJson()));
        }
        return builder.build();
    }

    private static JsonNode parseOrNull(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
