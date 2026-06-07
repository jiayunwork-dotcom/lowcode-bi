package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.AlertSeverity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AlertEventGroupResponse {
    private UUID ruleId;
    private String ruleName;
    private String dataModelName;
    private UUID dataModelId;
    private Long activeEventCount;
    private Long totalEventCount;
    private LocalDateTime lastTriggeredAt;
    private Double averageRecoveryMinutes;
    private SeverityDistribution severityDistribution;
    private List<AlertEventTimelineItem> timeline;
}

@Data
public class SeverityDistribution {
    private long infoCount;
    private long warningCount;
    private long criticalCount;
    private double infoPercent;
    private double warningPercent;
    private double criticalPercent;
}

@Data
public class AlertEventTimelineItem {
    private UUID eventId;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private AlertSeverity severity;
    private boolean isRecovered;
    private String triggerValue;
}
