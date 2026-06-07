package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.AlertEventStatus;
import com.lowcode.bi.common.enums.AlertSeverity;
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
@Table(name = "alert_events")
public class AlertEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @Column(name = "trigger_value", precision = 20, scale = 6, nullable = false)
    private BigDecimal triggerValue;

    @Column(name = "threshold", precision = 20, scale = 6, nullable = false)
    private BigDecimal threshold;

    @Column(name = "previous_value", precision = 20, scale = 6)
    private BigDecimal previousValue;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 32, nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 32, nullable = false)
    private AlertEventStatus eventStatus = AlertEventStatus.FIRING;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    @Column(name = "is_recovered", nullable = false)
    private Boolean isRecovered = false;

    @Column(name = "recovery_value", precision = 20, scale = 6)
    private BigDecimal recoveryValue;

    @OneToMany(mappedBy = "alertEvent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AlertNotification> notifications = new HashSet<>();
}
