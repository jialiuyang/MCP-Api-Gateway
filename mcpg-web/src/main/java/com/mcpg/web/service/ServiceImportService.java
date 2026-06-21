package com.mcpg.web.service;

import com.mcpg.core.exception.McpgException;
import com.mcpg.core.model.ParsedSpec;
import com.mcpg.core.spi.OpenApiSourceParser;
import com.mcpg.web.dto.ImportResultDto;
import com.mcpg.web.dto.ImportSwaggerRequest;
import com.mcpg.web.dto.ServiceDto;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.entity.ServiceSpecEntity;
import com.mcpg.web.event.ToolsChangedEvent;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.repository.ServiceSpecRepository;
import com.mcpg.web.repository.ToolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * Orchestrates the full "import Swagger URL" workflow.
 *
 * <ol>
 *   <li>Fetch raw spec content from {@link SwaggerFetcher}.</li>
 *   <li>Pick a parser through {@link ParserRegistry} and produce a
 *       {@link ParsedSpec}.</li>
 *   <li>Upsert the {@link ServiceEntity}.</li>
 *   <li>Replace the latest {@link ServiceSpecEntity}.</li>
 *   <li>Hand off to {@link ToolSyncService} to merge tool rows.</li>
 *   <li>Update {@code toolCount} and {@code lastSyncedAt} on the service.</li>
 * </ol>
 *
 * <p>The method runs in a single transaction so that a parser failure leaves
 * the previously-good state untouched.</p>
 */
@Service
public class ServiceImportService {

    private static final Logger log = LoggerFactory.getLogger(ServiceImportService.class);

    private final SwaggerFetcher fetcher;
    private final ParserRegistry parserRegistry;
    private final ServiceRepository serviceRepository;
    private final ServiceSpecRepository specRepository;
    private final ToolRepository toolRepository;
    private final ToolSyncService toolSyncService;
    private final ApplicationEventPublisher eventPublisher;

