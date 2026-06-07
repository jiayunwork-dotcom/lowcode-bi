package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.NotificationChannelType;
import com.lowcode.bi.common.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_notifications")
public class AlertNotification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_event_id", nullable = false)
    private AlertEvent alertEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 32, nullable = false)
    private NotificationChannelType channelType;

    @Column(name = "recipient", length = 512, nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
}
