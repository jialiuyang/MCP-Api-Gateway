package com.mcpg.web.security;

import com.mcpg.core.exception.McpgException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class UrlSafetyTest {

    private UrlSafety strict;
    private UrlSafety permissive;

    @BeforeEach
    void setUp() {
        SsrfProperties denyPrivate = new SsrfProperties();
        denyPrivate.setAllowPrivate(false);
        strict = new UrlSafety(denyPrivate);

        SsrfProperties allowPrivate = new SsrfProperties();
        allowPrivate.setAllowPrivate(true);
        permissive = new UrlSafety(allowPrivate);
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> strict.validateHttpUrl("file:///etc/passwd"))
                .isInstanceOf(McpgException.class)
                .hasMessageContaining("http/https");
    }

    @Test
    void rejectsLoopbackWhenPrivateDisallowed() {
        assertThatThrownBy(() -> strict.validateHttpUrl("http://127.0.0.1/v3/api-docs"))
                .isInstanceOf(McpgException.class)
                .hasMessageContaining("SSRF");
    }

    @Test
    void allowsLoopbackWhenPrivateAllowed() {
        assertThatCode(() -> permissive.validateHttpUrl("http://127.0.0.1/v3/api-docs"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsPublicHttps() {
        assertThatCode(() -> strict.validateHttpUrl("https://petstore3.swagger.io/api/v3/openapi.json"))
                .doesNotThrowAnyException();
    }
}
