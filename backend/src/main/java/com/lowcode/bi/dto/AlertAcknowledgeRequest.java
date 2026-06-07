package com.lowcode.bi.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AlertAcknowledgeRequest {
    private String eventId;
}

@Data
class AlertSubscriptionRequest {
    private String ruleId;
    private Boolean subscribed;
}

@Data
class WebhookPayload {
    private String ruleName;
    private String triggerValue;
    private String threshold;
    private String triggeredAt;
    private String severity;
    private String eventId;
    private String measureName;
}
