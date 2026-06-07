package com.lowcode.bi.controller;

import com.lowcode.bi.entity.Dashboard;
import com.lowcode.bi.entity.DashboardComponent;
import com.lowcode.bi.entity.DashboardTab;
import com.lowcode.bi.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @PostMapping
    public ResponseEntity<Dashboard> createDashboard(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        Dashboard dashboard = dashboardService.createDashboard(name, description);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dashboard> getDashboard(@PathVariable UUID id) {
        Dashboard dashboard = dashboardService.getDashboard(id);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<Dashboard> getDashboardWithDetails(@PathVariable UUID id) {
        Dashboard dashboard = dashboardService.getDashboardWithDetails(id);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping
    public ResponseEntity<List<Dashboard>> listDashboards() {
        List<Dashboard> dashboards = dashboardService.listDashboards();
        return ResponseEntity.ok(dashboards);
    }

    @GetMapping("/published")
    public ResponseEntity<List<Dashboard>> listPublishedDashboards() {
        List<Dashboard> dashboards = dashboardService.listPublishedDashboards();
        return ResponseEntity.ok(dashboards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dashboard> updateDashboard(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String theme = (String) request.get("theme");
        String refreshInterval = (String) request.get("refreshInterval");
        Boolean showTitle = (Boolean) request.get("showTitle");
        Boolean showFilters = (Boolean) request.get("showFilters");
        Boolean showToolbar = (Boolean) request.get("showToolbar");
        Boolean enableDrillDown = (Boolean) request.get("enableDrillDown");
        Boolean enableLinkage = (Boolean) request.get("enableLinkage");
        String globalFilters = (String) request.get("globalFilters");

        Dashboard dashboard = dashboardService.updateDashboard(id, name, description, theme,
                refreshInterval, showTitle, showFilters, showToolbar,
                enableDrillDown, enableLinkage, globalFilters);
        return ResponseEntity.ok(dashboard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDashboard(@PathVariable UUID id) {
        dashboardService.deleteDashboard(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Dashboard> publishDashboard(@PathVariable UUID id) {
        Dashboard dashboard = dashboardService.publishDashboard(id);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Dashboard> unpublishDashboard(@PathVariable UUID id) {
        Dashboard dashboard = dashboardService.unpublishDashboard(id);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<Dashboard> copyDashboard(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String newName = request.get("newName");
        Dashboard dashboard = dashboardService.copyDashboard(id, newName);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/{id}/template")
    public ResponseEntity<Dashboard> setAsTemplate(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String category = request.get("category");
        Dashboard dashboard = dashboardService.setAsTemplate(id, category);
        return ResponseEntity.ok(dashboard);
    }

    @DeleteMapping("/{id}/template")
    public ResponseEntity<Dashboard> removeFromTemplate(@PathVariable UUID id) {
        Dashboard dashboard = dashboardService.removeFromTemplate(id);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<Dashboard>> listTemplates() {
        List<Dashboard> templates = dashboardService.listTemplates();
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates/{templateId}/create")
    public ResponseEntity<Dashboard> createFromTemplate(
            @PathVariable UUID templateId,
            @RequestBody Map<String, String> request) {
        String newName = request.get("newName");
        Dashboard dashboard = dashboardService.createFromTemplate(templateId, newName);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/{id}/tabs")
    public ResponseEntity<DashboardTab> addTab(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        DashboardTab tab = dashboardService.addTab(id, name, description);
        return ResponseEntity.ok(tab);
    }

    @PutMapping("/tabs/{tabId}")
    public ResponseEntity<DashboardTab> updateTab(
            @PathVariable UUID tabId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Integer position = request.get("position") != null ? ((Number) request.get("position")).intValue() : null;
        String layoutConfig = (String) request.get("layoutConfig");
        Boolean isActive = (Boolean) request.get("isActive");

        DashboardTab tab = dashboardService.updateTab(tabId, name, description, position, layoutConfig, isActive);
        return ResponseEntity.ok(tab);
    }

    @DeleteMapping("/tabs/{tabId}")
    public ResponseEntity<Map<String, String>> removeTab(@PathVariable UUID tabId) {
        dashboardService.removeTab(tabId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/components")
    public ResponseEntity<DashboardComponent> addComponent(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID tabId = UUID.fromString((String) request.get("tabId"));
        UUID dataModelId = request.get("dataModelId") != null ? UUID.fromString((String) request.get("dataModelId")) : null;
        String name = (String) request.get("name");
        String title = (String) request.get("title");
        String chartType = (String) request.get("chartType");
        Integer positionX = request.get("positionX") != null ? ((Number) request.get("positionX")).intValue() : null;
        Integer positionY = request.get("positionY") != null ? ((Number) request.get("positionY")).intValue() : null;
        Integer width = request.get("width") != null ? ((Number) request.get("width")).intValue() : null;
        Integer height = request.get("height") != null ? ((Number) request.get("height")).intValue() : null;
        String dimensions = request.get("dimensions") != null ? request.get("dimensions").toString() : null;
        String measures = request.get("measures") != null ? request.get("measures").toString() : null;
        String filters = request.get("filters") != null ? request.get("filters").toString() : null;
        String chartConfig = request.get("chartConfig") != null ? request.get("chartConfig").toString() : null;
        String styleConfig = request.get("styleConfig") != null ? request.get("styleConfig").toString() : null;

        DashboardComponent component = dashboardService.addComponent(id, tabId, dataModelId,
                name, title, chartType, positionX, positionY, width, height,
                dimensions, measures, filters, chartConfig, styleConfig);
        return ResponseEntity.ok(component);
    }

    @PutMapping("/components/{componentId}")
    public ResponseEntity<DashboardComponent> updateComponent(
            @PathVariable UUID componentId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String title = (String) request.get("title");
        Integer positionX = request.get("positionX") != null ? ((Number) request.get("positionX")).intValue() : null;
        Integer positionY = request.get("positionY") != null ? ((Number) request.get("positionY")).intValue() : null;
        Integer width = request.get("width") != null ? ((Number) request.get("width")).intValue() : null;
        Integer height = request.get("height") != null ? ((Number) request.get("height")).intValue() : null;
        String dimensions = request.get("dimensions") != null ? request.get("dimensions").toString() : null;
        String measures = request.get("measures") != null ? request.get("measures").toString() : null;
        String filters = request.get("filters") != null ? request.get("filters").toString() : null;
        String chartConfig = request.get("chartConfig") != null ? request.get("chartConfig").toString() : null;
        String styleConfig = request.get("styleConfig") != null ? request.get("styleConfig").toString() : null;
        Boolean isLocked = (Boolean) request.get("isLocked");
        Boolean isHidden = (Boolean) request.get("isHidden");

        DashboardComponent component = dashboardService.updateComponent(componentId, name, title,
                positionX, positionY, width, height,
                dimensions, measures, filters, chartConfig, styleConfig, isLocked, isHidden);
        return ResponseEntity.ok(component);
    }

    @DeleteMapping("/components/{componentId}")
    public ResponseEntity<Map<String, String>> removeComponent(@PathVariable UUID componentId) {
        dashboardService.removeComponent(componentId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/components/{componentId}/position")
    public ResponseEntity<DashboardComponent> updateComponentPosition(
            @PathVariable UUID componentId,
            @RequestBody Map<String, Object> request) {
        Integer positionX = request.get("positionX") != null ? ((Number) request.get("positionX")).intValue() : null;
        Integer positionY = request.get("positionY") != null ? ((Number) request.get("positionY")).intValue() : null;
        Integer width = request.get("width") != null ? ((Number) request.get("width")).intValue() : null;
        Integer height = request.get("height") != null ? ((Number) request.get("height")).intValue() : null;

        DashboardComponent component = dashboardService.updateComponentPosition(
                componentId, positionX, positionY, width, height);
        return ResponseEntity.ok(component);
    }

    @PostMapping("/components/{componentId}/query")
    public ResponseEntity<Map<String, Object>> executeComponentQuery(
            @PathVariable UUID componentId,
            @RequestBody(required = false) Map<String, Object> params) {
        Map<String, Object> result = dashboardService.executeComponentQuery(
                componentId, params != null ? params : new HashMap<>());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/theme")
    public ResponseEntity<Dashboard> setTheme(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String theme = request.get("theme");
        Dashboard dashboard = dashboardService.setTheme(id, theme);
        return ResponseEntity.ok(dashboard);
    }

    @PutMapping("/{id}/refresh-interval")
    public ResponseEntity<Dashboard> setRefreshInterval(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String refreshInterval = request.get("refreshInterval");
        Dashboard dashboard = dashboardService.setRefreshInterval(id, refreshInterval);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/{id}/pause-refresh")
    public ResponseEntity<Map<String, String>> pauseRefresh(@PathVariable UUID id) {
        dashboardService.pauseRefresh(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "已暂停自动刷新");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resume-refresh")
    public ResponseEntity<Map<String, String>> resumeRefresh(@PathVariable UUID id) {
        dashboardService.resumeRefresh(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "已恢复自动刷新");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/global-filters")
    public ResponseEntity<Dashboard> updateGlobalFilters(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String filters = request.get("filters");
        Dashboard dashboard = dashboardService.updateGlobalFilters(id, filters);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/{id}/data")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> filterParams) {
        Map<String, Object> result = dashboardService.getDashboardData(
                id, filterParams != null ? filterParams : new HashMap<>());
        return ResponseEntity.ok(result);
    }
}
