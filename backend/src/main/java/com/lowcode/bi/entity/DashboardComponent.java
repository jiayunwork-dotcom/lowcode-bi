package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.ChartType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dashboard_components")
public class DashboardComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id", nullable = false)
    private DashboardTab tab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id")
    private DataModel dataModel;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_type", length = 32, nullable = false)
    private ChartType chartType;

    @Column(name = "position_x", nullable = false)
    private Integer positionX = 0;

    @Column(name = "position_y", nullable = false)
    private Integer positionY = 0;

    @Column(name = "width", nullable = false)
    private Integer width = 6;

    @Column(name = "height", nullable = false)
    private Integer height = 4;

    @Column(name = "z_index", nullable = false)
    private Integer zIndex = 1;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Column(name = "dimensions", columnDefinition = "json")
    private String dimensions;

    @Column(name = "measures", columnDefinition = "json")
    private String measures;

    @Column(name = "filters", columnDefinition = "json")
    private String filters;

    @Column(name = "sorts", columnDefinition = "json")
    private String sorts;

    @Column(name = "chart_config", columnDefinition = "json")
    private String chartConfig;

    @Column(name = "style_config", columnDefinition = "json")
    private String styleConfig;

    @Column(name = "custom_sql", columnDefinition = "text")
    private String customSql;

    @Column(name = "sql_params", columnDefinition = "json")
    private String sqlParams;

    @Column(name = "data_query_config", columnDefinition = "json")
    private String dataQueryConfig;

    @Column(name = "use_pre_aggregation", nullable = false)
    private Boolean usePreAggregation = false;

    @Column(name = "cache_ttl")
    private Integer cacheTtl;

    @Column(name = "enable_drill_down", nullable = false)
    private Boolean enableDrillDown = true;

    @Column(name = "drill_down_path", columnDefinition = "json")
    private String drillDownPath;

    @Column(name = "enable_linkage", nullable = false)
    private Boolean enableLinkage = true;

    @Column(name = "linkage_targets", columnDefinition = "json")
    private String linkageTargets;

    @Column(name = "linkage_config", columnDefinition = "json")
    private String linkageConfig;

    @Column(name = "time_comparison", columnDefinition = "json")
    private String timeComparison;

    @Column(name = "conditional_formatting", columnDefinition = "json")
    private String conditionalFormatting;

    @Column(name = "data_refresh_interval")
    private Integer dataRefreshInterval;

    @Column(name = "last_data_updated_at")
    private java.time.LocalDateTime lastDataUpdatedAt;

    @Column(name = "is_using_custom_query", nullable = false)
    private Boolean isUsingCustomQuery = false;

    @Column(name = "query_timeout")
    private Integer queryTimeout = 60;

    @Column(name = "max_rows")
    private Integer maxRows = 10000;

    @Column(name = "component_data", columnDefinition = "json")
    private String componentData;
}
