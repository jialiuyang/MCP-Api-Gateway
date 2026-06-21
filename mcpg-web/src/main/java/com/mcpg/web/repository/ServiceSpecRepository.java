package com.mcpg.web.repository;

import com.mcpg.web.entity.ServiceSpecEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceSpecRepository extends JpaRepository<ServiceSpecEntity, Long> {

    /**
     * Returns the latest spec row for a service. The "latest" wins-by-id
     * heuristic is sufficient because {@code id} is a strictly monotonic
     * identity column.
     */
    Optional<ServiceSpecEntity> findFirstByServiceIdOrderByIdDesc(Long serviceId);

    void deleteByServiceId(Long serviceId);
}
