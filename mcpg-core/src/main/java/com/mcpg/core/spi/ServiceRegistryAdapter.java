package com.mcpg.core.spi;

import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.RegistryConfig;

import java.util.List;

/**
 * SPI for plugging in service registry implementations.
 *
 * <p>Adapters are discovered by Spring as ordinary beans and keyed by
 * {@link #getType()}. New backends (Consul, Polaris, K8s service, Zookeeper,
 * etc.) can be added by implementing this interface without touching any
 * other module.</p>
 *
 * <h3>Contract notes</h3>
 * <ul>
 *   <li>Implementations must be stateless with respect to a single
 *       {@link RegistryConfig}: any session or client state should be
 *       opened, used and closed within a single method call (or kept inside
 *       a cache that respects the config equality).</li>
 *   <li>{@link #testConnection(RegistryConfig)} is allowed to perform a
 *       network round-trip and should return within a few seconds.</li>
 * </ul>
 */
public interface ServiceRegistryAdapter {

    /**
     * Identifier of this adapter type. Must be unique and lowercase. Examples:
     * {@code nacos}, {@code eureka}, {@code consul}, {@code polaris},
     * {@code k8s}, {@code zookeeper}.
     */
    String getType();

    /**
     * Whether this adapter is fully implemented and safe to use in production.
     * Stub adapters that only exist to advertise upcoming support should
     * return {@code false} so the UI can mark them as "coming soon".
     */
    default boolean isImplemented() {
        return true;
    }

    /**
     * Probes the registry to verify the configuration is correct and the
     * endpoint is reachable.
     *
     * @return {@code true} if connection succeeded.
     * @throws UnsupportedOperationException when {@link #isImplemented()} is {@code false}.
     */
    boolean testConnection(RegistryConfig config);

    /**
     * Pull a one-shot snapshot of all currently registered services.
     *
     * <p>Adapters are expected to translate registry-specific data into the
     * neutral {@link DiscoveredService} representation. Healthy instances of
     * the same logical service should be folded into a single entry whose
     * {@link DiscoveredService#getBaseUrls()} contains one URL per instance.</p>
     */
    List<DiscoveredService> listServices(RegistryConfig config);
}
