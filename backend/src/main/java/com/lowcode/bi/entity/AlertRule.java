package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_rules")
public class AlertRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measure_id", nullable = false)
    private Measure measure;

    @Column(name = "measure_name", length = 128, nullable = false)
    private String measureName;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 32, nullable = false)
    private AlertTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", length = 32, nullable = false)
    private AlertOperator operator;

    @Column(name = "threshold", precision = 20, scale = 6, nullable = false)
    private BigDecimal threshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_interval", length = 32, nullable = false)
    private AlertCheckInterval checkInterval;

    @Column(name = "silence_period", nullable = false)
    private Integer silencePeriod = 300;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 32, nullable = false)
    private AlertSeverity severity;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private AlertRuleStatus status = AlertRuleStatus.ACTIVE;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<AlertNotificationChannel> notificationChannels = new HashSet<>();

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AlertSubscription> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AlertEvent> events = new HashSet<>();

    @Column(name = "escalation_enabled", nullable = false)
    private Boolean escalationEnabled = false;

    @Column(name = "escalation_threshold", nullable = false)
    private Integer escalationThreshold = 3;

    @Column(name = "consecutive_trigger_count", nullable = false)
    private Integer consecutiveTriggerCount = 0;

    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_severity", length = 32, nullable = false)
    private AlertSeverity currentSeverity = AlertSeverity.WARNING;

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<AlertEscalationRecipient> escalationRecipients = new HashSet<>();
}
