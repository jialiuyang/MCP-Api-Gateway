package com.mcpg.registry.stub;

import com.mcpg.core.model.DiscoveredService;
import com.mcpg.core.model.RegistryConfig;
import com.mcpg.core.spi.ServiceRegistryAdapter;

import java.util.List;

/**
 * Base class for "coming soon" registry adapters.
 *
 * <p>A stub adapter advertises a registry type in the UI dropdown so users
 * can see the roadmap, but its {@link #isImplemented()} returns {@code false}
 * and any operational call throws {@link UnsupportedOperationException}.</p>
 *
 * <p>Concrete subclasses only need to declare the type identifier.</p>
 */
public abstract class AbstractStubRegistryAdapter implements ServiceRegistryAdapter {

    @Override
    public final boolean isImplemented() {
        return false;
    }

    @Override
    public final boolean testConnection(RegistryConfig config) {
        throw new UnsupportedOperationException(
                "Registry adapter '" + getType() + "' is not yet implemented.");
    }

    @Override
    public final List<DiscoveredService> listServices(RegistryConfig config) {
        throw new UnsupportedOperationException(
                "Registry adapter '" + getType() + "' is not yet implemented.");
    }
}
