package com.mcpg.web.repository;

import com.mcpg.web.entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ToolRepository extends JpaRepository<ToolEntity, Long> {

    Optional<ToolEntity> findByToolName(String toolName);

    List<ToolEntity> findByServiceId(Long serviceId);

    void deleteByServiceId(Long serviceId);

    /**
     * Keyword search over the short, indexable columns.
     *
     * <p>The {@code description} column is intentionally excluded: it is
     * declared {@code @Lob} (potentially {@code TEXT}/{@code CLOB}) and
     * Hibernate 6's JPQL validator refuses to apply {@code LOWER(LIKE ...)}
     * to LOB-mapped attributes. Free-text search across long descriptions
     * is a B5 concern (full-text index) anyway.</p>
     */
    @Query("""
            SELECT t FROM ToolEntity t
            WHERE (:keyword IS NULL
                   OR LOWER(t.toolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(t.summary, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.path) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(t.tags, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:serviceId IS NULL OR t.serviceId = :serviceId)
            ORDER BY t.serviceId, t.toolName
            """)
    List<ToolEntity> search(@Param("keyword") String keyword,
                             @Param("serviceId") Long serviceId);

    List<ToolEntity> findByPromotedTrue();

    /** Live count of non-deprecated tools across all services. */
    long countByDeprecatedFalse();

    /** Live count of non-deprecated tools that have been promoted by an operator. */
    long countByPromotedTrueAndDeprecatedFalse();

    /** Used by DIRECT_ALL exposure mode; deprecated rows stay hidden. */
    List<ToolEntity> findByDeprecatedFalse();
}
