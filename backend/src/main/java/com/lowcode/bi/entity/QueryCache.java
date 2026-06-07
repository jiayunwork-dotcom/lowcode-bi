package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "query_cache", indexes = {
    @Index(name = "idx_cache_key", columnList = "cache_key"),
    @Index(name = "idx_tenant_user", columnList = "tenant_id, user_id"),
    @Index(name = "idx_expire_at", columnList = "expire_at")
})
public class QueryCache extends BaseEntity {

    @Column(name = "cache_key", length = 64, nullable = false)
    private String cacheKey;

    @Column(name = "sql_hash", length = 64, nullable = false)
    private String sqlHash;

    @Column(name = "data_source_id")
    private java.util.UUID dataSourceId;

    @Column(name = "tenant_id", nullable = false)
    private java.util.UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "query_sql", columnDefinition = "text", nullable = false)
    private String querySql;

    @Column(name = "query_params", columnDefinition = "json")
    private String queryParams;

    @Column(name = "result_data", columnDefinition = "json")
    private String resultData;

    @Column(name = "row_count", nullable = false)
    private Long rowCount = 0L;

    @Column(name = "column_count", nullable = false)
    private Integer columnCount = 0;

    @Column(name = "data_size_bytes", nullable = false)
    private Long dataSizeBytes = 0L;

    @Column(name = "query_duration_ms", nullable = false)
    private Long queryDurationMs = 0L;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "hit_count", nullable = false)
    private Long hitCount = 0L;

    @Column(name = "last_hit_at")
    private LocalDateTime lastHitAt;

    @Column(name = "is_pre_aggregated", nullable = false)
    private Boolean isPreAggregated = false;

    @Column(name = "data_model_id")
    private java.util.UUID dataModelId;

    @Column(name = "dashboard_id")
    private java.util.UUID dashboardId;

    @Column(name = "component_id")
    private java.util.UUID componentId;
}
