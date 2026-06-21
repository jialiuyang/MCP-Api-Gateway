package com.mcpg.web.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks active MCP sessions keyed by their generated session id.
 *
 * <p>Implemented in-process for B2 (single instance). In B5+ this will be
 * replaced by a Redis-backed implementation so the gateway can be deployed
 * as multiple replicas behind a load balancer; the
 * {@link McpSessionManager} type is the seam where that swap happens.</p>
 */
@Component
public class McpSessionManager {

    private static final Logger log = LoggerFactory.getLogger(McpSessionManager.class);

    private final ConcurrentMap<String, McpSession> sessions = new ConcurrentHashMap<>();

    public McpSession open(SseEmitter emitter) {
        String id = UUID.randomUUID().toString();
        McpSession session = new McpSession(id, emitter);
        sessions.put(id, session);
        emitter.onCompletion(() -> close(id, "completion"));
        emitter.onTimeout(() -> close(id, "timeout"));
        emitter.onError(t -> {
            log.debug("Session {} errored: {}", id, t.toString());
            close(id, "error");
        });
        log.info("Opened MCP session {}", id);
        return session;
    }

    public McpSession get(String id) {
        return sessions.get(id);
    }

    public Collection<McpSession> all() {
        return sessions.values();
    }

    public void close(String id, String reason) {
        McpSession s = sessions.remove(id);
        if (s != null) {
            log.info("Closed MCP session {} ({})", id, reason);
            s.complete();
        }
    }

    /** Broadcast a server-initiated notification to every connected session. */
    public void broadcast(JsonNode message) {
        for (McpSession s : sessions.values()) {
            try {
                s.write(message);
            } catch (Exception e) {
                log.warn("Failed to broadcast to session {}; closing", s.getId(), e);
                close(s.getId(), "broadcast-failure");
            }
        }
    }
}
