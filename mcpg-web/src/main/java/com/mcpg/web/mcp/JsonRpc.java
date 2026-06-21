package com.mcpg.web.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Minimal JSON-RPC 2.0 message helpers used by the MCP transport layer.
 *
 * <p>Kept as a single utility class with nested {@link Request}/{@link Response}
 * records to keep the call sites compact - the MCP protocol uses JSON-RPC as
 * a thin wire envelope and we never need anything beyond the four
 * envelope types ({@code request}, {@code response}, {@code notification},
 * {@code error}).</p>
 */
public final class JsonRpc {

    public static final String VERSION = "2.0";

    /** Standard JSON-RPC error codes (subset used by MCP). */
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonRpc() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Parsed JSON-RPC request / notification. {@code id} is null for
     * notifications (no response expected) and non-null for requests.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(String jsonrpc, JsonNode id, String method, JsonNode params) {
        public boolean isNotification() {
            return id == null || id.isNull();
        }
    }

    /** Build a success response envelope. */
    public static ObjectNode successResponse(JsonNode id, JsonNode result) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("jsonrpc", VERSION);
        if (id != null) out.set("id", id);
        out.set("result", result == null ? MAPPER.nullNode() : result);
        return out;
    }

    /** Build an error response envelope. */
    public static ObjectNode errorResponse(JsonNode id, int code, String message) {
        return errorResponse(id, code, message, null);
    }

    public static ObjectNode errorResponse(JsonNode id, int code, String message, JsonNode data) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("jsonrpc", VERSION);
        if (id != null) out.set("id", id);
        ObjectNode err = out.putObject("error");
        err.put("code", code);
        err.put("message", message == null ? "" : message);
        if (data != null) err.set("data", data);
        return out;
    }

    /** Build a server-initiated notification (no id field). */
    public static ObjectNode notification(String method, JsonNode params) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("jsonrpc", VERSION);
        out.put("method", method);
        if (params != null) out.set("params", params);
        return out;
    }
}
