package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AlertEventResponse {
    private UUID id;
    private UUID alertRuleId;
    private String alertRuleName;
    private String measureName;
    private BigDecimal triggerValue;
    private BigDecimal threshold;
    private BigDecimal previousValue;
    private BigDecimal changePercent;
    private AlertSeverity severity;
    private AlertEventStatus eventStatus;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private Boolean isRecovered;
    private BigDecimal recoveryValue;
    private List<MetricDataPoint> trendData;
    private LocalDateTime triggerHighlightTime;
}

@Data
class MetricDataPoint {
    private LocalDateTime time;
    private BigDecimal value;
}
