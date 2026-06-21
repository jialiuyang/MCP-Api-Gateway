package com.mcpg.web.repository;

import com.mcpg.core.model.Environment;
import com.mcpg.web.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    Optional<ServiceEntity> findByNameAndEnvironment(String name, Environment environment);

    List<ServiceEntity> findBySourceType(ServiceEntity.SourceType sourceType);

    @Query("SELECT s FROM ServiceEntity s WHERE " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(s.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:env IS NULL OR s.environment = :env)")
    List<ServiceEntity> search(String keyword, Environment env);
}
