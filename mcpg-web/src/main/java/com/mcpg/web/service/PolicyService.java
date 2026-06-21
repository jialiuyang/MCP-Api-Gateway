package com.mcpg.web.service;

import com.mcpg.web.dto.PolicyDto;
import com.mcpg.web.dto.UpdatePolicyRequest;
import com.mcpg.web.entity.PolicyEntity;
import com.mcpg.web.repository.PolicyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Owns the curated governance policy cards.
 *
 * <p>The set of cards is opinionated and bootstrap-driven: every supported
 * policy key is materialized on first boot so the UI never sees an empty
 * state. Operators can toggle and tune cards, but adding a brand-new policy
 * key remains a code change - this is intentional, because each card maps
 * to a server-side enforcement hook.</p>
 */
@Service
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository repository;

    public PolicyService(PolicyRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void bootstrap() {
        seedIfMissing("write-op.guard", "Write operation guard", "governance",
                PolicyEntity.Severity.HIGH, true,
                """
                {"requireApproval":true,"approverGroups":["platform-leads"]}
                """,
                "Require human approval before any MCP-driven write operation (POST/PUT/PATCH/DELETE).");
        seedIfMissing("rate-limit.global", "Global rate limit", "traffic",
                PolicyEntity.Severity.MEDIUM, true,
                """
                {"requestsPerMinute":600,"perClient":true}
                """,
                "Cap MCP traffic per client to prevent runaway agents from overloading backends.");
        seedIfMissing("env.isolation", "Environment isolation", "governance",
                PolicyEntity.Severity.HIGH, true,
                """
                {"defaultEnv":"DEV","allowProdFor":["platform-leads"]}
                """,
                "Restrict production services to allow-listed roles; default new clients to DEV.");
        seedIfMissing("audit.retention", "Audit log retention", "compliance",
                PolicyEntity.Severity.LOW, true,
                """
                {"days":90,"archiveColdStorage":false}
                """,
                "Keep the audit trail for at least 90 days; longer windows pending compliance sign-off.");
        seedIfMissing("schema.redaction", "PII redaction", "compliance",
                PolicyEntity.Severity.HIGH, false,
                """
                {"fieldPatterns":["*.email","*.phone","*.idCard"]}
                """,
                "Strip well-known PII fields from MCP tool outputs before returning to the LLM.");
        seedIfMissing("sso.required", "SSO required", "auth",
                PolicyEntity.Severity.CRITICAL, false,
                """
                {"provider":"oidc","issuer":"https://sso.example.com"}
                """,
                "Require enterprise SSO for the console; disabled in the open-source demo profile.");
    }

    @Transactional(readOnly = true)
    public List<PolicyDto> list() {
        return repository.findAll().stream()
                .sorted(Comparator
                        .comparing((PolicyEntity p) -> p.getCategory())
                        .thenComparing(PolicyEntity::getPolicyKey))
                .map(PolicyDto::from)
                .toList();
    }

    @Transactional
    public PolicyDto update(Long id, UpdatePolicyRequest req) {
        PolicyEntity row = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy " + id + " not found"));
        boolean changed = false;
        if (req.getEnabled() != null && req.getEnabled() != row.isEnabled()) {
            row.setEnabled(req.getEnabled());
            changed = true;
        }
        if (req.getSeverity() != null && req.getSeverity() != row.getSeverity()) {
            row.setSeverity(req.getSeverity());
            changed = true;
        }
        if (req.getConfigJson() != null && !req.getConfigJson().equals(row.getConfigJson())) {
            row.setConfigJson(req.getConfigJson());
            changed = true;
        }
        if (changed) {
            row.setUpdatedAt(Instant.now());
            repository.save(row);
            log.info("Policy {} updated (enabled={}, severity={})",
                    row.getPolicyKey(), row.isEnabled(), row.getSeverity());
        }
        return PolicyDto.from(row);
    }

    private void seedIfMissing(String key, String name, String category,
                                PolicyEntity.Severity severity, boolean enabled,
                                String configJson, String description) {
        if (repository.findByPolicyKey(key).isPresent()) return;
        PolicyEntity row = new PolicyEntity();
        row.setPolicyKey(key);
        row.setName(name);
        row.setCategory(category);
        row.setSeverity(severity);
        row.setEnabled(enabled);
        row.setConfigJson(configJson.trim());
        row.setDescription(description);
        row.setUpdatedAt(Instant.now());
        repository.save(row);
        log.info("Seeded policy card: {}", key);
    }
}
