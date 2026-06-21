package com.mcpg.web.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.core.model.ToolInvocation;
import com.mcpg.core.model.ToolInvocationResult;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.invoker.HttpToolInvoker;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.repository.ToolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Meta tool that actually executes a backend operation.
 *
 * <p>This is the only tool that performs a side effect against a backend
 * service; therefore it is also the natural anchoring point for the future
 * governance hooks (audit logging in B5, write-tier confirmation in B4).
 * For B2 it simply forwards via the {@link HttpToolInvoker}.</p>
 */
@Component
public class CallApiTool extends AbstractMetaTool {

    private static final Logger log = LoggerFactory.getLogger(CallApiTool.class);

    private final ToolRepository toolRepository;
    private final ServiceRepository serviceRepository;
    private final HttpToolInvoker invoker;

    public CallApiTool(ToolRepository toolRepository,
                        ServiceRepository serviceRepository,
                        HttpToolInvoker invoker) {
        this.toolRepository = toolRepository;
        this.serviceRepository = serviceRepository;
        this.invoker = invoker;
    }

    @Override
    public String name() {
        return "call_api";
    }

    @Override
    public String description() {
        return "Invoke a backend API operation identified by tool_name. "
                + "Arguments must follow the JSON schema returned by get_api_schema "
                + "(typically an object with pathParams / queryParams / headers / body sub-objects).";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectSchema();
        addStringProp(schema, "tool_name",
                "Canonical tool name returned by search_api / get_api_schema.", true);
        addObjectProp(schema, "arguments",
                "Arguments object whose shape matches the tool's input schema.", false);
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
            return textResult("Tool `" + toolName + "` not found. "
                    + "Call list_services / search_api to discover available tools.", true);
        }
        ToolEntity tool = opt.get();
        Optional<ServiceEntity> serviceOpt = serviceRepository.findById(tool.getServiceId());
        if (serviceOpt.isEmpty()) {
            return textResult("Backing service missing for tool `" + toolName + "`.", true);
        }
        ServiceEntity service = serviceOpt.get();

        JsonNode forwardArgs = arguments == null ? null : arguments.get("arguments");
        if (forwardArgs == null || forwardArgs.isNull()) {
            forwardArgs = MAPPER.createObjectNode();
        }

        ToolInvocation invocation = ToolInvocation.builder()
                .serviceName(service.getName())
                .baseUrl(service.getBaseUrl())
                .httpMethod(tool.getHttpMethod())
                .path(tool.getPath())
                .arguments(forwardArgs)
                .environment(service.getEnvironment())
                .build();
        log.info("call_api → tool={} method={} path={}",
                tool.getToolName(), tool.getHttpMethod(), tool.getPath());
        ToolInvocationResult result = invoker.invoke(invocation);
        return textResult(render(tool, result), !result.isSuccess());
    }

    private String render(ToolEntity tool, ToolInvocationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("# `").append(tool.getToolName()).append("`\n");
        sb.append(result.isSuccess() ? "success" : "failure")
                .append(" — HTTP ").append(result.getStatusCode())
                .append(" — ").append(result.getElapsedMillis()).append(" ms\n\n");
        if (result.getErrorMessage() != null) {
            sb.append("**error:** ").append(result.getErrorMessage()).append("\n\n");
        }
        if (result.getBody() != null && !result.getBody().isBlank()) {
            sb.append("**response:**\n\n```\n").append(result.getBody()).append("\n```");
        }
        return sb.toString();
    }
}
