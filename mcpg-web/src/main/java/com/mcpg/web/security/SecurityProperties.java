package com.mcpg.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime security knobs for the management API and MCP transports.
 *
 * <p>{@code api-token} is the shared secret clients send as
 * {@code Authorization: Bearer &lt;token&gt;} or {@code X-MCPG-Token}.
 * When security is enabled and the token is blank at startup, a random
 * token is generated and written under {@code ./data/api-token.txt}.</p>
 */
@ConfigurationProperties(prefix = "mcpg.security")
public class SecurityProperties {

    /** Master switch. When false, every endpoint stays anonymous (tests only). */
    private boolean enabled = true;

    /** Shared bearer token. Empty means "auto-generate on first boot". */
    private String apiToken = "";

    /**
     * AES key material for encrypting registry passwords at rest. When blank,
     * a key is derived from {@link #apiToken}.
     */
    private String secretKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken == null ? "" : apiToken;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey == null ? "" : secretKey;
    }
}
