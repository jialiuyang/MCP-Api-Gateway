package com.mcpg.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates everything a {@code ToolInvoker} needs to call a backend
 * operation.
 *
 * <p>Decoupling the invoker from the persistent tool entity keeps the
 * invocation pipeline testable and free of database concerns.</p>
 */
public final class ToolInvocation {

    private final String serviceName;
    private final String baseUrl;
    private final String httpMethod;
    private final String path;
    private final JsonNode arguments;
    private final Map<String, String> headers;
    private final Environment environment;

    private ToolInvocation(Builder b) {
        this.serviceName = Objects.requireNonNull(b.serviceName, "serviceName");
        this.baseUrl = Objects.requireNonNull(b.baseUrl, "baseUrl");
        this.httpMethod = Objects.requireNonNull(b.httpMethod, "httpMethod");
        this.path = Objects.requireNonNull(b.path, "path");
        this.arguments = b.arguments;
        this.headers = b.headers == null ? Map.of() : Map.copyOf(b.headers);
        this.environment = b.environment == null ? Environment.UNKNOWN : b.environment;
    }

    public String getServiceName() { return serviceName; }
    public String getBaseUrl() { return baseUrl; }
    public String getHttpMethod() { return httpMethod; }
    public String getPath() { return path; }
    public JsonNode getArguments() { return arguments; }
    public Map<String, String> getHeaders() { return headers; }
    public Environment getEnvironment() { return environment; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String serviceName;
        private String baseUrl;
        private String httpMethod;
        private String path;
        private JsonNode arguments;
        private Map<String, String> headers;
        private Environment environment;

        public Builder serviceName(String v) { this.serviceName = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder httpMethod(String v) { this.httpMethod = v; return this; }
        public Builder path(String v) { this.path = v; return this; }
        public Builder arguments(JsonNode v) { this.arguments = v; return this; }
        public Builder headers(Map<String, String> v) { this.headers = v; return this; }
        public Builder environment(Environment v) { this.environment = v; return this; }

        public ToolInvocation build() {
            return new ToolInvocation(this);
        }
    }
}
