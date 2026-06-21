package com.mcpg.web.dto;

import java.util.List;

/**
 * Paged audit list response.
 *
 * <p>{@code total} is the count after filters but before paging - the UI uses
 * it to drive the pagination component.</p>
 */
public record AuditPageDto(
        List<AuditEventDto> items,
        long total,
        int page,
        int size
) {
}
