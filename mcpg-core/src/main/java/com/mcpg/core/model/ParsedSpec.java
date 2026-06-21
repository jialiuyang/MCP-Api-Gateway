package com.mcpg.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of parsing an OpenAPI / Swagger document.
 */
public final class ParsedSpec {

    private final String title;
    private final String version;
    private final String specVersion;
    private final String baseUrl;
    private final List<ParsedOperation> operations;
    private final String rawSpec;

    private ParsedSpec(Builder b) {
        this.title = b.title;
        this.version = b.version;
        this.specVersion = Objects.requireNonNull(b.specVersion, "specVersion");
        this.baseUrl = b.baseUrl;
        this.operations = b.operations == null ? Collections.emptyList() : List.copyOf(b.operations);
        this.rawSpec = b.rawSpec;
    }

    /** Human-readable title declared in the spec (e.g. info.title). */
    public String getTitle() { return title; }

    /** API version declared in the spec (e.g. info.version). */
    public String getVersion() { return version; }

    /**
     * Spec dialect, e.g. {@code swagger-2.0}, {@code openapi-3.0},
     * {@code openapi-3.1}. Used for diagnostics only; the parsed operations
     * are normalized regardless of dialect.
     */
    public String getSpecVersion() { return specVersion; }

    /** Resolved base URL (servers[0].url or scheme://host:port from Swagger 2). */
    public String getBaseUrl() { return baseUrl; }

    public List<ParsedOperation> getOperations() { return operations; }

    /** Original spec text, kept for audit and re-parsing. May be {@code null} for size reasons. */
    public String getRawSpec() { return rawSpec; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private String version;
        private String specVersion;
        private String baseUrl;
        private List<ParsedOperation> operations;
        private String rawSpec;

        public Builder title(String v) { this.title = v; return this; }
        public Builder version(String v) { this.version = v; return this; }
        public Builder specVersion(String v) { this.specVersion = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder operations(List<ParsedOperation> v) { this.operations = v; return this; }
        public Builder rawSpec(String v) { this.rawSpec = v; return this; }

        public ParsedSpec build() {
            return new ParsedSpec(this);
        }
    }
}
