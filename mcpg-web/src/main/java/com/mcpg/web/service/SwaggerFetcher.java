package com.mcpg.web.service;

import com.mcpg.core.exception.McpgException;
import com.mcpg.web.security.UrlSafety;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Downloads a remote OpenAPI / Swagger document with SSRF guards and timeouts.
 */
@Component
public class SwaggerFetcher {

    private static final Logger log = LoggerFactory.getLogger(SwaggerFetcher.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    private static final List<String> WELL_KNOWN_SUFFIXES = List.of(
            "/v3/api-docs",
            "/v3/api-docs.yaml",
            "/v2/api-docs",
            "/openapi.json",
            "/openapi.yaml",
            "/swagger.json"
    );

    private final RestClient client;
    private final UrlSafety urlSafety;

    public SwaggerFetcher(UrlSafety urlSafety) {
        this.urlSafety = urlSafety;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        this.client = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json, application/yaml, text/yaml, */*")
                .build();
    }

    public Result fetch(String url) {
        try {
            URI uri = urlSafety.validateHttpUrl(url);
            log.info("Fetching OpenAPI spec from {}", uri);
            String body = exchange(uri);
            if (looksLikeHtml(body)) {
                log.info("Response looks like HTML; trying well-known spec paths");
                Result alt = walkWellKnown(uri);
                if (alt != null) return alt;
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
        urlSafety.validateHttpUrl(uri.toString());
        return client.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    throw new McpgException(
                            "Spec endpoint returned " + resp.getStatusCode() + " for " + uri);
                })
                .body(String.class);
    }

    private Result walkWellKnown(URI base) {
        String origin = base.getScheme() + "://" + base.getAuthority();
        for (String suffix : WELL_KNOWN_SUFFIXES) {
            URI candidate = URI.create(origin + suffix);
            try {
                String body = exchange(candidate);
                if (!looksLikeHtml(body)) {
                    log.info("Found spec at {}", candidate);
                    return new Result(body, candidate.toString());
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

    public record Result(String body, String resolvedUrl) {
    }
}
