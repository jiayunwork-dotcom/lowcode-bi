package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dashboard_tabs")
public class DashboardTab extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "layout_config", columnDefinition = "json")
    private String layoutConfig;

    @Column(name = "grid_columns", nullable = false)
    private Integer gridColumns = 24;

    @Column(name = "grid_row_height", nullable = false)
    private Integer gridRowHeight = 50;

    @Column(name = "grid_gutter", nullable = false)
    private Integer gridGutter = 10;

    @Column(name = "background_color", length = 32)
    private String backgroundColor;

    @Column(name = "background_image", length = 512)
    private String backgroundImage;

    @Column(name = "tab_filters", columnDefinition = "json")
    private String tabFilters;
}
