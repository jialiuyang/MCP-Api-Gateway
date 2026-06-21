/**
 * OpenAPI / Swagger parser implementations.
 *
 * <p>Two concrete parsers are bundled and registered as Spring beans:</p>
 * <ul>
 *   <li>{@code OpenApiV2Parser} for Swagger 2.0.</li>
 *   <li>{@code OpenApiV3Parser} for OpenAPI 3.0 and 3.1.</li>
 * </ul>
 *
 * <p>Both delegate to {@link io.swagger.parser.OpenAPIParser swagger-parser}
 * for the low-level work but produce the gateway-internal
 * {@link com.mcpg.core.model.ParsedSpec} representation so that downstream
 * code never imports a swagger-parser type.</p>
 *
 * <p>Implementations live in {@code B2}. This module currently contains only
 * the package declaration so the build wires up cleanly.</p>
 */
package com.mcpg.parser;
