package com.lowcode.bi.repository;

import com.lowcode.bi.entity.DashboardComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardComponentRepository extends JpaRepository<DashboardComponent, UUID> {

    @Query("SELECT c FROM DashboardComponent c WHERE c.dashboard.id = :dashboardId AND c.deleted = false")
    List<DashboardComponent> findByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Query("SELECT c FROM DashboardComponent c WHERE c.tab.id = :tabId AND c.deleted = false ORDER BY c.positionY ASC, c.positionX ASC")
    List<DashboardComponent> findByTabId(@Param("tabId") UUID tabId);

    @Query("SELECT c FROM DashboardComponent c WHERE c.dashboard.id = :dashboardId AND c.id = :id AND c.deleted = false")
    Optional<DashboardComponent> findByIdAndDashboardId(@Param("id") UUID id, @Param("dashboardId") UUID dashboardId);

    @Query("SELECT COUNT(c) FROM DashboardComponent c WHERE c.dashboard.id = :dashboardId AND c.deleted = false")
    long countByDashboardId(@Param("dashboardId") UUID dashboardId);
}
