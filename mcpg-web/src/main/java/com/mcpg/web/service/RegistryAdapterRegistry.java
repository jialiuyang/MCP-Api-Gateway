package com.mcpg.web.service;

import com.mcpg.core.exception.RegistryAdapterNotFoundException;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central lookup of {@link ServiceRegistryAdapter} beans by type identifier.
 *
 * <p>All adapter implementations (real + stubs) are auto-wired by Spring at
 * startup and indexed in this component. The UI and the discovery service
 * always go through this registry so a new adapter only needs to be added
 * to the Spring component scan, never to a switch statement.</p>
 */
@Component
public class RegistryAdapterRegistry {

    private final Map<String, ServiceRegistryAdapter> byType;

    public RegistryAdapterRegistry(List<ServiceRegistryAdapter> adapters) {
        this.byType = adapters.stream().collect(Collectors.toUnmodifiableMap(
                a -> a.getType().toLowerCase(Locale.ROOT),
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException(
                            "Duplicate ServiceRegistryAdapter for type "
                                    + a.getType() + ": " + a + " vs " + b);
                }));
    }

    /** Resolve an adapter by type identifier. Case insensitive. */
    public ServiceRegistryAdapter require(String type) {
        return find(type).orElseThrow(() ->
                new RegistryAdapterNotFoundException("No registry adapter for type '" + type + "'"));
    }

    public Optional<ServiceRegistryAdapter> find(String type) {
        if (type == null) return Optional.empty();
        return Optional.ofNullable(byType.get(type.toLowerCase(Locale.ROOT)));
    }

    /** All adapters known at boot time. */
    public Collection<ServiceRegistryAdapter> all() {
        return byType.values();
    }
}
