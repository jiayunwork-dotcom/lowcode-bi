package com.lowcode.bi.repository;

import com.lowcode.bi.entity.ScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleConfigRepository extends JpaRepository<ScheduleConfig, UUID> {

    @Query("SELECT sc FROM ScheduleConfig sc WHERE sc.tenant.id = :tenantId AND sc.deleted = false")
    List<ScheduleConfig> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT sc FROM ScheduleConfig sc WHERE sc.dashboard.id = :dashboardId AND sc.deleted = false")
    List<ScheduleConfig> findByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Query("SELECT sc FROM ScheduleConfig sc WHERE sc.tenant.id = :tenantId AND sc.id = :id AND sc.deleted = false")
    Optional<ScheduleConfig> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT sc FROM ScheduleConfig sc WHERE sc.status = 'ACTIVE' AND sc.deleted = false")
    List<ScheduleConfig> findActiveSchedules();

    @Query("SELECT sc FROM ScheduleConfig sc WHERE sc.jobKey = :jobKey AND sc.deleted = false")
    Optional<ScheduleConfig> findByJobKey(@Param("jobKey") String jobKey);
}
