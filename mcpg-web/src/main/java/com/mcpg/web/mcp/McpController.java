package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * HTTP transport endpoints for the Model Context Protocol.
 *
 * <p>Two transports are exposed in parallel:</p>
 *
 * <h3>Streamable HTTP (recommended)</h3>
 * Spec: <a href="https://spec.modelcontextprotocol.io/specification/2025-03-26/basic/transports/">2025-03-26</a>.
 * Single URL on which the client POSTs JSON-RPC requests and optionally GETs
 * an SSE channel for server-initiated notifications.
 * <ul>
 *   <li>{@code POST /mcp} - JSON-RPC body, JSON-RPC response, or 202 for
 *       notifications.</li>
 *   <li>{@code GET /mcp} - SSE channel carrying server-initiated
 *       notifications (e.g. {@code tools/list_changed}).</li>
 * </ul>
 *
 * <h3>HTTP+SSE (legacy 2024-11)</h3>
 * The original transport with two endpoints.
 * <ul>
 *   <li>{@code GET /mcp/sse} - opens an SSE channel, first event tells the
 *       client which URL to POST to.</li>
 *   <li>{@code POST /mcp/message?sessionId=} - inbound messages.</li>
 * </ul>
 *
 * <p>Both transports share the same {@link McpDispatcher}, so semantics are
 * identical. Cursor 0.45+ uses Streamable HTTP; older clients use the
 * legacy transport.</p>
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L; // 1 hour

    private final McpSessionManager sessions;
    private final McpDispatcher dispatcher;
    private final Executor executor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
            r -> {
                Thread t = new Thread(r, "mcp-handler");
                t.setDaemon(true);
                return t;
            });

    public McpController(McpSessionManager sessions, McpDispatcher dispatcher) {
        this.sessions = sessions;
        this.dispatcher = dispatcher;
    }

    // ====================================================================
    // Streamable HTTP transport (POST /mcp + GET /mcp)
    // ====================================================================

    /**
     * Streamable HTTP entry point.
     *
     * <p>Per spec we may return either:
     * <ul>
     *   <li>{@code 200 OK} with a JSON-RPC response body (synchronous), or</li>
     *   <li>{@code 202 Accepted} with empty body when the inbound message is a
     *       notification or a response (no reply expected).</li>
     * </ul>
     * <p>For B2 we always take the synchronous path; SSE streaming on the POST
     * response is a B4 / B5 concern when long-running tool calls are
     * introduced.</p>
     */
    @PostMapping(
            value = { "", "/" },
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JsonNode> streamableHttp(@RequestBody JsonNode body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<JsonNode> result = body.isArray()
                ? dispatcher.dispatchBatch(body)
                : dispatcher.dispatch(body);
        return result
                .<ResponseEntity<JsonNode>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    /**
     * Streamable HTTP server-initiated channel.
     *
     * <p>Optional per spec: clients that want to receive
     * {@code notifications/tools/list_changed} (and future server-initiated
     * messages) GET this endpoint to subscribe.</p>
     */
    @GetMapping(value = { "", "/" }, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamableHttpStream() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        sessions.open(emitter);
        return emitter;
    }

    // ====================================================================
    // Legacy HTTP+SSE transport (GET /mcp/sse + POST /mcp/message)
    // ====================================================================

    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter openLegacySse(HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        McpSession session = sessions.open(emitter);
        try {
            String endpoint = request.getContextPath() + "/mcp/message?sessionId=" + session.getId();
            session.sendEndpointEvent(endpoint);
        } catch (IOException e) {
            log.warn("Failed to send endpoint event for session {}", session.getId(), e);
            sessions.close(session.getId(), "endpoint-send-failed");
        }
        return emitter;
    }

    @PostMapping(path = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveLegacy(@RequestParam("sessionId") String sessionId,
                                                @RequestBody JsonNode body) {
        McpSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("Received message for unknown session {}", sessionId);
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        CompletableFuture.runAsync(() -> {
            Optional<JsonNode> result = body != null && body.isArray()
                    ? dispatcher.dispatchBatch(body)
                    : dispatcher.dispatch(body);
            result.ifPresent(node -> {
                try {
                    session.write(node);
                } catch (IOException e) {
                    log.warn("Failed to write response for session {}", session.getId(), e);
                    sessions.close(session.getId(), "write-failed");
                }
            });
        }, executor).exceptionally(t -> {
            log.error("Unhandled error in MCP handler for session {}", sessionId, t);
            return null;
        });
        return ResponseEntity.accepted().build();
    }
}
