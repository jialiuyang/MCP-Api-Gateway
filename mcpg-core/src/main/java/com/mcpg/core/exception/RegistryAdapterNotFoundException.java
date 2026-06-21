package com.mcpg.core.exception;

/**
 * Thrown when the requested {@code ServiceRegistryAdapter} type is not
 * registered or not implemented yet.
 */
public class RegistryAdapterNotFoundException extends McpgException {
    public RegistryAdapterNotFoundException(String message) {
        super(message);
    }
}
