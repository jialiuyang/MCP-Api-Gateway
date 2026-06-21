package com.mcpg.core.exception;

/**
 * Thrown when no registered {@code OpenApiSourceParser} can handle the
 * provided spec content.
 */
public class ParserNotFoundException extends McpgException {
    public ParserNotFoundException(String message) {
        super(message);
    }
}
