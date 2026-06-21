package com.mcpg.web.event;

import org.springframework.context.ApplicationEvent;

/**
 * Fired whenever the set of tools advertised by the gateway may have
 * changed (after Swagger import, refresh, deletion, or promote toggle).
 *
 * <p>The MCP layer listens for this and broadcasts a
 * {@code notifications/tools/list_changed} notification to every connected
 * client so they re-pull {@code tools/list} - the standard MCP feedback
 * loop.</p>
 */
public class ToolsChangedEvent extends ApplicationEvent {

    private final String reason;

    public ToolsChangedEvent(Object source, String reason) {
        super(source);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
