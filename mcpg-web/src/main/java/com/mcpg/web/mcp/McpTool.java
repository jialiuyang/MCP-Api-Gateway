package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Programmatic view of a tool advertised over the MCP protocol.
 *
 * <p>Implementations come in two flavors:</p>
 * <ul>
 *   <li>The four built-in meta tools (one Spring bean each).</li>
 *   <li>"Promoted" tools, which are produced dynamically from rows in
 *       {@code mcpg_tool} with {@code promoted=true} so that the LLM sees
 *       them as first-class operations (HYBRID exposure mode, B4).</li>
 * </ul>
 */
public interface McpTool {

    /** Stable identifier shown to the client (must match the MCP spec rules). */
    String name();

    /** Human-readable summary surfaced in {@code tools/list}. */
    String description();

    /** JSON Schema for the tool's input. Must be a non-null object schema. */
    JsonNode inputSchema();

    /**
     * Invoke the tool. Implementations are expected to be synchronous - the
     * MCP framework will marshal the result onto the outbound SSE stream.
     */
    Result invoke(JsonNode arguments) throws Exception;

    /** Result of a tool invocation in the MCP shape: {@code content[]} + isError. */
    record Result(JsonNode content, boolean isError) {
    }
}
