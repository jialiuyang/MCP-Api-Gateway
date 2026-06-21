package com.mcpg.web.service;

import com.mcpg.web.dto.SiteSettingsDto;
import com.mcpg.web.dto.UpdateSiteSettingsRequest;
import com.mcpg.web.entity.SiteSettingsEntity;
import com.mcpg.web.repository.SiteSettingsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Owns the singleton {@link SiteSettingsEntity} row.
 *
 * <p>Mirror of {@link ExposureSettingsService} - kept separate because the
 * two surfaces evolve independently and conflating them would force the UI
 * to refetch unrelated state on every change.</p>
 */
@Service
public class SiteSettingsService {

    private static final Logger log = LoggerFactory.getLogger(SiteSettingsService.class);

    private final SiteSettingsRepository repository;

    public SiteSettingsService(SiteSettingsRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void ensureBootstrap() {
        if (!repository.existsById(SiteSettingsEntity.SINGLETON_ID)) {
            SiteSettingsEntity row = new SiteSettingsEntity();
            row.setId(SiteSettingsEntity.SINGLETON_ID);
            row.setUpdatedAt(Instant.now());
            repository.save(row);
            log.info("Initialized site settings with defaults");
        }
    }

    @Transactional(readOnly = true)
    public SiteSettingsDto get() {
        SiteSettingsEntity row = repository.findById(SiteSettingsEntity.SINGLETON_ID)
                .orElseGet(this::syntheticDefault);
        return SiteSettingsDto.from(row);
    }

    @Transactional
    public SiteSettingsDto update(UpdateSiteSettingsRequest req) {
        SiteSettingsEntity row = repository.findById(SiteSettingsEntity.SINGLETON_ID)
                .orElseGet(this::syntheticDefault);
        if (req.getSiteName() != null && !req.getSiteName().isBlank()) {
            row.setSiteName(req.getSiteName().trim());
        }
        if (req.getDefaultEnvironment() != null) {
            row.setDefaultEnvironment(req.getDefaultEnvironment());
        }
        if (req.getRefreshCron() != null && !req.getRefreshCron().isBlank()) {
            row.setRefreshCron(req.getRefreshCron().trim());
        }
        if (req.getMaxToolsPerService() != null) {
            row.setMaxToolsPerService(req.getMaxToolsPerService());
        }
        if (req.getSsoEnabled() != null) {
            row.setSsoEnabled(req.getSsoEnabled());
        }
        if (req.getAuditRetentionDays() != null) {
            row.setAuditRetentionDays(req.getAuditRetentionDays());
        }
        if (req.getDemoMode() != null) {
            row.setDemoMode(req.getDemoMode());
        }
        row.setId(SiteSettingsEntity.SINGLETON_ID);
        row.setUpdatedAt(Instant.now());
        repository.save(row);
        log.info("Site settings updated");
        return SiteSettingsDto.from(row);
    }

    private SiteSettingsEntity syntheticDefault() {
        SiteSettingsEntity row = new SiteSettingsEntity();
        row.setId(SiteSettingsEntity.SINGLETON_ID);
        row.setUpdatedAt(Instant.now());
        return row;
    }
}
