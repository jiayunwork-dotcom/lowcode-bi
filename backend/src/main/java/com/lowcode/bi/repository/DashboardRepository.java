package com.lowcode.bi.repository;

import com.lowcode.bi.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    @Query("SELECT d FROM Dashboard d WHERE d.tenant.id = :tenantId AND d.deleted = false")
    List<Dashboard> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Dashboard d WHERE d.tenant.id = :tenantId AND d.id = :id AND d.deleted = false")
    Optional<Dashboard> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Dashboard d WHERE d.tenant.id = :tenantId AND d.isPublished = true AND d.deleted = false")
    List<Dashboard> findPublishedByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Dashboard d WHERE d.tenant.id = :tenantId AND d.isTemplate = true AND d.deleted = false")
    List<Dashboard> findTemplatesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM Dashboard d WHERE d.isTemplate = true AND d.deleted = false")
    List<Dashboard> findAllTemplates();

    @Query("SELECT d FROM Dashboard d WHERE d.refreshInterval != 'OFF' AND d.refreshPaused = false AND d.deleted = false")
    List<Dashboard> findDashboardsNeedingRefresh();

    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.tenant.id = :tenantId AND d.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantIdAndDeletedFalse(String name, UUID tenantId);
}
