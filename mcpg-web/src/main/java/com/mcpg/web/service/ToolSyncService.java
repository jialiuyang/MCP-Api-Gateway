package com.mcpg.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpg.core.model.ParsedOperation;
import com.mcpg.core.model.ParsedSpec;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.repository.ToolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronises the {@code mcpg_tool} rows for a service with the latest
 * {@link ParsedSpec}.
 *
 * <p>The algorithm is "merge by stable tool_name":</p>
 *
 * <ol>
 *   <li>Load all existing tools for the service.</li>
 *   <li>For each parsed operation, look up the existing row by tool name.
 *       <ul>
 *         <li>If found, update mutable fields in place. Preserves the
 *             {@code promoted} flag the operator set previously.</li>
 *         <li>If not found, insert.</li>
 *       </ul>
 *   </li>
 *   <li>Any existing rows whose tool name did not appear in the new spec are
 *       deleted (they correspond to operations removed upstream).</li>
 * </ol>
 *
 * <p>This guarantees idempotency: re-importing the same spec is a no-op,
 * and the {@code promoted} bit is never silently lost.</p>
 */
@Service
public class ToolSyncService {

    private static final Logger log = LoggerFactory.getLogger(ToolSyncService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolRepository toolRepository;

    public ToolSyncService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Transactional
    public Stats sync(ServiceEntity service, ParsedSpec spec) {
        Map<String, ToolEntity> existing = new HashMap<>();
        for (ToolEntity t : toolRepository.findByServiceId(service.getId())) {
            existing.put(t.getToolName(), t);
        }

        int added = 0, updated = 0;
        Map<String, ToolEntity> next = new LinkedHashMap<>();
        List<ParsedOperation> operations = spec.getOperations() == null
                ? List.of() : spec.getOperations();
        for (ParsedOperation op : operations) {
            String toolName = ToolNaming.toolName(service.getName(), op.getOperationId());
            ToolEntity entity = existing.remove(toolName);
            boolean isNew = entity == null;
            if (isNew) {
                entity = new ToolEntity();
                entity.setServiceId(service.getId());
                entity.setToolName(toolName);
                added++;
            } else {
                updated++;
            }
            entity.setOperationId(op.getOperationId());
            entity.setHttpMethod(op.getHttpMethod());
            entity.setPath(op.getPath());
            entity.setSummary(op.getSummary());
            entity.setDescription(op.getDescription());
            entity.setTags(joinTags(op));
            entity.setInputSchemaJson(stringify(op.getInputSchema()));
            entity.setOutputSchemaJson(stringify(op.getOutputSchema()));
            entity.setRiskLevel(ToolNaming.inferRiskLevel(op.getHttpMethod(), op.getOperationId(), op.getPath()));
            entity.setDeprecated(op.isDeprecated());
            next.put(toolName, entity);
        }

        if (!next.isEmpty()) {
            toolRepository.saveAll(next.values());
        }

        int removed = existing.size();
        if (removed > 0) {
            toolRepository.deleteAll(existing.values());
            log.info("Removed {} stale tool rows for service {}", removed, service.getName());
        }

        return new Stats(added, updated, removed, next.size());
    }

    private String joinTags(ParsedOperation op) {
        if (op.getTags() == null || op.getTags().isEmpty()) return null;
        List<String> values = new ArrayList<>(op.getTags().values());
        return String.join(",", values);
    }

    private String stringify(JsonNode node) {
        if (node == null) return null;
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Failed to serialize schema, falling back to toString: {}", e.getMessage());
            return node.toString();
        }
    }

    public record Stats(int added, int updated, int removed, int total) {
    }
}
