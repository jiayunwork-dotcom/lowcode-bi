package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AlertRuleCreateRequest {
    private String name;
    private String description;
    private String dataModelId;
    private String measureId;
    private AlertTriggerType triggerType;
    private AlertOperator operator;
    private BigDecimal threshold;
    private AlertCheckInterval checkInterval;
    private Integer silencePeriod;
    private AlertSeverity severity;
    private List<NotificationChannelConfig> notificationChannels;
}

@Data
class NotificationChannelConfig {
    private NotificationChannelType type;
    private Map<String, Object> config;
    private Boolean enabled;
}
