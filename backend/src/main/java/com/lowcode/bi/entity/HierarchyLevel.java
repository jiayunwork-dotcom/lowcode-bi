package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "hierarchy_levels")
public class HierarchyLevel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hierarchy_id", nullable = false)
    private DimensionHierarchy hierarchy;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "table_alias", length = 64, nullable = false)
    private String tableAlias;

    @Column(name = "column_name", length = 128, nullable = false)
    private String columnName;

    @Column(name = "date_trunc", length = 32)
    private String dateTrunc;

    @Column(name = "format_pattern", length = 128)
    private String formatPattern;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
