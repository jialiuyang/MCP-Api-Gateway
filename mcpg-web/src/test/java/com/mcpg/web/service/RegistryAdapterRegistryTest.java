package com.mcpg.web.service;

import com.mcpg.core.exception.RegistryAdapterNotFoundException;
import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.RegistryConfig;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RegistryAdapterRegistry} that exercise lookup, case
 * insensitivity, and duplicate detection. The tests use minimal hand-rolled
 * adapter instances so they stay independent of the real Nacos / Eureka
 * adapters' dependencies.
 */
class RegistryAdapterRegistryTest {

    @Test
    void resolvesByTypeCaseInsensitively() {
        ServiceRegistryAdapter a = new FakeAdapter("nacos", true);
        RegistryAdapterRegistry registry = new RegistryAdapterRegistry(List.of(a));

        assertThat(registry.find("nacos")).contains(a);
        assertThat(registry.find("NACOS")).contains(a);
        assertThat(registry.find("Nacos")).contains(a);
    }

    @Test
    void requireThrowsForUnknownType() {
        RegistryAdapterRegistry registry = new RegistryAdapterRegistry(
                List.of(new FakeAdapter("nacos", true)));

        assertThatThrownBy(() -> registry.require("consul"))
                .isInstanceOf(RegistryAdapterNotFoundException.class);
    }

    @Test
    void duplicateAdapterFailsAtConstruction() {
        ServiceRegistryAdapter a = new FakeAdapter("nacos", true);
        ServiceRegistryAdapter b = new FakeAdapter("nacos", false);

        assertThatThrownBy(() -> new RegistryAdapterRegistry(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate ServiceRegistryAdapter");
    }

    @Test
    void allReturnsEveryRegisteredAdapter() {
        ServiceRegistryAdapter a = new FakeAdapter("nacos", true);
        ServiceRegistryAdapter b = new FakeAdapter("eureka", false);
        RegistryAdapterRegistry registry = new RegistryAdapterRegistry(List.of(a, b));

        assertThat(registry.all()).containsExactlyInAnyOrder(a, b);
    }

    private record FakeAdapter(String type, boolean implemented) implements ServiceRegistryAdapter {
        @Override public String getType() { return type; }
        @Override public boolean isImplemented() { return implemented; }
        @Override public boolean testConnection(RegistryConfig config) { return true; }
        @Override public List<DiscoveredService> listServices(RegistryConfig config) {
            return List.of();
        }
    }
}
