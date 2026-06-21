package com.mcpg.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Connection configuration for a service registry.
 *
 * <p>Concrete adapters interpret {@link #getExtra()} according to their own
 * needs (Nacos uses {@code namespace}/{@code group}, Eureka uses {@code zone},
 * etc.). Keep the core record simple so that adding a new registry type does
 * not require a schema change.</p>
 */
public final class RegistryConfig {

    private final String name;
    private final String type;
    private final String endpoint;
    private final String username;
    private final String password;
    private final Map<String, String> extra;

    public RegistryConfig(String name,
                          String type,
                          String endpoint,
                          String username,
                          String password,
                          Map<String, String> extra) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.username = username;
        this.password = password;
        this.extra = extra == null ? Map.of() : Map.copyOf(extra);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, String> getExtra() {
        return extra;
    }
}
