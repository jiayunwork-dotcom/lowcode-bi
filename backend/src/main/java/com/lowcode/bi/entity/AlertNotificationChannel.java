package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.NotificationChannelType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_notification_channels")
public class AlertNotificationChannel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 32, nullable = false)
    private NotificationChannelType channelType;

    @Column(name = "config", columnDefinition = "json", nullable = false)
    private String config;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
}
