package com.mcpg.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight representation of a service discovered from a registry.
 *
 * <p>This is the contract between {@code ServiceRegistryAdapter}
 * implementations and the discovery service that ingests results. It
 * deliberately exposes only the minimum information needed to fetch a
 * Swagger/OpenAPI document afterwards.</p>
 */
public final class DiscoveredService {

    private final String name;
    private final List<String> baseUrls;
    private final String sourceType;
    private final String sourceRef;
    private final Environment environment;
    private final Map<String, String> metadata;

    private DiscoveredService(Builder b) {
        this.name = Objects.requireNonNull(b.name, "name");
        this.baseUrls = b.baseUrls == null ? Collections.emptyList() : List.copyOf(b.baseUrls);
        this.sourceType = Objects.requireNonNull(b.sourceType, "sourceType");
        this.sourceRef = b.sourceRef;
        this.environment = b.environment == null ? Environment.UNKNOWN : b.environment;
        this.metadata = b.metadata == null ? Map.of() : Map.copyOf(b.metadata);
    }

    /** Logical service name (e.g. {@code order-service}). */
    public String getName() {
        return name;
    }

    /** Candidate HTTP base URLs (one per healthy instance). */
    public List<String> getBaseUrls() {
        return baseUrls;
    }

    /** Adapter type that produced this service (e.g. {@code nacos}, {@code eureka}). */
    public String getSourceType() {
        return sourceType;
    }

    /** Identifier within the source (e.g. Nacos namespace + group + service). */
    public String getSourceRef() {
        return sourceRef;
    }

    public Environment getEnvironment() {
        return environment;
    }

    /** Arbitrary metadata provided by the registry (e.g. weight, zone). */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private List<String> baseUrls;
        private String sourceType;
        private String sourceRef;
        private Environment environment;
        private Map<String, String> metadata;

        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder baseUrls(List<String> baseUrls) {
            this.baseUrls = baseUrls;
            return this;
        }
        public Builder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }
        public Builder sourceRef(String sourceRef) {
            this.sourceRef = sourceRef;
            return this;
        }
        public Builder environment(Environment environment) {
            this.environment = environment;
            return this;
        }
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }
        public DiscoveredService build() {
            return new DiscoveredService(this);
        }
    }
}
