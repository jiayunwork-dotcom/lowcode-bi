package com.lowcode.bi.service;

import com.lowcode.bi.entity.DashboardPermission;
import com.lowcode.bi.entity.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PermissionService {

    boolean canViewDashboard(UUID dashboardId, UUID userId);

    boolean canEditDashboard(UUID dashboardId, UUID userId);

    boolean canDeleteDashboard(UUID dashboardId, UUID userId);

    boolean canShareDashboard(UUID dashboardId, UUID userId);

    boolean canExportDashboard(UUID dashboardId, UUID userId);

    boolean isDashboardOwner(UUID dashboardId, UUID userId);

    DashboardPermission grantPermission(UUID dashboardId, UUID userId,
                                        Boolean canView, Boolean canEdit,
                                        Boolean canDelete, Boolean canShare,
                                        Boolean canExport, Boolean isOwner);

    DashboardPermission updatePermission(UUID permissionId,
                                        Boolean canView, Boolean canEdit,
                                        Boolean canDelete, Boolean canShare,
                                        Boolean canExport);

    void revokePermission(UUID permissionId);

    List<DashboardPermission> getDashboardPermissions(UUID dashboardId);

    List<DashboardPermission> getUserPermissions(UUID userId);

    String getRowPermissionFilter(UUID userId, UUID dataModelId);

    List<Map<String, Object>> validateRowPermissionRules(String rules);

    User updateUserRole(UUID userId, String role);

    User updateUserRowPermissions(UUID userId, String rowPermissionFilters);

    List<UUID> getViewableDashboardIds(UUID userId);

    List<UUID> getEditableDashboardIds(UUID userId);

    void checkTenantAdmin(UUID userId);

    void checkEditorRole(UUID userId);

    void ensureTenantIsolation(UUID entityTenantId);
}
