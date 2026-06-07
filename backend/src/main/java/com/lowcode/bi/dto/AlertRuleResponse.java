package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AlertRuleResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID dataModelId;
    private String dataModelName;
    private UUID measureId;
    private String measureName;
    private AlertTriggerType triggerType;
    private AlertOperator operator;
    private BigDecimal threshold;
    private AlertCheckInterval checkInterval;
    private Integer silencePeriod;
    private AlertSeverity severity;
    private Boolean isEnabled;
    private AlertRuleStatus status;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime lastCheckedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<NotificationChannelResponse> notificationChannels;
    private Long eventCount;
    private Long subscriberCount;
}

@Data
class NotificationChannelResponse {
    private UUID id;
    private NotificationChannelType type;
    private Object config;
    private Boolean enabled;
}
