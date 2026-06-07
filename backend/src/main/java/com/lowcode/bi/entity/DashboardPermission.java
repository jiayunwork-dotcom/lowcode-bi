package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dashboard_permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_dashboard_user", columnNames = {"dashboard_id", "user_id"})
})
public class DashboardPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "can_view", nullable = false)
    private Boolean canView = true;

    @Column(name = "can_edit", nullable = false)
    private Boolean canEdit = false;

    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = false;

    @Column(name = "can_share", nullable = false)
    private Boolean canShare = false;

    @Column(name = "can_export", nullable = false)
    private Boolean canExport = false;

    @Column(name = "row_permission_override", columnDefinition = "json")
    private String rowPermissionOverride;

    @Column(name = "expire_at")
    private java.time.LocalDateTime expireAt;

    @Column(name = "is_owner", nullable = false)
    private Boolean isOwner = false;
}
