package com.mcpg.web.controller;

import com.mcpg.web.dto.PromoteRequest;
import com.mcpg.web.dto.ToolDto;
import com.mcpg.web.entity.ToolEntity;
import com.mcpg.web.event.ToolsChangedEvent;
import com.mcpg.web.repository.ToolRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRepository toolRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ToolController(ToolRepository toolRepository, ApplicationEventPublisher eventPublisher) {
        this.toolRepository = toolRepository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    public List<ToolDto> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long serviceId) {
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        return toolRepository.search(safeKeyword, serviceId).stream()
                .map(t -> ToolDto.from(t, false))
                .toList();
    }

    @GetMapping("/{id}")
    public ToolDto get(@PathVariable Long id) {
        ToolEntity entity = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool " + id + " not found"));
        return ToolDto.from(entity, true);
    }

    @PostMapping("/{id}/promote")
    public ToolDto promote(@PathVariable Long id, @RequestBody PromoteRequest request) {
        ToolEntity entity = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tool " + id + " not found"));
        boolean changed = entity.isPromoted() != request.isPromoted();
        entity.setPromoted(request.isPromoted());
        toolRepository.save(entity);
        if (changed) {
            eventPublisher.publishEvent(new ToolsChangedEvent(this, "promote:" + entity.getToolName()));
        }
        return ToolDto.from(entity, false);
    }
}
