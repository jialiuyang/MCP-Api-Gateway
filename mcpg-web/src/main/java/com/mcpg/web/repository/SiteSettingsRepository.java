package com.mcpg.web.repository;

import com.mcpg.web.entity.SiteSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettingsEntity, Long> {
}
