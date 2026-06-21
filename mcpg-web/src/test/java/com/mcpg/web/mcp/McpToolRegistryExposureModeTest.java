package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpg.core.model.ExposureMode;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ToolRepository;
import com.mcpg.web.service.ExposureSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link McpToolRegistry#list()} honors the active
 * {@link ExposureMode}.
 *
 * <p>Wired up with hand-built fakes instead of {@code @SpringBootTest} so we
 * test the composition rule directly without standing up JPA / web layers.</p>
 */
class McpToolRegistryExposureModeTest {

    private ToolRepository toolRepository;
    private ExposureSettingsService exposureSettings;
    private PromotedToolFactory promotedFactory;

    private List<McpTool> metaTools;
    private ToolEntity promotedEntity;
    private ToolEntity plainEntity;
    private ToolEntity deprecatedEntity;

    @BeforeEach
    void setUp() {
        metaTools = List.of(
                stubMcpTool("list_services"),
                stubMcpTool("search_api"),
                stubMcpTool("get_api_schema"),
                stubMcpTool("call_api"));

        promotedEntity = entity("orders_create", false, true);
        plainEntity = entity("orders_get", false, false);
        deprecatedEntity = entity("orders_legacy", true, true);

        toolRepository = mock(ToolRepository.class);
        lenient().when(toolRepository.findByPromotedTrue())
                .thenReturn(List.of(promotedEntity, deprecatedEntity));
        lenient().when(toolRepository.findByDeprecatedFalse())
                .thenReturn(List.of(promotedEntity, plainEntity));

        promotedFactory = mock(PromotedToolFactory.class);
        lenient().when(promotedFactory.create(any())).thenAnswer(inv -> {
            ToolEntity e = inv.getArgument(0, ToolEntity.class);
            return stubMcpTool(e.getToolName());
        });

        exposureSettings = mock(ExposureSettingsService.class);
    }

    @Test
    void metaModeAdvertisesOnlyMetaTools() {
        when(exposureSettings.currentMode()).thenReturn(ExposureMode.META);
        McpToolRegistry registry = new McpToolRegistry(
                metaTools, toolRepository, promotedFactory, exposureSettings);

        List<String> names = registry.list().stream().map(McpTool::name).toList();
        assertThat(names).containsExactlyInAnyOrder(
                "list_services", "search_api", "get_api_schema", "call_api");
    }

    @Test
    void hybridModeAddsPromotedToolsButSkipsDeprecated() {
        when(exposureSettings.currentMode()).thenReturn(ExposureMode.HYBRID);
        McpToolRegistry registry = new McpToolRegistry(
                metaTools, toolRepository, promotedFactory, exposureSettings);

        List<String> names = registry.list().stream().map(McpTool::name).toList();
        assertThat(names)
                .contains("list_services", "search_api", "get_api_schema", "call_api")
                .contains("orders_create")
                .doesNotContain("orders_get", "orders_legacy");
    }

    @Test
    void directAllModeExposesAllNonDeprecatedToolsAndDropsMetaTools() {
        when(exposureSettings.currentMode()).thenReturn(ExposureMode.DIRECT_ALL);
        McpToolRegistry registry = new McpToolRegistry(
                metaTools, toolRepository, promotedFactory, exposureSettings);

        List<String> names = registry.list().stream().map(McpTool::name).toList();
        assertThat(names)
                .containsExactlyInAnyOrder("orders_create", "orders_get")
                .doesNotContain("list_services", "search_api", "get_api_schema", "call_api");
    }

    private ToolEntity entity(String name, boolean deprecated, boolean promoted) {
        ToolEntity e = new ToolEntity();
        e.setToolName(name);
        e.setDeprecated(deprecated);
        e.setPromoted(promoted);
        return e;
    }

    private static McpTool stubMcpTool(String name) {
        return new McpTool() {
            @Override public String name() { return name; }
            @Override public String description() { return name + " desc"; }
            @Override public JsonNode inputSchema() {
                return new ObjectMapper().createObjectNode().put("type", "object");
            }
            @Override public Result invoke(JsonNode arguments) {
                return new Result(new ObjectMapper().createObjectNode(), false);
            }
        };
    }
}
