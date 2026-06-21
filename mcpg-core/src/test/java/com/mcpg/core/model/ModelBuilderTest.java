package com.mcpg.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelBuilderTest {

    @Test
    void discoveredServiceBuildsWithMandatoryFields() {
        DiscoveredService svc = DiscoveredService.builder()
                .name("order-service")
                .sourceType("nacos")
                .baseUrls(List.of("http://10.0.0.1:8080"))
                .environment(Environment.DEV)
                .metadata(Map.of("zone", "az-1"))
                .build();

        assertEquals("order-service", svc.getName());
        assertEquals("nacos", svc.getSourceType());
        assertEquals(Environment.DEV, svc.getEnvironment());
        assertEquals(1, svc.getBaseUrls().size());
        assertEquals("az-1", svc.getMetadata().get("zone"));
    }

    @Test
    void discoveredServiceRejectsNullName() {
        assertThrows(NullPointerException.class, () ->
                DiscoveredService.builder().sourceType("nacos").build());
    }

    @Test
    void parsedSpecHandlesEmptyOperations() {
        ParsedSpec spec = ParsedSpec.builder()
                .title("demo")
                .version("1.0")
                .specVersion("openapi-3.0")
                .baseUrl("http://demo")
                .build();

        assertNotNull(spec.getOperations());
        assertTrue(spec.getOperations().isEmpty());
        assertEquals("openapi-3.0", spec.getSpecVersion());
    }

    @Test
    void toolInvocationResultErrorFactory() {
        ToolInvocationResult result = ToolInvocationResult.error("connect timeout", 1234L);
        assertFalse(result.isSuccess());
        assertEquals("connect timeout", result.getErrorMessage());
        assertEquals(1234L, result.getElapsedMillis());
    }
}
