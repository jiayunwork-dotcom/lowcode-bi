package com.lowcode.bi.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class AlertStatisticsResponse {
    private Long activeAlertCount;
    private Long todayNewCount;
    private List<TopRuleInfo> topTriggeredRules;
    private Double averageRecoveryMinutes;
}

@Data
class TopRuleInfo {
    private UUID ruleId;
    private String ruleName;
    private Long triggerCount;
}
