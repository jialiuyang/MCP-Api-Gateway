/**
 * MCP server-side implementation.
 *
 * <p>This module realizes the four meta tools described in
 * {@link com.mcpg.core.model.ExposureMode#META META mode}:</p>
 * <ol>
 *   <li>{@code list_services}</li>
 *   <li>{@code search_api}</li>
 *   <li>{@code get_api_schema}</li>
 *   <li>{@code call_api}</li>
 * </ol>
 *
 * <p>Concrete tool implementations and the HTTP/SSE transport binding are
 * delivered in {@code B2}. This module currently provides only the package
 * declaration so that downstream modules can already depend on it.</p>
 */
package com.mcpg.server;
