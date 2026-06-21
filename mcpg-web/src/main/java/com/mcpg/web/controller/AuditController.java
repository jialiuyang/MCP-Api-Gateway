package com.mcpg.web.controller;

import com.mcpg.web.dto.AuditPageDto;
import com.mcpg.web.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only audit log surface.
 *
 * <p>Currently backed by an in-memory synthetic generator
 * ({@link AuditService}); the persistence-backed implementation will land
 * after the open-source release. The HTTP shape is intentionally stable
 * so the UI does not need to change.</p>
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    public AuditPageDto list(@RequestParam(required = false) String outcome,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return auditService.page(outcome, keyword, page, size);
    }
}
