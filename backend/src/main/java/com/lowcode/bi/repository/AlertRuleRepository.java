package com.lowcode.bi.repository;

import com.lowcode.bi.common.enums.AlertRuleStatus;
import com.lowcode.bi.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    Optional<AlertRule> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AlertRule> findByTenantId(UUID tenantId);

    Page<AlertRule> findByTenantId(UUID tenantId, Pageable pageable);

    List<AlertRule> findByDataModelIdAndTenantId(UUID dataModelId, UUID tenantId);

    List<AlertRule> findByMeasureIdAndTenantId(UUID measureId, UUID tenantId);

    @Query("SELECT ar FROM AlertRule ar WHERE ar.tenant.id = :tenantId AND ar.isEnabled = true AND ar.status = 'ACTIVE'")
    List<AlertRule> findActiveRules(@Param("tenantId") UUID tenantId);

    @Query("SELECT ar FROM AlertRule ar WHERE ar.isEnabled = true AND ar.status = 'ACTIVE'")
    List<AlertRule> findAllActiveRules();

    @Query("SELECT ar FROM AlertRule ar WHERE ar.isEnabled = true AND ar.status = 'ACTIVE' AND (ar.lastCheckedAt IS NULL OR ar.lastCheckedAt <= :checkTime)")
    List<AlertRule> findRulesToCheck(@Param("checkTime") LocalDateTime checkTime);

    @Query("SELECT ar.alertRule.id as ruleId, ar.alertRule.name as ruleName, COUNT(ae.id) as eventCount " +
           "FROM AlertEvent ae JOIN ae.alertRule ar " +
           "WHERE ar.tenant.id = :tenantId AND ae.triggeredAt >= :startTime " +
           "GROUP BY ar.alertRule.id, ar.alertRule.name " +
           "ORDER BY eventCount DESC")
    List<Object[]> findTopTriggeredRules(@Param("tenantId") UUID tenantId, @Param("startTime") LocalDateTime startTime, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, AlertRuleStatus status);

    @Query("SELECT ar FROM AlertRule ar JOIN ar.subscriptions s " +
           "WHERE ar.tenant.id = :tenantId AND s.user.id = :userId AND s.isSubscribed = true " +
           "ORDER BY s.subscribedAt DESC")
    Page<AlertRule> findSubscribedRulesByUserAndTenant(
        @Param("userId") UUID userId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable);
}
