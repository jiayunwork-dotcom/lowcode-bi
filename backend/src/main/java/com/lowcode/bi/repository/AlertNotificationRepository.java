package com.lowcode.bi.repository;

import com.lowcode.bi.entity.AlertNotification;
import com.lowcode.bi.common.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, UUID> {

    List<AlertNotification> findByAlertEventId(UUID eventId);

    List<AlertNotification> findByAlertRuleId(UUID ruleId);

    @Query("SELECT n FROM AlertNotification n WHERE n.status = :status AND n.retryCount < :maxRetries " +
           "AND (n.updatedAt IS NULL OR n.updatedAt <= :retryTime) ORDER BY n.createdAt ASC")
    List<AlertNotification> findPendingNotifications(@Param("status") NotificationStatus status,
                                                      @Param("maxRetries") int maxRetries,
                                                      @Param("retryTime") LocalDateTime retryTime);
}
