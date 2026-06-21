package com.mcpg.web.mcp;

import com.mcpg.web.event.ToolsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bridges domain-level "tools changed" events into MCP protocol
 * {@code notifications/tools/list_changed} broadcasts.
 *
 * <p>{@link Async @Async} is intentional: the broadcast iterates over every
 * open session and writes to its SSE emitter, which we do not want to do on
 * the import path's transaction thread.</p>
 */
@Component
public class ToolsChangedListener {

    private static final Logger log = LoggerFactory.getLogger(ToolsChangedListener.class);

    private final McpSessionManager sessions;

    public ToolsChangedListener(McpSessionManager sessions) {
        this.sessions = sessions;
    }

    @Async
    @EventListener
    public void onToolsChanged(ToolsChangedEvent event) {
        log.info("Broadcasting tools/list_changed (reason: {})", event.getReason());
        sessions.broadcast(JsonRpc.notification("notifications/tools/list_changed", null));
    }
}
