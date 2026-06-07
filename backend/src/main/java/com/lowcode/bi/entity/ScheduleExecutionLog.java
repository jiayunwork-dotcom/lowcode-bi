package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "schedule_execution_logs")
public class ScheduleExecutionLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ScheduleConfig schedule;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "screenshot_path", length = 1024)
    private String screenshotPath;

    @Column(name = "screenshot_size")
    private Long screenshotSize;

    @Column(name = "query_duration_ms")
    private Long queryDurationMs;

    @Column(name = "render_duration_ms")
    private Long renderDurationMs;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    @Column(name = "email_recipients", columnDefinition = "json")
    private String emailRecipients;

    @Column(name = "email_subject", length = 256)
    private String emailSubject;

    @Column(name = "dashboard_snapshot", columnDefinition = "json")
    private String dashboardSnapshot;

    @Column(name = "is_data_timeout", nullable = false)
    private Boolean isDataTimeout = false;

    @Column(name = "retry_attempt", nullable = false)
    private Integer retryAttempt = 0;
}
