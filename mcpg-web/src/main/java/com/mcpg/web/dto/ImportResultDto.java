package com.mcpg.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportResultDto {
    private ServiceDto service;
    private int toolCount;
    private int added;
    private int updated;
    private int removed;
    private String specVersion;
}
