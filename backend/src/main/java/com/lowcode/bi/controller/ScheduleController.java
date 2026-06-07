package com.lowcode.bi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.ScheduleConfig;
import com.lowcode.bi.entity.ScheduleExecutionLog;
import com.lowcode.bi.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> createSchedule(@RequestBody Map<String, Object> request) {
        try {
            UUID dashboardId = UUID.fromString((String) request.get("dashboardId"));
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String cronExpression = (String) request.get("cronExpression");
            String timezone = (String) request.get("timezone");

            @SuppressWarnings("unchecked")
            List<String> recipients = request.get("recipients") != null ?
                    (List<String>) request.get("recipients") : null;

            String emailSubject = (String) request.get("emailSubject");
            String emailBody = (String) request.get("emailBody");
            Boolean includeAttachment = (Boolean) request.get("includeAttachment");
            Boolean includeDashboardLink = (Boolean) request.get("includeDashboardLink");

            @SuppressWarnings("unchecked")
            Map<String, Object> filters = request.get("filters") != null ?
                    (Map<String, Object>) request.get("filters") : null;

            @SuppressWarnings("unchecked")
            List<String> tabsToInclude = request.get("tabsToInclude") != null ?
                    (List<String>) request.get("tabsToInclude") : null;

            LocalDateTime startDate = request.get("startDate") != null ?
                    LocalDateTime.parse((String) request.get("startDate")) : null;
            LocalDateTime endDate = request.get("endDate") != null ?
                    LocalDateTime.parse((String) request.get("endDate")) : null;

            Integer screenshotWidth = request.get("screenshotWidth") != null ?
                    ((Number) request.get("screenshotWidth")).intValue() : null;
            Integer screenshotHeight = request.get("screenshotHeight") != null ?
                    ((Number) request.get("screenshotHeight")).intValue() : null;
            Integer waitForRenderMs = request.get("waitForRenderMs") != null ?
                    ((Number) request.get("waitForRenderMs")).intValue() : null;

            ScheduleConfig schedule = scheduleService.createSchedule(
                    dashboardId, name, description,
                    cronExpression, timezone,
                    recipients, emailSubject, emailBody,
                    includeAttachment, includeDashboardLink,
                    filters, tabsToInclude,
                    startDate, endDate,
                    screenshotWidth, screenshotHeight, waitForRenderMs
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", schedule);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Create schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "创建定时任务失败: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> updateSchedule(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String cronExpression = (String) request.get("cronExpression");
            String status = (String) request.get("status");

            @SuppressWarnings("unchecked")
            List<String> recipients = request.get("recipients") != null ?
                    (List<String>) request.get("recipients") : null;

            String emailSubject = (String) request.get("emailSubject");
            String emailBody = (String) request.get("emailBody");

            @SuppressWarnings("unchecked")
            Map<String, Object> filters = request.get("filters") != null ?
                    (Map<String, Object>) request.get("filters") : null;

            ScheduleConfig schedule = scheduleService.updateSchedule(
                    id, name, description, cronExpression, status,
                    recipients, emailSubject, emailBody, filters
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", schedule);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Update schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "更新定时任务失败: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> deleteSchedule(@PathVariable UUID id) {
        try {
            scheduleService.deleteSchedule(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "定时任务已删除");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Delete schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "删除定时任务失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getSchedule(@PathVariable UUID id) {
        try {
            ScheduleConfig schedule = scheduleService.getSchedule(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", schedule);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取定时任务失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/dashboard/{dashboardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getSchedulesByDashboard(@PathVariable UUID dashboardId) {
        try {
            List<ScheduleConfig> schedules = scheduleService.getSchedulesByDashboard(dashboardId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", schedules);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get schedules error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取定时任务列表失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getSchedulesByTenant() {
        try {
            List<ScheduleConfig> schedules = scheduleService.getSchedulesByTenant();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", schedules);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get schedules error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取定时任务列表失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> executeSchedule(@PathVariable UUID id) {
        try {
            ScheduleExecutionLog log = scheduleService.executeSchedule(id, true);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", log);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Execute schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "执行定时任务失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getExecutionLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executionTime"));
            Page<ScheduleExecutionLog> logs = scheduleService.getExecutionLogs(id, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get execution logs error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取执行日志失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getScheduleStats(@PathVariable UUID id) {
        try {
            Map<String, Object> stats = scheduleService.getScheduleStats(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get schedule stats error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> pauseSchedule(@PathVariable UUID id) {
        try {
            scheduleService.pauseSchedule(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "定时任务已暂停");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Pause schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "暂停定时任务失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> resumeSchedule(@PathVariable UUID id) {
        try {
            scheduleService.resumeSchedule(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "定时任务已恢复");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Resume schedule error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "恢复定时任务失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/cleanup-logs")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> cleanupOldLogs(@RequestBody Map<String, Object> request) {
        try {
            int retentionDays = request.get("retentionDays") != null ?
                    ((Number) request.get("retentionDays")).intValue() : 30;

            scheduleService.cleanupOldLogs(retentionDays);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "日志清理完成");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Cleanup logs error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "清理日志失败: " + e.getMessage()
            ));
        }
    }
}
