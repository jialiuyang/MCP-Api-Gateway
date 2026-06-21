package com.mcpg.web.config;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Forwards SPA deep links to {@code index.html} so that Vue Router (HTML5
 * history mode) can take over after the page loads.
 *
 * <p>The approach is to register an {@link ErrorViewResolver} that intercepts
 * 404s and, when the request is not an API call or a static asset, returns
 * the SPA shell. This keeps the configuration tiny and avoids brittle
 * regular expressions in view-controller routing.</p>
 */
@Configuration
public class SpaForwardingConfig {

    @Bean
    public ErrorViewResolver spaErrorViewResolver() {
        return (HttpServletRequest request, HttpStatus status, Map<String, Object> model) -> {
            if (status != HttpStatus.NOT_FOUND) {
                return null;
            }
            String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");
            if (path == null) {
                path = request.getRequestURI();
            }
            if (path == null || isBackendPath(path) || looksLikeAsset(path)) {
                return null;
            }
            return new ModelAndView("forward:/index.html");
        };
    }

    /**
     * Whether the path belongs to one of the backend prefixes that should
     * surface a real 404 instead of the SPA shell.
     */
    private boolean isBackendPath(String path) {
        return path.startsWith("/api")
                || path.startsWith("/mcp")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }

    /**
     * Heuristic: paths that contain a file extension are treated as static
     * assets, which the SPA should never claim ownership over.
     */
    private boolean looksLikeAsset(String path) {
        int lastSlash = path.lastIndexOf('/');
        int dot = path.indexOf('.', Math.max(0, lastSlash));
        return dot > 0;
    }
}
