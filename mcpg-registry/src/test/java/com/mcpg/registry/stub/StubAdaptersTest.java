package com.mcpg.registry.stub;

import com.mcpg.core.model.RegistryConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StubAdaptersTest {

    @Test
    void allStubAdaptersReportNotImplemented() {
        Stream.of(
                new ConsulRegistryAdapter(),
                new PolarisRegistryAdapter(),
                new KubernetesRegistryAdapter(),
                new ZookeeperRegistryAdapter()
        ).forEach(adapter -> {
            assertFalse(adapter.isImplemented(),
                    () -> adapter.getType() + " should be marked as not implemented");
        });
    }

    @Test
    void stubAdapterThrowsOnUse() {
        ConsulRegistryAdapter adapter = new ConsulRegistryAdapter();
        RegistryConfig config = new RegistryConfig(
                "demo", "consul", "http://localhost:8500", null, null, Map.of());
        assertThrows(UnsupportedOperationException.class, () -> adapter.testConnection(config));
        assertThrows(UnsupportedOperationException.class, () -> adapter.listServices(config));
    }
}
