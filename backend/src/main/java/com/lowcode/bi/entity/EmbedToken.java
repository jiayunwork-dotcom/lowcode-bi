package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "embed_tokens")
public class EmbedToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "token", columnDefinition = "text", nullable = false)
    private String token;

    @Column(name = "token_type", length = 32, nullable = false)
    private String tokenType = "JWT";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "validity_seconds", nullable = false)
    private Integer validitySeconds;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "hide_title", nullable = false)
    private Boolean hideTitle = false;

    @Column(name = "hide_toolbar", nullable = false)
    private Boolean hideToolbar = true;

    @Column(name = "hide_filters", nullable = false)
    private Boolean hideFilters = true;

    @Column(name = "hide_tabs", nullable = false)
    private Boolean hideTabs = false;

    @Column(name = "enable_fullscreen", nullable = false)
    private Boolean enableFullscreen = false;

    @Column(name = "enable_export", nullable = false)
    private Boolean enableExport = false;

    @Column(name = "enable_drilldown", nullable = false)
    private Boolean enableDrilldown = false;

    @Column(name = "enable_filter_interaction", nullable = false)
    private Boolean enableFilterInteraction = false;

    @Column(name = "default_filters", columnDefinition = "json")
    private String defaultFilters;

    @Column(name = "default_tab_id", length = 64)
    private String defaultTabId;

    @Column(name = "row_permission_rules", columnDefinition = "json")
    private String rowPermissionRules;

    @Column(name = "allowed_domains", columnDefinition = "json")
    private String allowedDomains;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    private Integer currentUses = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "last_used_ip", length = 64)
    private String lastUsedIp;

    @Column(name = "theme", length = 32)
    private String theme = "light";

    @Column(name = "locale", length = 16)
    private String locale = "zh-CN";

    @Column(name = "iframe_width", length = 16)
    private String iframeWidth = "100%";

    @Column(name = "iframe_height", length = 16)
    private String iframeHeight = "600px";

    @Column(name = "border_style", length = 128)
    private String borderStyle;

    @Column(name = "custom_css", columnDefinition = "text")
    private String customCss;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (this.currentUses == null) {
            this.currentUses = 0;
        }
    }
}
