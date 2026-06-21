package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.core.model.ExposureMode;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ToolRepository;
import com.mcpg.web.service.ExposureSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Materializes the list of tools that the gateway advertises to the LLM.
 *
 * <p>Composition rule depends on the active {@link ExposureMode} resolved
 * fresh from {@link ExposureSettingsService} on every call:</p>
 * <ul>
 *   <li><b>META</b> - only the four meta tools ({@code list_services},
 *       {@code search_api}, {@code get_api_schema}, {@code call_api}).
 *       Recommended default for production gateways aggregating many services
 *       so the LLM tool budget stays sane.</li>
 *   <li><b>HYBRID</b> - meta tools <em>plus</em> any non-deprecated row with
 *       {@code promoted=true}, surfaced as a direct tool.</li>
 *   <li><b>DIRECT_ALL</b> - every non-deprecated row is exposed as a direct
 *       tool; meta tools are dropped because there's nothing to "discover"
 *       behind them. Suitable for demos / single-service setups; not
 *       recommended once the gateway aggregates many services.</li>
 * </ul>
 *
 * <p>The list is rebuilt on every {@code tools/list} call so that operator
 * changes (mode toggles, promotion toggles, new imports, deletions) propagate
 * without restarting the gateway.</p>
 */
@Component
public class McpToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<McpTool> metaTools;
    private final ToolRepository toolRepository;
    private final PromotedToolFactory promotedFactory;
    private final ExposureSettingsService exposureSettings;

    public McpToolRegistry(List<McpTool> metaTools,
                           ToolRepository toolRepository,
                           PromotedToolFactory promotedFactory,
                           ExposureSettingsService exposureSettings) {
        this.metaTools = List.copyOf(metaTools);
        this.toolRepository = toolRepository;
        this.promotedFactory = promotedFactory;
        this.exposureSettings = exposureSettings;
    }

    public List<McpTool> list() {
        ExposureMode mode = exposureSettings.currentMode();
        return switch (mode) {
            case META -> new ArrayList<>(metaTools);
            case HYBRID -> {
                List<McpTool> all = new ArrayList<>(metaTools);
                for (ToolEntity entity : toolRepository.findByPromotedTrue()) {
                    if (entity.isDeprecated()) continue;
                    all.add(promotedFactory.create(entity));
                }
                yield all;
            }
            case DIRECT_ALL -> {
                List<McpTool> all = new ArrayList<>();
                for (ToolEntity entity : toolRepository.findByDeprecatedFalse()) {
                    all.add(promotedFactory.create(entity));
                }
                yield all;
            }
        };
    }

    public Optional<McpTool> findByName(String name) {
        if (name == null) return Optional.empty();
        for (McpTool t : list()) {
            if (name.equals(t.name())) return Optional.of(t);
        }
        // META hides the direct call_api shortcut from "advertised" tools but
        // we still allow invocation by name when the client knows the path -
        // it routes through the call_api meta tool internally. So leave the
        // fallthrough to "not found" here; meta tools always appear in
        // {@link #list()} except in DIRECT_ALL where the LLM is expected to
        // call operations directly anyway.
        return Optional.empty();
    }

    /** Build the JSON payload that {@code tools/list} returns. */
    public JsonNode listAsJson() {
        ArrayNode tools = MAPPER.createArrayNode();
        Map<String, McpTool> seen = new HashMap<>();
        for (McpTool t : list()) {
            if (seen.putIfAbsent(t.name(), t) != null) {
                log.warn("Skipping duplicate tool name in tools/list: {}", t.name());
                continue;
            }
            ObjectNode entry = tools.addObject();
            entry.put("name", t.name());
            entry.put("description", t.description());
            entry.set("inputSchema", t.inputSchema());
        }
        ObjectNode out = MAPPER.createObjectNode();
        out.set("tools", tools);
        return out;
    }
}
