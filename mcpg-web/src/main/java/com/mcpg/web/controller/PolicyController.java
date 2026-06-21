package com.mcpg.web.controller;

import com.mcpg.web.dto.PolicyDto;
import com.mcpg.web.dto.UpdatePolicyRequest;
import com.mcpg.web.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<PolicyDto> list() {
        return policyService.list();
    }

    @PutMapping("/{id}")
    public PolicyDto update(@PathVariable Long id,
                            @Valid @RequestBody UpdatePolicyRequest req) {
        return policyService.update(id, req);
    }
}
