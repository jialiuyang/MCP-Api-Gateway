package com.mcpg.web.startup;

import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

/**
 * One-shot data migration that fixes services whose persisted {@code baseUrl}
 * is a relative path instead of an absolute URL.
 *
 * <p>This is the classic OpenAPI 3 pitfall: many generators (FastAPI's default
 * config, Springdoc's relative-path mode, the Swagger Petstore sample, ...)
 * emit {@code servers[0].url = "/api/v3"}, which is legal per spec but only
 * meaningful when paired with the location where the document is served. The
 * fix landed in the parser, but pre-existing rows imported with the broken
 * version of the code keep their relative {@code baseUrl}; this runner heals
 * them on the next startup.</p>
 *
 * <p>The healer is idempotent and only writes rows whose value actually
 * changes, so it costs essentially nothing on subsequent boots.</p>
 */
@Component
@Order(10)
public class BadBaseUrlHealer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BadBaseUrlHealer.class);

    private final ServiceRepository serviceRepository;

    public BadBaseUrlHealer(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ServiceEntity> all = serviceRepository.findAll();
        int healed = 0;
        for (ServiceEntity s : all) {
            String current = s.getBaseUrl();
            if (looksAbsolute(current)) continue;
            String fixed = repair(current, s.getSpecUrl());
            if (fixed == null || fixed.equals(current)) {
                log.warn("Service '{}' has non-absolute baseUrl '{}' but no recoverable spec URL "
                                + "to derive a host from; manual fix required (edit on Services page).",
                        s.getName(), current);
                continue;
            }
            log.warn("Healing service '{}': baseUrl '{}' -> '{}'", s.getName(), current, fixed);
            s.setBaseUrl(fixed);
            serviceRepository.save(s);
            healed++;
        }
        if (healed > 0) {
            log.info("BadBaseUrlHealer: fixed {} service(s) with non-absolute baseUrl", healed);
        }
    }

    private boolean looksAbsolute(String url) {
        return url != null
                && (url.regionMatches(true, 0, "http://", 0, 7)
                        || url.regionMatches(true, 0, "https://", 0, 8));
    }

    private String repair(String relative, String specUrl) {
        if (relative == null || specUrl == null) return null;
        String origin = originOf(specUrl);
        if (origin == null) return null;
        String suffix = relative.startsWith("/") ? relative : "/" + relative;
        String combined = origin + suffix;
        return stripTrailingSlash(combined);
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

    private String stripTrailingSlash(String s) {
        return s.endsWith("/") && s.length() > 1 ? s.substring(0, s.length() - 1) : s;
    }
}
