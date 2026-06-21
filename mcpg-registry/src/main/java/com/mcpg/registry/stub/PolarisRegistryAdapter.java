package com.mcpg.registry.stub;

import org.springframework.stereotype.Component;

/** Placeholder for Tencent Polaris. Will be implemented in a future release. */
@Component
public class PolarisRegistryAdapter extends AbstractStubRegistryAdapter {
    @Override public String getType() { return "polaris"; }
}
