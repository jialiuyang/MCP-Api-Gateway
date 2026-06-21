package com.mcpg.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

/**
 * A single REST operation parsed from an OpenAPI/Swagger document.
 *
 * <p>Conceptually maps one-to-one to a future MCP tool. The
 * {@link #getInputSchema()} field is already a JSON Schema usable as the
 * {@code inputSchema} of an MCP tool definition.</p>
 */
public final class ParsedOperation {

    private final String operationId;
    private final String httpMethod;
    private final String path;
    private final String summary;
    private final String description;
    private final JsonNode inputSchema;
    private final JsonNode outputSchema;
    private final Map<String, String> tags;
    private final boolean deprecated;

    private ParsedOperation(Builder b) {
        this.operationId = Objects.requireNonNull(b.operationId, "operationId");
        this.httpMethod = Objects.requireNonNull(b.httpMethod, "httpMethod");
        this.path = Objects.requireNonNull(b.path, "path");
        this.summary = b.summary;
        this.description = b.description;
        this.inputSchema = b.inputSchema;
        this.outputSchema = b.outputSchema;
        this.tags = b.tags == null ? Map.of() : Map.copyOf(b.tags);
        this.deprecated = b.deprecated;
    }

    public String getOperationId() { return operationId; }
    public String getHttpMethod() { return httpMethod; }
    public String getPath() { return path; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public JsonNode getInputSchema() { return inputSchema; }
    public JsonNode getOutputSchema() { return outputSchema; }
    public Map<String, String> getTags() { return tags; }
    public boolean isDeprecated() { return deprecated; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String operationId;
        private String httpMethod;
        private String path;
        private String summary;
        private String description;
        private JsonNode inputSchema;
        private JsonNode outputSchema;
        private Map<String, String> tags;
        private boolean deprecated;

        public Builder operationId(String v) { this.operationId = v; return this; }
        public Builder httpMethod(String v) { this.httpMethod = v; return this; }
        public Builder path(String v) { this.path = v; return this; }
        public Builder summary(String v) { this.summary = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder inputSchema(JsonNode v) { this.inputSchema = v; return this; }
        public Builder outputSchema(JsonNode v) { this.outputSchema = v; return this; }
        public Builder tags(Map<String, String> v) { this.tags = v; return this; }
        public Builder deprecated(boolean v) { this.deprecated = v; return this; }

        public ParsedOperation build() {
            return new ParsedOperation(this);
        }
    }
}
