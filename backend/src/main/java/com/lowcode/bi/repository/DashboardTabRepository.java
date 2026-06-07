package com.lowcode.bi.repository;

import com.lowcode.bi.entity.DashboardTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardTabRepository extends JpaRepository<DashboardTab, UUID> {

    @Query("SELECT dt FROM DashboardTab dt WHERE dt.dashboard.id = :dashboardId AND dt.deleted = false ORDER BY dt.position ASC")
    List<DashboardTab> findByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Query("SELECT dt FROM DashboardTab dt WHERE dt.dashboard.id = :dashboardId AND dt.isActive = true AND dt.deleted = false")
    Optional<DashboardTab> findActiveTabByDashboardId(@Param("dashboardId") UUID dashboardId);

    boolean existsByDashboardIdAndNameAndDeletedFalse(UUID dashboardId, String name);
}
