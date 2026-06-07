package com.lowcode.bi.repository;

import com.lowcode.bi.entity.AlertEvent;
import com.lowcode.bi.common.enums.AlertEventStatus;
import com.lowcode.bi.common.enums.AlertSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {

    Optional<AlertEvent> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AlertEvent> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AlertEvent> findByTenantIdAndEventStatus(UUID tenantId, AlertEventStatus status, Pageable pageable);

    Page<AlertEvent> findByTenantIdAndSeverity(UUID tenantId, AlertSeverity severity, Pageable pageable);

    Page<AlertEvent> findByTenantIdAndEventStatusAndSeverity(UUID tenantId, AlertEventStatus status, AlertSeverity severity, Pageable pageable);

    List<AlertEvent> findByAlertRuleIdAndTenantId(UUID alertRuleId, UUID tenantId, Pageable pageable);

    @Query("SELECT ae FROM AlertEvent ae WHERE ae.alertRule.id = :ruleId AND ae.eventStatus = 'FIRING' AND ae.isRecovered = false ORDER BY ae.triggeredAt DESC")
    List<AlertEvent> findLatestFiringEvent(@Param("ruleId") UUID ruleId, Pageable pageable);

    @Query("SELECT ae FROM AlertEvent ae WHERE ae.alertRule.id = :ruleId AND ae.triggeredAt >= :startTime ORDER BY ae.triggeredAt ASC")
    List<AlertEvent> findEventsForTrend(@Param("ruleId") UUID ruleId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(ae) FROM AlertEvent ae WHERE ae.tenant.id = :tenantId AND ae.eventStatus = 'FIRING' AND ae.isRecovered = false")
    long countActiveAlerts(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(ae) FROM AlertEvent ae WHERE ae.tenant.id = :tenantId AND ae.triggeredAt >= :startTime")
    long countNewAlertsSince(@Param("tenantId") UUID tenantId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (ae.resolvedAt - ae.triggeredAt))) " +
           "FROM AlertEvent ae WHERE ae.tenant.id = :tenantId AND ae.isRecovered = true AND ae.resolvedAt IS NOT NULL")
    Double getAverageRecoveryTime(@Param("tenantId") UUID tenantId);

    @Query("SELECT ae.triggeredAt as time, ae.triggerValue as value FROM AlertEvent ae " +
           "WHERE ae.alertRule.id = :ruleId AND ae.triggeredAt >= :startTime " +
           "ORDER BY ae.triggeredAt ASC")
    List<Object[]> getMetricTrendData(@Param("ruleId") UUID ruleId, @Param("startTime") LocalDateTime startTime);

    List<AlertEvent> findByAlertRuleIdAndIsRecoveredFalse(UUID ruleId);
}
