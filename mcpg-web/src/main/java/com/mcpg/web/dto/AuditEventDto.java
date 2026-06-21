package com.mcpg.web.dto;

import java.time.Instant;

/**
 * Read-only DTO for one audit row exposed to the UI.
 *
 * <p>B5 ships a synthetic in-memory generator; the JPA-backed audit table
 * will land together with the first governance milestone after open-source
 * release. Keeping the DTO stable means the UI doesn't need to change when
 * the persistence layer arrives.</p>
 */
public record AuditEventDto(
        long id,
        Instant timestamp,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        String outcome,
        Integer httpStatus,
        Long durationMs,
        String clientIp,
        String userAgent,
        String detail
) {
}
