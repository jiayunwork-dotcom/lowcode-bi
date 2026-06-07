package com.lowcode.bi.repository;

import com.lowcode.bi.entity.AlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, UUID> {

    Optional<AlertSubscription> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<AlertSubscription> findByAlertRuleIdAndUserIdAndTenantId(UUID alertRuleId, UUID userId, UUID tenantId);

    List<AlertSubscription> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<AlertSubscription> findByAlertRuleIdAndIsSubscribedTrue(UUID alertRuleId);

    @Query("SELECT s FROM AlertSubscription s WHERE s.alertRule.id = :ruleId AND s.isSubscribed = true")
    List<AlertSubscription> findSubscribersForRule(@Param("ruleId") UUID ruleId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AlertSubscription s " +
           "WHERE s.alertRule.id = :ruleId AND s.user.id = :userId AND s.isSubscribed = true")
    boolean isUserSubscribed(@Param("ruleId") UUID ruleId, @Param("userId") UUID userId);
}
