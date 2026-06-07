package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.ColumnDataType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "calculated_fields")
public class CalculatedField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 32, nullable = false)
    private ColumnDataType dataType;

    @Column(name = "expression", columnDefinition = "text", nullable = false)
    private String expression;

    @Column(name = "expression_parsed", columnDefinition = "text")
    private String expressionParsed;

    @Column(name = "is_dimension", nullable = false)
    private Boolean isDimension = true;

    @Column(name = "is_measure", nullable = false)
    private Boolean isMeasure = false;

    @Column(name = "format_pattern", length = 128)
    private String formatPattern;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "dependencies", columnDefinition = "json")
    private String dependencies;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;
}
