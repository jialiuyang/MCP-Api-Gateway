package com.mcpg.web.service;

import com.mcpg.core.model.ExposureMode;
import com.mcpg.web.dto.ExposureSettingsDto;
import com.mcpg.web.dto.UpdateExposureRequest;
import com.mcpg.web.entity.ExposureSettingsEntity;
import com.mcpg.web.event.ToolsChangedEvent;
import com.mcpg.web.repository.ExposureSettingsRepository;
import com.mcpg.web.repository.ToolRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Owns the gateway-wide {@link ExposureMode} configuration.
 *
 * <p>The service ensures a singleton row exists on first boot
 * ({@link #ensureBootstrap()}), exposes a read model with live tool counters
 * for the UI, and publishes a {@link ToolsChangedEvent} whenever the active
 * mode changes so connected MCP clients refresh their tool list.</p>
 */
@Service
public class ExposureSettingsService {

    private static final Logger log = LoggerFactory.getLogger(ExposureSettingsService.class);

    /** Constant count of meta tools registered by the gateway (B2 baseline). */
    private static final int META_TOOL_COUNT = 4;

    private final ExposureSettingsRepository settingsRepository;
    private final ToolRepository toolRepository;
    private final ApplicationEventPublisher events;

    public ExposureSettingsService(ExposureSettingsRepository settingsRepository,
                                   ToolRepository toolRepository,
                                   ApplicationEventPublisher events) {
        this.settingsRepository = settingsRepository;
        this.toolRepository = toolRepository;
        this.events = events;
    }

    /**
     * Insert the singleton row with the default {@link ExposureMode#HYBRID}
     * mode if it doesn't already exist. Executed eagerly so the first call
     * to {@link #currentMode()} after startup never races with a missing row.
     */
    @PostConstruct
    @Transactional
    public void ensureBootstrap() {
        if (!settingsRepository.existsById(ExposureSettingsEntity.SINGLETON_ID)) {
            ExposureSettingsEntity row = new ExposureSettingsEntity();
            row.setId(ExposureSettingsEntity.SINGLETON_ID);
            row.setMode(ExposureMode.HYBRID);
            row.setUpdatedAt(Instant.now());
            settingsRepository.save(row);
            log.info("Initialized exposure settings with default mode HYBRID");
        }
    }

    /**
     * Resolve the currently active mode. Falls back to HYBRID if the
     * singleton row is missing for any reason (race during DDL recreation).
     */
    @Transactional(readOnly = true)
    public ExposureMode currentMode() {
        return settingsRepository.findById(ExposureSettingsEntity.SINGLETON_ID)
                .map(ExposureSettingsEntity::getMode)
                .orElse(ExposureMode.HYBRID);
    }

    @Transactional(readOnly = true)
    public ExposureSettingsDto get() {
        ExposureSettingsEntity row = settingsRepository
                .findById(ExposureSettingsEntity.SINGLETON_ID)
                .orElseGet(this::syntheticDefault);
        return toDto(row);
    }

    /**
     * Persist the new mode/note. Always fires {@link ToolsChangedEvent} even
     * when the mode is unchanged: the {@code note} alone may have changed but
     * leaving stale data on the client is harmless and we avoid second-guessing.
     */
    @Transactional
    public ExposureSettingsDto update(UpdateExposureRequest req) {
        ExposureSettingsEntity row = settingsRepository
                .findById(ExposureSettingsEntity.SINGLETON_ID)
                .orElseGet(this::syntheticDefault);
        ExposureMode previous = row.getMode();
        row.setId(ExposureSettingsEntity.SINGLETON_ID);
        row.setMode(req.getMode());
        row.setNote(req.getNote());
        row.setUpdatedAt(Instant.now());
        settingsRepository.save(row);

        if (previous != req.getMode()) {
            log.info("Exposure mode changed: {} -> {}", previous, req.getMode());
            events.publishEvent(new ToolsChangedEvent(
                    this, "exposure-mode-changed: " + previous + " -> " + req.getMode()));
        }
        return toDto(row);
    }

    private ExposureSettingsDto toDto(ExposureSettingsEntity row) {
        long total = toolRepository.countByDeprecatedFalse();
        long promoted = toolRepository.countByPromotedTrueAndDeprecatedFalse();
        int effective = switch (row.getMode()) {
            case META -> META_TOOL_COUNT;
            case HYBRID -> (int) (META_TOOL_COUNT + promoted);
            case DIRECT_ALL -> (int) total;
        };
        return new ExposureSettingsDto(
                row.getMode(),
                row.getNote(),
                row.getUpdatedBy(),
                row.getUpdatedAt(),
                total,
                promoted,
                META_TOOL_COUNT,
                effective);
    }

    private ExposureSettingsEntity syntheticDefault() {
        ExposureSettingsEntity row = new ExposureSettingsEntity();
        row.setId(ExposureSettingsEntity.SINGLETON_ID);
        row.setMode(ExposureMode.HYBRID);
        row.setUpdatedAt(Instant.now());
        return row;
    }
}
