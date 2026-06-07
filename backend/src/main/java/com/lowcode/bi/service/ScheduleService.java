package com.lowcode.bi.service;

import com.lowcode.bi.entity.ScheduleConfig;
import com.lowcode.bi.entity.ScheduleExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ScheduleService {

    ScheduleConfig createSchedule(UUID dashboardId, String name, String description,
                                  String cronExpression, String timezone,
                                  List<String> recipients, String emailSubject,
                                  String emailBody, Boolean includeAttachment,
                                  Boolean includeDashboardLink,
                                  Map<String, Object> filters, List<String> tabsToInclude,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  Integer screenshotWidth, Integer screenshotHeight,
                                  Integer waitForRenderMs);

    ScheduleConfig updateSchedule(UUID id, String name, String description,
                                  String cronExpression, String status,
                                  List<String> recipients, String emailSubject,
                                  String emailBody, Map<String, Object> filters);

    void deleteSchedule(UUID id);

    ScheduleConfig getSchedule(UUID id);

    List<ScheduleConfig> getSchedulesByDashboard(UUID dashboardId);

    List<ScheduleConfig> getSchedulesByTenant();

    ScheduleExecutionLog executeSchedule(UUID id, boolean manualTrigger);

    Page<ScheduleExecutionLog> getExecutionLogs(UUID scheduleId, Pageable pageable);

    Map<String, Object> getScheduleStats(UUID scheduleId);

    void pauseSchedule(UUID id);

    void resumeSchedule(UUID id);

    void runActiveSchedules();

    void cleanupOldLogs(int retentionDays);

    void checkAndPauseFailedSchedules();
}
