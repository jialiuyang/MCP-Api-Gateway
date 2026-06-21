package com.mcpg.web.repository;

import com.mcpg.web.entity.RegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** JPA repository for {@link RegistryEntity}. */
public interface RegistryRepository extends JpaRepository<RegistryEntity, Long> {

    Optional<RegistryEntity> findByName(String name);

    /** Used by the periodic discovery scheduler to skip disabled rows. */
    List<RegistryEntity> findByEnabledTrue();
}
