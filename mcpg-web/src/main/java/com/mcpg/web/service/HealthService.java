package com.mcpg.web.service;

import com.mcpg.web.dto.HealthOverviewDto;
import com.mcpg.web.dto.HealthOverviewDto.LatencyBucket;
import com.mcpg.web.dto.HealthOverviewDto.TimeSeriesPoint;
import com.mcpg.web.dto.ToolHealthDto;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.repository.ToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Computes derived "tool health" metrics for the UI.
 *
 * <p>The numbers are intentionally synthetic for B5 - they are produced by a
 * deterministic PRNG seeded with the tool's persistent id, which gives every
 * tool a stable, plausible-looking trace. A future milestone replaces the
 * generator with a real metrics store; the DTO shape is already aligned with
 * the planned schema so the UI does not need to change.</p>
 */
@Service
public class HealthService {

    /** PRNG seed offset so health and audit datasets stay distinct. */
    private static final long SEED_OFFSET = 9009L;

    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:00");

    private static final String[] LATENCY_RANGES = {
            "<50ms", "50-100ms", "100-200ms", "200-500ms", "500ms-1s", ">1s"
    };

    private final ToolRepository toolRepository;
    private final ServiceRepository serviceRepository;

    public HealthService(ToolRepository toolRepository,
                          ServiceRepository serviceRepository) {
        this.toolRepository = toolRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public HealthOverviewDto overview() {
        List<ToolEntity> tools = toolRepository.findByDeprecatedFalse();
        Map<Long, String> serviceNames = new HashMap<>();
        for (ServiceEntity s : serviceRepository.findAll()) {
            serviceNames.put(s.getId(), displayName(s));
        }

        List<ToolHealthDto> perTool = new ArrayList<>(tools.size());
        long aggregateCalls = 0;
        double weightedSuccess = 0.0;
        long latencySum = 0;
        long[] bucketCounts = new long[LATENCY_RANGES.length];

        for (ToolEntity t : tools) {
            ToolHealthDto h = buildToolHealth(t, serviceNames);
            perTool.add(h);
            aggregateCalls += h.callsLast24h();
            weightedSuccess += h.callsLast24h() * h.successRate();
            latencySum += h.p50LatencyMs() * h.callsLast24h();
            bucketCounts[bucketOf(h.p50LatencyMs())] += h.callsLast24h();
        }

        double globalSuccess = aggregateCalls == 0 ? 1.0 : weightedSuccess / aggregateCalls;
        long avgLatency = aggregateCalls == 0 ? 0 : latencySum / aggregateCalls;

        List<LatencyBucket> histogram = new ArrayList<>(LATENCY_RANGES.length);
        for (int i = 0; i < LATENCY_RANGES.length; i++) {
            histogram.add(new LatencyBucket(LATENCY_RANGES[i], bucketCounts[i]));
        }

        List<ToolHealthDto> topTools = perTool.stream()
                .sorted(Comparator.comparingLong(ToolHealthDto::callsLast24h).reversed())
                .limit(15)
                .toList();

        return new HealthOverviewDto(
                tools.size(),
                tools.size(),
                aggregateCalls,
                globalSuccess,
                avgLatency,
                histogram,
                hourlyVolume24h(tools),
                topTools);
    }

    private ToolHealthDto buildToolHealth(ToolEntity t, Map<Long, String> serviceNames) {
        Random rng = new Random(SEED_OFFSET + t.getId());
        long calls = 50L + rng.nextInt(2000);
        double successRate = 0.92 + rng.nextDouble() * 0.08;
        if (rng.nextInt(20) == 0) successRate = 0.7 + rng.nextDouble() * 0.15;
        long p50 = 30L + rng.nextInt(170);
        long p95 = p50 + 50L + rng.nextInt(450);
        long p99 = p95 + 100L + rng.nextInt(800);
        Instant lastInvoked = Instant.now().minusSeconds(rng.nextInt(3600));
        String lastError = successRate < 0.95
                ? sampleError(rng)
                : null;
        return new ToolHealthDto(
                t.getId(),
                t.getToolName(),
                serviceNames.getOrDefault(t.getServiceId(), "service-" + t.getServiceId()),
                calls,
                successRate,
                p50,
                p95,
                p99,
                lastInvoked,
                lastError);
    }

    /**
     * Bucket a p50 latency value into one of the histogram ranges defined
     * in {@link #LATENCY_RANGES}.
     */
    private static int bucketOf(long p50) {
        if (p50 < 50) return 0;
        if (p50 < 100) return 1;
        if (p50 < 200) return 2;
        if (p50 < 500) return 3;
        if (p50 < 1000) return 4;
        return 5;
    }

    private List<TimeSeriesPoint> hourlyVolume24h(List<ToolEntity> tools) {
        Random rng = new Random(SEED_OFFSET);
        List<TimeSeriesPoint> series = new ArrayList<>(24);
        LocalDateTime base = LocalDateTime.now(ZoneId.systemDefault()).withMinute(0).withSecond(0).withNano(0)
                .minusHours(23);
        long toolCount = Math.max(1, tools.size());
        for (int i = 0; i < 24; i++) {
            LocalDateTime hour = base.plusHours(i);
            int dayFactor = workHoursMultiplier(hour.getHour());
            long success = toolCount * (5L + dayFactor + rng.nextInt(6));
            long failure = Math.max(0, success / (8 + rng.nextInt(6)));
            series.add(new TimeSeriesPoint(hour.format(HOUR_FMT), success, failure));
        }
        return series;
    }

    /** Bell-shaped multiplier so the chart looks like a real work day. */
    private static int workHoursMultiplier(int hour) {
        return switch (hour) {
            case 9, 10, 11, 14, 15, 16 -> 8;
            case 8, 12, 13, 17, 18 -> 5;
            case 7, 19, 20 -> 2;
            default -> 1;
        };
    }

    private static String sampleError(Random rng) {
        return switch (rng.nextInt(5)) {
            case 0 -> "Upstream returned 503 - circuit breaker open";
            case 1 -> "Timeout after 5s waiting for backend";
            case 2 -> "Schema validation failed - missing required field";
            case 3 -> "Rate limit exceeded - retry after 30s";
            default -> "Connection reset by peer";
        };
    }

    private static String displayName(ServiceEntity s) {
        return s.getDisplayName() != null && !s.getDisplayName().isBlank()
                ? s.getDisplayName()
                : s.getName();
    }
}
