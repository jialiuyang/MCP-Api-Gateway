package com.mcpg.web.dto;

import com.mcpg.core.model.ExposureMode;

import java.time.Instant;

/**
 * Read model returned by {@code GET /api/exposure}.
 *
 * <p>Includes live counters so the UI can render &ldquo;will expose N tools&rdquo;
 * without a second round-trip.</p>
 *
 * @param mode             current strategy
 * @param note             operator-supplied rationale (nullable)
 * @param updatedBy        operator that last changed the setting (nullable)
 * @param updatedAt        last change timestamp
 * @param totalTools       count of non-deprecated rows in {@code mcpg_tool}
 * @param promotedTools    subset of {@link #totalTools} with {@code promoted=true}
 * @param metaToolCount    number of meta tools currently registered (constant 4 in B4)
 * @param effectiveCount   tools actually exposed under the active mode
 */
public record ExposureSettingsDto(
        ExposureMode mode,
        String note,
        String updatedBy,
        Instant updatedAt,
        long totalTools,
        long promotedTools,
        int metaToolCount,
        int effectiveCount) {
}
