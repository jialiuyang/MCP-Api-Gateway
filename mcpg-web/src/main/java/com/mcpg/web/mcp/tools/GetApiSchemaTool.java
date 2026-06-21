package com.mcpg.web.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.repository.ToolRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Meta tool that returns the full JSON Schema for a single operation.
 *
 * <p>The output combines a human-readable header with the raw input schema
 * (and output schema, when available). LLMs use it to learn the exact
 * argument shape before invoking {@code call_api}.</p>
 */
@Component
public class GetApiSchemaTool extends AbstractMetaTool {

    private final ToolRepository toolRepository;
    private final ServiceRepository serviceRepository;

    public GetApiSchemaTool(ToolRepository toolRepository, ServiceRepository serviceRepository) {
        this.toolRepository = toolRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public String name() {
        return "get_api_schema";
    }

    @Override
    public String description() {
        return "Return the full JSON Schema describing a single API operation. "
                + "Use this after search_api to learn how to shape the arguments for call_api.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectSchema();
        addStringProp(schema, "tool_name",
                "The canonical tool name (e.g. order_service__getOrder). "
                        + "Obtain it via search_api.",
                true);
        return schema;
    }

    @Override
    public Result invoke(JsonNode arguments) {
        String toolName = stringArg(arguments, "tool_name", null);
        if (toolName == null || toolName.isBlank()) {
            return textResult("`tool_name` is required.", true);
        }
        Optional<ToolEntity> opt = toolRepository.findByToolName(toolName);
        if (opt.isEmpty()) {
            return textResult("Tool `" + toolName + "` not found.", true);
        }
        ToolEntity tool = opt.get();
        ServiceEntity service = serviceRepository.findById(tool.getServiceId()).orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("# `").append(tool.getToolName()).append("`\n\n");
        sb.append("**").append(tool.getHttpMethod()).append("** `").append(tool.getPath()).append("`  \n");
        if (service != null) {
            sb.append("service: `").append(service.getName()).append("`")
                    .append(" (").append(service.getEnvironment()).append(")")
                    .append("  \n")
                    .append("baseUrl: `").append(service.getBaseUrl()).append("`  \n");
        }
        sb.append("risk: `").append(tool.getRiskLevel()).append('`');
        if (tool.isDeprecated()) sb.append(" — **DEPRECATED**");
        sb.append("\n\n");

        if (tool.getSummary() != null) sb.append("**Summary:** ").append(tool.getSummary()).append("\n\n");
        if (tool.getDescription() != null && !tool.getDescription().isBlank()) {
            sb.append("**Description:**\n").append(tool.getDescription()).append("\n\n");
        }

        sb.append("**Input schema:**\n\n```json\n")
                .append(tool.getInputSchemaJson() == null ? "{}" : tool.getInputSchemaJson())
                .append("\n```\n");

        if (tool.getOutputSchemaJson() != null && !tool.getOutputSchemaJson().isBlank()) {
            sb.append("\n**Output schema:**\n\n```json\n")
                    .append(tool.getOutputSchemaJson()).append("\n```\n");
        }

        sb.append("\nNext step: ")
                .append("`call_api(tool_name=\"").append(tool.getToolName())
                .append("\", arguments={...})`");
        return textResult(sb.toString(), false);
    }
}
