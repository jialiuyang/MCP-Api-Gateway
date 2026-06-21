package com.mcpg.parser.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.mcpg.core.model.ParsedOperation;
import com.mcpg.core.model.ParsedSpec;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared parsing helper used by both the Swagger 2.0 and OpenAPI 3.x parser
 * beans.
 *
 * <p>The swagger-parser library internally up-converts a Swagger 2.0 document
 * into an {@link OpenAPI} 3 model, which is what makes a single backend
 * implementation viable. The two parser beans differ only in
 * {@code supports()} pre-checks and the {@code specVersion} label they
 * attach to the produced {@link ParsedSpec}.</p>
 */
public final class OpenApiParsingSupport {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParsingSupport.class);

    private OpenApiParsingSupport() {
    }

    public static ParsedSpec parse(String specContent, String baseUrl, String specVersionLabel) {
        ParseOptions opts = new ParseOptions();
        opts.setResolve(true);
        opts.setResolveFully(true);
        opts.setFlatten(false);

        SwaggerParseResult result = new OpenAPIParser()
                .readContents(specContent, null, opts);
        if (result.getOpenAPI() == null) {
            List<String> messages = result.getMessages() == null ? List.of() : result.getMessages();
            throw new IllegalArgumentException(
                    "Failed to parse OpenAPI spec: " + String.join("; ", messages));
        }
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            log.debug("OpenAPI parser warnings: {}", result.getMessages());
        }

        OpenAPI api = result.getOpenAPI();
        String resolvedBase = resolveBaseUrl(api, baseUrl);

        List<ParsedOperation> operations = new ArrayList<>();
        if (api.getPaths() != null) {
            for (Map.Entry<String, PathItem> e : api.getPaths().entrySet()) {
                operations.addAll(extract(api, e.getKey(), e.getValue()));
            }
        }

        return ParsedSpec.builder()
                .title(api.getInfo() != null ? api.getInfo().getTitle() : null)
                .version(api.getInfo() != null ? api.getInfo().getVersion() : null)
                .specVersion(specVersionLabel)
                .baseUrl(resolvedBase)
                .operations(operations)
                .rawSpec(specContent)
                .build();
    }

    private static List<ParsedOperation> extract(OpenAPI api, String path, PathItem item) {
        Map<PathItem.HttpMethod, Operation> map = new LinkedHashMap<>();
        if (item.getGet() != null)     map.put(PathItem.HttpMethod.GET, item.getGet());
        if (item.getPost() != null)    map.put(PathItem.HttpMethod.POST, item.getPost());
        if (item.getPut() != null)     map.put(PathItem.HttpMethod.PUT, item.getPut());
        if (item.getDelete() != null)  map.put(PathItem.HttpMethod.DELETE, item.getDelete());
        if (item.getPatch() != null)   map.put(PathItem.HttpMethod.PATCH, item.getPatch());
        if (item.getHead() != null)    map.put(PathItem.HttpMethod.HEAD, item.getHead());
        if (item.getOptions() != null) map.put(PathItem.HttpMethod.OPTIONS, item.getOptions());

        List<ParsedOperation> out = new ArrayList<>();
        for (Map.Entry<PathItem.HttpMethod, Operation> e : map.entrySet()) {
            String method = e.getKey().name();
            Operation op = e.getValue();
            // Inherit path-level parameters when the operation does not declare its own.
            if ((op.getParameters() == null || op.getParameters().isEmpty())
                    && item.getParameters() != null && !item.getParameters().isEmpty()) {
                op.setParameters(item.getParameters());
            }
            String operationId = op.getOperationId();
            if (operationId == null || operationId.isBlank()) {
                operationId = synthesizeOperationId(method, path);
            }
            JsonNode inputSchema = SchemaConverter.buildInputSchema(api, op);
            JsonNode outputSchema = SchemaConverter.buildOutputSchema(op);
            Map<String, String> tags = tagMap(op);
            out.add(ParsedOperation.builder()
                    .operationId(operationId)
                    .httpMethod(method)
                    .path(path)
                    .summary(op.getSummary())
                    .description(op.getDescription())
                    .inputSchema(inputSchema)
                    .outputSchema(outputSchema)
                    .tags(tags)
                    .deprecated(Boolean.TRUE.equals(op.getDeprecated()))
                    .build());
        }
        return out;
    }

    private static Map<String, String> tagMap(Operation op) {
        if (op.getTags() == null || op.getTags().isEmpty()) return Map.of();
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < op.getTags().size(); i++) {
            m.put(String.valueOf(i), op.getTags().get(i));
        }
        return m;
    }

    private static String synthesizeOperationId(String method, String path) {
        StringBuilder sb = new StringBuilder(method.toLowerCase(Locale.ROOT));
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) continue;
            sb.append('_');
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (Character.isLetterOrDigit(c)) sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Resolve the effective base URL for the operations.
     *
     * <p>Per the OpenAPI 3.1 spec, {@code servers[].url} <strong>may be a
     * relative URL</strong>, in which case it is interpreted relative to the
     * location where the OpenAPI document is served (which we pass in as
     * {@code documentOrigin}). FastAPI, Springdoc and many in-house Swagger
     * generators emit relative servers such as {@code /api/v3} - if we do not
     * resolve them, the invoker will later fail with "not a valid HTTP URL".</p>
     *
     * <p>Resolution rules (first match wins):</p>
     * <ol>
     *   <li>If {@code servers} is missing or only contains {@code "/"},
     *       fall back to {@code documentOrigin}.</li>
     *   <li>If the first {@code servers[].url} is already absolute
     *       ({@code http://} / {@code https://}), use it verbatim.</li>
     *   <li>If it is protocol-relative ({@code //host/path}), borrow the
     *       scheme from {@code documentOrigin}.</li>
     *   <li>Otherwise it is a path; concatenate with the origin from
     *       {@code documentOrigin}.</li>
     * </ol>
     */
    private static String resolveBaseUrl(OpenAPI api, String documentOrigin) {
        if (api.getServers() == null || api.getServers().isEmpty()) {
            return documentOrigin;
        }
        Server s = api.getServers().get(0);
        String url = s.getUrl();
        if (url == null || url.isBlank() || "/".equals(url)) {
            return documentOrigin;
        }
        return combineServerUrl(url.trim(), documentOrigin);
    }

    /** Combine a possibly-relative server URL with the document origin. */
    static String combineServerUrl(String serverUrl, String documentOrigin) {
        if (serverUrl.regionMatches(true, 0, "http://", 0, 7)
                || serverUrl.regionMatches(true, 0, "https://", 0, 8)) {
            return stripTrailingSlash(serverUrl);
        }
        String origin = extractOrigin(documentOrigin);
        if (serverUrl.startsWith("//")) {
            String scheme = "https";
            if (origin != null) {
                int colon = origin.indexOf(':');
                if (colon > 0) scheme = origin.substring(0, colon);
            }
            return stripTrailingSlash(scheme + ":" + serverUrl);
        }
        if (origin == null) {
            // Best effort: caller will likely surface a downstream error,
            // but we at least preserve the original string for diagnostics.
            return serverUrl;
        }
        String suffix = serverUrl.startsWith("/") ? serverUrl : "/" + serverUrl;
        return stripTrailingSlash(origin + suffix);
    }

    /** Return {@code scheme://authority} from an absolute URL, or {@code null}. */
    private static String extractOrigin(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getAuthority() == null) return null;
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            return null;
        }
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") && s.length() > 1 ? s.substring(0, s.length() - 1) : s;
    }
}
