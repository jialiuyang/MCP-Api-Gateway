package com.mcpg.core.spi;

import com.mcpg.core.model.ParsedSpec;

/**
 * SPI for parsing OpenAPI / Swagger documents into the neutral
 * {@link ParsedSpec} model.
 *
 * <p>Two implementations are bundled by default:</p>
 * <ul>
 *   <li>{@code OpenApiV2Parser} for Swagger / OpenAPI 2.0</li>
 *   <li>{@code OpenApiV3Parser} for OpenAPI 3.0 and 3.1
 *       (FastAPI, springdoc, etc. all emit OpenAPI 3)</li>
 * </ul>
 *
 * <p>The parser registry picks an implementation via {@link #supports(String)}
 * which inspects the document content. Future formats (gRPC reflection,
 * GraphQL schema, AsyncAPI) can be plugged in without changes to the
 * gateway core.</p>
 */
public interface OpenApiSourceParser {

    /** Stable identifier of the parser, e.g. {@code openapi-v3}. */
    String getType();

    /**
     * Quick content-based check used by the registry to choose a parser.
     * Implementations should be cheap and accept normalized JSON or YAML text.
     */
    boolean supports(String specContent);

    /**
     * Parse the spec into the neutral domain model.
     *
     * @param specContent raw text (JSON or YAML)
     * @param baseUrl     fallback base URL to use when the spec does not
     *                    declare one (e.g. Swagger 2 without {@code host}).
     */
    ParsedSpec parse(String specContent, String baseUrl);
}
