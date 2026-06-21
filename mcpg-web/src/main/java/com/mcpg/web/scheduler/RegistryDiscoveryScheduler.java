package com.mcpg.web.scheduler;

import com.mcpg.web.entity.RegistryEntity;
import com.mcpg.web.repository.RegistryRepository;
import com.mcpg.web.service.RegistryDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically polls every enabled registry for the current service list and
 * imports newly-discovered services into the gateway.
 *
 * <p>Two knobs in {@code application.yml}:</p>
 * <ul>
 *   <li>{@code mcpg.scheduler.registry-discovery.enabled} - master switch.</li>
 *   <li>{@code mcpg.scheduler.registry-discovery.fixed-delay-ms} - delay
 *       between runs in milliseconds (default 5 minutes).</li>
 * </ul>
 *
 * <p>The scheduler uses {@code fixedDelay} rather than {@code fixedRate} so a
 * slow discovery never overlaps itself. Per-registry failures are isolated
 * via try/catch and recorded in the registry row by the discovery service.</p>
 */
@Component
public class RegistryDiscoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RegistryDiscoveryScheduler.class);

    private final RegistryRepository registryRepository;
    private final RegistryDiscoveryService discoveryService;
    private final boolean enabled;

    public RegistryDiscoveryScheduler(RegistryRepository registryRepository,
                                      RegistryDiscoveryService discoveryService,
                                      @Value("${mcpg.scheduler.registry-discovery.enabled:true}") boolean enabled) {
        this.registryRepository = registryRepository;
        this.discoveryService = discoveryService;
        this.enabled = enabled;
    }

    @Scheduled(
            fixedDelayString = "${mcpg.scheduler.registry-discovery.fixed-delay-ms:300000}",
            initialDelayString = "${mcpg.scheduler.registry-discovery.initial-delay-ms:30000}")
    public void runAll() {
        if (!enabled) {
            log.debug("Registry discovery scheduler is disabled");
            return;
        }
        for (RegistryEntity registry : registryRepository.findByEnabledTrue()) {
            try {
                discoveryService.discover(registry);
            } catch (Exception e) {
                log.warn("Scheduled discovery failed for registry '{}' ({}): {}",
                        registry.getName(), registry.getType(), e.getMessage());
            }
        }
    }
}
