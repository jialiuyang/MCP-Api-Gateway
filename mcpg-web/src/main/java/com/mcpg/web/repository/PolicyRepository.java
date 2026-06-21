package com.mcpg.web.repository;

import com.mcpg.web.entity.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<PolicyEntity, Long> {

    Optional<PolicyEntity> findByPolicyKey(String policyKey);
}
