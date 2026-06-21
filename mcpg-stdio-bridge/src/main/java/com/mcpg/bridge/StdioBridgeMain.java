package com.mcpg.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the stdio bridge.
 *
 * <p>When an MCP client (e.g. Cursor configured with a {@code "command": ...}
 * server entry) launches this jar, it speaks newline-delimited JSON-RPC over
 * stdin/stdout. The bridge forwards those frames to the central gateway's
 * HTTP/SSE endpoint and pipes the responses back.</p>
 *
 * <p>The full implementation is delivered in {@code B2}. This skeleton only
 * prints a friendly banner so packaging can be verified end-to-end.</p>
 *
 * <h3>Planned CLI flags</h3>
 * <pre>
 *   --endpoint &lt;url&gt;   e.g. http://localhost:8080/mcp/sse
 *   --token    &lt;jwt&gt;   optional bearer token for SSO
 *   --timeout  &lt;sec&gt;   per-request timeout (default 30)
 * </pre>
 */
public final class StdioBridgeMain {

    private static final Logger log = LoggerFactory.getLogger(StdioBridgeMain.class);

    private StdioBridgeMain() {
        // Utility entry point.
    }

    public static void main(String[] args) {
        log.info("MCP Gateway Enterprise - stdio bridge (B1 skeleton)");
        log.info("Implementation arrives in B2. Arguments received: {}", String.join(" ", args));
        // Intentionally exit 0: B1 only verifies the artifact builds and runs.
    }
}
