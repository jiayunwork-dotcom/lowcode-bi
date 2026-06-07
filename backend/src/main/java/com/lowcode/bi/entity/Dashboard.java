package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.RefreshInterval;
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
@Table(name = "dashboards")
public class Dashboard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Dashboard parent;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "cover_image", length = 512)
    private String coverImage;

    @Column(name = "status", length = 32, nullable = false)
    private String status = "DRAFT";

    @Column(name = "is_template", nullable = false)
    private Boolean isTemplate = false;

    @Column(name = "template_category", length = 64)
    private String templateCategory;

    @Column(name = "theme", length = 32, nullable = false)
    private String theme = "light";

    @Column(name = "layout_config", columnDefinition = "json")
    private String layoutConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "refresh_interval", length = 32, nullable = false)
    private RefreshInterval refreshInterval = RefreshInterval.OFF;

    @Column(name = "refresh_paused", nullable = false)
    private Boolean refreshPaused = false;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "last_refresh_at")
    private java.time.LocalDateTime lastRefreshAt;

    @Column(name = "last_refresh_success", nullable = false)
    private Boolean lastRefreshSuccess = true;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private java.time.LocalDateTime publishedAt;

    @Column(name = "published_by", length = 64)
    private String publishedBy;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "enable_drill_down", nullable = false)
    private Boolean enableDrillDown = true;

    @Column(name = "enable_linkage", nullable = false)
    private Boolean enableLinkage = true;

    @Column(name = "show_title", nullable = false)
    private Boolean showTitle = true;

    @Column(name = "show_filters", nullable = false)
    private Boolean showFilters = true;

    @Column(name = "show_toolbar", nullable = false)
    private Boolean showToolbar = true;

    @Column(name = "global_filters", columnDefinition = "json")
    private String globalFilters;

    @Column(name = "permission_config", columnDefinition = "json")
    private String permissionConfig;

    @Column(name = "embed_enabled", nullable = false)
    private Boolean embedEnabled = false;

    @Column(name = "embed_secret", length = 64)
    private String embedSecret;

    @Column(name = "embed_settings", columnDefinition = "json")
    private String embedSettings;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "favorite_count", nullable = false)
    private Long favoriteCount = 0L;

    @Column(name = "tags", columnDefinition = "json")
    private String tags;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<DashboardTab> tabs = new ArrayList<>();

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<DashboardComponent> components = new HashSet<>();

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<DashboardPermission> permissions = new HashSet<>();

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<ScheduleConfig> schedules = new HashSet<>();
}
