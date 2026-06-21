package com.mcpg.web.dto;

import java.time.Instant;

/**
 * Per-tool health snapshot returned to the UI.
 *
 * <p>B5 synthesizes the metrics deterministically from the tool's persistent
 * id so the values are stable across page refreshes (useful for demos). A
 * future milestone will swap in a real time-series store.</p>
 */
public record ToolHealthDto(
        Long toolId,
        String toolName,
        String serviceName,
        long callsLast24h,
        double successRate,
        long p50LatencyMs,
        long p95LatencyMs,
        long p99LatencyMs,
        Instant lastInvokedAt,
        String lastError
) {
}
