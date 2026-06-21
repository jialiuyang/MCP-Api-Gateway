package com.mcpg.registry.stub;

import org.springframework.stereotype.Component;

/** Placeholder for Apache Zookeeper. Will be implemented in a future release. */
@Component
public class ZookeeperRegistryAdapter extends AbstractStubRegistryAdapter {
    @Override public String getType() { return "zookeeper"; }
}
