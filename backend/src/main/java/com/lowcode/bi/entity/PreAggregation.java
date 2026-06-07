package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "pre_aggregations")
public class PreAggregation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "target_table", length = 128, nullable = false)
    private String targetTable;

    @Column(name = "dimensions", columnDefinition = "json", nullable = false)
    private String dimensions;

    @Column(name = "measures", columnDefinition = "json", nullable = false)
    private String measures;

    @Column(name = "filters", columnDefinition = "json")
    private String filters;

    @Column(name = "aggregation_sql", columnDefinition = "text", nullable = false)
    private String aggregationSql;

    @Column(name = "cron_expression", length = 64, nullable = false)
    private String cronExpression;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "refresh_strategy", length = 32)
    private String refreshStrategy = "FULL";

    @Column(name = "incremental_column", length = 128)
    private String incrementalColumn;

    @Column(name = "last_refresh_at")
    private LocalDateTime lastRefreshAt;

    @Column(name = "last_refresh_status", length = 32)
    private String lastRefreshStatus;

    @Column(name = "last_refresh_message", length = 1024)
    private String lastRefreshMessage;

    @Column(name = "last_refresh_row_count")
    private Long lastRefreshRowCount;

    @Column(name = "last_refresh_duration_ms")
    private Long lastRefreshDurationMs;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "is_paused", nullable = false)
    private Boolean isPaused = false;

    @Column(name = "pause_reason", length = 512)
    private String pauseReason;

    @Column(name = "data_size_bytes")
    private Long dataSizeBytes;

    @Column(name = "hit_count", nullable = false)
    private Long hitCount = 0L;

    @Column(name = "last_hit_at")
    private LocalDateTime lastHitAt;
}