    public ServiceImportService(SwaggerFetcher fetcher,
                                 ParserRegistry parserRegistry,
                                 ServiceRepository serviceRepository,
                                 ServiceSpecRepository specRepository,
                                 ToolRepository toolRepository,
                                 ToolSyncService toolSyncService,
                                 ApplicationEventPublisher eventPublisher) {
        this.fetcher = fetcher;
        this.parserRegistry = parserRegistry;
        this.serviceRepository = serviceRepository;
        this.specRepository = specRepository;
        this.toolRepository = toolRepository;
        this.toolSyncService = toolSyncService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ImportResultDto importFromUrl(ImportSwaggerRequest req) {
        log.info("Importing service '{}' from {}", req.getName(), req.getUrl());

        SwaggerFetcher.Result fetched = fetcher.fetch(req.getUrl());
        OpenApiSourceParser parser = parserRegistry.pick(fetched.body());
        log.info("Parser selected: {}", parser.getType());

        // Always seed the parser with the origin of the doc URL so that
        // relative ``servers[].url`` entries (e.g. Petstore's "/api/v3") are
        // resolved to absolute URLs. An operator-supplied override is
        // applied AFTER parsing so it wins outright over whatever the spec
        // declares.
        String docOrigin = originOf(fetched.resolvedUrl());
        ParsedSpec parsed = parser.parse(fetched.body(), docOrigin);

        String effectiveBaseUrl = (req.getBaseUrl() != null && !req.getBaseUrl().isBlank())
                ? req.getBaseUrl().trim()
                : parsed.getBaseUrl();
        if (effectiveBaseUrl == null || effectiveBaseUrl.isBlank()
                || !looksLikeAbsoluteHttpUrl(effectiveBaseUrl)) {
            throw new McpgException(
                    "Could not resolve a usable base URL for service " + req.getName()
                            + " (parsed='" + parsed.getBaseUrl() + "', "
                            + "doc origin='" + docOrigin + "'). "
                            + "Provide one explicitly via the 'Base URL' field.");
        }

        ServiceEntity entity = serviceRepository
                .findByNameAndEnvironment(req.getName(), req.getEnvironment())
                .orElseGet(ServiceEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setName(req.getName());
        entity.setDisplayName(Optional.ofNullable(req.getDisplayName()).orElse(parsed.getTitle()));
        entity.setEnvironment(req.getEnvironment());
        // Honour an explicit sourceType from registry-driven discovery; fall
        // back to MANUAL for human-driven imports so legacy callers keep
        // working unchanged.
        entity.setSourceType(req.getSourceType() == null
                ? ServiceEntity.SourceType.MANUAL
                : req.getSourceType());
        entity.setSourceRef(req.getSourceRef() == null || req.getSourceRef().isBlank()
                ? fetched.resolvedUrl()
                : req.getSourceRef());
        entity.setSpecUrl(fetched.resolvedUrl());
        entity.setBaseUrl(effectiveBaseUrl);
        entity.setStatus(ServiceEntity.Status.ACTIVE);
        entity.setLastError(null);
        entity.setLastSyncedAt(Instant.now());
        entity = serviceRepository.save(entity);

        ServiceSpecEntity specEntity = new ServiceSpecEntity();
        specEntity.setServiceId(entity.getId());
        specEntity.setSpecVersion(parsed.getSpecVersion());
        specEntity.setTitle(parsed.getTitle());
        specEntity.setApiVersion(parsed.getVersion());
        specEntity.setRawContent(parsed.getRawSpec() == null ? fetched.body() : parsed.getRawSpec());
        specEntity.setOperationCount(parsed.getOperations() == null ? 0 : parsed.getOperations().size());
        specEntity.setFetchedAt(Instant.now());
        specRepository.save(specEntity);

        ToolSyncService.Stats stats = toolSyncService.sync(entity, parsed);
        entity.setToolCount(stats.total());
        serviceRepository.save(entity);

        log.info("Service '{}' imported: {} added, {} updated, {} removed (total {})",
                entity.getName(), stats.added(), stats.updated(), stats.removed(), stats.total());

        if (stats.added() + stats.removed() + stats.updated() > 0) {
            eventPublisher.publishEvent(new ToolsChangedEvent(this, "import:" + entity.getName()));
        }

        return ImportResultDto.builder()
                .service(ServiceDto.from(entity))
                .toolCount(stats.total())
                .added(stats.added() + (isNew ? 0 : 0))
                .updated(stats.updated())
                .removed(stats.removed())
                .specVersion(parsed.getSpecVersion())
                .build();
    }

    /**
     * Re-pull and re-parse a previously imported service. This is called both
     * by the daily refresh job and by the operator-driven "refresh" button.
     */
    @Transactional
    public ImportResultDto refresh(Long serviceId) {
        ServiceEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new McpgException("Service " + serviceId + " not found"));
        if (entity.getSpecUrl() == null || entity.getSpecUrl().isBlank()) {
            throw new McpgException("Service " + entity.getName() + " has no spec URL configured");
        }
        ImportSwaggerRequest req = new ImportSwaggerRequest();
        req.setName(entity.getName());
        req.setDisplayName(entity.getDisplayName());
        req.setUrl(entity.getSpecUrl());
        req.setBaseUrl(entity.getBaseUrl());
        req.setEnvironment(entity.getEnvironment());
        return importFromUrl(req);
    }

    @Transactional
    public void delete(Long serviceId) {
        toolRepository.deleteByServiceId(serviceId);
        specRepository.deleteByServiceId(serviceId);
        serviceRepository.deleteById(serviceId);
        eventPublisher.publishEvent(new ToolsChangedEvent(this, "delete:" + serviceId));
    }

    private String originOf(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getAuthority() == null) return null;
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean looksLikeAbsoluteHttpUrl(String s) {
        return s != null
                && (s.regionMatches(true, 0, "http://", 0, 7)
                        || s.regionMatches(true, 0, "https://", 0, 8));
    }
}
