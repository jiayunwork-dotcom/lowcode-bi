package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_subscriptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "alert_rule_id", "user_id"})
})
public class AlertSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_subscribed", nullable = false)
    private Boolean isSubscribed = true;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt = LocalDateTime.now();

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
}
