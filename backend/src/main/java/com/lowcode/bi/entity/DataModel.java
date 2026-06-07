package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "data_models")
public class DataModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSource dataSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "status", length = 32, nullable = false)
    private String status = "DRAFT";

    @Column(name = "is_template", nullable = false)
    private Boolean isTemplate = false;

    @Column(name = "template_category", length = 64)
    private String templateCategory;

    @OneToMany(mappedBy = "dataModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<ModelTable> modelTables = new HashSet<>();

    @OneToMany(mappedBy = "dataModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<ModelRelation> modelRelations = new HashSet<>();

    @OneToMany(mappedBy = "dataModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CalculatedField> calculatedFields = new ArrayList<>();

    @OneToMany(mappedBy = "dataModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Measure> measures = new ArrayList<>();

    @OneToMany(mappedBy = "dataModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<DimensionHierarchy> dimensionHierarchies = new ArrayList<>();

    @Column(name = "row_permission_rules", columnDefinition = "json")
    private String rowPermissionRules;

    @Column(name = "cache_config", columnDefinition = "json")
    private String cacheConfig;

    @Column(name = "pre_aggregation_config", columnDefinition = "json")
    private String preAggregationConfig;

    @Column(name = "sql_template", columnDefinition = "text")
    private String sqlTemplate;

    @Column(name = "published_at")
    private java.time.LocalDateTime publishedAt;

    @Column(name = "published_by", length = 64)
    private String publishedBy;

    @OneToMany(mappedBy = "dataModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DashboardComponent> components = new HashSet<>();
}
