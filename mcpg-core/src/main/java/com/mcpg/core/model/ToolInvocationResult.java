package com.mcpg.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Outcome of executing a {@link ToolInvocation}.
 */
public final class ToolInvocationResult {

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;
    private final long elapsedMillis;
    private final boolean success;
    private final String errorMessage;

    private ToolInvocationResult(Builder b) {
        this.statusCode = b.statusCode;
        this.body = b.body;
        this.headers = b.headers == null ? Map.of() : Map.copyOf(b.headers);
        this.elapsedMillis = b.elapsedMillis;
        this.success = b.success;
        this.errorMessage = b.errorMessage;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
    public long getElapsedMillis() { return elapsedMillis; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    public static Builder builder() {
        return new Builder();
    }

    public static ToolInvocationResult error(String message, long elapsedMillis) {
        return builder()
                .success(false)
                .errorMessage(Objects.requireNonNull(message, "message"))
                .elapsedMillis(elapsedMillis)
                .build();
    }

    public static final class Builder {
        private int statusCode;
        private String body;
        private Map<String, String> headers;
        private long elapsedMillis;
        private boolean success;
        private String errorMessage;

        public Builder statusCode(int v) { this.statusCode = v; return this; }
        public Builder body(String v) { this.body = v; return this; }
        public Builder headers(Map<String, String> v) { this.headers = v; return this; }
        public Builder elapsedMillis(long v) { this.elapsedMillis = v; return this; }
        public Builder success(boolean v) { this.success = v; return this; }
        public Builder errorMessage(String v) { this.errorMessage = v; return this; }

        public ToolInvocationResult build() {
            return new ToolInvocationResult(this);
        }
    }
}
