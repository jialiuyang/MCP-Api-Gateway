package com.mcpg.registry.stub;

import org.springframework.stereotype.Component;

/**
 * Placeholder for Kubernetes Service discovery (via Kubernetes API).
 * Will be implemented in a future release.
 */
@Component
public class KubernetesRegistryAdapter extends AbstractStubRegistryAdapter {
    @Override public String getType() { return "k8s"; }
}
