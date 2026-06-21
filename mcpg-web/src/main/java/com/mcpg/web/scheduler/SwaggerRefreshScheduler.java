package com.mcpg.web.scheduler;

import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.service.ServiceImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically re-pulls every known service's OpenAPI / Swagger document.
 *
 * <p>The cron expression is configurable via {@code mcpg.scheduler.swagger-refresh-cron}
 * and defaults to 03:00 local time every day. The job iterates services
 * sequentially and swallows per-service exceptions so a single bad spec
 * never blocks the rest of the refresh.</p>
 */
@Component
public class SwaggerRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(SwaggerRefreshScheduler.class);

    private final ServiceRepository serviceRepository;
    private final ServiceImportService importService;
    private final boolean enabled;

    public SwaggerRefreshScheduler(ServiceRepository serviceRepository,
                                   ServiceImportService importService,
                                   @Value("${mcpg.scheduler.swagger-refresh.enabled:true}") boolean enabled) {
        this.serviceRepository = serviceRepository;
        this.importService = importService;
        this.enabled = enabled;
    }

    /**
     * Runs daily at 03:00 by default. The actual expression is taken from
     * {@code mcpg.scheduler.swagger-refresh.cron} so production deployments
     * can stagger refreshes across instances.
     */
    @Scheduled(cron = "${mcpg.scheduler.swagger-refresh.cron:0 0 3 * * *}")
    public void refreshAll() {
        if (!enabled) {
            log.debug("Swagger refresh scheduler is disabled");
            return;
        }
        long start = System.currentTimeMillis();
        int total = 0;
        int ok = 0;
        int failed = 0;
        for (ServiceEntity svc : serviceRepository.findAll()) {
            if (svc.getSpecUrl() == null || svc.getSpecUrl().isBlank()) continue;
            total++;
            try {
                importService.refresh(svc.getId());
                ok++;
            } catch (Exception e) {
                failed++;
                log.warn("Scheduled refresh failed for service '{}' ({}): {}",
                        svc.getName(), svc.getSpecUrl(), e.getMessage());
            }
        }
        log.info("Scheduled Swagger refresh finished in {} ms: total={}, ok={}, failed={}",
                System.currentTimeMillis() - start, total, ok, failed);
    }
}
