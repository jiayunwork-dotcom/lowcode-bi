package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dimension_hierarchies")
public class DimensionHierarchy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @OneToMany(mappedBy = "hierarchy", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("level ASC")
    private List<HierarchyLevel> levels = new ArrayList<>();

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "hierarchy_type", length = 32, nullable = false)
    private String hierarchyType = "STANDARD";
}
