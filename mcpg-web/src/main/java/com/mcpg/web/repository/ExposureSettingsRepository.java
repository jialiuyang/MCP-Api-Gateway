package com.mcpg.web.repository;

import com.mcpg.web.entity.ExposureSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for the singleton {@link ExposureSettingsEntity} row.
 *
 * <p>No custom finders are needed; callers always read/write
 * {@link ExposureSettingsEntity#SINGLETON_ID}.</p>
 */
public interface ExposureSettingsRepository extends JpaRepository<ExposureSettingsEntity, Long> {
}
