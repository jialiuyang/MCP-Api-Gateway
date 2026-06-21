package com.mcpg.web.controller;

import com.mcpg.core.model.Environment;
import com.mcpg.web.dto.ImportResultDto;
import com.mcpg.web.dto.ImportSwaggerRequest;
import com.mcpg.web.dto.ServiceDto;
import com.mcpg.web.dto.UpdateServiceRequest;
import com.mcpg.web.entity.ServiceEntity;
import com.mcpg.web.repository.ServiceRepository;
import com.mcpg.web.service.ServiceImportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final ServiceImportService serviceImportService;

    public ServiceController(ServiceRepository serviceRepository,
                             ServiceImportService serviceImportService) {
        this.serviceRepository = serviceRepository;
        this.serviceImportService = serviceImportService;
    }

    @GetMapping
    public List<ServiceDto> list(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Environment environment) {
        return serviceRepository.search(blankToNull(keyword), environment).stream()
                .map(ServiceDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ServiceDto get(@PathVariable Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service " + id + " not found"));
        return ServiceDto.from(entity);
    }

    @PostMapping("/import-swagger")
    public ImportResultDto importSwagger(@Valid @RequestBody ImportSwaggerRequest request) {
        return serviceImportService.importFromUrl(request);
    }

    @PutMapping("/{id}")
    public ServiceDto update(@PathVariable Long id,
                              @Valid @RequestBody UpdateServiceRequest request) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service " + id + " not found"));
        if (request.getDisplayName() != null) {
            entity.setDisplayName(request.getDisplayName().isBlank() ? null : request.getDisplayName());
        }
        if (request.getBaseUrl() != null && !request.getBaseUrl().isBlank()) {
            entity.setBaseUrl(request.getBaseUrl());
        }
        if (request.getEnvironment() != null) {
            entity.setEnvironment(request.getEnvironment());
        }
        return ServiceDto.from(serviceRepository.save(entity));
    }

    @PostMapping("/{id}/refresh")
    public ImportResultDto refresh(@PathVariable Long id) {
        return serviceImportService.refresh(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceImportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
