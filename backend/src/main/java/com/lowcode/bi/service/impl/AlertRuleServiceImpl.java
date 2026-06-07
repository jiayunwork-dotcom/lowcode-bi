package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.alert.AlertDetectionEngine;
import com.lowcode.bi.common.enums.*;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.dto.*;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.dto.AlertEventGroupResponse;
import com.lowcode.bi.dto.AlertEventTimelineItem;
import com.lowcode.bi.dto.EscalationRecipientResponse;
import com.lowcode.bi.repository.*;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.AlertRuleService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final AlertSubscriptionRepository subscriptionRepository;
    private final AlertNotificationChannelRepository channelRepository;
    private final AlertEscalationRecipientRepository escalationRecipientRepository;
    private final DataModelRepository dataModelRepository;
    private final MeasureRepository measureRepository;
    private final UserRepository userRepository;
    private final AlertDetectionEngine detectionEngine;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public AlertRuleResponse createRule(AlertRuleCreateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        DataModel dataModel = dataModelRepository.findByIdAndTenantId(
            UUID.fromString(request.getDataModelId()), tenantId)
            .orElseThrow(() -> new BusinessException("数据模型不存在"));

        Measure measure = measureRepository.findByIdAndTenantId(
            UUID.fromString(request.getMeasureId()), tenantId)
            .orElseThrow(() -> new BusinessException("度量不存在"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        AlertRule rule = new AlertRule();
        rule.setTenant(dataModel.getTenant());
        rule.setDataModel(dataModel);
        rule.setMeasure(measure);
        rule.setMeasureName(measure.getName());
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setTriggerType(request.getTriggerType());
        rule.setOperator(request.getOperator());
        rule.setThreshold(request.getThreshold());
        rule.setCheckInterval(request.getCheckInterval() != null ?
            request.getCheckInterval() : AlertCheckInterval.EVERY_HOUR);
        rule.setSilencePeriod(request.getSilencePeriod() != null ?
            request.getSilencePeriod() : 300);
        rule.setSeverity(request.getSeverity() != null ?
            request.getSeverity() : AlertSeverity.WARNING);
        rule.setCurrentSeverity(request.getSeverity() != null ?
            request.getSeverity() : AlertSeverity.WARNING);
        rule.setIsEnabled(true);
        rule.setStatus(AlertRuleStatus.ACTIVE);
        rule.setCreatedBy(user);

        rule.setEscalationEnabled(request.getEscalationEnabled() != null ?
            request.getEscalationEnabled() : false);
        rule.setEscalationThreshold(request.getEscalationThreshold() != null ?
            request.getEscalationThreshold() : 3);
        rule.setConsecutiveTriggerCount(0);
        rule.setEscalationLevel(0);

        rule = alertRuleRepository.save(rule);

        if (request.getNotificationChannels() != null) {
            for (var channelConfig : request.getNotificationChannels()) {
                createNotificationChannel(rule, channelConfig);
            }
        }

        if (Boolean.TRUE.equals(request.getEscalationEnabled()) &&
            request.getEscalationRecipientUserIds() != null) {
            for (String userId : request.getEscalationRecipientUserIds()) {
                createEscalationRecipient(rule, userId);
            }
        }

        createDefaultSubscription(rule, user);

        return toResponse(rule);
    }

    @Override
    @Transactional
    public AlertRuleResponse updateRule(UUID id, AlertRuleUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("告警规则不存在"));

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getTriggerType() != null) rule.setTriggerType(request.getTriggerType());
        if (request.getOperator() != null) rule.setOperator(request.getOperator());
        if (request.getThreshold() != null) rule.setThreshold(request.getThreshold());
        if (request.getCheckInterval() != null) rule.setCheckInterval(request.getCheckInterval());
        if (request.getSilencePeriod() != null) rule.setSilencePeriod(request.getSilencePeriod());
        if (request.getSeverity() != null) {
            rule.setSeverity(request.getSeverity());
            if (rule.getCurrentSeverity() == null || rule.getEscalationLevel() == 0) {
                rule.setCurrentSeverity(request.getSeverity());
            }
        }
        if (request.getIsEnabled() != null) {
            rule.setIsEnabled(request.getIsEnabled());
            if (!request.getIsEnabled()) {
                rule.setStatus(AlertRuleStatus.DISABLED);
            } else if (rule.getStatus() == AlertRuleStatus.DISABLED) {
                rule.setStatus(AlertRuleStatus.ACTIVE);
            }
        }

        if (request.getEscalationEnabled() != null) {
            rule.setEscalationEnabled(request.getEscalationEnabled());
        }
        if (request.getEscalationThreshold() != null) {
            rule.setEscalationThreshold(request.getEscalationThreshold());
        }

        if (request.getNotificationChannels() != null) {
            updateNotificationChannels(rule, request.getNotificationChannels());
        }

        if (request.getEscalationRecipientUserIds() != null) {
            updateEscalationRecipients(rule, request.getEscalationRecipientUserIds());
        }

        rule = alertRuleRepository.save(rule);
        return toResponse(rule);
    }

    @Override
    @Transactional
    public void deleteRule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("告警规则不存在"));
        alertRuleRepository.delete(rule);
    }

    @Override
    public AlertRuleResponse getRule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("告警规则不存在"));
        return toResponse(rule);
    }

    @Override
    public Page<AlertRuleResponse> getRules(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return alertRuleRepository.findByTenantId(tenantId, sortedPageable)
            .map(this::toResponse);
    }

    @Override
    public List<AlertRuleResponse> getRulesByDataModel(UUID dataModelId) {
        UUID tenantId = TenantContext.getTenantId();
        return alertRuleRepository.findByDataModelIdAndTenantId(dataModelId, tenantId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertRuleResponse enableRule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("告警规则不存在"));
        rule.setIsEnabled(true);
        rule.setStatus(AlertRuleStatus.ACTIVE);
        rule = alertRuleRepository.save(rule);
        return toResponse(rule);
    }

    @Override
    @Transactional
    public AlertRuleResponse disableRule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AlertRule rule = alertRuleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("告警规则不存在"));
        rule.setIsEnabled(false);
        rule.setStatus(AlertRuleStatus.DISABLED);
        rule = alertRuleRepository.save(rule);
        return toResponse(rule);
    }

    @Override
    public Page<AlertEventResponse> getEvents(AlertEventStatus status, AlertSeverity severity, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "triggeredAt")
        );

        Page<AlertEvent> events;
        if (status != null && severity != null) {
            events = alertEventRepository.findByTenantIdAndEventStatusAndSeverity(
                tenantId, status, severity, sortedPageable);
        } else if (status != null) {
            events = alertEventRepository.findByTenantIdAndEventStatus(
                tenantId, status, sortedPageable);
        } else if (severity != null) {
            events = alertEventRepository.findByTenantIdAndSeverity(
                tenantId, severity, sortedPageable);
        } else {
            events = alertEventRepository.findByTenantId(tenantId, sortedPageable);
        }

        return events.map(this::toEventResponse);
    }

    @Override
    public AlertEventResponse getEvent(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AlertEvent event = alertEventRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("告警事件不存在"));

        AlertEventResponse response = toEventResponse(event);

        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        List<Map<String, Object>> trendData = detectionEngine.getMetricTrendData(
            event.getAlertRule(), startTime);

        List<MetricDataPoint> dataPoints = trendData.stream()
            .map(row -> {
                MetricDataPoint dp = new MetricDataPoint();
                dp.setTime((LocalDateTime) row.get("time"));
                dp.setValue((BigDecimal) row.get("value"));
                return dp;
            })
            .collect(Collectors.toList());

        response.setTrendData(dataPoints);
        response.setTriggerHighlightTime(event.getTriggeredAt());

        return response;
    }

    @Override
    @Transactional
    public AlertEventResponse acknowledgeEvent(UUID eventId) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        AlertEvent event = alertEventRepository.findByIdAndTenantId(eventId, tenantId)
            .orElseThrow(() -> new BusinessException("告警事件不存在"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        event.setEventStatus(AlertEventStatus.ACKNOWLEDGED);
        event.setAcknowledgedAt(LocalDateTime.now());
        event.setAcknowledgedBy(user);

        AlertRule rule = event.getAlertRule();
        if (rule != null) {
            rule.setConsecutiveTriggerCount(0);
            alertRuleRepository.save(rule);
        }

        event = alertEventRepository.save(event);
        return toEventResponse(event);
    }

    @Override
    public AlertStatisticsResponse getStatistics() {
        UUID tenantId = TenantContext.getTenantId();

        long activeCount = alertEventRepository.countActiveAlerts(tenantId);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNewCount = alertEventRepository.countNewAlertsSince(tenantId, todayStart);

        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        List<Object[]> topRulesData = alertRuleRepository.findTopTriggeredRules(
            tenantId, weekStart, PageRequest.of(0, 3));

        List<TopRuleInfo> topRules = topRulesData.stream()
            .map(row -> {
                TopRuleInfo info = new TopRuleInfo();
                info.setRuleId((UUID) row[0]);
                info.setRuleName((String) row[1]);
                info.setTriggerCount((Long) row[2]);
                return info;
            })
            .collect(Collectors.toList());

        Double avgRecoverySeconds = alertEventRepository.getAverageRecoveryTime(tenantId);
        Double avgRecoveryMinutes = avgRecoverySeconds != null ?
            avgRecoverySeconds / 60.0 : null;

        AlertStatisticsResponse stats = new AlertStatisticsResponse();
        stats.setActiveAlertCount(activeCount);
        stats.setTodayNewCount(todayNewCount);
        stats.setTopTriggeredRules(topRules);
        stats.setAverageRecoveryMinutes(avgRecoveryMinutes);

        return stats;
    }

    @Override
    public Page<AlertEventResponse> getEventsByRule(UUID ruleId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "triggeredAt")
        );
        return alertEventRepository.findByAlertRuleIdAndTenantId(ruleId, tenantId, sortedPageable)
            .map(this::toEventResponse);
    }

    @Override
    @Transactional
    public void subscribe(UUID ruleId, boolean subscribed) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        AlertRule rule = alertRuleRepository.findByIdAndTenantId(ruleId, tenantId)
            .orElseThrow(() -> new BusinessException("告警规则不存在"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        Optional<AlertSubscription> existingSub = subscriptionRepository
            .findByAlertRuleIdAndUserIdAndTenantId(ruleId, userId, tenantId);

        if (existingSub.isPresent()) {
            AlertSubscription sub = existingSub.get();
            sub.setIsSubscribed(subscribed);
            if (subscribed) {
                sub.setSubscribedAt(LocalDateTime.now());
                sub.setUnsubscribedAt(null);
            } else {
                sub.setUnsubscribedAt(LocalDateTime.now());
            }
            subscriptionRepository.save(sub);
        } else if (subscribed) {
            AlertSubscription sub = new AlertSubscription();
            sub.setTenant(rule.getTenant());
            sub.setAlertRule(rule);
            sub.setUser(user);
            sub.setIsSubscribed(true);
            sub.setSubscribedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        }
    }

    @Override
    public boolean isSubscribed(UUID ruleId) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        return subscriptionRepository.isUserSubscribed(ruleId, userId);
    }

    @Override
    public Page<AlertRuleResponse> getSubscribedRules(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        return alertRuleRepository.findSubscribedRulesByUserAndTenant(userId, tenantId, pageable)
            .map(this::toResponse);
    }

    @Override
    public List<AlertEventResponse> getRecentEvents(UUID ruleId, int limit) {
        UUID tenantId = TenantContext.getTenantId();
        return alertEventRepository.findByAlertRuleIdAndTenantId(
                ruleId, tenantId, PageRequest.of(0, limit))
            .stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    }

    private void createNotificationChannel(AlertRule rule, NotificationChannelConfig config) {
        AlertNotificationChannel channel = new AlertNotificationChannel();
        channel.setTenant(rule.getTenant());
        channel.setAlertRule(rule);
        channel.setChannelType(config.getType());

        try {
            channel.setConfig(objectMapper.writeValueAsString(config.getConfig()));
        } catch (Exception e) {
            throw new BusinessException("序列化通知配置失败: " + e.getMessage());
        }

        channel.setIsEnabled(config.getEnabled() != null ? config.getEnabled() : true);
        channelRepository.save(channel);
    }

    private void updateNotificationChannels(AlertRule rule, List<NotificationChannelConfig> configs) {
        List<AlertNotificationChannel> existingChannels = channelRepository
            .findByAlertRuleIdAndTenantId(rule.getId(), rule.getTenant().getId());

        for (AlertNotificationChannel channel : existingChannels) {
            channelRepository.delete(channel);
        }

        for (NotificationChannelConfig config : configs) {
            createNotificationChannel(rule, config);
        }
    }

    private void createDefaultSubscription(AlertRule rule, User user) {
        AlertSubscription sub = new AlertSubscription();
        sub.setTenant(rule.getTenant());
        sub.setAlertRule(rule);
        sub.setUser(user);
        sub.setIsSubscribed(true);
        sub.setSubscribedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
    }

    private AlertRuleResponse toResponse(AlertRule rule) {
        AlertRuleResponse response = new AlertRuleResponse();
        response.setId(rule.getId());
        response.setName(rule.getName());
        response.setDescription(rule.getDescription());
        response.setDataModelId(rule.getDataModel().getId());
        response.setDataModelName(rule.getDataModel().getName());
        response.setMeasureId(rule.getMeasure().getId());
        response.setMeasureName(rule.getMeasureName());
        response.setTriggerType(rule.getTriggerType());
        response.setOperator(rule.getOperator());
        response.setThreshold(rule.getThreshold());
        response.setCheckInterval(rule.getCheckInterval());
        response.setSilencePeriod(rule.getSilencePeriod());
        response.setSeverity(rule.getSeverity());
        response.setIsEnabled(rule.getIsEnabled());
        response.setStatus(rule.getStatus());
        response.setLastTriggeredAt(rule.getLastTriggeredAt());
        response.setLastCheckedAt(rule.getLastCheckedAt());
        response.setCreatedBy(rule.getCreatedBy().getUsername());
        response.setCreatedAt(rule.getCreatedAt());
        response.setEventCount((long) rule.getEvents().size());
        response.setSubscriberCount(rule.getSubscriptions().stream()
            .filter(AlertSubscription::getIsSubscribed).count());

        List<NotificationChannelResponse> channels = channelRepository
            .findByAlertRuleIdAndTenantId(rule.getId(), rule.getTenant().getId())
            .stream()
            .map(this::toChannelResponse)
            .collect(Collectors.toList());
        response.setNotificationChannels(channels);

        response.setEscalationEnabled(rule.getEscalationEnabled());
        response.setEscalationThreshold(rule.getEscalationThreshold());
        response.setConsecutiveTriggerCount(rule.getConsecutiveTriggerCount());
        response.setEscalationLevel(rule.getEscalationLevel());
        response.setCurrentSeverity(rule.getCurrentSeverity() != null ?
            rule.getCurrentSeverity() : rule.getSeverity());

        List<EscalationRecipientResponse> recipients = escalationRecipientRepository
            .findByAlertRuleId(rule.getId())
            .stream()
            .map(this::toEscalationRecipientResponse)
            .collect(Collectors.toList());
        response.setEscalationRecipients(recipients);

        return response;
    }

    private EscalationRecipientResponse toEscalationRecipientResponse(AlertEscalationRecipient recipient) {
        EscalationRecipientResponse response = new EscalationRecipientResponse();
        response.setUserId(recipient.getUser().getId());
        response.setUsername(recipient.getUser().getUsername());
        response.setEmail(recipient.getUser().getEmail());
        return response;
    }

    private NotificationChannelResponse toChannelResponse(AlertNotificationChannel channel) {
        NotificationChannelResponse response = new NotificationChannelResponse();
        response.setId(channel.getId());
        response.setType(channel.getChannelType());
        response.setEnabled(channel.getIsEnabled());

        try {
            response.setConfig(objectMapper.readValue(channel.getConfig(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)));
        } catch (Exception e) {
            response.setConfig(channel.getConfig());
        }

        return response;
    }

    private AlertEventResponse toEventResponse(AlertEvent event) {
        AlertEventResponse response = new AlertEventResponse();
        response.setId(event.getId());
        response.setAlertRuleId(event.getAlertRule().getId());
        response.setAlertRuleName(event.getAlertRule().getName());
        response.setMeasureName(event.getAlertRule().getMeasureName());
        response.setTriggerValue(event.getTriggerValue());
        response.setThreshold(event.getThreshold());
        response.setPreviousValue(event.getPreviousValue());
        response.setChangePercent(event.getChangePercent());
        response.setSeverity(event.getSeverity());
        response.setEventStatus(event.getEventStatus());
        response.setTriggeredAt(event.getTriggeredAt());
        response.setResolvedAt(event.getResolvedAt());
        response.setAcknowledgedAt(event.getAcknowledgedAt());
        if (event.getAcknowledgedBy() != null) {
            response.setAcknowledgedBy(event.getAcknowledgedBy().getUsername());
        }
        response.setIsRecovered(event.getIsRecovered());
        response.setRecoveryValue(event.getRecoveryValue());

        return response;
    }

    private void createEscalationRecipient(AlertRule rule, String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findByIdAndTenantId(userId, rule.getTenant().getId())
            .orElseThrow(() -> new BusinessException("升级接收人用户不存在: " + userIdStr));

        AlertEscalationRecipient recipient = new AlertEscalationRecipient();
        recipient.setTenant(rule.getTenant());
        recipient.setAlertRule(rule);
        recipient.setUser(user);
        escalationRecipientRepository.save(recipient);
    }

    private void updateEscalationRecipients(AlertRule rule, List<String> userIds) {
        escalationRecipientRepository.deleteByAlertRuleId(rule.getId());
        for (String userId : userIds) {
            createEscalationRecipient(rule, userId);
        }
    }

    @Override
    public List<AlertEventGroupResponse> getEventGroups(AlertSeverity severity, UUID dataModelId, String sortBy) {
        UUID tenantId = TenantContext.getTenantId();
        List<AlertRule> rules = alertRuleRepository.findByTenantId(tenantId);

        if (dataModelId != null) {
            rules = rules.stream()
                .filter(r -> r.getDataModel().getId().equals(dataModelId))
                .collect(Collectors.toList());
        }

        List<AlertEventGroupResponse> groups = rules.stream()
            .map(rule -> buildEventGroup(rule, severity))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if ("activeCount".equals(sortBy)) {
            groups.sort((g1, g2) -> Long.compare(g2.getActiveEventCount(), g1.getActiveEventCount()));
        } else {
            groups.sort((g1, g2) -> {
                LocalDateTime t1 = g1.getLastTriggeredAt();
                LocalDateTime t2 = g2.getLastTriggeredAt();
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                return t2.compareTo(t1);
            });
        }

        return groups;
    }

    private AlertEventGroupResponse buildEventGroup(AlertRule rule, AlertSeverity severityFilter) {
        List<AlertEvent> events = alertEventRepository.findByAlertRuleIdAndTenantId(
            rule.getId(), rule.getTenant().getId(), PageRequest.of(0, 1000));

        if (events.isEmpty()) {
            return null;
        }

        if (severityFilter != null) {
            boolean hasMatchingSeverity = events.stream()
                .anyMatch(e -> e.getSeverity() == severityFilter);
            if (!hasMatchingSeverity) {
                return null;
            }
        }

        AlertEventGroupResponse group = new AlertEventGroupResponse();
        group.setRuleId(rule.getId());
        group.setRuleName(rule.getName());
        group.setDataModelId(rule.getDataModel().getId());
        group.setDataModelName(rule.getDataModel().getName());

        long activeCount = events.stream()
            .filter(e -> e.getEventStatus() == AlertEventStatus.FIRING && !e.getIsRecovered())
            .count();
        group.setActiveEventCount(activeCount);
        group.setTotalEventCount((long) events.size());

        Optional<LocalDateTime> lastTriggered = events.stream()
            .map(AlertEvent::getTriggeredAt)
            .max(LocalDateTime::compareTo);
        group.setLastTriggeredAt(lastTriggered.orElse(null));

        Double avgRecovery = calculateAverageRecovery(events);
        group.setAverageRecoveryMinutes(avgRecovery);

        SeverityDistribution distribution = calculateSeverityDistribution(events);
        group.setSeverityDistribution(distribution);

        return group;
    }

    private Double calculateAverageRecovery(List<AlertEvent> events) {
        List<AlertEvent> recovered = events.stream()
            .filter(e -> e.getIsRecovered() && e.getResolvedAt() != null)
            .collect(Collectors.toList());

        if (recovered.isEmpty()) {
            return null;
        }

        double totalMinutes = recovered.stream()
            .mapToDouble(e -> java.time.Duration.between(e.getTriggeredAt(), e.getResolvedAt()).toMinutes())
            .average()
            .orElse(0);

        return Math.round(totalMinutes * 10.0) / 10.0;
    }

    private SeverityDistribution calculateSeverityDistribution(List<AlertEvent> events) {
        long total = events.size();
        long infoCount = events.stream().filter(e -> e.getSeverity() == AlertSeverity.INFO).count();
        long warningCount = events.stream().filter(e -> e.getSeverity() == AlertSeverity.WARNING).count();
        long criticalCount = events.stream().filter(e -> e.getSeverity() == AlertSeverity.CRITICAL).count();

        SeverityDistribution dist = new SeverityDistribution();
        dist.setInfoCount(infoCount);
        dist.setWarningCount(warningCount);
        dist.setCriticalCount(criticalCount);
        dist.setInfoPercent(total > 0 ? Math.round(infoCount * 1000.0 / total) / 10.0 : 0);
        dist.setWarningPercent(total > 0 ? Math.round(warningCount * 1000.0 / total) / 10.0 : 0);
        dist.setCriticalPercent(total > 0 ? Math.round(criticalCount * 1000.0 / total) / 10.0 : 0);

        return dist;
    }

    @Override
    public List<AlertEventTimelineItem> getEventTimeline(UUID ruleId) {
        UUID tenantId = TenantContext.getTenantId();
        List<AlertEvent> events = alertEventRepository.findByAlertRuleIdAndTenantId(
            ruleId, tenantId, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "triggeredAt")));

        return events.stream()
            .map(this::toTimelineItem)
            .collect(Collectors.toList());
    }

    private AlertEventTimelineItem toTimelineItem(AlertEvent event) {
        AlertEventTimelineItem item = new AlertEventTimelineItem();
        item.setEventId(event.getId());
        item.setTriggeredAt(event.getTriggeredAt());
        item.setResolvedAt(event.getResolvedAt());
        item.setSeverity(event.getSeverity());
        item.setRecovered(event.getIsRecovered());
        item.setTriggerValue(event.getTriggerValue() != null ? event.getTriggerValue().toString() : "");
        return item;
    }
}
