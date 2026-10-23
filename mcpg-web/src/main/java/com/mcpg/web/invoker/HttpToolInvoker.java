package com.mcpg.web.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpg.core.exception.McpgException;
import com.mcpg.core.model.ToolInvocation;
import com.mcpg.core.model.ToolInvocationResult;
import com.mcpg.core.spi.ToolInvoker;
import com.mcpg.web.security.UrlSafety;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Translates a {@link ToolInvocation} into a real HTTP call against the
 * backend service, with SSRF checks, timeouts, and header allow-listing.
 */
@Component
public class HttpToolInvoker implements ToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(HttpToolInvoker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private static final Set<String> BLOCKED_REQUEST_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding", "connection",
            "keep-alive", "upgrade", "te", "trailer", "proxy-authorization",
            "proxy-authenticate", "cookie", "set-cookie"
    );

    private final RestClient restClient;
    private final UrlSafety urlSafety;

    public HttpToolInvoker(UrlSafety urlSafety) {
        this.urlSafety = urlSafety;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "mcp-gateway-enterprise/1.0")
                .build();
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public ToolInvocationResult invoke(ToolInvocation invocation) {
        Instant start = Instant.now();
        try {
            urlSafety.validateHttpUrl(invocation.getBaseUrl());
            String resolvedPath = substitutePathParams(invocation.getPath(), invocation.getArguments());
            assertSafePath(resolvedPath);
            URI uri = buildUri(invocation.getBaseUrl(), resolvedPath, invocation.getArguments());
            urlSafety.validateHttpUrl(uri.toString());
            HttpMethod method = HttpMethod.valueOf(invocation.getHttpMethod().toUpperCase(Locale.ROOT));

            log.info("Invoking {} {} (service={})", method, uri, invocation.getServiceName());

            RestClient.RequestBodySpec spec = restClient
                    .method(method)
                    .uri(uri)
                    .headers(h -> {
                        applyStaticHeaders(h, invocation.getHeaders());
                        applyHeaderArgs(h, invocation.getArguments());
                    });

            JsonNode bodyNode = invocation.getArguments() == null
                    ? null : invocation.getArguments().get("body");
            ResponseEntity<String> response;
            if (bodyNode != null && !bodyNode.isNull()) {
                spec.contentType(MediaType.APPLICATION_JSON);
                response = spec.body(MAPPER.writeValueAsString(bodyNode))
                        .retrieve()
                        .toEntity(String.class);
            } else {
                response = spec.retrieve().toEntity(String.class);
            }

            long elapsed = Duration.between(start, Instant.now()).toMillis();
            return ToolInvocationResult.builder()
                    .statusCode(response.getStatusCode().value())
                    .body(response.getBody())
                    .headers(flattenHeaders(response.getHeaders()))
                    .elapsedMillis(elapsed)
                    .success(response.getStatusCode().is2xxSuccessful())
                    .build();
        } catch (Exception e) {
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            log.warn("Tool invocation failed (service={}, path={}): {}",
                    invocation.getServiceName(), invocation.getPath(), e.getMessage());
            return ToolInvocationResult.error(
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
                    elapsed);
        }
    }

    private String substitutePathParams(String template, JsonNode arguments) {
        if (arguments == null) return template;
        JsonNode pathParams = arguments.get("pathParams");
        if (pathParams == null || !pathParams.isObject()) return template;
        String result = template;
        Iterator<String> names = pathParams.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode value = pathParams.get(name);
            String stringValue = value.isTextual() ? value.asText() : value.toString();
            if (stringValue.contains("..") || stringValue.contains("/") || stringValue.contains("\\")) {
                throw new McpgException("Illegal path parameter value for '" + name + "'");
            }
            result = result.replace("{" + name + "}",
                    UriUtils.encodePathSegment(stringValue, StandardCharsets.UTF_8));
        }
        return result;
    }

    private void assertSafePath(String path) {
        if (path == null) return;
        if (path.contains("..") || path.contains("\\") || path.contains("//")) {
            throw new McpgException("Illegal path after parameter substitution: " + path);
        }
    }

    private URI buildUri(String baseUrl, String path, JsonNode arguments) {
        String safeBase = baseUrl.endsWith("/") && path != null && path.startsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String combined = path == null ? safeBase : safeBase + path;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(combined);
        if (arguments != null) {
            JsonNode q = arguments.get("queryParams");
            if (q != null && q.isObject()) {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                Iterator<Map.Entry<String, JsonNode>> it = q.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    JsonNode v = e.getValue();
                    if (v.isArray()) {
                        for (JsonNode item : v) params.add(e.getKey(), nodeToString(item));
                    } else if (!v.isNull()) {
                        params.add(e.getKey(), nodeToString(v));
                    }
                }
                builder.queryParams(params);
            }
        }
        return builder.build(true).toUri();
    }

    private void applyStaticHeaders(HttpHeaders headers, Map<String, String> staticHeaders) {
        if (staticHeaders == null) return;
        staticHeaders.forEach((k, v) -> {
            if (isAllowedHeader(k)) headers.add(k, v);
        });
    }

    private void applyHeaderArgs(HttpHeaders headers, JsonNode arguments) {
        if (arguments == null) return;
        JsonNode h = arguments.get("headers");
        if (h == null || !h.isObject()) return;
        Map<String, JsonNode> map = new LinkedHashMap<>();
        h.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue()));
        for (Map.Entry<String, JsonNode> e : map.entrySet()) {
            if (!isAllowedHeader(e.getKey())) continue;
            JsonNode v = e.getValue();
            if (v.isArray()) {
                for (JsonNode item : v) headers.add(e.getKey(), nodeToString(item));
            } else if (!v.isNull()) {
                headers.add(e.getKey(), nodeToString(v));
            }
        }
    }

    private boolean isAllowedHeader(String name) {
        if (name == null || name.isBlank()) return false;
        return !BLOCKED_REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> out = new LinkedHashMap<>();
        headers.forEach((k, vals) -> {
            if (!vals.isEmpty()) out.put(k, String.join(", ", vals));
        });
        return out;
    }

    private String nodeToString(JsonNode node) {
        return node.isTextual() ? node.asText() : node.toString();
    }
}
