package com.lowcode.bi.repository;

import com.lowcode.bi.entity.DashboardPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardPermissionRepository extends JpaRepository<DashboardPermission, UUID> {

    @Query("SELECT dp FROM DashboardPermission dp WHERE dp.dashboard.id = :dashboardId AND dp.deleted = false")
    List<DashboardPermission> findByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Query("SELECT dp FROM DashboardPermission dp WHERE dp.user.id = :userId AND dp.deleted = false")
    List<DashboardPermission> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT dp FROM DashboardPermission dp WHERE dp.dashboard.id = :dashboardId AND dp.user.id = :userId AND dp.deleted = false")
    Optional<DashboardPermission> findByDashboardIdAndUserId(@Param("dashboardId") UUID dashboardId, @Param("userId") UUID userId);

    @Query("SELECT dp.dashboard.id FROM DashboardPermission dp WHERE dp.user.id = :userId AND dp.canView = true AND dp.deleted = false")
    List<UUID> findViewableDashboardIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT dp.dashboard.id FROM DashboardPermission dp WHERE dp.user.id = :userId AND dp.canEdit = true AND dp.deleted = false")
    List<UUID> findEditableDashboardIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT dp FROM DashboardPermission dp WHERE dp.dashboard.id = :dashboardId AND dp.isOwner = true AND dp.deleted = false")
    Optional<DashboardPermission> findOwnerByDashboardId(@Param("dashboardId") UUID dashboardId);

    boolean existsByDashboardIdAndUserIdAndDeletedFalse(UUID dashboardId, UUID userId);
}
