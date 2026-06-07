package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.AggregationType;
import com.lowcode.bi.listener.DataModelAlertListener;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "measures")
@EntityListeners(DataModelAlertListener.class)
public class Measure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "table_alias", length = 64, nullable = false)
    private String tableAlias;

    @Column(name = "column_name", length = 128, nullable = false)
    private String columnName;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", length = 32, nullable = false)
    private AggregationType aggregationType;

    @Column(name = "filter_condition", length = 1024)
    private String filterCondition;

    @Column(name = "expression", columnDefinition = "text")
    private String expression;

    @Column(name = "is_distinct", nullable = false)
    private Boolean isDistinct = false;

    @Column(name = "format_pattern", length = 128)
    private String formatPattern;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "decimal_places")
    private Integer decimalPlaces;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "is_pre_aggregated", nullable = false)
    private Boolean isPreAggregated = false;

    @Column(name = "pre_aggregation_schedule", length = 128)
    private String preAggregationSchedule;

    @Column(name = "pre_aggregation_table", length = 128)
    private String preAggregationTable;
}
