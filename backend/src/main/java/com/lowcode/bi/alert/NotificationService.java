package com.lowcode.bi.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.enums.*;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AlertNotificationRepository alertNotificationRepository;
    private final AlertNotificationChannelRepository channelRepository;
    private final SystemMessageRepository systemMessageRepository;
    private final EmailQueueRepository emailQueueRepository;
    private final AlertSubscriptionRepository alertSubscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.alert.max-retries:3}")
    private int maxRetries;

    @Value("${app.alert.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    @Value("${app.webhook.timeout:10000}")
    private int webhookTimeout;

    @Scheduled(cron = "0 */2 * * * ?")
    @Transactional
    public void processPendingNotifications() {
        LocalDateTime retryTime = LocalDateTime.now().minusMinutes(retryDelayMinutes);

        List<AlertNotification> pending = alertNotificationRepository.findPendingNotifications(
            NotificationStatus.PENDING, maxRetries, retryTime);

        List<AlertNotification> retrying = alertNotificationRepository.findPendingNotifications(
            NotificationStatus.RETRYING, maxRetries, retryTime);

        List<AlertNotification> allPending = new ArrayList<>();
        allPending.addAll(pending);
        allPending.addAll(retrying);

        log.info("Found {} pending notifications to process", allPending.size());

        for (AlertNotification notification : allPending) {
            try {
                dispatchNotification(notification);
            } catch (Exception e) {
                log.error("Failed to process notification {}: {}", notification.getId(), e.getMessage(), e);
                handleNotificationFailure(notification, e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void processEmailQueue() {
        List<EmailQueue> pending = emailQueueRepository.findPendingEmails(
            NotificationStatus.PENDING, maxRetries, PageRequest.of(0, 100));

        log.info("Found {} pending emails to process", pending.size());

        for (EmailQueue email : pending) {
            try {
                processEmail(email);
            } catch (Exception e) {
                log.error("Failed to process email {}: {}", email.getId(), e.getMessage(), e);
                handleEmailFailure(email, e.getMessage());
            }
        }
    }

    @Async("notificationExecutor")
    public void dispatchNotifications(AlertEvent event, AlertRule rule) {
        log.info("Dispatching notifications for event {} rule {}", event.getId(), rule.getId());
    }

    @Transactional
    protected void dispatchNotification(AlertNotification notification) {
        notification.setRetryCount(notification.getRetryCount() + 1);

        NotificationChannelType type = notification.getChannelType();

        try {
            switch (type) {
                case IN_APP -> sendInAppNotification(notification);
                case EMAIL -> sendEmailNotification(notification);
                case WEBHOOK -> sendWebhookNotification(notification);
            }

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);

        } catch (Exception e) {
            log.error("Notification dispatch failed for {}: {}", notification.getId(), e.getMessage());
            handleNotificationFailure(notification, e.getMessage());
        }

        alertNotificationRepository.save(notification);
    }

    private void sendInAppNotification(AlertNotification notification) {
        AlertEvent event = notification.getAlertEvent();
        AlertRule rule = notification.getAlertRule();
        UUID userId = UUID.fromString(notification.getRecipient());

        SystemMessage message = new SystemMessage();
        message.setTenant(notification.getTenant());
        message.setUser(getUserById(userId));
        message.setMessageType(SystemMessageType.ALERT);
        message.setTitle(buildAlertTitle(rule, event));
        message.setContent(buildAlertContent(rule, event));
        message.setRelatedType("ALERT_EVENT");
        message.setRelatedId(event.getId());
        message.setIsRead(false);

        systemMessageRepository.save(message);
        log.info("Sent in-app notification to user {} for event {}", userId, event.getId());
    }

    private void sendEmailNotification(AlertNotification notification) {
        AlertEvent event = notification.getAlertEvent();
        AlertRule rule = notification.getAlertRule();
        String toEmail = notification.getRecipient();

        EmailQueue email = new EmailQueue();
        email.setTenant(notification.getTenant());
        email.setToEmail(toEmail);
        email.setSubject(buildAlertTitle(rule, event));
        email.setBody(buildAlertEmailContent(rule, event));
        email.setIsHtml(true);
        email.setStatus(NotificationStatus.PENDING);
        email.setPriority(getPriorityFromSeverity(rule.getSeverity()));

        emailQueueRepository.save(email);
        log.info("Queued email notification to {} for event {}", toEmail, event.getId());
    }

    private void sendWebhookNotification(AlertNotification notification) {
        AlertEvent event = notification.getAlertEvent();
        AlertRule rule = notification.getAlertRule();
        String webhookUrl = notification.getRecipient();

        Optional<AlertNotificationChannel> channelOpt = channelRepository
            .findByAlertRuleIdAndChannelTypeAndIsEnabledTrue(rule.getId(), NotificationChannelType.WEBHOOK)
            .stream().findFirst();

        if (channelOpt.isEmpty()) {
            throw new RuntimeException("Webhook channel not found for rule: " + rule.getId());
        }

        AlertNotificationChannel channel = channelOpt.get();
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(channel.getConfig(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new RuntimeException("Invalid webhook config: " + e.getMessage());
        }

        Map<String, String> headers = (Map<String, String>) config.get("headers");
        if (headers == null) {
            headers = new HashMap<>();
        }

        WebhookPayload payload = new WebhookPayload();
        payload.setRuleName(rule.getName());
        payload.setTriggerValue(event.getTriggerValue().toString());
        payload.setThreshold(event.getThreshold().toString());
        payload.setTriggeredAt(event.getTriggeredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.setSeverity(event.getSeverity().name());
        payload.setEventId(event.getId().toString());
        payload.setMeasureName(rule.getMeasureName());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpHeaders.set(entry.getKey(), entry.getValue());
        }

        HttpEntity<WebhookPayload> request = new HttpEntity<>(payload, httpHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Webhook returned non-2xx status: " + response.getStatusCode());
            }

            log.info("Webhook notification sent to {} for event {}", webhookUrl, event.getId());
        } catch (Exception e) {
            throw new RuntimeException("Webhook call failed: " + e.getMessage(), e);
        }
    }

    private void processEmail(EmailQueue email) {
        email.setRetryCount(email.getRetryCount() + 1);

        try {
            sendEmail(email);
            email.setStatus(NotificationStatus.SENT);
            email.setSentAt(LocalDateTime.now());
            email.setErrorMessage(null);
        } catch (Exception e) {
            handleEmailFailure(email, e.getMessage());
        }

        emailQueueRepository.save(email);
    }

    private void sendEmail(EmailQueue email) {
        log.info("Would send email to {}: {} - {}", email.getToEmail(), email.getSubject(), email.getBody());
    }

    private void handleNotificationFailure(AlertNotification notification, String errorMessage) {
        notification.setErrorMessage(errorMessage);

        if (notification.getRetryCount() >= maxRetries) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("Notification {} failed permanently after {} retries: {}",
                notification.getId(), maxRetries, errorMessage);
        } else {
            notification.setStatus(NotificationStatus.RETRYING);
            log.warn("Notification {} failed, will retry (attempt {}/{}): {}",
                notification.getId(), notification.getRetryCount(), maxRetries, errorMessage);
        }
    }

    private void handleEmailFailure(EmailQueue email, String errorMessage) {
        email.setErrorMessage(errorMessage);

        if (email.getRetryCount() >= maxRetries) {
            email.setStatus(NotificationStatus.FAILED);
            log.error("Email {} failed permanently after {} retries: {}",
                email.getId(), maxRetries, errorMessage);
        } else {
            email.setStatus(NotificationStatus.RETRYING);
            log.warn("Email {} failed, will retry (attempt {}/{}): {}",
                email.getId(), email.getRetryCount(), maxRetries, errorMessage);
        }
    }

    private String buildAlertTitle(AlertRule rule, AlertEvent event) {
        String severityEmoji = switch (rule.getSeverity()) {
            case INFO -> "ℹ️";
            case WARNING -> "⚠️";
            case CRITICAL -> "🔴";
        };

        return String.format("%s 告警触发: %s - %s", severityEmoji, rule.getName(), rule.getMeasureName());
    }

    private String buildAlertContent(AlertRule rule, AlertEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("告警规则: ").append(rule.getName()).append("\n");
        sb.append("度量指标: ").append(rule.getMeasureName()).append("\n");
        sb.append("严重等级: ").append(rule.getSeverity()).append("\n");
        sb.append("触发条件: ").append(formatCondition(rule)).append("\n");
        sb.append("触发值: ").append(event.getTriggerValue()).append("\n");
        sb.append("阈值: ").append(event.getThreshold()).append("\n");

        if (event.getChangePercent() != null) {
            sb.append("变化率: ").append(event.getChangePercent()).append("%\n");
        }

        sb.append("触发时间: ").append(event.getTriggeredAt()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");

        return sb.toString();
    }

    private String buildAlertEmailContent(AlertRule rule, AlertEvent event) {
        String severityColor = switch (rule.getSeverity()) {
            case INFO -> "#17a2b8";
            case WARNING -> "#ffc107";
            case CRITICAL -> "#dc3545";
        };

        String changeInfo = "";
        if (event.getChangePercent() != null) {
            changeInfo = String.format("""
                <tr>
                    <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">变化率</td>
                    <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">%.2f%%</td>
                </tr>
                """, event.getChangePercent());
        }

        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: %s; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                    <h2 style="margin: 0;">🔔 告警通知</h2>
                </div>
                <div style="background: #ffffff; padding: 30px; border: 1px solid #e9ecef; border-radius: 0 0 8px 8px;">
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef; font-weight: bold; width: 30%%;">告警规则</td>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef; font-weight: bold;">度量指标</td>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef; font-weight: bold;">严重等级</td>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">
                                <span style="background: %s; color: white; padding: 4px 12px; border-radius: 4px;">%s</span>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef; font-weight: bold;">触发条件</td>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef; font-weight: bold;">触发值</td>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef; font-weight: bold;">阈值</td>
                            <td style="padding: 12px; border-bottom: 1px solid #e9ecef;">%s</td>
                        </tr>
                        %s
                        <tr>
                            <td style="padding: 12px; font-weight: bold;">触发时间</td>
                            <td style="padding: 12px;">%s</td>
                        </tr>
                    </table>
                    <div style="margin-top: 20px; padding-top: 20px; border-top: 1px solid #e9ecef; text-align: center; color: #999; font-size: 12px;">
                        此邮件由低代码BI平台自动发送，请勿直接回复。
                    </div>
                </div>
            </div>
            """,
            severityColor,
            rule.getName(),
            rule.getMeasureName(),
            severityColor, rule.getSeverity(),
            formatCondition(rule),
            event.getTriggerValue(),
            event.getThreshold(),
            changeInfo,
            event.getTriggeredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String formatCondition(AlertRule rule) {
        String operatorStr = switch (rule.getOperator()) {
            case GREATER_THAN, GREATER_THAN_OR_EQUAL -> ">";
            case LESS_THAN, LESS_THAN_OR_EQUAL -> "<";
            case EQUAL -> "=";
            case NOT_EQUAL -> "!=";
            case INCREASE_PERCENT -> "环比上涨 >";
            case DECREASE_PERCENT -> "环比下跌 >";
            case CHANGE_PERCENT -> "环比变化绝对值 >";
        };

        if (rule.getTriggerType() == AlertTriggerType.RELATIVE_CHANGE) {
            return String.format("%s %.2f%%", operatorStr, rule.getThreshold());
        } else {
            return String.format("%s %s", operatorStr, rule.getThreshold());
        }
    }

    private int getPriorityFromSeverity(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 2;
            case WARNING -> 1;
            case INFO -> 0;
        };
    }

    private User getUserById(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    @Transactional
    public void sendEscalationInAppNotification(User user, AlertRule rule, AlertEvent event,
                                                AlertSeverity fromSeverity, AlertSeverity toSeverity,
                                                boolean isMaxLevel) {
        SystemMessage message = new SystemMessage();
        message.setTenant(rule.getTenant());
        message.setUser(user);
        message.setMessageType(SystemMessageType.ALERT);
        message.setTitle(buildEscalationTitle(rule, fromSeverity, toSeverity, isMaxLevel));
        message.setContent(buildEscalationContent(rule, event, fromSeverity, toSeverity, isMaxLevel));
        message.setRelatedType("ALERT_EVENT");
        message.setRelatedId(event.getId());
        message.setIsRead(false);

        systemMessageRepository.save(message);
        log.info("Sent escalation in-app notification to user {} for rule {}", user.getId(), rule.getId());
    }

    private String buildEscalationTitle(AlertRule rule, AlertSeverity fromSeverity,
                                        AlertSeverity toSeverity, boolean isMaxLevel) {
        if (isMaxLevel) {
            return String.format("🔴 告警升级提醒: %s - 持续触发中", rule.getName());
        }
        return String.format("⬆️ 告警升级: %s - %s → %s", rule.getName(), fromSeverity, toSeverity);
    }

    private String buildEscalationContent(AlertRule rule, AlertEvent event,
                                          AlertSeverity fromSeverity, AlertSeverity toSeverity,
                                          boolean isMaxLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("【告警升级通知】\n\n");
        sb.append("告警规则: ").append(rule.getName()).append("\n");
        sb.append("度量指标: ").append(rule.getMeasureName()).append("\n");

        if (isMaxLevel) {
            sb.append("当前等级: 严重 (已达最高级)\n");
            sb.append("⚠️ 警告: 该告警已达到最高严重等级，但仍持续触发且未被确认，请及时处理！\n");
        } else {
            sb.append("等级变更: ").append(getSeverityText(fromSeverity))
              .append(" → ").append(getSeverityText(toSeverity)).append("\n");
            sb.append("升级次数: ").append(rule.getEscalationLevel()).append("\n");
        }

        sb.append("\n连续触发次数已达到阈值 (").append(rule.getEscalationThreshold()).append("次)\n");
        sb.append("当前触发值: ").append(event.getTriggerValue()).append("\n");
        sb.append("阈值: ").append(event.getThreshold()).append("\n");
        sb.append("触发时间: ").append(event.getTriggeredAt()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("\n请尽快确认并处理此告警！");

        return sb.toString();
    }

    private String getSeverityText(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> "信息";
            case WARNING -> "警告";
            case CRITICAL -> "严重";
        };
    }

    public static class WebhookPayload {
        private String ruleName;
        private String triggerValue;
        private String threshold;
        private String triggeredAt;
        private String severity;
        private String eventId;
        private String measureName;

        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getTriggerValue() { return triggerValue; }
        public void setTriggerValue(String triggerValue) { this.triggerValue = triggerValue; }
        public String getThreshold() { return threshold; }
        public void setThreshold(String threshold) { this.threshold = threshold; }
        public String getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(String triggeredAt) { this.triggeredAt = triggeredAt; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getMeasureName() { return measureName; }
        public void setMeasureName(String measureName) { this.measureName = measureName; }
    }
}
