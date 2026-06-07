package com.lowcode.bi.repository;

import com.lowcode.bi.entity.ScheduleExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleExecutionLogRepository extends JpaRepository<ScheduleExecutionLog, UUID> {

    List<ScheduleExecutionLog> findByScheduleIdOrderByExecutionTimeDesc(UUID scheduleId);

    Page<ScheduleExecutionLog> findByScheduleIdOrderByExecutionTimeDesc(UUID scheduleId, Pageable pageable);

    @Query("SELECT sel FROM ScheduleExecutionLog sel WHERE sel.schedule.tenant.id = :tenantId " +
           "AND sel.executionTime >= :startTime ORDER BY sel.executionTime DESC")
    List<ScheduleExecutionLog> findByTenantIdAndExecutionTimeAfter(
            @Param("tenantId") UUID tenantId,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT sel FROM ScheduleExecutionLog sel WHERE sel.schedule.id = :scheduleId " +
           "AND sel.status = 'SUCCESS' ORDER BY sel.executionTime DESC LIMIT 1")
    Optional<ScheduleExecutionLog> findLastSuccessByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT COUNT(sel) FROM ScheduleExecutionLog sel WHERE sel.schedule.id = :scheduleId " +
           "AND sel.status = :status AND sel.executionTime >= :startTime")
    long countByScheduleIdAndStatusAndExecutionTimeAfter(
            @Param("scheduleId") UUID scheduleId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduleExecutionLog sel WHERE sel.scheduleId = :scheduleId")
    int deleteByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduleExecutionLog sel WHERE sel.executionTime < :cutoffTime")
    int deleteOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT sel FROM ScheduleExecutionLog sel WHERE sel.schedule.id = :scheduleId " +
           "AND sel.retryAttempt > 0 ORDER BY sel.executionTime DESC")
    List<ScheduleExecutionLog> findRetryLogsByScheduleId(@Param("scheduleId") UUID scheduleId);
}
