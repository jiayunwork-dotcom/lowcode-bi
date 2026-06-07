package com.lowcode.bi.service;

import com.lowcode.bi.common.enums.AlertEventStatus;
import com.lowcode.bi.common.enums.AlertSeverity;
import com.lowcode.bi.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AlertRuleService {
    AlertRuleResponse createRule(AlertRuleCreateRequest request);
    AlertRuleResponse updateRule(UUID id, AlertRuleUpdateRequest request);
    void deleteRule(UUID id);
    AlertRuleResponse getRule(UUID id);
    Page<AlertRuleResponse> getRules(Pageable pageable);
    List<AlertRuleResponse> getRulesByDataModel(UUID dataModelId);
    AlertRuleResponse enableRule(UUID id);
    AlertRuleResponse disableRule(UUID id);
    Page<AlertEventResponse> getEvents(AlertEventStatus status, AlertSeverity severity, Pageable pageable);
    AlertEventResponse getEvent(UUID id);
    AlertEventResponse acknowledgeEvent(UUID eventId);
    AlertStatisticsResponse getStatistics();
    List<AlertEventResponse> getEventsByRule(UUID ruleId, Pageable pageable);
    void subscribe(UUID ruleId, boolean subscribed);
    boolean isSubscribed(UUID ruleId);
    Page<AlertRuleResponse> getSubscribedRules(Pageable pageable);
    List<AlertEventResponse> getRecentEvents(UUID ruleId, int limit);
}
