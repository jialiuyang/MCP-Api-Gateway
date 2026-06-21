package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One active MCP client session.
 *
 * <p>Wraps a Spring {@link SseEmitter} - the outgoing side - and the metadata
 * needed for routing JSON-RPC responses back to the right client when the
 * client posts to {@code /mcp/message?sessionId=...}.</p>
 *
 * <p>The {@link #write} method is synchronized because Spring's
 * {@code SseEmitter} is not guaranteed to be safe under concurrent senders
 * (notifications can fire concurrently with responses).</p>
 */
public class McpSession {

    private static final Logger log = LoggerFactory.getLogger(McpSession.class);

    private final String id;
    private final SseEmitter emitter;
    private final Instant createdAt = Instant.now();
    private final AtomicBoolean initialized = new AtomicBoolean();
    private volatile Instant lastActivityAt = createdAt;

    public McpSession(String id, SseEmitter emitter) {
        this.id = id;
        this.emitter = emitter;
    }

    public String getId() { return id; }
    public SseEmitter getEmitter() { return emitter; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public boolean isInitialized() { return initialized.get(); }
    public void markInitialized() { initialized.set(true); }

    /** Send the special {@code endpoint} event used by the SSE transport handshake. */
    public synchronized void sendEndpointEvent(String url) throws IOException {
        emitter.send(SseEmitter.event().name("endpoint").data(url));
        touch();
    }

    /** Send a JSON-RPC message back to the client over the SSE channel. */
    public synchronized void write(JsonNode message) throws IOException {
        String payload = JsonRpc.mapper().writeValueAsString(message);
        log.trace("→ session {}: {}", id, payload);
        emitter.send(SseEmitter.event().name("message").data(payload));
        touch();
    }

    public void complete() {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("Error completing emitter for session {}", id, e);
        }
    }

    public void error(Throwable t) {
        try {
            emitter.completeWithError(t);
        } catch (Exception e) {
            log.debug("Error closing emitter with error for session {}", id, e);
        }
    }

    private void touch() {
        lastActivityAt = Instant.now();
    }
}
