package com.mcpg.web.service;

import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.RegistryConfig;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import com.mcpg.web.dto.DiscoveryResultDto;
import com.mcpg.web.dto.ImportSwaggerRequest;
import com.mcpg.web.entity.RegistryEntity;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.repository.RegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a discovery pass for one {@link RegistryEntity}.
 *
 * <p>The flow is:</p>
 * <ol>
 *   <li>Resolve the adapter via {@link RegistryAdapterRegistry}.</li>
 *   <li>Convert the entity to a {@link RegistryConfig} (transient, never
 *       persisted) and call {@link ServiceRegistryAdapter#listServices}.</li>
 *   <li>For each {@link DiscoveredService}, walk the candidate base URLs and
 *       well-known Swagger suffixes, calling {@link ServiceImportService}
 *       on the first URL that produces a parsable spec.</li>
 *   <li>Update the registry row with status, last error and timestamp.</li>
 * </ol>
 *
 * <p>The service is intentionally non-fatal at the per-service level: one
 * bad endpoint should not abort discovery for the whole registry.</p>
 */
@Service
public class RegistryDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryDiscoveryService.class);

    /**
     * Well-known suffixes the gateway probes when a service does not expose
     * its Swagger path through metadata. Ordered most-common first to keep
     * the discovery latency low on the happy path.
     */
    private static final List<String> SWAGGER_SUFFIXES = List.of(
            "/v3/api-docs",
            "/v3/api-docs.yaml",
            "/v2/api-docs",
            "/openapi.json",
            "/openapi.yaml",
            "/swagger.json"
    );

    private final RegistryAdapterRegistry adapters;
    private final ServiceImportService importService;
    private final RegistryRepository registryRepository;

    public RegistryDiscoveryService(RegistryAdapterRegistry adapters,
                                    ServiceImportService importService,
                                    RegistryRepository registryRepository) {
        this.adapters = adapters;
        this.importService = importService;
        this.registryRepository = registryRepository;
    }

    /**
     * Discover services for the given registry id. The method is intentionally
     * not transactional: each successful per-service import already runs in
     * its own transaction, and we want the registry row updates to land even
     * if half the services failed.
     */
    public DiscoveryResultDto discover(Long registryId) {
        RegistryEntity registry = registryRepository.findById(registryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Registry " + registryId + " not found"));
        return discover(registry);
    }

    public DiscoveryResultDto discover(RegistryEntity registry) {
        ServiceRegistryAdapter adapter = adapters.require(registry.getType());
        if (!adapter.isImplemented()) {
            throw new UnsupportedOperationException(
                    "Registry adapter '" + registry.getType() + "' is not implemented yet.");
        }

        long start = System.currentTimeMillis();
        List<DiscoveredService> discovered;
        try {
            discovered = adapter.listServices(toConfig(registry));
        } catch (Exception e) {
            markFailure(registry, e.getMessage());
            throw e;
        }

        List<DiscoveryResultDto.DiscoveredServiceOutcome> outcomes = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        for (DiscoveredService svc : discovered) {
            DiscoveryResultDto.DiscoveredServiceOutcome outcome = importOne(registry, svc);
            outcomes.add(outcome);
            if ("IMPORTED".equals(outcome.getStatus()) || "UPDATED".equals(outcome.getStatus())) {
                imported++;
            } else {
                skipped++;
            }
        }

        markSuccess(registry, discovered.size());
        long elapsed = System.currentTimeMillis() - start;
        log.info("Discovery for registry '{}' completed in {} ms: {} discovered, {} imported, {} skipped",
                registry.getName(), elapsed, discovered.size(), imported, skipped);

        return DiscoveryResultDto.builder()
                .registryId(registry.getId())
                .registryName(registry.getName())
                .discovered(discovered.size())
                .imported(imported)
                .skipped(skipped)
                .items(outcomes)
                .build();
    }

    private DiscoveryResultDto.DiscoveredServiceOutcome importOne(
            RegistryEntity registry, DiscoveredService svc) {
        List<String> candidates = buildSpecUrlCandidates(svc.getBaseUrls());
        if (candidates.isEmpty()) {
            return DiscoveryResultDto.DiscoveredServiceOutcome.builder()
                    .name(svc.getName())
                    .baseUrl(null)
                    .status("SKIPPED")
                    .message("no usable base URL")
                    .build();
        }

        Throwable lastError = null;
        for (String specUrl : candidates) {
            try {
                ImportSwaggerRequest req = new ImportSwaggerRequest();
                req.setName(svc.getName());
                req.setUrl(specUrl);
                req.setEnvironment(registry.getEnvironment());
                req.setSourceType(toServiceSourceType(registry.getType()));
                req.setSourceRef(registry.getName() + ":" + svc.getName());

                importService.importFromUrl(req);
                return DiscoveryResultDto.DiscoveredServiceOutcome.builder()
                        .name(svc.getName())
                        .baseUrl(specUrl)
                        .status("IMPORTED")
                        .message(null)
                        .build();
            } catch (Exception e) {
                lastError = e;
                log.debug("Spec candidate {} failed for service {}: {}",
                        specUrl, svc.getName(), e.getMessage());
            }
        }

        return DiscoveryResultDto.DiscoveredServiceOutcome.builder()
                .name(svc.getName())
                .baseUrl(svc.getBaseUrls().isEmpty() ? null : svc.getBaseUrls().get(0))
                .status("SKIPPED")
                .message(lastError == null ? "no spec found" : lastError.getMessage())
                .build();
    }

    /**
     * Expand each healthy base URL with the well-known Swagger suffixes,
     * preserving order so the most-likely candidate is tried first. Duplicates
     * are filtered to avoid hammering the same endpoint twice.
     */
    private List<String> buildSpecUrlCandidates(List<String> baseUrls) {
        if (baseUrls == null || baseUrls.isEmpty()) return List.of();
        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
        for (String base : baseUrls) {
            String trimmed = stripTrailingSlash(base);
            for (String suffix : SWAGGER_SUFFIXES) {
                seen.putIfAbsent(trimmed + suffix, Boolean.TRUE);
            }
        }
        return new ArrayList<>(seen.keySet());
    }

    private RegistryConfig toConfig(RegistryEntity registry) {
        Map<String, String> extra = new HashMap<>();
        if (registry.getNamespace() != null) extra.put("namespace", registry.getNamespace());
        if (registry.getGroupName() != null) extra.put("group", registry.getGroupName());
        if (registry.getEnvironment() != null) {
            extra.put("environment", registry.getEnvironment().name());
        }
        if (registry.getExtra() != null && !registry.getExtra().isBlank()) {
            for (String pair : registry.getExtra().split("[;\\n]")) {
                int eq = pair.indexOf('=');
                if (eq > 0) extra.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return new RegistryConfig(
                registry.getName(),
                registry.getType(),
                registry.getEndpoint(),
                registry.getUsername(),
                registry.getPassword(),
                extra);
    }

    private ServiceEntity.SourceType toServiceSourceType(String type) {
        if (type == null) return ServiceEntity.SourceType.MANUAL;
        return switch (type.toLowerCase()) {
            case "nacos" -> ServiceEntity.SourceType.NACOS;
            case "eureka" -> ServiceEntity.SourceType.EUREKA;
            case "consul" -> ServiceEntity.SourceType.CONSUL;
            case "polaris" -> ServiceEntity.SourceType.POLARIS;
            case "k8s", "kubernetes" -> ServiceEntity.SourceType.K8S;
            case "zookeeper" -> ServiceEntity.SourceType.ZOOKEEPER;
            default -> ServiceEntity.SourceType.MANUAL;
        };
    }

    @Transactional
    protected void markSuccess(RegistryEntity registry, int serviceCount) {
        registry.setStatus(RegistryEntity.Status.OK);
        registry.setLastError(null);
        registry.setLastServiceCount(serviceCount);
        registry.setLastSyncedAt(Instant.now());
        registryRepository.save(registry);
    }

    @Transactional
    protected void markFailure(RegistryEntity registry, String error) {
        registry.setStatus(RegistryEntity.Status.ERROR);
        registry.setLastError(error);
        registry.setLastSyncedAt(Instant.now());
        registryRepository.save(registry);
    }

    private String stripTrailingSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}
