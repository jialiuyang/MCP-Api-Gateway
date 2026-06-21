package com.mcpg.parser.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts swagger-parser models into the gateway's neutral
 * {@link JsonNode}-based representation that mirrors the MCP
 * {@code inputSchema}.
 *
 * <h2>Why a custom converter?</h2>
 * The MCP tool {@code inputSchema} is a JSON Schema document with three
 * top-level keys: {@code type}, {@code properties}, {@code required}. OpenAPI
 * parameters span path, query, header, cookie locations plus an optional
 * request body. We flatten all of those into a single JSON object schema by
 * convention:
 *
 * <pre>
 * {
 *   "type": "object",
 *   "properties": {
 *     "pathParams":   { "type": "object", "properties": {...}, "required": [...] },
 *     "queryParams":  { "type": "object", "properties": {...}, "required": [...] },
 *     "headers":      { "type": "object", "properties": {...}, "required": [...] },
 *     "body":         &lt;request body schema&gt;
 *   },
 *   "required": [<sub-objects that have at least one required field>]
 * }
 * </pre>
 *
 * <p>This shape is friendly to LLMs: they can fill any subset of the four
 * locations without ambiguity, and the invoker can decode the structured
 * arguments back into a real HTTP call without re-consulting the OpenAPI
 * spec.</p>
 */
