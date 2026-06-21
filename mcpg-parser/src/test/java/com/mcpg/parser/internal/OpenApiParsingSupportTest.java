package com.mcpg.parser.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Targeted tests for {@link OpenApiParsingSupport#combineServerUrl(String, String)}.
 *
 * <p>This logic is the source of subtle production bugs (e.g. Petstore's
 * {@code servers[].url = "/api/v3"}) so we lock the behaviour in here.</p>
 */
class OpenApiParsingSupportTest {

    @Test
    void absoluteHttpsServerUrl_isReturnedVerbatim() {
        assertEquals("https://api.example.com/v1",
                OpenApiParsingSupport.combineServerUrl(
                        "https://api.example.com/v1", "https://docs.example.com"));
    }

    @Test
    void trailingSlashIsStripped() {
        assertEquals("https://api.example.com/v1",
                OpenApiParsingSupport.combineServerUrl(
                        "https://api.example.com/v1/", "https://docs.example.com"));
    }

    @Test
    void rootRelativeServerUrl_isResolvedAgainstDocumentOrigin() {
        // The Petstore case: servers[].url = "/api/v3" served from petstore3.swagger.io
        assertEquals("https://petstore3.swagger.io/api/v3",
                OpenApiParsingSupport.combineServerUrl(
                        "/api/v3", "https://petstore3.swagger.io"));
    }

    @Test
    void protocolRelativeServerUrl_borrowsSchemeFromOrigin() {
        assertEquals("https://api.example.com/v1",
                OpenApiParsingSupport.combineServerUrl(
                        "//api.example.com/v1", "https://docs.example.com"));
    }

    @Test
    void relativePathWithoutLeadingSlash_isPrefixedWithOriginAndSlash() {
        assertEquals("https://docs.example.com/api/v1",
                OpenApiParsingSupport.combineServerUrl(
                        "api/v1", "https://docs.example.com"));
    }

    @Test
    void documentOriginWithPath_isHonoredAtAuthorityLevel() {
        // The fallback we pass in is always already an origin (scheme://host[:port]).
        // Defensive: even if someone passes a full URL, we extract just the origin.
        assertEquals("https://docs.example.com/api/v3",
                OpenApiParsingSupport.combineServerUrl(
                        "/api/v3", "https://docs.example.com/openapi.json"));
    }

    @Test
    void nullDocumentOrigin_keepsRelativeUrlForDiagnostics() {
        assertEquals("/api/v3", OpenApiParsingSupport.combineServerUrl("/api/v3", null));
    }
}
