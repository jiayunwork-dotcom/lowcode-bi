package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AlertRuleUpdateRequest {
    private String name;
    private String description;
    private AlertTriggerType triggerType;
    private AlertOperator operator;
    private BigDecimal threshold;
    private AlertCheckInterval checkInterval;
    private Integer silencePeriod;
    private AlertSeverity severity;
    private Boolean isEnabled;
    private List<NotificationChannelConfig> notificationChannels;
    private Boolean escalationEnabled;
    private Integer escalationThreshold;
    private List<String> escalationRecipientUserIds;
}

@Data
class UpdateChannelConfig {
    private String id;
    private NotificationChannelType type;
    private Map<String, Object> config;
    private Boolean enabled;
}