public final class SchemaConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> PARAM_LOCATIONS_SUPPORTED =
            Set.of("path", "query", "header", "cookie");

    private SchemaConverter() {
    }

    /**
     * Build the MCP-style inputSchema for an operation. {@code openAPI} is
     * required so that {@code $ref} schemas can be resolved to their final
     * definition (swagger-parser stores them inline if the spec was loaded
     * with {@code resolve=true}, but we defensively re-walk the tree).
     */
    public static JsonNode buildInputSchema(OpenAPI openAPI, Operation op) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "object");
        ObjectNode properties = root.putObject("properties");
        ArrayNode required = MAPPER.createArrayNode();

        // Group OpenAPI parameters by their `in` location.
        Map<String, List<Parameter>> grouped = new LinkedHashMap<>();
        List<Parameter> allParams = mergeOperationAndPathParams(op);
        for (Parameter p : allParams) {
            String in = p.getIn() == null ? "" : p.getIn().toLowerCase(Locale.ROOT);
            if (!PARAM_LOCATIONS_SUPPORTED.contains(in)) continue;
            grouped.computeIfAbsent(toGroupKey(in), k -> new ArrayList<>()).add(p);
        }

        boolean anyRequired = false;
        for (Map.Entry<String, List<Parameter>> e : grouped.entrySet()) {
            ObjectNode group = buildParamGroup(e.getValue());
            properties.set(e.getKey(), group);
            if (groupHasRequired(e.getValue())) {
                required.add(e.getKey());
                anyRequired = true;
            }
        }

        // Request body (if any).
        RequestBody body = op.getRequestBody();
        if (body != null) {
            JsonNode bodySchema = convertRequestBody(body);
            if (bodySchema != null) {
                properties.set("body", bodySchema);
                if (Boolean.TRUE.equals(body.getRequired())) {
                    required.add("body");
                    anyRequired = true;
                }
            }
        }

        if (anyRequired) {
            root.set("required", required);
        }

        return root;
    }

    /** Build the response schema (best-effort, only the first 2xx response). */
    public static JsonNode buildOutputSchema(Operation op) {
        if (op.getResponses() == null) return null;
        for (Map.Entry<String, io.swagger.v3.oas.models.responses.ApiResponse> e : op.getResponses().entrySet()) {
            String code = e.getKey();
            if (code == null) continue;
            if (code.startsWith("2") || "default".equalsIgnoreCase(code)) {
                io.swagger.v3.oas.models.responses.ApiResponse resp = e.getValue();
                if (resp.getContent() == null) return null;
                MediaType media = preferJson(resp.getContent());
                if (media == null) return null;
                return schemaToNode(media.getSchema());
            }
        }
        return null;
    }

    // --- internals ----------------------------------------------------------

    private static List<Parameter> mergeOperationAndPathParams(Operation op) {
        // swagger-parser usually merges already, but be defensive: callers may
        // hand us an unresolved Operation.
        List<Parameter> list = new ArrayList<>();
        if (op.getParameters() != null) list.addAll(op.getParameters());
        return list;
    }

    private static String toGroupKey(String openApiIn) {
        return switch (openApiIn) {
            case "path" -> "pathParams";
            case "query" -> "queryParams";
            case "header" -> "headers";
            case "cookie" -> "cookies";
            default -> openApiIn;
        };
    }

    private static boolean groupHasRequired(List<Parameter> params) {
        for (Parameter p : params) {
            if (Boolean.TRUE.equals(p.getRequired())) return true;
        }
        return false;
    }

    private static ObjectNode buildParamGroup(List<Parameter> params) {
        ObjectNode group = MAPPER.createObjectNode();
        group.put("type", "object");
        ObjectNode props = group.putObject("properties");
        ArrayNode req = MAPPER.createArrayNode();
        Set<String> requiredNames = new HashSet<>();
        for (Parameter p : params) {
            JsonNode schema = schemaToNode(p.getSchema());
            if (schema == null) schema = stringSchema();
            // Merge description from parameter level if missing on the schema.
            if (p.getDescription() != null
                    && schema instanceof ObjectNode on
                    && !on.has("description")) {
                on.put("description", p.getDescription());
            }
            props.set(p.getName(), schema);
            if (Boolean.TRUE.equals(p.getRequired()) && requiredNames.add(p.getName())) {
                req.add(p.getName());
            }
        }
        if (req.size() > 0) {
            group.set("required", req);
        }
        return group;
    }

    private static JsonNode convertRequestBody(RequestBody body) {
        if (body.getContent() == null) return null;
        MediaType media = preferJson(body.getContent());
        if (media == null) return null;
        JsonNode node = schemaToNode(media.getSchema());
        return node;
    }

    private static MediaType preferJson(io.swagger.v3.oas.models.media.Content content) {
        if (content == null) return null;
        for (String key : List.of("application/json", "application/*+json")) {
            MediaType m = content.get(key);
            if (m != null) return m;
        }
        // Fallback to whatever the first declared media type is.
        return content.values().stream().findFirst().orElse(null);
    }

    /** Recursive schema → JsonNode conversion. */
    @SuppressWarnings("rawtypes")
    private static JsonNode schemaToNode(Schema schema) {
        if (schema == null) return null;
        ObjectNode node = MAPPER.createObjectNode();
        if (schema.getType() != null) {
            node.put("type", schema.getType());
        }
        if (schema.getFormat() != null) {
            node.put("format", schema.getFormat());
        }
        if (schema.getDescription() != null) {
            node.put("description", schema.getDescription());
        }
        if (schema.getExample() != null) {
            node.putPOJO("example", schema.getExample());
        }
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            ArrayNode enumNode = node.putArray("enum");
            for (Object v : schema.getEnum()) {
                enumNode.add(String.valueOf(v));
            }
        }
        if (schema instanceof ObjectSchema os && os.getProperties() != null) {
            ObjectNode props = node.putObject("properties");
            for (Object entry : os.getProperties().entrySet()) {
                Map.Entry<String, Schema> e = (Map.Entry<String, Schema>) entry;
                JsonNode child = schemaToNode(e.getValue());
                if (child != null) props.set(e.getKey(), child);
            }
            if (os.getRequired() != null && !os.getRequired().isEmpty()) {
                ArrayNode req = node.putArray("required");
                for (String r : os.getRequired()) req.add(r);
            }
        } else if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            // Generic Schema with properties (no specific subtype).
            ObjectNode props = node.putObject("properties");
            for (Object entry : schema.getProperties().entrySet()) {
                Map.Entry<String, Schema> e = (Map.Entry<String, Schema>) entry;
                JsonNode child = schemaToNode(e.getValue());
                if (child != null) props.set(e.getKey(), child);
            }
            if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                ArrayNode req = node.putArray("required");
                for (Object r : schema.getRequired()) req.add(String.valueOf(r));
            }
        }
        if (schema instanceof ArraySchema arr && arr.getItems() != null) {
            JsonNode items = schemaToNode(arr.getItems());
            if (items != null) node.set("items", items);
        }
        // $ref fallthrough: if the parser left it unresolved, surface the ref
        // so a downstream consumer can see what was expected.
        if (schema.get$ref() != null && node.size() == 0) {
            node.put("$ref", schema.get$ref());
        }
        return node;
    }

    private static ObjectNode stringSchema() {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("type", "string");
        return n;
    }
}
