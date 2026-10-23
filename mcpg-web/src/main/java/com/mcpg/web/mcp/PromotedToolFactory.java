package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.core.exception.McpgException;
import com.mcpg.core.model.ToolInvocation;
import com.mcpg.core.model.ToolInvocationResult;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.invoker.HttpToolInvoker;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.security.GovernanceGuard;
import com.mcpg.web.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Builds {@link McpTool} adapters for individual promoted operations.
 *
 * <p>Each promoted row in {@code mcpg_tool} becomes a first-class MCP tool;
 * the LLM sees the same {@code inputSchema} that was stored at import time
 * and the gateway routes the {@code tools/call} through the
 * {@link HttpToolInvoker}.</p>
 */
@Component
public class PromotedToolFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ServiceRepository serviceRepository;
    private final HttpToolInvoker httpToolInvoker;
    private final GovernanceGuard governanceGuard;
    private final AuditService auditService;

    public PromotedToolFactory(ServiceRepository serviceRepository,
                                HttpToolInvoker httpToolInvoker,
                                GovernanceGuard governanceGuard,
                                AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.httpToolInvoker = httpToolInvoker;
        this.governanceGuard = governanceGuard;
        this.auditService = auditService;
    }

    public McpTool create(ToolEntity entity) {
        return new Promoted(entity);
    }

    final class Promoted implements McpTool {
        private final ToolEntity entity;

        Promoted(ToolEntity entity) {
            this.entity = entity;
        }

        @Override
        public String name() {
            return entity.getToolName();
        }

        @Override
        public String description() {
            StringBuilder sb = new StringBuilder();
            if (entity.getSummary() != null) sb.append(entity.getSummary());
            if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(entity.getDescription());
            }
            if (sb.length() == 0) {
                sb.append(entity.getHttpMethod()).append(' ').append(entity.getPath());
            }
            return sb.toString();
        }

        @Override
        public JsonNode inputSchema() {
            try {
                JsonNode parsed = entity.getInputSchemaJson() == null
                        ? null : MAPPER.readTree(entity.getInputSchemaJson());
                if (parsed != null && parsed.isObject()) return parsed;
            } catch (Exception ignored) {
            }
            return MAPPER.createObjectNode().put("type", "object");
        }

        @Override
        public Result invoke(JsonNode arguments) throws Exception {
            ServiceEntity service = serviceRepository.findById(entity.getServiceId())
                    .orElseThrow(() -> new McpgException(
                            "Backing service not found for promoted tool " + entity.getToolName()));
            String actor = currentActor();
            governanceGuard.assertInvocationAllowed(
                    entity.getHttpMethod(), service.getEnvironment(), actor);
            ToolInvocation invocation = ToolInvocation.builder()
                    .serviceName(service.getName())
                    .baseUrl(service.getBaseUrl())
                    .httpMethod(entity.getHttpMethod())
                    .path(entity.getPath())
                    .arguments(arguments == null ? MAPPER.createObjectNode() : arguments)
                    .environment(service.getEnvironment())
                    .build();
            ToolInvocationResult result = httpToolInvoker.invoke(invocation);
            auditService.record(actor, "mcp.promoted_call", "tool", entity.getToolName(),
                    result.isSuccess() ? "SUCCESS" : "FAILURE",
                    result.getStatusCode(),
                    result.getElapsedMillis(),
                    result.isSuccess() ? "OK" : result.getErrorMessage());
            return buildResult(result);
        }

        private static String currentActor() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                return auth.getName();
            }
            return "mcp-client";
        }

        private Result buildResult(ToolInvocationResult result) {
            ArrayNode content = MAPPER.createArrayNode();
            ObjectNode block = content.addObject();
            block.put("type", "text");
            block.put("text", renderText(result));
            return new Result(content, !result.isSuccess());
        }

        private String renderText(ToolInvocationResult result) {
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP ").append(result.getStatusCode())
                    .append(" (").append(result.getElapsedMillis()).append(" ms)\n");
            if (result.getErrorMessage() != null) {
                sb.append("error: ").append(result.getErrorMessage()).append('\n');
            }
            if (result.getBody() != null && !result.getBody().isBlank()) {
                sb.append(result.getBody());
            }
            return sb.toString();
        }
    }
}
