package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Transport-independent MCP / JSON-RPC dispatcher.
 *
 * <p>Takes one inbound JSON-RPC message and produces an outbound JSON-RPC
 * envelope (or {@link Optional#empty()} for notifications, which by spec
 * never receive a response).</p>
 *
 * <p>The two HTTP transports we support delegate here:</p>
 * <ul>
 *   <li><strong>Streamable HTTP</strong> ({@code POST /mcp}) returns the
 *       result synchronously as the POST body.</li>
 *   <li><strong>HTTP+SSE (legacy)</strong> ({@code POST /mcp/message?sessionId=})
 *       writes the result onto the matching SSE session.</li>
 * </ul>
 */
@Component
public class McpDispatcher {

    private static final Logger log = LoggerFactory.getLogger(McpDispatcher.class);

    private static final String SERVER_NAME = "mcp-gateway-enterprise";
    private static final String SERVER_VERSION = "0.1.0";
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final ObjectMapper mapper = JsonRpc.mapper();
    private final McpToolRegistry registry;

    public McpDispatcher(McpToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * Dispatch a single message. For batches the caller is expected to
     * iterate over the array and stitch the results back together.
     */
    public Optional<JsonNode> dispatch(JsonNode body) {
        if (body == null) {
            return Optional.of(JsonRpc.errorResponse(TextNode.valueOf(""), JsonRpc.PARSE_ERROR, "Empty body"));
        }
        String method = textOrNull(body.get("method"));
        JsonNode id = body.get("id");
        JsonNode params = body.get("params");
        boolean isNotification = id == null || id.isNull();

        if (method == null) {
            if (isNotification) return Optional.empty();
            return Optional.of(JsonRpc.errorResponse(id, JsonRpc.INVALID_REQUEST, "Missing 'method'"));
        }
        log.debug("← {} (notification={})", method, isNotification);

        try {
            JsonNode result = switch (method) {
                case "initialize" -> initializeResult();
                case "ping" -> mapper.createObjectNode();
                case "tools/list" -> registry.listAsJson();
                case "tools/call" -> handleToolCall(params);
                case "notifications/initialized",
                     "notifications/cancelled",
                     "notifications/progress" -> null;
                default -> {
                    if (isNotification) yield null;
                    yield error(id, JsonRpc.METHOD_NOT_FOUND, "Unknown method: " + method);
                }
            };
            if (isNotification) return Optional.empty();
            if (result == null) {
                return Optional.of(JsonRpc.successResponse(id, mapper.nullNode()));
            }
            if (result.has("error") && result.has("jsonrpc")) {
                return Optional.of(result);
            }
            return Optional.of(JsonRpc.successResponse(id, result));
        } catch (Exception e) {
            log.error("Dispatcher error for method {}", method, e);
            if (isNotification) return Optional.empty();
            return Optional.of(JsonRpc.errorResponse(id, JsonRpc.INTERNAL_ERROR,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    /** Returns the {@code result} payload for an {@code initialize} request. */
    public ObjectNode initializeResult() {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);
        ObjectNode capabilities = result.putObject("capabilities");
        ObjectNode tools = capabilities.putObject("tools");
        tools.put("listChanged", true);
        ObjectNode info = result.putObject("serverInfo");
        info.put("name", SERVER_NAME);
        info.put("version", SERVER_VERSION);
        return result;
    }

    private JsonNode handleToolCall(JsonNode params) {
        if (params == null || !params.isObject()) {
            return error(null, JsonRpc.INVALID_PARAMS, "params must be an object");
        }
        String name = textOrNull(params.get("name"));
        if (name == null) {
            return error(null, JsonRpc.INVALID_PARAMS, "params.name required");
        }
        var tool = registry.findByName(name);
        if (tool.isEmpty()) {
            return error(null, JsonRpc.METHOD_NOT_FOUND, "Unknown tool: " + name);
        }
        try {
            McpTool.Result r = tool.get().invoke(params.get("arguments"));
            ObjectNode payload = mapper.createObjectNode();
            payload.set("content", r.content());
            payload.put("isError", r.isError());
            return payload;
        } catch (Exception e) {
            log.error("Tool {} threw", name, e);
            return error(null, JsonRpc.INTERNAL_ERROR,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /**
     * Helper that returns an error sub-envelope (no jsonrpc/id wrapper) so the
     * caller can decide whether to wrap it as a top-level JSON-RPC error.
     */
    private ObjectNode error(JsonNode id, int code, String message) {
        // Build a stand-in object that the dispatch loop recognises as an
        // already-wrapped error response (has jsonrpc + error keys).
        return JsonRpc.errorResponse(id == null ? TextNode.valueOf("") : id, code, message);
    }

    /** Build a JSON-RPC array response from a JSON-RPC array request. */
    public Optional<JsonNode> dispatchBatch(JsonNode array) {
        ArrayNode out = mapper.createArrayNode();
        for (JsonNode item : array) {
            dispatch(item).ifPresent(out::add);
        }
        if (out.size() == 0) return Optional.empty();
        return Optional.of(out);
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
