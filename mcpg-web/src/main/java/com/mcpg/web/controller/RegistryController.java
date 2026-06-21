package com.mcpg.web.controller;

import com.mcpg.web.dto.CreateRegistryRequest;
import com.mcpg.web.dto.DiscoveryResultDto;
import com.mcpg.web.dto.RegistryDto;
import com.mcpg.web.dto.RegistryTypeDto;
import com.mcpg.web.dto.TestConnectionResult;
import com.mcpg.web.dto.UpdateRegistryRequest;
import com.mcpg.web.service.RegistryDiscoveryService;
import com.mcpg.web.service.RegistryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for the Service Registry page in the UI.
 *
 * <p>Read-only endpoints are intentionally separated from action endpoints
 * ({@code /test}, {@code /discover}) so future authorization rules can apply
 * stricter checks to the latter (test/discover may hit production registries
 * with credentials).</p>
 */
@RestController
@RequestMapping("/api/registries")
public class RegistryController {

    private final RegistryService registryService;
    private final RegistryDiscoveryService discoveryService;

    public RegistryController(RegistryService registryService,
                              RegistryDiscoveryService discoveryService) {
        this.registryService = registryService;
        this.discoveryService = discoveryService;
    }

    @GetMapping
    public List<RegistryDto> list() {
        return registryService.list();
    }

    @GetMapping("/types")
    public List<RegistryTypeDto> types() {
        return registryService.listTypes();
    }

    @GetMapping("/{id}")
    public RegistryDto get(@PathVariable Long id) {
        return registryService.get(id);
    }

    @PostMapping
    public RegistryDto create(@Valid @RequestBody CreateRegistryRequest req) {
        return registryService.create(req);
    }

    @PutMapping("/{id}")
    public RegistryDto update(@PathVariable Long id, @Valid @RequestBody UpdateRegistryRequest req) {
        return registryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        registryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public TestConnectionResult test(@PathVariable Long id) {
        return registryService.test(id);
    }

    @PostMapping("/{id}/discover")
    public DiscoveryResultDto discover(@PathVariable Long id) {
        return discoveryService.discover(id);
    }
}
