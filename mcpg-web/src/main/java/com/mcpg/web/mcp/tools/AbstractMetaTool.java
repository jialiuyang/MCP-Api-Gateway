package com.mcpg.web.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpg.web.mcp.McpTool;

/**
 * Shared helpers for the four meta tools.
 *
 * <p>Concentrates the boilerplate that builds the MCP {@code Result} envelope
 * so each concrete tool can focus on its query logic.</p>
 */
abstract class AbstractMetaTool implements McpTool {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /** Wrap a plain text payload in the MCP content envelope. */
    protected Result textResult(String text, boolean isError) {
        ArrayNode content = MAPPER.createArrayNode();
        ObjectNode block = content.addObject();
        block.put("type", "text");
        block.put("text", text == null ? "" : text);
        return new Result(content, isError);
    }

    /**
     * Convenience builder for an MCP-style {@code inputSchema} with an
     * {@code object} root and a {@code properties} sub-tree.
     */
    protected static ObjectNode objectSchema() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "object");
        root.putObject("properties");
        return root;
    }

    protected static ObjectNode addStringProp(ObjectNode schema, String name,
                                               String description, boolean required) {
        ObjectNode props = (ObjectNode) schema.get("properties");
        props.putObject(name).put("type", "string").put("description", description);
        if (required) {
            ArrayNode req = schema.has("required") ? (ArrayNode) schema.get("required")
                    : schema.putArray("required");
            req.add(name);
        }
        return schema;
    }

    protected static ObjectNode addIntegerProp(ObjectNode schema, String name,
                                                String description, int defaultValue) {
        ObjectNode props = (ObjectNode) schema.get("properties");
        ObjectNode field = props.putObject(name);
        field.put("type", "integer");
        field.put("description", description);
        field.put("default", defaultValue);
        return schema;
    }

    protected static ObjectNode addObjectProp(ObjectNode schema, String name,
                                               String description, boolean required) {
        ObjectNode props = (ObjectNode) schema.get("properties");
        props.putObject(name).put("type", "object").put("description", description);
        if (required) {
            ArrayNode req = schema.has("required") ? (ArrayNode) schema.get("required")
                    : schema.putArray("required");
            req.add(name);
        }
        return schema;
    }

    protected String stringArg(JsonNode args, String name, String fallback) {
        if (args == null) return fallback;
        JsonNode v = args.get(name);
        if (v == null || v.isNull()) return fallback;
        return v.isTextual() ? v.asText() : v.toString();
    }

    protected int intArg(JsonNode args, String name, int fallback) {
        if (args == null) return fallback;
        JsonNode v = args.get(name);
        return v == null || !v.canConvertToInt() ? fallback : v.asInt();
    }
}
