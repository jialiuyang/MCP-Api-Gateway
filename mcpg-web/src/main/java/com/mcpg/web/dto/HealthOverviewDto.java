package com.mcpg.web.dto;

import java.util.List;

/**
 * Aggregated health snapshot for the dashboard top strip + the per-tool
 * table on the Tool Health page.
 */
public record HealthOverviewDto(
        long totalTools,
        long activeTools,
        long callsLast24h,
        double globalSuccessRate,
        long avgLatencyMs,
        List<LatencyBucket> latencyHistogram,
        List<TimeSeriesPoint> callVolume24h,
        List<ToolHealthDto> topTools
) {
    public record LatencyBucket(String range, long count) {}

    public record TimeSeriesPoint(String hour, long success, long failure) {}
}
