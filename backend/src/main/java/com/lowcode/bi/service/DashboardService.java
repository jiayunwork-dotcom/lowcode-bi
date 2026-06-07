package com.lowcode.bi.service;

import com.lowcode.bi.entity.Dashboard;
import com.lowcode.bi.entity.DashboardComponent;
import com.lowcode.bi.entity.DashboardTab;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {

    Dashboard createDashboard(String name, String description);

    Dashboard getDashboard(UUID id);

    Dashboard getDashboardWithDetails(UUID id);

    List<Dashboard> listDashboards();

    List<Dashboard> listPublishedDashboards();

    Dashboard updateDashboard(UUID id, String name, String description, String theme,
                              String refreshInterval, Boolean showTitle, Boolean showFilters,
                              Boolean showToolbar, Boolean enableDrillDown, Boolean enableLinkage,
                              String globalFilters);

    void deleteDashboard(UUID id);

    Dashboard publishDashboard(UUID id);

    Dashboard unpublishDashboard(UUID id);

    Dashboard copyDashboard(UUID id, String newName);

    Dashboard setAsTemplate(UUID id, String category);

    Dashboard removeFromTemplate(UUID id);

    List<Dashboard> listTemplates();

    Dashboard createFromTemplate(UUID templateId, String newName);

    DashboardTab addTab(UUID dashboardId, String name, String description);

    DashboardTab updateTab(UUID tabId, String name, String description, Integer position,
                           String layoutConfig, Boolean isActive);

    void removeTab(UUID tabId);

    DashboardComponent addComponent(UUID dashboardId, UUID tabId, UUID dataModelId,
                                    String name, String title, String chartType,
                                    Integer positionX, Integer positionY, Integer width, Integer height,
                                    String dimensions, String measures, String filters,
                                    String chartConfig, String styleConfig);

    DashboardComponent updateComponent(UUID componentId, String name, String title,
                                       Integer positionX, Integer positionY, Integer width, Integer height,
                                       String dimensions, String measures, String filters,
                                       String chartConfig, String styleConfig, Boolean isLocked, Boolean isHidden);

    void removeComponent(UUID componentId);

    DashboardComponent updateComponentPosition(UUID componentId, Integer positionX, Integer positionY,
                                               Integer width, Integer height);

    Map<String, Object> executeComponentQuery(UUID componentId, Map<String, Object> params);

    Dashboard setTheme(UUID id, String theme);

    Dashboard setRefreshInterval(UUID id, String refreshInterval);

    void pauseRefresh(UUID id);

    void resumeRefresh(UUID id);

    void recordRefreshResult(UUID id, boolean success);

    List<Dashboard> getDashboardsNeedingRefresh();

    Dashboard updateGlobalFilters(UUID id, String filters);

    Map<String, Object> getDashboardData(UUID id, Map<String, Object> filterParams);
}
