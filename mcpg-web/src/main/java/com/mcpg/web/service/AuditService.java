package com.mcpg.web.service;

import com.mcpg.web.dto.AuditEventDto;
import com.mcpg.web.dto.AuditPageDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Synthesizes an in-memory audit feed so the UI has something believable to
 * render before the real audit pipeline lands.
 *
 * <p>The events are generated once at startup with a fixed PRNG seed so the
 * output is deterministic between restarts (helps screenshots and demos).
 * Filters and paging are applied on every read.</p>
 *
 * <p>Replacing this service with a JPA-backed implementation is a drop-in:
 * the controller only depends on {@link #page(String, String, int, int)}.</p>
 */
@Service
public class AuditService {

    /** Fixed seed keeps the dataset stable across restarts. */
    private static final long SEED = 20260621L;

    /** How many events to materialize at startup. */
    private static final int SAMPLE_SIZE = 64;

    private static final String[] ACTIONS = {
            "service.import", "service.refresh", "service.delete",
            "tool.promote", "tool.demote",
            "exposure.update", "policy.update", "settings.update",
            "registry.connect", "registry.test",
            "mcp.call_api", "mcp.search_api", "mcp.get_api_schema", "mcp.list_services"
    };

    private static final String[] ACTORS = {
            "alice@acme.io", "bob@acme.io", "carol@acme.io",
            "platform-bot", "ops-bot@station", "ci-runner"
    };

    private static final String[] RESOURCE_TYPES = {
            "service", "tool", "registry", "exposure", "policy", "settings"
    };

    private static final String[] USER_AGENTS = {
            "MCP-Client/1.0 (streamable)",
            "MCP-Client/1.1",
            "MCPG-Web-Console/1.0",
            "Mozilla/5.0 (Macintosh; Intel) AppleWebKit/537.36",
            "MCPG-Stdio-Bridge/1.0"
    };

    private final List<AuditEventDto> events = new ArrayList<>();

    @PostConstruct
    void generate() {
        Random rng = new Random(SEED);
        Instant base = Instant.now().minus(2, ChronoUnit.DAYS);
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            String action = ACTIONS[rng.nextInt(ACTIONS.length)];
            String actor = ACTORS[rng.nextInt(ACTORS.length)];
            String resourceType = RESOURCE_TYPES[rng.nextInt(RESOURCE_TYPES.length)];
            String resourceId = resourceType + "-" + (1000 + rng.nextInt(8999));
            boolean failed = rng.nextInt(100) < 12;
            String outcome = failed ? "FAILURE" : "SUCCESS";
            int status = failed
                    ? (rng.nextBoolean() ? 500 : 403)
                    : (action.startsWith("mcp.") ? 200 : 204);
            long duration = action.startsWith("mcp.")
                    ? 50L + rng.nextInt(450)
                    : 5L + rng.nextInt(95);
            String ip = (192 + rng.nextInt(64)) + ".168.1." + (10 + rng.nextInt(245));
            String ua = USER_AGENTS[rng.nextInt(USER_AGENTS.length)];
            String detail = describe(action, resourceId, failed, rng);
            Instant ts = base.plusSeconds(i * 1700L + rng.nextInt(900));
            events.add(new AuditEventDto(
                    SAMPLE_SIZE - i,
                    ts,
                    actor,
                    action,
                    resourceType,
                    resourceId,
                    outcome,
                    status,
                    duration,
                    ip,
                    ua,
                    detail));
        }
        events.sort(Comparator.comparing(AuditEventDto::timestamp).reversed());
    }

    /**
     * Filter + paginate the synthetic event list. Both filters are optional;
     * {@code null} or blank means no constraint.
     *
     * @param outcome    {@code SUCCESS}, {@code FAILURE} or {@code null}
     * @param keyword    matched against action / actor / resourceId (case-insensitive)
     * @param page       0-based page index
     * @param size       page size (capped at 200 to keep responses light)
     */
    public AuditPageDto page(String outcome, String keyword, int page, int size) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String oc = outcome == null ? "" : outcome.trim().toUpperCase(Locale.ROOT);
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);

        List<AuditEventDto> filtered = events.stream()
                .filter(e -> oc.isEmpty() || oc.equals(e.outcome()))
                .filter(e -> kw.isEmpty()
                        || e.action().toLowerCase(Locale.ROOT).contains(kw)
                        || e.actor().toLowerCase(Locale.ROOT).contains(kw)
                        || (e.resourceId() != null && e.resourceId().toLowerCase(Locale.ROOT).contains(kw)))
                .toList();

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        return new AuditPageDto(filtered.subList(from, to), filtered.size(), safePage, safeSize);
    }

    private static String describe(String action, String resourceId, boolean failed, Random rng) {
        if (failed) {
            return switch (action) {
                case "service.import" -> "Failed to fetch OpenAPI document - host unreachable";
                case "service.refresh" -> "Refresh failed - upstream returned 5xx";
                case "mcp.call_api" -> "Backend invocation failed - timeout after 5s";
                case "registry.connect" -> "Connection refused - registry node may be down";
                default -> "Operation rejected by policy guard";
            };
        }
        return switch (action) {
            case "service.import" -> "Imported " + resourceId + " (" + (5 + rng.nextInt(40)) + " operations)";
            case "service.refresh" -> "Refreshed " + resourceId + " (diff: +" + rng.nextInt(3) + ", -" + rng.nextInt(2) + ")";
            case "tool.promote" -> "Promoted to first-class MCP tool";
            case "tool.demote" -> "Demoted - reverted to meta-only";
            case "exposure.update" -> "Exposure mode changed";
            case "policy.update" -> "Policy rule updated";
            case "mcp.call_api" -> "MCP call_api completed";
            case "mcp.search_api" -> "MCP search_api matched " + (1 + rng.nextInt(6)) + " operations";
            case "mcp.get_api_schema" -> "MCP get_api_schema delivered";
            case "mcp.list_services" -> "MCP list_services returned " + (2 + rng.nextInt(5)) + " services";
            default -> "OK";
        };
    }
}
