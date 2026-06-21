package com.mcpg.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Result of {@code POST /api/registries/{id}/test}. */
@Data
@Builder
@AllArgsConstructor
public class TestConnectionResult {

    private boolean ok;

    /** Localized error message on failure, {@code null} on success. */
    private String message;

    /** Round-trip duration, useful to spot lazy registries. */
    private long elapsedMs;
}
