package com.mcpg.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Describes a registry adapter type advertised by the gateway, used to
 * populate the "Type" dropdown in the UI without hard-coding the list in
 * the frontend.
 */
@Data
@Builder
@AllArgsConstructor
public class RegistryTypeDto {

    /** Stable identifier matching {@code ServiceRegistryAdapter.getType()}. */
    private String type;

    /** Human-friendly name shown in the dropdown (e.g. {@code Nacos}). */
    private String label;

    /** {@code true} when the adapter is functional, {@code false} for stubs. */
    private boolean implemented;
}
