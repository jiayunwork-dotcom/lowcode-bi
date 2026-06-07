package com.lowcode.bi.repository;

import com.lowcode.bi.entity.AlertNotificationChannel;
import com.lowcode.bi.common.enums.NotificationChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertNotificationChannelRepository extends JpaRepository<AlertNotificationChannel, UUID> {

    Optional<AlertNotificationChannel> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AlertNotificationChannel> findByAlertRuleIdAndTenantId(UUID alertRuleId, UUID tenantId);

    List<AlertNotificationChannel> findByAlertRuleIdAndIsEnabledTrue(UUID alertRuleId);

    List<AlertNotificationChannel> findByAlertRuleIdAndChannelTypeAndIsEnabledTrue(UUID alertRuleId, NotificationChannelType channelType);
}
