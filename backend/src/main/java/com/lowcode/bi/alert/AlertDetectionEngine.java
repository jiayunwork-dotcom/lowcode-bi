package com.lowcode.bi.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.enums.*;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.query.SqlQueryEngine;
import com.lowcode.bi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDetectionEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final AlertNotificationRepository alertNotificationRepository;
    private final AlertNotificationChannelRepository channelRepository;
    private final AlertSubscriptionRepository subscriptionRepository;
    private final SqlQueryEngine sqlQueryEngine;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.alert.max-retries:3}")
    private int maxRetries;

    @Value("${app.alert.silence-check-enabled:true}")
    private boolean silenceCheckEnabled;

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void checkAlertsEvery5Minutes() {
        log.info("Starting 5-minute alert check cycle");
        checkAlertsForInterval(AlertCheckInterval.EVERY_5_MINUTES);
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkAlertsEveryHour() {
        log.info("Starting hourly alert check cycle");
        checkAlertsForInterval(AlertCheckInterval.EVERY_HOUR);
        checkAlertsForInterval(AlertCheckInterval.EVERY_15_MINUTES);
        checkAlertsForInterval(AlertCheckInterval.EVERY_30_MINUTES);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkAlertsEveryDay() {
        log.info("Starting daily alert check cycle");
        checkAlertsForInterval(AlertCheckInterval.EVERY_DAY);
        checkAlertsForInterval(AlertCheckInterval.EVERY_6_HOURS);
        checkAlertsForInterval(AlertCheckInterval.EVERY_12_HOURS);
    }

    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional
    public void checkRecovery() {
        log.info("Starting recovery check cycle");
        checkAlertRecovery();
    }

    private void checkAlertsForInterval(AlertCheckInterval interval) {
        List<AlertRule> rules = alertRuleRepository.findAllActiveRules().stream()
                .filter(r -> r.getCheckInterval() == interval)
                .filter(r -> shouldCheckRule(r))
                .toList();

        log.info("Found {} rules to check for interval {}", rules.size(), interval);

        for (AlertRule rule : rules) {
            try {
                processRuleCheck(rule);
            } catch (Exception e) {
                log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    private boolean shouldCheckRule(AlertRule rule) {
        if (!rule.getIsEnabled() || rule.getStatus() != AlertRuleStatus.ACTIVE) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastChecked = rule.getLastCheckedAt();

        if (lastChecked == null) {
            return true;
        }

        long minutesSinceLastCheck = ChronoUnit.MINUTES.between(lastChecked, now);
        long intervalMinutes = rule.getCheckInterval().getDuration().toMinutes();

        return minutesSinceLastCheck >= intervalMinutes;
    }

    private void processRuleCheck(AlertRule rule) {
        LocalDateTime now = LocalDateTime.now();
        rule.setLastCheckedAt(now);

        try {
            BigDecimal currentValue = queryCurrentMetricValue(rule);
            BigDecimal previousValue = null;
            BigDecimal changePercent = null;

            if (rule.getTriggerType() == AlertTriggerType.RELATIVE_CHANGE) {
                previousValue = queryPreviousMetricValue(rule);
                if (previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                    changePercent = currentValue.subtract(previousValue)
                            .divide(previousValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                }
            }

            boolean shouldTrigger = evaluateCondition(rule, currentValue, changePercent);

            if (shouldTrigger) {
                if (isInSilencePeriod(rule)) {
                    log.info("Rule {} triggered but in silence period, skipping", rule.getId());
                    return;
                }
                createAlertEvent(rule, currentValue, previousValue, changePercent);
                rule.setLastTriggeredAt(now);
            }

        } catch (Exception e) {
            log.error("Failed to process rule {}: {}", rule.getId(), e.getMessage(), e);
            throw new BusinessException("告警检测失败: " + e.getMessage());
        }

        alertRuleRepository.save(rule);
    }

    private BigDecimal queryCurrentMetricValue(AlertRule rule) {
        String sql = buildMetricQuery(rule, 0);
        return executeMetricQuery(rule, sql);
    }

    private BigDecimal queryPreviousMetricValue(AlertRule rule) {
        String sql = buildMetricQuery(rule, 1);
        try {
            return executeMetricQuery(rule, sql);
        } catch (Exception e) {
            log.warn("Failed to query previous value for rule {}: {}", rule.getId(), e.getMessage());
            return null;
        }
    }

    private String buildMetricQuery(AlertRule rule, int periodsAgo) {
        Measure measure = rule.getMeasure();
        DataModel dataModel = rule.getDataModel();
        DataSource dataSource = dataModel.getDataSource();

        String timeColumn = determineTimeColumn(dataModel);
        String tableAlias = measure.getTableAlias();
        String columnName = measure.getColumnName();
        String aggregation = measure.getAggregationType().name();

        AlertCheckInterval interval = rule.getCheckInterval();
        long periodSeconds = interval.getDuration().getSeconds();
        long offsetSeconds = periodSeconds * periodsAgo;

        String whereClause = "WHERE 1=1";
        if (timeColumn != null) {
            whereClause += String.format(
                " AND %s >= NOW() - INTERVAL '%d seconds' - INTERVAL '%d seconds'" +
                " AND %s < NOW() - INTERVAL '%d seconds'",
                timeColumn, periodSeconds, offsetSeconds,
                timeColumn, offsetSeconds
            );
        }

        if (measure.getFilterCondition() != null && !measure.getFilterCondition().isEmpty()) {
            whereClause += " AND (" + measure.getFilterCondition() + ")";
        }

        return String.format(
            "SELECT %s(%s.%s) as metric_value FROM %s %s %s",
            aggregation, tableAlias, columnName,
            getTableNameFromDataModel(dataModel, tableAlias),
            tableAlias,
            whereClause
        );
    }

    private String determineTimeColumn(DataModel dataModel) {
        for (ModelTable table : dataModel.getModelTables()) {
            if (table.getTableMetadata() != null) {
                for (ColumnMetadata col : table.getTableMetadata().getColumns()) {
                    if (col.getDataType().name().contains("TIME") ||
                        col.getDataType().name().contains("DATE")) {
                        return table.getAlias() + "." + col.getName();
                    }
                }
            }
        }
        return null;
    }

    private String getTableNameFromDataModel(DataModel dataModel, String alias) {
        return dataModel.getModelTables().stream()
                .filter(t -> t.getAlias().equals(alias))
                .map(ModelTable::getTableName)
                .findFirst()
                .orElseThrow(() -> new BusinessException("找不到表: " + alias));
    }

    private BigDecimal executeMetricQuery(AlertRule rule, String sql) {
        DataSource dataSource = rule.getDataModel().getDataSource();

        SqlQueryEngine.QueryRequest request = new SqlQueryEngine.QueryRequest();
        request.setDataSourceId(dataSource.getId());
        request.setSql(sql);
        request.setMaxRows(1);
        request.setUseCache(false);
        request.setTimeout(30);

        SqlQueryEngine.QueryResult result = sqlQueryEngine.executeQuery(request);

        if (result.getRows() == null || result.getRows().isEmpty()) {
            return BigDecimal.ZERO;
        }

        Object value = result.getRows().get(0).get("metric_value");
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else {
            return new BigDecimal(value.toString());
        }
    }

    private boolean evaluateCondition(AlertRule rule, BigDecimal currentValue, BigDecimal changePercent) {
        AlertOperator operator = rule.getOperator();
        BigDecimal threshold = rule.getThreshold();
        BigDecimal valueToCompare = rule.getTriggerType() == AlertTriggerType.RELATIVE_CHANGE ? changePercent : currentValue;

        if (valueToCompare == null) {
            return false;
        }

        return switch (operator) {
            case GREATER_THAN -> valueToCompare.compareTo(threshold) > 0;
            case LESS_THAN -> valueToCompare.compareTo(threshold) < 0;
            case EQUAL -> valueToCompare.compareTo(threshold) == 0;
            case NOT_EQUAL -> valueToCompare.compareTo(threshold) != 0;
            case GREATER_THAN_OR_EQUAL -> valueToCompare.compareTo(threshold) >= 0;
            case LESS_THAN_OR_EQUAL -> valueToCompare.compareTo(threshold) <= 0;
            case INCREASE_PERCENT -> changePercent != null && changePercent.compareTo(threshold) > 0;
            case DECREASE_PERCENT -> changePercent != null && changePercent.compareTo(threshold.negate()) < 0;
            case CHANGE_PERCENT -> changePercent != null && changePercent.abs().compareTo(threshold) > 0;
        };
    }

    private boolean isInSilencePeriod(AlertRule rule) {
        if (!silenceCheckEnabled || rule.getLastTriggeredAt() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime silenceEnd = rule.getLastTriggeredAt().plusSeconds(rule.getSilencePeriod());
        return now.isBefore(silenceEnd);
    }

    @Transactional
    protected void createAlertEvent(AlertRule rule, BigDecimal triggerValue, BigDecimal previousValue, BigDecimal changePercent) {
        List<AlertEvent> existingFiring = alertEventRepository.findLatestFiringEvent(
            rule.getId(), PageRequest.of(0, 1));

        if (!existingFiring.isEmpty()) {
            log.info("Rule {} already has an active firing event, skipping creation", rule.getId());
            return;
        }

        AlertEvent event = new AlertEvent();
        event.setTenant(rule.getTenant());
        event.setAlertRule(rule);
        event.setTriggerValue(triggerValue);
        event.setThreshold(rule.getThreshold());
        event.setPreviousValue(previousValue);
        event.setChangePercent(changePercent);
        event.setSeverity(rule.getSeverity());
        event.setEventStatus(AlertEventStatus.FIRING);
        event.setTriggeredAt(LocalDateTime.now());
        event.setIsRecovered(false);

        event = alertEventRepository.save(event);
        log.info("Created alert event {} for rule {}", event.getId(), rule.getId());

        createNotifications(event, rule);
    }

    private void createNotifications(AlertEvent event, AlertRule rule) {
        List<AlertNotificationChannel> channels = channelRepository.findByAlertRuleIdAndIsEnabledTrue(rule.getId());
        List<AlertSubscription> subscribers = subscriptionRepository.findSubscribersForRule(rule.getId());

        for (AlertNotificationChannel channel : channels) {
            try {
                createChannelNotifications(event, rule, channel, subscribers);
            } catch (Exception e) {
                log.error("Failed to create notifications for channel {}: {}", channel.getId(), e.getMessage(), e);
            }
        }

        notificationService.dispatchNotifications(event, rule);
    }

    private void createChannelNotifications(AlertEvent event, AlertRule rule,
                                            AlertNotificationChannel channel,
                                            List<AlertSubscription> subscribers) {
        NotificationChannelType type = channel.getChannelType();

        try {
            Map<String, Object> config = objectMapper.readValue(
                channel.getConfig(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );

            if (type == NotificationChannelType.IN_APP || type == NotificationChannelType.EMAIL) {
                for (AlertSubscription sub : subscribers) {
                    if (!sub.getIsSubscribed()) continue;

                    String recipient = type == NotificationChannelType.EMAIL ?
                        sub.getUser().getEmail() : sub.getUser().getId().toString();

                    createNotificationRecord(event, rule, type, recipient);
                }
            } else if (type == NotificationChannelType.WEBHOOK) {
                String webhookUrl = (String) config.get("url");
                if (webhookUrl != null) {
                    createNotificationRecord(event, rule, type, webhookUrl);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process channel {}: {}", channel.getId(), e.getMessage());
        }
    }

    private void createNotificationRecord(AlertEvent event, AlertRule rule,
                                          NotificationChannelType type, String recipient) {
        AlertNotification notification = new AlertNotification();
        notification.setTenant(rule.getTenant());
        notification.setAlertEvent(event);
        notification.setAlertRule(rule);
        notification.setChannelType(type);
        notification.setRecipient(recipient);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        alertNotificationRepository.save(notification);
    }

    @Transactional
    protected void checkAlertRecovery() {
        List<AlertRule> rules = alertRuleRepository.findAllActiveRules();

        for (AlertRule rule : rules) {
            try {
                List<AlertEvent> unrecoveredEvents = alertEventRepository.findByAlertRuleIdAndIsRecoveredFalse(rule.getId());

                for (AlertEvent event : unrecoveredEvents) {
                    checkRecoveryForEvent(rule, event);
                }
            } catch (Exception e) {
                log.error("Error checking recovery for rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    private void checkRecoveryForEvent(AlertRule rule, AlertEvent event) {
        try {
            BigDecimal currentValue = queryCurrentMetricValue(rule);
            boolean hasRecovered = !evaluateCondition(rule, currentValue, null);

            if (hasRecovered) {
                event.setIsRecovered(true);
                event.setEventStatus(AlertEventStatus.RESOLVED);
                event.setResolvedAt(LocalDateTime.now());
                event.setRecoveryValue(currentValue);
                alertEventRepository.save(event);
                log.info("Alert event {} has recovered", event.getId());
            }
        } catch (Exception e) {
            log.error("Error checking recovery for event {}: {}", event.getId(), e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getMetricTrendData(AlertRule rule, LocalDateTime startTime) {
        Measure measure = rule.getMeasure();
        DataModel dataModel = rule.getDataModel();
        DataSource dataSource = dataModel.getDataSource();
        String timeColumn = determineTimeColumn(dataModel);

        if (timeColumn == null) {
            return getEventBasedTrendData(rule, startTime);
        }

        String tableAlias = measure.getTableAlias();
        String columnName = measure.getColumnName();
        String aggregation = measure.getAggregationType().name();
        String tableName = getTableNameFromDataModel(dataModel, tableAlias);

        String sql = String.format(
            "SELECT toStartOfFiveMinute(%s) as time_bucket, %s(%s.%s) as value " +
            "FROM %s %s " +
            "WHERE %s >= NOW() - INTERVAL '24 hours' " +
            "GROUP BY time_bucket " +
            "ORDER BY time_bucket ASC",
            timeColumn, aggregation, tableAlias, columnName,
            tableName, tableAlias,
            timeColumn
        );

        try {
            SqlQueryEngine.QueryRequest request = new SqlQueryEngine.QueryRequest();
            request.setDataSourceId(dataSource.getId());
            request.setSql(sql);
            request.setUseCache(false);
            request.setTimeout(30);

            SqlQueryEngine.QueryResult result = sqlQueryEngine.executeQuery(request);

            List<Map<String, Object>> trendData = new ArrayList<>();
            for (Map<String, Object> row : result.getRows()) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("time", row.get("time_bucket"));
                point.put("value", row.get("value"));
                trendData.add(point);
            }

            return trendData;
        } catch (Exception e) {
            log.warn("Failed to query trend data from ClickHouse, using event data: {}", e.getMessage());
            return getEventBasedTrendData(rule, startTime);
        }
    }

    private List<Map<String, Object>> getEventBasedTrendData(AlertRule rule, LocalDateTime startTime) {
        List<Object[]> rawData = alertEventRepository.getMetricTrendData(rule.getId(), startTime);
        List<Map<String, Object>> trendData = new ArrayList<>();

        for (Object[] row : rawData) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("time", row[0]);
            point.put("value", row[1]);
            trendData.add(point);
        }

        return trendData;
    }
}
