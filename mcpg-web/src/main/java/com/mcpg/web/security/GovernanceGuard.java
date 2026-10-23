package com.mcpg.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpg.core.exception.McpgException;
import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.PolicyEntity;
import com.mcpg.web.repository.PolicyRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-side enforcement for curated policy cards.
 */
@Component
public class GovernanceGuard {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PolicyRepository policyRepository;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public GovernanceGuard(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public void assertInvocationAllowed(String httpMethod, Environment environment, String clientKey) {
        assertWriteAllowed(httpMethod);
        assertEnvironmentAllowed(environment);
        assertRateLimit(clientKey == null ? "anonymous" : clientKey);
    }

    public void assertWriteAllowed(String httpMethod) {
        if (httpMethod == null) return;
        String method = httpMethod.trim().toUpperCase(Locale.ROOT);
        if (!WRITE_METHODS.contains(method)) return;
        Optional<PolicyEntity> policy = policyRepository.findByPolicyKey("write-op.guard");
        if (policy.isEmpty() || !policy.get().isEnabled()) return;
        boolean requireApproval = true;
        try {
            JsonNode cfg = MAPPER.readTree(policy.get().getConfigJson());
            if (cfg.has("requireApproval")) {
                requireApproval = cfg.get("requireApproval").asBoolean(true);
            }
        } catch (Exception ignored) {
        }
        if (requireApproval) {
            throw new McpgException(
                    "Write operation blocked by policy write-op.guard. "
                            + "Disable the policy or set requireApproval=false in its config "
                            + "after an explicit operator decision.");
        }
    }

    public void assertEnvironmentAllowed(Environment environment) {
        if (environment == null || environment == Environment.DEV) {
            return;
        }
        Optional<PolicyEntity> policy = policyRepository.findByPolicyKey("env.isolation");
        if (policy.isEmpty() || !policy.get().isEnabled()) return;
        // Open-source build has no role system yet: block non-DEV when isolation is on.
        throw new McpgException(
                "Environment " + environment + " blocked by policy env.isolation. "
                        + "Disable the policy card or tag the service as DEV until SSO roles land.");
    }

    public void assertRateLimit(String clientKey) {
        Optional<PolicyEntity> policy = policyRepository.findByPolicyKey("rate-limit.global");
        if (policy.isEmpty() || !policy.get().isEnabled()) return;
        int rpm = 600;
        try {
            JsonNode cfg = MAPPER.readTree(policy.get().getConfigJson());
            if (cfg.has("requestsPerMinute")) {
                rpm = Math.max(1, cfg.get("requestsPerMinute").asInt(600));
            }
        } catch (Exception ignored) {
        }
        long minute = System.currentTimeMillis() / 60_000L;
        Window w = windows.compute(clientKey, (k, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new Window(minute, new AtomicInteger(0));
            }
            return existing;
        });
        if (w.count.incrementAndGet() > rpm) {
            throw new McpgException("Rate limit exceeded (" + rpm + "/min) for client " + clientKey);
        }
    }

    private record Window(long minute, AtomicInteger count) {
    }
}
