package com.mcpg.web.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.repository.ServiceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Meta tool that returns every service currently known to the gateway.
 *
 * <p>This is intentionally narrow: the LLM uses it as a discovery primitive
 * ("what kinds of APIs can I work with?") before drilling down with
 * {@code search_api}. Returning the list as Markdown keeps the response
 * compact and human-readable for in-IDE tool inspectors.</p>
 */
@Component
public class ListServicesTool extends AbstractMetaTool {

    private final ServiceRepository serviceRepository;

    public ListServicesTool(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public String name() {
        return "list_services";
    }

    @Override
    public String description() {
        return "List all services registered with the gateway. "
                + "Each entry shows the service name, environment, and tool count. "
                + "Use this first to discover which services are available.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectSchema();
        addStringProp(schema, "environment",
                "Optional environment filter (DEV, STAGING, PROD). Returns all envs when omitted.",
                false);
        return schema;
    }

    @Override
    public Result invoke(JsonNode arguments) {
        Environment env = parseEnvironment(stringArg(arguments, "environment", null));
        List<ServiceEntity> services = serviceRepository.search(null, env);
        if (services.isEmpty()) {
            return textResult("No services registered.\n"
                    + "Use the UI ('Services' page) to import a Swagger / OpenAPI URL.",
                    false);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Services (").append(services.size()).append(")\n\n");
        for (ServiceEntity s : services) {
            sb.append("- **").append(s.getName()).append("** (")
                    .append(s.getEnvironment()).append(")")
                    .append(" — ").append(s.getToolCount()).append(" tools");
            if (s.getDisplayName() != null && !s.getDisplayName().isBlank()
                    && !s.getDisplayName().equals(s.getName())) {
                sb.append(" — ").append(s.getDisplayName());
            }
            sb.append('\n');
            if (s.getBaseUrl() != null) sb.append("  baseUrl: `").append(s.getBaseUrl()).append("`\n");
            if (s.getStatus() != ServiceEntity.Status.ACTIVE) {
                sb.append("  status: ").append(s.getStatus()).append('\n');
            }
        }
        return textResult(sb.toString(), false);
    }

    private Environment parseEnvironment(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Environment.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
