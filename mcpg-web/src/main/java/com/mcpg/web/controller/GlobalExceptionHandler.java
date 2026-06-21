package com.mcpg.web.controller;

import com.mcpg.core.exception.McpgException;
import com.mcpg.core.exception.ParserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central exception → JSON mapping for the REST API and SPA forwarding for
 * client-side routes.
 *
 * <p>Two behaviors live together here on purpose:</p>
 * <ul>
 *   <li>For real API errors, return the stable
 *       {@code {timestamp, status, code, message}} envelope. The SPA's axios
 *       interceptor depends on this shape.</li>
 *   <li>For {@link NoResourceFoundException} (raised by Spring 6.1+ when a
 *       static resource is missing), forward to {@code /index.html} so that
 *       Vue Router can take over - unless the path looks like a backend
 *       endpoint, in which case we surface a normal 404.</li>
 * </ul>
 */
@RestControllerAdvice
@Order(0)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Spring 6.1+ throws this for every unmatched GET against a static
     * resource. We translate it into:
     * <ul>
     *   <li>A {@code forward:/index.html} for SPA-style paths so deep links
     *       and hard refreshes work, and</li>
     *   <li>A real 404 JSON for backend paths so an API client never sees
     *       HTML.</li>
     * </ul>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleStaticNotFound(NoResourceFoundException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) path = "";
        if (isBackendPath(path) || looksLikeAsset(path)) {
            return body(HttpStatus.NOT_FOUND, "not_found", "No resource: " + path);
        }
        log.debug("Forwarding SPA path {} → /index.html", path);
        return new ModelAndView("forward:/index.html");
    }

    @ExceptionHandler(ParserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleParserMissing(ParserNotFoundException e) {
        log.warn("Parser missing: {}", e.getMessage());
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "parser_not_found", e.getMessage());
    }

    @ExceptionHandler(McpgException.class)
    public ResponseEntity<Map<String, Object>> handleMcpg(McpgException e) {
        log.warn("Gateway error: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, "gateway_error", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return body(HttpStatus.BAD_REQUEST, "validation_error", msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException e) {
        return body(HttpStatus.BAD_REQUEST, "validation_error", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        log.error("Unhandled exception", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                e.getMessage() == null ? "Internal server error" : e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("timestamp", Instant.now().toString());
        out.put("status", status.value());
        out.put("code", code);
        out.put("message", message);
        return ResponseEntity.status(status).body(out);
    }

    private boolean isBackendPath(String path) {
        return path.startsWith("/api")
                || path.startsWith("/mcp")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }

    /**
     * Heuristic: paths whose last segment contains a dot are treated as
     * static assets (favicon.ico, logo.svg, app.js, etc.) and should not be
     * masqueraded as the SPA shell.
     */
    private boolean looksLikeAsset(String path) {
        int lastSlash = path.lastIndexOf('/');
        int dot = path.indexOf('.', Math.max(0, lastSlash));
        return dot > 0;
    }
}
