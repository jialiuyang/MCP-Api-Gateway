package com.mcpg.registry.stub;

import org.springframework.stereotype.Component;

/** Placeholder for HashiCorp Consul. Will be implemented in a future release. */
@Component
public class ConsulRegistryAdapter extends AbstractStubRegistryAdapter {
    @Override public String getType() { return "consul"; }
}
