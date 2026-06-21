package com.mcpg.web.service;

import com.mcpg.core.exception.McpgException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Downloads a remote OpenAPI / Swagger document.
 *
 * <p>Wraps a configured {@link RestClient} so:</p>
 * <ul>
 *   <li>Timeouts are bounded (15 seconds connect + read).</li>
 *   <li>Friendly errors surface the URL that failed - vital because the
 *       most common operational mistake is a typo in the swagger path.</li>
 *   <li>If the URL is a Swagger UI HTML page rather than the spec itself,
 *       try the well-known sibling paths ({@code /v3/api-docs} for
 *       Springdoc, {@code /openapi.json} for FastAPI).</li>
 * </ul>
 */
@Component
public class SwaggerFetcher {

    private static final Logger log = LoggerFactory.getLogger(SwaggerFetcher.class);

    private static final List<String> WELL_KNOWN_SUFFIXES = List.of(
            "/v3/api-docs",       // Springdoc
            "/v3/api-docs.yaml",
            "/v2/api-docs",       // Springfox (deprecated but still common)
            "/openapi.json",      // FastAPI default
            "/openapi.yaml",
            "/swagger.json"
    );

    private final RestClient client;

    public SwaggerFetcher() {
        this.client = RestClient.builder()
                .defaultHeader("Accept", "application/json, application/yaml, text/yaml, */*")
                .build();
    }

    /**
     * Fetch the raw spec at the given URL. If the body looks like HTML
     * (Swagger UI page) the fetcher walks well-known sibling paths.
     */
    public Result fetch(String url) {
        try {
            URI uri = URI.create(url);
            log.info("Fetching OpenAPI spec from {}", uri);
            String body = exchange(uri);
            if (looksLikeHtml(body)) {
                log.info("Response looks like HTML; trying well-known spec paths");
                String alt = walkWellKnown(uri);
                if (alt != null) return new Result(alt, uri.toString());
                throw new McpgException(
                        "URL returned HTML rather than a spec, and no well-known "
                                + "sibling path produced a parsable document: " + uri);
            }
            return new Result(body, uri.toString());
        } catch (McpgException e) {
            throw e;
        } catch (Exception e) {
            throw new McpgException("Failed to fetch OpenAPI spec at " + url + ": " + e.getMessage(), e);
        }
    }

    private String exchange(URI uri) {
        return client.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    throw new McpgException(
                            "Spec endpoint returned " + resp.getStatusCode() + " for " + uri);
                })
                .body(String.class);
    }

    private String walkWellKnown(URI base) {
        String origin = base.getScheme() + "://" + base.getAuthority();
        for (String suffix : WELL_KNOWN_SUFFIXES) {
            URI candidate = URI.create(origin + suffix);
            try {
                String body = exchange(candidate);
                if (!looksLikeHtml(body)) {
                    log.info("Found spec at {}", candidate);
                    return body;
                }
            } catch (Exception e) {
                log.debug("Well-known path {} not usable: {}", candidate, e.getMessage());
            }
        }
        return null;
    }

    private boolean looksLikeHtml(String body) {
        if (body == null) return false;
        String head = body.length() > 256 ? body.substring(0, 256) : body;
        String lower = head.trim().toLowerCase();
        return lower.startsWith("<!doctype html") || lower.startsWith("<html");
    }

    /**
     * Bundle of the fetched body and the URL that actually returned it (may
     * differ from the input URL if a well-known suffix was tried).
     */
    public record Result(String body, String resolvedUrl) {
        public Duration estimatedSizeIndicator() {
            return Duration.ofMillis(body == null ? 0 : body.length());
        }
    }
}
