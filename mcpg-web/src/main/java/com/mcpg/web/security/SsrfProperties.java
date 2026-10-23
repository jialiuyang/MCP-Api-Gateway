package com.mcpg.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls outbound URL fetching (OpenAPI import / refresh) and invocation
 * base URLs to mitigate server-side request forgery.
 */
@ConfigurationProperties(prefix = "mcpg.ssrf")
public class SsrfProperties {

    /**
     * When true, private / loopback / link-local targets are allowed.
     * Keep false outside local air-gapped demos.
     */
    private boolean allowPrivate = false;

    /** Optional host allow-list (exact or {@code *.example.com}). Empty = any public host. */
    private List<String> allowedHosts = new ArrayList<>();

    public boolean isAllowPrivate() {
        return allowPrivate;
    }

    public void setAllowPrivate(boolean allowPrivate) {
        this.allowPrivate = allowPrivate;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts == null ? new ArrayList<>() : allowedHosts;
    }
}
