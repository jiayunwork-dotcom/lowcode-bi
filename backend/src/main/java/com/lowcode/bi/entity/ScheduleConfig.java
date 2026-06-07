package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "schedule_configs")
public class ScheduleConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "cron_expression", length = 128, nullable = false)
    private String cronExpression;

    @Column(name = "timezone", length = 64, nullable = false)
    private String timezone = "Asia/Shanghai";

    @Column(name = "status", length = 32, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "schedule_type", length = 32, nullable = false)
    private String scheduleType = "EMAIL";

    @Column(name = "recipients", columnDefinition = "json")
    private String recipients;

    @Column(name = "email_subject", length = 256)
    private String emailSubject;

    @Column(name = "email_body", columnDefinition = "text")
    private String emailBody;

    @Column(name = "include_attachment", nullable = false)
    private Boolean includeAttachment = true;

    @Column(name = "attachment_format", length = 32)
    private String attachmentFormat = "PNG";

    @Column(name = "include_dashboard_link", nullable = false)
    private Boolean includeDashboardLink = true;

    @Column(name = "screenshot_width", nullable = false)
    private Integer screenshotWidth = 1920;

    @Column(name = "screenshot_height", nullable = false)
    private Integer screenshotHeight = 1080;

    @Column(name = "wait_for_render_ms", nullable = false)
    private Integer waitForRenderMs = 5000;

    @Column(name = "filters", columnDefinition = "json")
    private String filters;

    @Column(name = "tabs_to_include", columnDefinition = "json")
    private String tabsToInclude;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "last_execution_status", length = 32)
    private String lastExecutionStatus;

    @Column(name = "last_execution_error", length = 1024)
    private String lastExecutionError;

    @Column(name = "success_count", nullable = false)
    private Long successCount = 0L;

    @Column(name = "failure_count", nullable = false)
    private Long failureCount = 0L;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "retry_on_failure", nullable = false)
    private Boolean retryOnFailure = true;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "notify_on_success", nullable = false)
    private Boolean notifyOnSuccess = false;

    @Column(name = "notify_on_failure", nullable = false)
    private Boolean notifyOnFailure = true;

    @Column(name = "notification_recipients", columnDefinition = "json")
    private String notificationRecipients;

    @Column(name = "job_key", length = 128)
    private String jobKey;

    @Column(name = "trigger_key", length = 128)
    private String triggerKey;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ScheduleExecutionLog> executionLogs = new HashSet<>();
}
