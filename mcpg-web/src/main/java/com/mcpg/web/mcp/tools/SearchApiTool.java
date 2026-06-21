package com.mcpg.web.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ToolRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Meta tool that finds operations matching a free-text keyword.
 *
 * <p>Output is intentionally lean (name + method + path + short summary) so
 * the LLM can scan many candidates cheaply before drilling into a specific
 * one with {@code get_api_schema}.</p>
 */
@Component
public class SearchApiTool extends AbstractMetaTool {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ToolRepository toolRepository;

    public SearchApiTool(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Override
    public String name() {
        return "search_api";
    }

    @Override
    public String description() {
        return "Search across every imported API operation by keyword. "
                + "Matches against tool name, summary, URL path and tags. "
                + "Returns a short list; call get_api_schema next to inspect a single tool "
                + "(including its full description and JSON schema).";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectSchema();
        addStringProp(schema, "keyword",
                "Free-text keyword. Case-insensitive substring match.", true);
        addStringProp(schema, "service",
                "Optional service name to scope the search to one service.", false);
        addIntegerProp(schema, "limit",
                "Maximum number of results (1-100, default 20).", DEFAULT_LIMIT);
        return schema;
    }

    @Override
    public Result invoke(JsonNode arguments) {
        String keyword = stringArg(arguments, "keyword", null);
        if (keyword == null || keyword.isBlank()) {
            return textResult("`keyword` is required.", true);
        }
        int limit = Math.min(Math.max(1, intArg(arguments, "limit", DEFAULT_LIMIT)), MAX_LIMIT);
        String serviceName = stringArg(arguments, "service", null);

        List<ToolEntity> all = toolRepository.search(keyword, null);
        List<ToolEntity> filtered = all.stream()
                .filter(t -> serviceName == null || t.getToolName().startsWith(serviceName + "__"))
                .filter(t -> !t.isDeprecated())
                .limit(limit)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return textResult("No operations matched `" + keyword + "`.", false);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Matches (").append(filtered.size());
        if (all.size() > filtered.size()) sb.append(" of ").append(all.size());
        sb.append(")\n\n");
        for (ToolEntity t : filtered) {
            sb.append("- `").append(t.getToolName()).append("`")
                    .append(" — **").append(t.getHttpMethod()).append("** ")
                    .append(t.getPath());
            if (t.getSummary() != null && !t.getSummary().isBlank()) {
                sb.append(" — ").append(t.getSummary());
            }
            sb.append('\n');
        }
        sb.append("\nNext step: call `get_api_schema(tool_name=\"<one of the above>\")`");
        return textResult(sb.toString(), false);
    }
}
