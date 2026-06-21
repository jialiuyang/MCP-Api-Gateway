package com.mcpg.web.service;

import com.mcpg.core.model.RiskLevel;

import java.util.Locale;
import java.util.Set;

/**
 * Utility helpers for naming MCP tools and inferring their risk level.
 *
 * <p>Both functions are deterministic so that the same {@code (service,
 * operationId)} pair always produces the same identity. This guarantees
 * idempotent imports: re-importing a spec updates existing rows in place
 * instead of creating duplicates.</p>
 */
public final class ToolNaming {

    private static final int MAX_TOOL_NAME_LENGTH = 64;

    /**
     * HTTP methods that mutate state. Combined with naming heuristics to
     * arrive at an initial {@link RiskLevel}; operators can always override
     * after the fact via the governance UI.
     */
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "delete", "remove", "drop", "purge", "destroy",
            "deactivate", "disable", "revoke", "ban", "kick",
            "refund", "chargeback", "payout", "wire", "transfer",
            "shutdown", "terminate", "kill"
    );

    private ToolNaming() {
    }

    /** Produce the canonical MCP tool name. */
    public static String toolName(String serviceName, String operationId) {
        String safeService = sanitize(serviceName);
        String safeOp = sanitize(operationId);
        String combined = safeService + "__" + safeOp;
        if (combined.length() <= MAX_TOOL_NAME_LENGTH) return combined;
        // Truncate from the operationId side to keep service prefix intact.
        int budget = MAX_TOOL_NAME_LENGTH - safeService.length() - 2;
        if (budget < 8) {
            // Pathological case: service name itself too long. Truncate both halves.
            String svc = safeService.substring(0, Math.min(safeService.length(), 24));
            String op = safeOp.substring(0, Math.min(safeOp.length(),
                    MAX_TOOL_NAME_LENGTH - svc.length() - 2));
            return svc + "__" + op;
        }
        return safeService + "__" + safeOp.substring(0, budget);
    }

    /**
     * Replace characters MCP tool names disallow with underscores.
     * Spec: <a href="https://spec.modelcontextprotocol.io/specification/server/tools/">tools spec</a>
     * recommends ASCII identifiers, so we conservatively only keep
     * letters, digits and underscores.
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) return "unnamed";
        StringBuilder sb = new StringBuilder(input.length());
        boolean lastUnderscore = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                lastUnderscore = false;
            } else if (c == '_' || c == '-' || c == '.' || Character.isWhitespace(c)) {
                if (!lastUnderscore && sb.length() > 0) {
                    sb.append('_');
                    lastUnderscore = true;
                }
            }
            // Other characters dropped entirely.
        }
        // Trim trailing underscore.
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.length() == 0 ? "unnamed" : sb.toString();
    }

    /**
     * Infer initial risk level from HTTP method and operation name keywords.
     *
     * <ul>
     *   <li>GET / HEAD / OPTIONS → READ</li>
     *   <li>Mutating method + high-risk keyword → WRITE_HIGH</li>
     *   <li>Mutating method otherwise → WRITE_LOW</li>
     * </ul>
     */
    public static RiskLevel inferRiskLevel(String httpMethod, String operationId, String path) {
        String upperMethod = httpMethod == null ? "GET" : httpMethod.toUpperCase(Locale.ROOT);
        if (!WRITE_METHODS.contains(upperMethod)) {
            return RiskLevel.READ;
        }
        String haystack = ((operationId == null ? "" : operationId) + " "
                + (path == null ? "" : path)).toLowerCase(Locale.ROOT);
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (haystack.contains(keyword)) return RiskLevel.WRITE_HIGH;
        }
        return RiskLevel.WRITE_LOW;
    }
}
