package com.mcpg.web.service;

import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.Environment;
import com.mcpg.core.model.RegistryConfig;
import com.mcpg.core.spi.ServiceRegistryAdapter;
import com.mcpg.web.dto.DiscoveryResultDto;
import com.mcpg.web.dto.ImportResultDto;
import com.mcpg.web.dto.ImportSwaggerRequest;
import com.mcpg.web.dto.ServiceDto;
import com.mcpg.web.entity.RegistryEntity;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.repository.RegistryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link RegistryDiscoveryService}: the registry repository
 * and the import service are mocked so the test runs in memory without
 * Spring context.
 *
 * <p>What we want to verify:</p>
 * <ul>
 *   <li>Each {@code DiscoveredService} triggers an import attempt against the
 *       expected Swagger URL ({@code baseUrl + /v3/api-docs} first).</li>
 *   <li>A failing first candidate falls back to the next suffix.</li>
 *   <li>When every candidate fails, the service is marked SKIPPED but the
 *       overall discovery still completes.</li>
 *   <li>The registry row is updated with status + last service count.</li>
 * </ul>
 */
class RegistryDiscoveryServiceTest {

    private RegistryAdapterRegistry adapters;
    private ServiceImportService importService;
    private RegistryRepository registryRepository;

    @BeforeEach
    void setUp() {
        importService = mock(ServiceImportService.class);
        registryRepository = mock(RegistryRepository.class);
    }

    @Test
    void importsDiscoveredServicesOnFirstWorkingSuffix() {
        RegistryEntity registry = newRegistry();
        adapters = new RegistryAdapterRegistry(List.of(new TestAdapter(List.of(
                DiscoveredService.builder()
                        .name("order-service")
                        .baseUrls(List.of("http://order:8080"))
                        .sourceType("nacos")
                        .environment(Environment.DEV)
                        .build()
        ))));

        // /v3/api-docs succeeds on first call.
        when(importService.importFromUrl(any())).thenAnswer(this::okImport);

        RegistryDiscoveryService discovery = new RegistryDiscoveryService(
                adapters, importService, registryRepository);
        DiscoveryResultDto result = discovery.discover(registry);

        assertThat(result.getDiscovered()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getSkipped()).isZero();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("IMPORTED");
        assertThat(result.getItems().get(0).getBaseUrl()).isEqualTo("http://order:8080/v3/api-docs");
        verify(importService, times(1)).importFromUrl(any());
    }

    @Test
    void marksServiceSkippedWhenAllCandidatesFail() {
        RegistryEntity registry = newRegistry();
        adapters = new RegistryAdapterRegistry(List.of(new TestAdapter(List.of(
                DiscoveredService.builder()
                        .name("legacy-service")
                        .baseUrls(List.of("http://legacy:9000"))
                        .sourceType("nacos")
                        .environment(Environment.DEV)
                        .build()
        ))));

        when(importService.importFromUrl(any()))
                .thenThrow(new RuntimeException("404 Not Found"));

        RegistryDiscoveryService discovery = new RegistryDiscoveryService(
                adapters, importService, registryRepository);
        DiscoveryResultDto result = discovery.discover(registry);

        assertThat(result.getImported()).isZero();
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("SKIPPED");
        assertThat(result.getItems().get(0).getMessage()).contains("404");
    }

    @Test
    void recoversIfSecondSuffixSucceeds() {
        RegistryEntity registry = newRegistry();
        adapters = new RegistryAdapterRegistry(List.of(new TestAdapter(List.of(
                DiscoveredService.builder()
                        .name("springfox-svc")
                        .baseUrls(List.of("http://legacy:9000"))
                        .sourceType("nacos")
                        .environment(Environment.DEV)
                        .build()
        ))));

        AtomicInteger calls = new AtomicInteger();
        when(importService.importFromUrl(any())).thenAnswer(invocation -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                // /v3/api-docs fails on legacy stacks.
                throw new RuntimeException("404");
            }
            return okImport(invocation);
        });

        RegistryDiscoveryService discovery = new RegistryDiscoveryService(
                adapters, importService, registryRepository);
        DiscoveryResultDto result = discovery.discover(registry);

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("IMPORTED");
        assertThat(result.getItems().get(0).getBaseUrl()).contains("/v3/api-docs.yaml");
    }

    @Test
    void emitsEmptyResultForRegistryWithNoServices() {
        RegistryEntity registry = newRegistry();
        adapters = new RegistryAdapterRegistry(List.of(new TestAdapter(List.of())));

        RegistryDiscoveryService discovery = new RegistryDiscoveryService(
                adapters, importService, registryRepository);
        DiscoveryResultDto result = discovery.discover(registry);

        assertThat(result.getDiscovered()).isZero();
        assertThat(result.getImported()).isZero();
        assertThat(result.getSkipped()).isZero();
        assertThat(result.getItems()).isEmpty();
    }

    private RegistryEntity newRegistry() {
        RegistryEntity e = new RegistryEntity();
        e.setId(42L);
        e.setName("nacos-test");
        e.setType("nacos");
        e.setEndpoint("127.0.0.1:8848");
        e.setEnvironment(Environment.DEV);
        return e;
    }

    private ImportResultDto okImport(InvocationOnMock invocation) {
        ImportSwaggerRequest req = invocation.getArgument(0);
        ServiceEntity svc = new ServiceEntity();
        svc.setId(1L);
        svc.setName(req.getName());
        svc.setEnvironment(req.getEnvironment());
        svc.setBaseUrl("http://stub");
        return ImportResultDto.builder()
                .service(ServiceDto.from(svc))
                .toolCount(0)
                .added(0).updated(0).removed(0)
                .specVersion("openapi-3.0")
                .build();
    }

    /** Adapter wrapper that returns a fixed result for testing. */
    private record TestAdapter(List<DiscoveredService> result) implements ServiceRegistryAdapter {
        @Override public String getType() { return "nacos"; }
        @Override public boolean isImplemented() { return true; }
        @Override public boolean testConnection(RegistryConfig config) { return true; }
        @Override public List<DiscoveredService> listServices(RegistryConfig config) {
            return new ArrayList<>(result);
        }
    }
}
