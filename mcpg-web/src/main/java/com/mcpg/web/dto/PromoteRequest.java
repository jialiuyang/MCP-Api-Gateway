package com.mcpg.web.dto;

import lombok.Data;

/**
 * Toggle the {@code promoted} flag on a tool. Used by the UI button on each
 * row of the tool list (see B4 for the HYBRID mode wiring).
 */
@Data
public class PromoteRequest {
    private boolean promoted;
}
