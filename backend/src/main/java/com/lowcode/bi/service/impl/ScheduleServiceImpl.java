package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.Dashboard;
import com.lowcode.bi.entity.ScheduleConfig;
import com.lowcode.bi.entity.ScheduleExecutionLog;
import com.lowcode.bi.entity.Tenant;
import com.lowcode.bi.repository.DashboardRepository;
import com.lowcode.bi.repository.ScheduleConfigRepository;
import com.lowcode.bi.repository.ScheduleExecutionLogRepository;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.EmailService;
import com.lowcode.bi.service.ScheduleService;
import com.lowcode.bi.service.ScreenshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleConfigRepository scheduleConfigRepository;
    private final ScheduleExecutionLogRepository executionLogRepository;
    private final DashboardRepository dashboardRepository;
    private final ScreenshotService screenshotService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.schedule.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    @Value("${app.schedule.log-retention-days:30}")
    private int logRetentionDays;

    @Override
    @Transactional
    public ScheduleConfig createSchedule(UUID dashboardId, String name, String description,
                                         String cronExpression, String timezone,
                                         List<String> recipients, String emailSubject,
                                         String emailBody, Boolean includeAttachment,
                                         Boolean includeDashboardLink,
                                         Map<String, Object> filters, List<String> tabsToInclude,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         Integer screenshotWidth, Integer screenshotHeight,
                                         Integer waitForRenderMs) {
        UUID tenantId = TenantContext.getTenantId();

        Dashboard dashboard = dashboardRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));

        validateCronExpression(cronExpression);

        ScheduleConfig schedule = new ScheduleConfig();
        schedule.setDashboard(dashboard);
        schedule.setTenant(dashboard.getTenant());
        schedule.setName(name);
        schedule.setDescription(description);
        schedule.setCronExpression(cronExpression);
        schedule.setTimezone(timezone != null ? timezone : "Asia/Shanghai");
        schedule.setStatus("ACTIVE");

        try {
            if (recipients != null) {
                schedule.setRecipients(objectMapper.writeValueAsString(recipients));
            }
            if (filters != null) {
                schedule.setFilters(objectMapper.writeValueAsString(filters));
            }
            if (tabsToInclude != null) {
                schedule.setTabsToInclude(objectMapper.writeValueAsString(tabsToInclude));
            }
        } catch (Exception e) {
            throw new BusinessException("序列化配置失败: " + e.getMessage());
        }

        schedule.setEmailSubject(emailSubject);
        schedule.setEmailBody(emailBody);
        schedule.setIncludeAttachment(includeAttachment != null ? includeAttachment : true);
        schedule.setIncludeDashboardLink(includeDashboardLink != null ? includeDashboardLink : true);
        schedule.setScreenshotWidth(screenshotWidth != null ? screenshotWidth : 1920);
        schedule.setScreenshotHeight(screenshotHeight != null ? screenshotHeight : 1080);
        schedule.setWaitForRenderMs(waitForRenderMs != null ? waitForRenderMs : 5000);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        schedule.setJobKey("job_" + UUID.randomUUID().toString().replace("-", ""));
        schedule.setTriggerKey("trigger_" + UUID.randomUUID().toString().replace("-", ""));

        return scheduleConfigRepository.save(schedule);
    }

    @Override
    @Transactional
    public ScheduleConfig updateSchedule(UUID id, String name, String description,
                                         String cronExpression, String status,
                                         List<String> recipients, String emailSubject,
                                         String emailBody, Map<String, Object> filters) {
        UUID tenantId = TenantContext.getTenantId();
        ScheduleConfig schedule = scheduleConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));

        if (name != null) schedule.setName(name);
        if (description != null) schedule.setDescription(description);
        if (cronExpression != null) {
            validateCronExpression(cronExpression);
            schedule.setCronExpression(cronExpression);
        }
        if (status != null) {
            schedule.setStatus(status);
            if ("PAUSED".equals(status)) {
                schedule.setConsecutiveFailures(0);
            }
        }

        try {
            if (recipients != null) {
                schedule.setRecipients(objectMapper.writeValueAsString(recipients));
            }
            if (filters != null) {
                schedule.setFilters(objectMapper.writeValueAsString(filters));
            }
        } catch (Exception e) {
            throw new BusinessException("序列化配置失败: " + e.getMessage());
        }

        if (emailSubject != null) schedule.setEmailSubject(emailSubject);
        if (emailBody != null) schedule.setEmailBody(emailBody);

        return scheduleConfigRepository.save(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        ScheduleConfig schedule = scheduleConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));

        executionLogRepository.deleteByScheduleId(id);
        scheduleConfigRepository.delete(schedule);
    }

    @Override
    public ScheduleConfig getSchedule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return scheduleConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));
    }

    @Override
    public List<ScheduleConfig> getSchedulesByDashboard(UUID dashboardId) {
        UUID tenantId = TenantContext.getTenantId();
        dashboardRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));
        return scheduleConfigRepository.findByDashboardId(dashboardId);
    }

    @Override
    public List<ScheduleConfig> getSchedulesByTenant() {
        UUID tenantId = TenantContext.getTenantId();
        return scheduleConfigRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional
    public ScheduleExecutionLog executeSchedule(UUID id, boolean manualTrigger) {
        UUID tenantId = TenantContext.getTenantId();
        ScheduleConfig schedule = scheduleConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));

        if (!manualTrigger && "PAUSED".equals(schedule.getStatus())) {
            throw new BusinessException("定时任务已暂停");
        }

        if (!manualTrigger && schedule.getEndDate() != null
                && schedule.getEndDate().isBefore(LocalDateTime.now())) {
            schedule.setStatus("EXPIRED");
            scheduleConfigRepository.save(schedule);
            throw new BusinessException("定时任务已过期");
        }

        long totalStartTime = System.currentTimeMillis();

        ScheduleExecutionLog executionLog = new ScheduleExecutionLog();
        executionLog.setSchedule(schedule);
        executionLog.setExecutionTime(LocalDateTime.now());
        executionLog.setStatus("RUNNING");

        try {
            List<String> recipients = parseRecipients(schedule.getRecipients());
            if (recipients.isEmpty()) {
                throw new BusinessException("收件人列表为空");
            }

            Map<String, Object> filters = parseFilters(schedule.getFilters());

            long queryStart = System.currentTimeMillis();
            ScreenshotService.ScreenshotResult screenshotResult =
                    screenshotService.captureDashboard(
                            schedule.getDashboard().getId(), schedule, filters);
            long queryDuration = System.currentTimeMillis() - queryStart;

            executionLog.setQueryDurationMs(queryDuration);
            executionLog.setRenderDurationMs(screenshotResult.getRenderDurationMs());
            executionLog.setIsDataTimeout(screenshotResult.isDataTimeout());

            byte[] imageData = screenshotResult.getImageData();
            if (imageData != null) {
                executionLog.setScreenshotSize((long) imageData.length);
            }

            String subject = schedule.getEmailSubject() != null ?
                    schedule.getEmailSubject() :
                    "仪表板报告 - " + schedule.getDashboard().getName();

            String body = schedule.getEmailBody() != null ?
                    schedule.getEmailBody() :
                    "这是定时生成的仪表板报告。";

            long emailStart = System.currentTimeMillis();
            boolean emailSent = emailService.sendDashboardReport(
                    recipients,
                    subject,
                    body,
                    imageData,
                    screenshotResult.getDashboardLink(),
                    screenshotResult.isDataTimeout()
            );

            executionLog.setEmailSent(emailSent);
            executionLog.setEmailRecipients(schedule.getRecipients());
            executionLog.setEmailSubject(subject);

            if (!screenshotResult.isSuccess() && !screenshotResult.isDataTimeout()) {
                throw new BusinessException(screenshotResult.getErrorMessage());
            }

            executionLog.setStatus("SUCCESS");
            schedule.setLastExecutedAt(LocalDateTime.now());
            schedule.setLastExecutionStatus("SUCCESS");
            schedule.setLastExecutionError(null);
            schedule.setSuccessCount(schedule.getSuccessCount() + 1);
            schedule.setConsecutiveFailures(0);

            if (schedule.getNotifyOnSuccess() && schedule.getNotificationRecipients() != null) {
                List<String> notifyRecipients = parseRecipients(schedule.getNotificationRecipients());
                emailService.sendNotification(notifyRecipients,
                        "定时任务执行成功 - " + schedule.getName(),
                        "定时任务 " + schedule.getName() + " 执行成功。",
                        true);
            }

        } catch (Exception e) {
            log.error("定时任务执行失败: {}", e.getMessage(), e);
            executionLog.setStatus("FAILED");
            executionLog.setErrorMessage(e.getMessage());

            schedule.setLastExecutionStatus("FAILED");
            schedule.setLastExecutionError(e.getMessage());
            schedule.setFailureCount(schedule.getFailureCount() + 1);
            schedule.setConsecutiveFailures(schedule.getConsecutiveFailures() + 1);

            if (schedule.getConsecutiveFailures() >= maxConsecutiveFailures) {
                schedule.setStatus("PAUSED");
                log.warn("定时任务{}因连续失败{}次已自动暂停",
                        schedule.getName(), maxConsecutiveFailures);

                if (schedule.getNotifyOnFailure() && schedule.getNotificationRecipients() != null) {
                    List<String> notifyRecipients = parseRecipients(schedule.getNotificationRecipients());
                    emailService.sendNotification(notifyRecipients,
                            "定时任务已暂停 - " + schedule.getName(),
                            "定时任务 " + schedule.getName() + " 连续失败" + maxConsecutiveFailures +
                                    "次，已自动暂停。错误信息: " + e.getMessage(),
                            false);
                }
            } else if (schedule.getNotifyOnFailure() && schedule.getNotificationRecipients() != null) {
                List<String> notifyRecipients = parseRecipients(schedule.getNotificationRecipients());
                emailService.sendNotification(notifyRecipients,
                        "定时任务执行失败 - " + schedule.getName(),
                        "定时任务 " + schedule.getName() + " 执行失败。错误信息: " + e.getMessage(),
                        false);
            }
        }

        executionLog.setTotalDurationMs(System.currentTimeMillis() - totalStartTime);
        executionLog = executionLogRepository.save(executionLog);
        scheduleConfigRepository.save(schedule);

        return executionLog;
    }

    @Override
    public Page<ScheduleExecutionLog> getExecutionLogs(UUID scheduleId, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        scheduleConfigRepository.findByIdAndTenantId(scheduleId, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));
        return executionLogRepository.findByScheduleIdOrderByExecutionTimeDesc(scheduleId, pageable);
    }

    @Override
    public Map<String, Object> getScheduleStats(UUID scheduleId) {
        ScheduleConfig schedule = getSchedule(scheduleId);

        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        long successCount = executionLogRepository.countByScheduleIdAndStatusAndExecutionTimeAfter(
                scheduleId, "SUCCESS", startTime);
        long failureCount = executionLogRepository.countByScheduleIdAndStatusAndExecutionTimeAfter(
                scheduleId, "FAILED", startTime);

        Map<String, Object> stats = new HashMap<>();
        stats.put("id", schedule.getId());
        stats.put("name", schedule.getName());
        stats.put("status", schedule.getStatus());
        stats.put("totalSuccess", schedule.getSuccessCount());
        stats.put("totalFailure", schedule.getFailureCount());
        stats.put("consecutiveFailures", schedule.getConsecutiveFailures());
        stats.put("last30DaysSuccess", successCount);
        stats.put("last30DaysFailure", failureCount);
        stats.put("lastExecutedAt", schedule.getLastExecutedAt());
        stats.put("lastExecutionStatus", schedule.getLastExecutionStatus());
        stats.put("lastExecutionError", schedule.getLastExecutionError());
        stats.put("cronExpression", schedule.getCronExpression());

        return stats;
    }

    @Override
    @Transactional
    public void pauseSchedule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        ScheduleConfig schedule = scheduleConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));
        schedule.setStatus("PAUSED");
        schedule.setConsecutiveFailures(0);
        scheduleConfigRepository.save(schedule);
    }

    @Override
    @Transactional
    public void resumeSchedule(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        ScheduleConfig schedule = scheduleConfigRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("定时任务不存在"));
        schedule.setStatus("ACTIVE");
        schedule.setConsecutiveFailures(0);
        scheduleConfigRepository.save(schedule);
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void runActiveSchedules() {
        List<ScheduleConfig> activeSchedules = scheduleConfigRepository.findActiveSchedules();
        LocalDateTime now = LocalDateTime.now();

        for (ScheduleConfig schedule : activeSchedules) {
            try {
                if (!shouldExecuteNow(schedule, now)) {
                    continue;
                }

                TenantContext.setTenantId(schedule.getTenant().getId());
                TenantContext.setUserId(UUID.fromString(schedule.getCreatedBy()));

                executeSchedule(schedule.getId(), false);

            } catch (Exception e) {
                log.error("执行定时任务失败: {} - {}", schedule.getName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldLogs(int retentionDays) {
        int days = retentionDays > 0 ? retentionDays : logRetentionDays;
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        int deleted = executionLogRepository.deleteOldLogs(cutoffTime);
        log.info("清理了{}条过期的执行日志", deleted);
    }

    @Override
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void checkAndPauseFailedSchedules() {
        List<ScheduleConfig> activeSchedules = scheduleConfigRepository.findActiveSchedules();
        for (ScheduleConfig schedule : activeSchedules) {
            if (schedule.getConsecutiveFailures() >= maxConsecutiveFailures) {
                schedule.setStatus("PAUSED");
                scheduleConfigRepository.save(schedule);
                log.warn("定时任务{}因连续失败{}次已自动暂停",
                        schedule.getName(), maxConsecutiveFailures);
            }
        }
    }

    private void validateCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
        } catch (Exception e) {
            throw new BusinessException("无效的Cron表达式: " + e.getMessage());
        }
    }

    private boolean shouldExecuteNow(ScheduleConfig schedule, LocalDateTime now) {
        if (!"ACTIVE".equals(schedule.getStatus())) {
            return false;
        }

        if (schedule.getStartDate() != null && schedule.getStartDate().isAfter(now)) {
            return false;
        }

        if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(now)) {
            return false;
        }

        try {
            CronExpression cron = CronExpression.parse(schedule.getCronExpression());
            LocalDateTime lastExecuted = schedule.getLastExecutedAt();
            LocalDateTime nextExecution = lastExecuted != null ?
                    cron.next(lastExecuted) : cron.next(now.minusMinutes(1));

            return nextExecution != null && !nextExecution.isAfter(now);
        } catch (Exception e) {
            log.warn("解析Cron表达式失败: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseRecipients(String json) {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析收件人列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFilters(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            log.warn("解析过滤器失败: {}", e.getMessage());
            return Map.of();
        }
    }
}
