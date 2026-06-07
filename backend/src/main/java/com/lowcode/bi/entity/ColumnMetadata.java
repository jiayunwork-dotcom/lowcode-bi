package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.ColumnDataType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "column_metadata")
public class ColumnMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableMetadata table;

    @Column(name = "column_name", length = 128, nullable = false)
    private String columnName;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 32, nullable = false)
    private ColumnDataType dataType;

    @Column(name = "native_type", length = 64)
    private String nativeType;

    @Column(name = "precision")
    private Integer precision;

    @Column(name = "scale")
    private Integer scale;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "is_nullable", nullable = false)
    private Boolean isNullable = true;

    @Column(name = "is_primary_key", nullable = false)
    private Boolean isPrimaryKey = false;

    @Column(name = "is_foreign_key", nullable = false)
    private Boolean isForeignKey = false;

    @Column(name = "is_indexed", nullable = false)
    private Boolean isIndexed = false;

    @Column(name = "default_value", length = 256)
    private String defaultValue;

    @Column(name = "distinct_count")
    private Long distinctCount;

    @Column(name = "min_value", length = 256)
    private String minValue;

    @Column(name = "max_value", length = 256)
    private String maxValue;

    @Column(name = "sample_values", columnDefinition = "json")
    private String sampleValues;

    @Column(name = "is_dimension", nullable = false)
    private Boolean isDimension = true;

    @Column(name = "is_measure", nullable = false)
    private Boolean isMeasure = false;

    @Column(name = "format_pattern", length = 128)
    private String formatPattern;

    @Column(name = "unit", length = 32)
    private String unit;
}
