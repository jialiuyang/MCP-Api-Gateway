package com.mcpg.core.exception;

/**
 * Base unchecked exception for the MCP Gateway runtime.
 *
 * <p>All exceptions thrown by gateway internals should be modeled either as
 * subtypes of this class or as the standard Java exception types that
 * already convey meaning (e.g. {@link IllegalArgumentException}).</p>
 */
public class McpgException extends RuntimeException {

    public McpgException(String message) {
        super(message);
    }

    public McpgException(String message, Throwable cause) {
        super(message, cause);
    }
}
