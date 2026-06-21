package com.mcpg.core.spi;

import com.mcpg.core.model.ToolInvocation;
import com.mcpg.core.model.ToolInvocationResult;

/**
 * SPI for executing a {@link ToolInvocation} against a backend service.
 *
 * <p>The default implementation calls plain HTTP/REST, but the interface
 * deliberately allows future protocols (gRPC, GraphQL, Dubbo) to plug in.</p>
 */
public interface ToolInvoker {

    /**
     * Protocol this invoker handles, e.g. {@code http}, {@code grpc}.
     */
    String getProtocol();

    /**
     * Execute the call. Implementations should never throw for predictable
     * failure modes (5xx, timeout, connection refused); instead they should
     * return a {@link ToolInvocationResult} with {@code success=false} and a
     * descriptive {@code errorMessage}. Unchecked exceptions are reserved for
     * truly unexpected programming errors.
     */
    ToolInvocationResult invoke(ToolInvocation invocation);
}
