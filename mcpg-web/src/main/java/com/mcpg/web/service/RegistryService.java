package com.mcpg.web.service;

import com.mcpg.core.exception.McpgException;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import com.mcpg.web.dto.CreateRegistryRequest;
import com.mcpg.web.dto.RegistryDto;
import com.mcpg.web.dto.RegistryTypeDto;
import com.mcpg.web.dto.TestConnectionResult;
import com.mcpg.web.dto.UpdateRegistryRequest;
import com.mcpg.web.entity.RegistryEntity;
import com.mcpg.web.repository.RegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRUD + test + lookup operations for {@link RegistryEntity}.
 *
 * <p>The service intentionally lives separately from
 * {@link RegistryDiscoveryService} so the controller layer can compose a
 * "save and then discover" flow without the persistence calls being entangled
 * with the (potentially slow) registry round-trips.</p>
 */
@Service
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private final RegistryRepository repository;
    private final RegistryAdapterRegistry adapters;

    public RegistryService(RegistryRepository repository, RegistryAdapterRegistry adapters) {
        this.repository = repository;
        this.adapters = adapters;
    }

    public List<RegistryDto> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(RegistryEntity::getName))
                .map(RegistryDto::from)
                .toList();
    }

    public RegistryDto get(Long id) {
        return RegistryDto.from(require(id));
    }

    @Transactional
    public RegistryDto create(CreateRegistryRequest req) {
        repository.findByName(req.getName()).ifPresent(existing -> {
            throw new McpgException("Registry name '" + req.getName() + "' already exists.");
        });
        assertAdapterImplemented(req.getType());

        RegistryEntity e = new RegistryEntity();
        e.setName(req.getName());
        e.setDisplayName(req.getDisplayName());
        e.setType(req.getType().toLowerCase());
        e.setEndpoint(req.getEndpoint());
        e.setUsername(blankToNull(req.getUsername()));
        e.setPassword(blankToNull(req.getPassword()));
        if (req.getEnvironment() != null) e.setEnvironment(req.getEnvironment());
        e.setNamespace(blankToNull(req.getNamespace()));
        e.setGroupName(blankToNull(req.getGroupName()));
        e.setExtra(blankToNull(req.getExtra()));
        if (req.getEnabled() != null) e.setEnabled(req.getEnabled());

        e = repository.save(e);
        log.info("Registry '{}' ({}) created at {}", e.getName(), e.getType(), e.getEndpoint());
        return RegistryDto.from(e);
    }

    @Transactional
    public RegistryDto update(Long id, UpdateRegistryRequest req) {
        RegistryEntity e = require(id);
        if (req.getDisplayName() != null) e.setDisplayName(req.getDisplayName());
        if (req.getEndpoint() != null) e.setEndpoint(req.getEndpoint());
        if (req.getUsername() != null) e.setUsername(blankToNull(req.getUsername()));
        // Special password semantics: null = keep, empty = clear, otherwise replace.
        if (req.getPassword() != null) {
            e.setPassword(req.getPassword().isEmpty() ? null : req.getPassword());
        }
        if (req.getEnvironment() != null) e.setEnvironment(req.getEnvironment());
        if (req.getNamespace() != null) e.setNamespace(blankToNull(req.getNamespace()));
        if (req.getGroupName() != null) e.setGroupName(blankToNull(req.getGroupName()));
        if (req.getExtra() != null) e.setExtra(blankToNull(req.getExtra()));
        if (req.getEnabled() != null) e.setEnabled(req.getEnabled());

        e = repository.save(e);
        log.info("Registry '{}' updated", e.getName());
        return RegistryDto.from(e);
    }

    @Transactional
    public void delete(Long id) {
        RegistryEntity e = require(id);
        repository.delete(e);
        log.info("Registry '{}' deleted", e.getName());
    }

    public TestConnectionResult test(Long id) {
        RegistryEntity entity = require(id);
        ServiceRegistryAdapter adapter = adapters.require(entity.getType());
        long start = System.currentTimeMillis();
        try {
            adapter.testConnection(toConfig(entity));
            entity.setStatus(RegistryEntity.Status.OK);
            entity.setLastError(null);
            entity.setLastSyncedAt(Instant.now());
            repository.save(entity);
            return TestConnectionResult.builder()
                    .ok(true)
                    .message(null)
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception ex) {
            entity.setStatus(RegistryEntity.Status.ERROR);
            entity.setLastError(ex.getMessage());
            entity.setLastSyncedAt(Instant.now());
            repository.save(entity);
            return TestConnectionResult.builder()
                    .ok(false)
                    .message(ex.getMessage())
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    public List<RegistryTypeDto> listTypes() {
        return adapters.all().stream()
                .map(a -> RegistryTypeDto.builder()
                        .type(a.getType())
                        .label(humanizeType(a.getType()))
                        .implemented(a.isImplemented())
                        .build())
                .sorted(Comparator
                        .comparing(RegistryTypeDto::isImplemented).reversed()
                        .thenComparing(RegistryTypeDto::getType))
                .toList();
    }

    private void assertAdapterImplemented(String type) {
        ServiceRegistryAdapter adapter = adapters.find(type)
                .orElseThrow(() -> new McpgException(
                        "Unknown registry type '" + type + "'. Known: "
                                + adapters.all().stream()
                                .map(ServiceRegistryAdapter::getType)
                                .collect(Collectors.joining(", "))));
        if (!adapter.isImplemented()) {
            throw new McpgException(
                    "Registry type '" + type + "' is not yet implemented "
                            + "(advertised as a roadmap placeholder).");
        }
    }

    private com.mcpg.core.model.RegistryConfig toConfig(RegistryEntity registry) {
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
        return new com.mcpg.core.model.RegistryConfig(
                registry.getName(),
                registry.getType(),
                registry.getEndpoint(),
                registry.getUsername(),
                registry.getPassword(),
                extra);
    }

    private RegistryEntity require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new McpgException("Registry " + id + " not found"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String humanizeType(String type) {
        if (type == null || type.isEmpty()) return type;
        return switch (type.toLowerCase()) {
            case "nacos" -> "Nacos";
            case "eureka" -> "Eureka";
            case "consul" -> "Consul";
            case "polaris" -> "Polaris";
            case "k8s", "kubernetes" -> "Kubernetes";
            case "zookeeper" -> "Zookeeper";
            default -> Character.toUpperCase(type.charAt(0)) + type.substring(1);
        };
    }
}
