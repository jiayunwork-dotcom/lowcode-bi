package com.lowcode.bi.controller;

import com.lowcode.bi.entity.DashboardPermission;
import com.lowcode.bi.entity.User;
import com.lowcode.bi.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @PostMapping("/dashboard/{dashboardId}/grant")
    public ResponseEntity<DashboardPermission> grantPermission(
            @PathVariable UUID dashboardId,
            @RequestBody Map<String, Object> request) {
        UUID userId = UUID.fromString((String) request.get("userId"));
        Boolean canView = (Boolean) request.get("canView");
        Boolean canEdit = (Boolean) request.get("canEdit");
        Boolean canDelete = (Boolean) request.get("canDelete");
        Boolean canShare = (Boolean) request.get("canShare");
        Boolean canExport = (Boolean) request.get("canExport");
        Boolean isOwner = (Boolean) request.get("isOwner");

        DashboardPermission permission = permissionService.grantPermission(
                dashboardId, userId, canView, canEdit, canDelete, canShare, canExport, isOwner);
        return ResponseEntity.ok(permission);
    }

    @PutMapping("/{permissionId}")
    public ResponseEntity<DashboardPermission> updatePermission(
            @PathVariable UUID permissionId,
            @RequestBody Map<String, Object> request) {
        Boolean canView = (Boolean) request.get("canView");
        Boolean canEdit = (Boolean) request.get("canEdit");
        Boolean canDelete = (Boolean) request.get("canDelete");
        Boolean canShare = (Boolean) request.get("canShare");
        Boolean canExport = (Boolean) request.get("canExport");

        DashboardPermission permission = permissionService.updatePermission(
                permissionId, canView, canEdit, canDelete, canShare, canExport);
        return ResponseEntity.ok(permission);
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Map<String, String>> revokePermission(@PathVariable UUID permissionId) {
        permissionService.revokePermission(permissionId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "权限撤销成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/{dashboardId}")
    public ResponseEntity<List<DashboardPermission>> getDashboardPermissions(@PathVariable UUID dashboardId) {
        List<DashboardPermission> permissions = permissionService.getDashboardPermissions(dashboardId);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DashboardPermission>> getUserPermissions(@PathVariable UUID userId) {
        List<DashboardPermission> permissions = permissionService.getUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/dashboard/{dashboardId}/can-view/{userId}")
    public ResponseEntity<Map<String, Boolean>> canViewDashboard(
            @PathVariable UUID dashboardId,
            @PathVariable UUID userId) {
        boolean canView = permissionService.canViewDashboard(dashboardId, userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("canView", canView);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/{dashboardId}/can-edit/{userId}")
    public ResponseEntity<Map<String, Boolean>> canEditDashboard(
            @PathVariable UUID dashboardId,
            @PathVariable UUID userId) {
        boolean canEdit = permissionService.canEditDashboard(dashboardId, userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("canEdit", canEdit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-row-permissions")
    public ResponseEntity<List<Map<String, Object>>> validateRowPermissionRules(
            @RequestBody Map<String, String> request) {
        String rules = request.get("rules");
        List<Map<String, Object>> errors = permissionService.validateRowPermissionRules(rules);
        return ResponseEntity.ok(errors);
    }

    @PutMapping("/user/{userId}/role")
    public ResponseEntity<User> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request) {
        String role = request.get("role");
        User user = permissionService.updateUserRole(userId, role);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/user/{userId}/row-permissions")
    public ResponseEntity<User> updateUserRowPermissions(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request) {
        String rowPermissionFilters = request.get("rowPermissionFilters");
        User user = permissionService.updateUserRowPermissions(userId, rowPermissionFilters);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/user/{userId}/viewable-dashboards")
    public ResponseEntity<List<UUID>> getViewableDashboardIds(@PathVariable UUID userId) {
        List<UUID> ids = permissionService.getViewableDashboardIds(userId);
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/user/{userId}/editable-dashboards")
    public ResponseEntity<List<UUID>> getEditableDashboardIds(@PathVariable UUID userId) {
        List<UUID> ids = permissionService.getEditableDashboardIds(userId);
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/row-filter/{userId}/{modelId}")
    public ResponseEntity<Map<String, String>> getRowPermissionFilter(
            @PathVariable UUID userId,
            @PathVariable UUID modelId) {
        String filter = permissionService.getRowPermissionFilter(userId, modelId);
        Map<String, String> response = new HashMap<>();
        response.put("filter", filter);
        return ResponseEntity.ok(response);
    }
}
