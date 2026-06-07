package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.enums.AggregationType;
import com.lowcode.bi.common.enums.ChartType;
import com.lowcode.bi.common.enums.RefreshInterval;
import com.lowcode.bi.common.enums.RoleType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.query.SqlGenerator;
import com.lowcode.bi.query.SqlQueryEngine;
import com.lowcode.bi.repository.*;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.DashboardService;
import com.lowcode.bi.service.DataModelService;
import com.lowcode.bi.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DashboardTabRepository dashboardTabRepository;

    @Autowired
    private DashboardComponentRepository dashboardComponentRepository;

    @Autowired
    private DashboardPermissionRepository dashboardPermissionRepository;

    @Autowired
    private DataModelRepository dataModelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SqlGenerator sqlGenerator;

    @Autowired
    private SqlQueryEngine sqlQueryEngine;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID getCurrentTenantId() {
        return TenantContext.getCurrentTenantId();
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    @Override
    @Transactional
    public Dashboard createDashboard(String name, String description) {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();

        permissionService.checkEditorRole(userId);

        if (dashboardRepository.existsByNameAndTenantIdAndDeletedFalse(name, tenantId)) {
            throw new BusinessException("仪表板名称已存在");
        }

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Dashboard dashboard = new Dashboard();
        dashboard.setTenant(tenant);
        dashboard.setName(name);
        dashboard.setDescription(description);
        dashboard.setStatus("DRAFT");
        dashboard.setTheme("light");
        dashboard.setRefreshInterval(RefreshInterval.OFF);
        dashboard.setIsPublished(false);
        dashboard.setEnableDrillDown(true);
        dashboard.setEnableLinkage(true);
        dashboard.setShowTitle(true);
        dashboard.setShowFilters(true);
        dashboard.setShowToolbar(true);

        dashboard = dashboardRepository.save(dashboard);

        User user = new User();
        user.setId(userId);
        DashboardPermission permission = new DashboardPermission();
        permission.setDashboard(dashboard);
        permission.setUser(user);
        permission.setCanView(true);
        permission.setCanEdit(true);
        permission.setCanDelete(true);
        permission.setCanShare(true);
        permission.setCanExport(true);
        permission.setIsOwner(true);
        dashboardPermissionRepository.save(permission);

        DashboardTab tab = new DashboardTab();
        tab.setDashboard(dashboard);
        tab.setName("Tab 1");
        tab.setPosition(1);
        tab.setIsActive(true);
        dashboardTabRepository.save(tab);

        return dashboard;
    }

    @Override
    public Dashboard getDashboard(UUID id) {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();

        if (!permissionService.canViewDashboard(id, userId)) {
            throw new BusinessException("没有权限查看该仪表板");
        }

        return dashboardRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));
    }

    @Override
    public Dashboard getDashboardWithDetails(UUID id) {
        Dashboard dashboard = getDashboard(id);
        dashboard.getTabs().size();
        dashboard.getComponents().size();
        dashboard.getPermissions().size();
        return dashboard;
    }

    @Override
    public List<Dashboard> listDashboards() {
        UUID userId = getCurrentUserId();
        List<UUID> viewableIds = permissionService.getViewableDashboardIds(userId);
        if (viewableIds.isEmpty()) {
            return Collections.emptyList();
        }
        return dashboardRepository.findAllById(viewableIds).stream()
                .filter(d -> !d.getDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public List<Dashboard> listPublishedDashboards() {
        UUID tenantId = getCurrentTenantId();
        UUID userId = getCurrentUserId();
        List<Dashboard> allPublished = dashboardRepository.findPublishedByTenantId(tenantId);
        return allPublished.stream()
                .filter(d -> permissionService.canViewDashboard(d.getId(), userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Dashboard updateDashboard(UUID id, String name, String description, String theme,
                                   String refreshInterval, Boolean showTitle, Boolean showFilters,
                                   Boolean showToolbar, Boolean enableDrillDown, Boolean enableLinkage,
                                   String globalFilters) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        Dashboard dashboard = dashboardRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));

        if (name != null && !name.equals(dashboard.getName())) {
            if (dashboardRepository.existsByNameAndTenantIdAndDeletedFalse(name, tenantId)) {
                throw new BusinessException("仪表板名称已存在");
            }
            dashboard.setName(name);
        }

        if (description != null) dashboard.setDescription(description);
        if (theme != null) dashboard.setTheme(theme);
        if (refreshInterval != null) dashboard.setRefreshInterval(RefreshInterval.valueOf(refreshInterval));
        if (showTitle != null) dashboard.setShowTitle(showTitle);
        if (showFilters != null) dashboard.setShowFilters(showFilters);
        if (showToolbar != null) dashboard.setShowToolbar(showToolbar);
        if (enableDrillDown != null) dashboard.setEnableDrillDown(enableDrillDown);
        if (enableLinkage != null) dashboard.setEnableLinkage(enableLinkage);
        if (globalFilters != null) dashboard.setGlobalFilters(globalFilters);

        return dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public void deleteDashboard(UUID id) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canDeleteDashboard(id, userId)) {
            throw new BusinessException("没有权限删除该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setDeleted(true);
        dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public Dashboard publishDashboard(UUID id) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限发布该仪表板");
        }
        Dashboard dashboard = getDashboard(id);

        if (CollectionUtils.isEmpty(dashboard.getTabs())) {
            throw new BusinessException("仪表板至少需要一个Tab页");
        }

        dashboard.setIsPublished(true);
        dashboard.setStatus("PUBLISHED");
        dashboard.setPublishedAt(LocalDateTime.now());
        dashboard.setPublishedBy(TenantContext.getCurrentUsername());

        return dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public Dashboard unpublishDashboard(UUID id) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限取消发布该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setIsPublished(false);
        dashboard.setStatus("DRAFT");
        return dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public Dashboard copyDashboard(UUID id, String newName) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        if (!permissionService.canViewDashboard(id, userId)) {
            throw new BusinessException("没有权限查看该仪表板");
        }

        if (dashboardRepository.existsByNameAndTenantIdAndDeletedFalse(newName, tenantId)) {
            throw new BusinessException("仪表板名称已存在");
        }

        Dashboard source = getDashboardWithDetails(id);

        Dashboard copy = new Dashboard();
        copy.setTenant(source.getTenant());
        copy.setName(newName);
        copy.setDescription(source.getDescription() + " (副本)");
        copy.setStatus("DRAFT");
        copy.setTheme(source.getTheme());
        copy.setRefreshInterval(source.getRefreshInterval());
        copy.setIsTemplate(false);
        copy.setEnableDrillDown(source.getEnableDrillDown());
        copy.setEnableLinkage(source.getEnableLinkage());
        copy.setShowTitle(source.getShowTitle());
        copy.setShowFilters(source.getShowFilters());
        copy.setShowToolbar(source.getShowToolbar());
        copy.setGlobalFilters(source.getGlobalFilters());
        copy = dashboardRepository.save(copy);

        User user = new User();
        user.setId(userId);
        DashboardPermission permission = new DashboardPermission();
        permission.setDashboard(copy);
        permission.setUser(user);
        permission.setCanView(true);
        permission.setCanEdit(true);
        permission.setCanDelete(true);
        permission.setCanShare(true);
        permission.setCanExport(true);
        permission.setIsOwner(true);
        dashboardPermissionRepository.save(permission);

        Map<UUID, DashboardTab> tabMap = new HashMap<>();
        for (DashboardTab tab : source.getTabs()) {
            DashboardTab tabCopy = new DashboardTab();
            tabCopy.setDashboard(copy);
            tabCopy.setName(tab.getName());
            tabCopy.setDescription(tab.getDescription());
            tabCopy.setPosition(tab.getPosition());
            tabCopy.setIsActive(tab.getIsActive());
            tabCopy.setLayoutConfig(tab.getLayoutConfig());
            tabCopy.setGridColumns(tab.getGridColumns());
            tabCopy.setGridRowHeight(tab.getGridRowHeight());
            tabCopy.setGridGutter(tab.getGridGutter());
            tabCopy.setBackgroundColor(tab.getBackgroundColor());
            tabCopy.setBackgroundImage(tab.getBackgroundImage());
            tabCopy.setTabFilters(tab.getTabFilters());
            tabCopy = dashboardTabRepository.save(tabCopy);
            tabMap.put(tab.getId(), tabCopy);
        }

        for (DashboardComponent component : source.getComponents()) {
            DashboardComponent compCopy = new DashboardComponent();
            compCopy.setDashboard(copy);
            compCopy.setTab(tabMap.get(component.getTab().getId()));
            compCopy.setDataModel(component.getDataModel());
            compCopy.setName(component.getName());
            compCopy.setTitle(component.getTitle());
            compCopy.setDescription(component.getDescription());
            compCopy.setChartType(component.getChartType());
            compCopy.setPositionX(component.getPositionX());
            compCopy.setPositionY(component.getPositionY());
            compCopy.setWidth(component.getWidth());
            compCopy.setHeight(component.getHeight());
            compCopy.setZIndex(component.getZIndex());
            compCopy.setDimensions(component.getDimensions());
            compCopy.setMeasures(component.getMeasures());
            compCopy.setFilters(component.getFilters());
            compCopy.setSorts(component.getSorts());
            compCopy.setChartConfig(component.getChartConfig());
            compCopy.setStyleConfig(component.getStyleConfig());
            compCopy.setCustomSql(component.getCustomSql());
            compCopy.setSqlParams(component.getSqlParams());
            compCopy.setEnableDrillDown(component.getEnableDrillDown());
            compCopy.setEnableLinkage(component.getEnableLinkage());
            compCopy.setTimeComparison(component.getTimeComparison());
            dashboardComponentRepository.save(compCopy);
        }

        return copy;
    }

    @Override
    @Transactional
    public Dashboard setAsTemplate(UUID id, String category) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限设置模板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setIsTemplate(true);
        dashboard.setTemplateCategory(category);
        return dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public Dashboard removeFromTemplate(UUID id) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限取消模板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setIsTemplate(false);
        dashboard.setTemplateCategory(null);
        return dashboardRepository.save(dashboard);
    }

    @Override
    public List<Dashboard> listTemplates() {
        return dashboardRepository.findAllTemplates();
    }

    @Override
    @Transactional
    public Dashboard createFromTemplate(UUID templateId, String newName) {
        return copyDashboard(templateId, newName);
    }

    @Override
    @Transactional
    public DashboardTab addTab(UUID dashboardId, String name, String description) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(dashboardId, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        Dashboard dashboard = getDashboard(dashboardId);

        if (dashboardTabRepository.existsByDashboardIdAndNameAndDeletedFalse(dashboardId, name)) {
            throw new BusinessException("Tab名称已存在");
        }

        List<DashboardTab> tabs = dashboardTabRepository.findByDashboardId(dashboardId);

        DashboardTab tab = new DashboardTab();
        tab.setDashboard(dashboard);
        tab.setName(name);
        tab.setDescription(description);
        tab.setPosition(tabs.size() + 1);
        tab.setIsActive(tabs.isEmpty());

        return dashboardTabRepository.save(tab);
    }

    @Override
    @Transactional
    public DashboardTab updateTab(UUID tabId, String name, String description, Integer position,
                                String layoutConfig, Boolean isActive) {
        DashboardTab tab = dashboardTabRepository.findById(tabId)
                .orElseThrow(() -> new BusinessException("Tab不存在"));

        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(tab.getDashboard().getId(), userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        if (name != null && !name.equals(tab.getName())) {
            if (dashboardTabRepository.existsByDashboardIdAndNameAndDeletedFalse(
                    tab.getDashboard().getId(), name)) {
                throw new BusinessException("Tab名称已存在");
            }
            tab.setName(name);
        }

        if (description != null) tab.setDescription(description);
        if (position != null) tab.setPosition(position);
        if (layoutConfig != null) tab.setLayoutConfig(layoutConfig);

        if (Boolean.TRUE.equals(isActive)) {
            List<DashboardTab> tabs = dashboardTabRepository.findByDashboardId(tab.getDashboard().getId());
            for (DashboardTab t : tabs) {
                t.setIsActive(false);
                dashboardTabRepository.save(t);
            }
            tab.setIsActive(true);
        }

        return dashboardTabRepository.save(tab);
    }

    @Override
    @Transactional
    public void removeTab(UUID tabId) {
        DashboardTab tab = dashboardTabRepository.findById(tabId)
                .orElseThrow(() -> new BusinessException("Tab不存在"));

        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(tab.getDashboard().getId(), userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        List<DashboardComponent> components = dashboardComponentRepository.findByTabId(tabId);
        for (DashboardComponent component : components) {
            component.setDeleted(true);
            dashboardComponentRepository.save(component);
        }

        tab.setDeleted(true);
        dashboardTabRepository.save(tab);
    }

    @Override
    @Transactional
    public DashboardComponent addComponent(UUID dashboardId, UUID tabId, UUID dataModelId,
                                         String name, String title, String chartType,
                                         Integer positionX, Integer positionY, Integer width, Integer height,
                                         String dimensions, String measures, String filters,
                                         String chartConfig, String styleConfig) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(dashboardId, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        Dashboard dashboard = getDashboard(dashboardId);
        DashboardTab tab = dashboardTabRepository.findById(tabId)
                .orElseThrow(() -> new BusinessException("Tab不存在"));

        DataModel dataModel = null;
        if (dataModelId != null) {
            dataModel = dataModelService.getModel(dataModelId);
        }

        DashboardComponent component = new DashboardComponent();
        component.setDashboard(dashboard);
        component.setTab(tab);
        component.setDataModel(dataModel);
        component.setName(name);
        component.setTitle(title != null ? title : name);
        component.setChartType(ChartType.valueOf(chartType));
        component.setPositionX(positionX != null ? positionX : 0);
        component.setPositionY(positionY != null ? positionY : 0);
        component.setWidth(width != null ? width : 6);
        component.setHeight(height != null ? height : 4);
        component.setZIndex(1);
        component.setDimensions(dimensions);
        component.setMeasures(measures);
        component.setFilters(filters);
        component.setChartConfig(chartConfig);
        component.setStyleConfig(styleConfig);
        component.setEnableDrillDown(dashboard.getEnableDrillDown());
        component.setEnableLinkage(dashboard.getEnableLinkage());

        return dashboardComponentRepository.save(component);
    }

    @Override
    @Transactional
    public DashboardComponent updateComponent(UUID componentId, String name, String title,
                                            Integer positionX, Integer positionY, Integer width, Integer height,
                                            String dimensions, String measures, String filters,
                                            String chartConfig, String styleConfig, Boolean isLocked, Boolean isHidden) {
        DashboardComponent component = dashboardComponentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException("组件不存在"));

        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(component.getDashboard().getId(), userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        if (name != null) component.setName(name);
        if (title != null) component.setTitle(title);
        if (positionX != null) component.setPositionX(positionX);
        if (positionY != null) component.setPositionY(positionY);
        if (width != null) component.setWidth(width);
        if (height != null) component.setHeight(height);
        if (dimensions != null) component.setDimensions(dimensions);
        if (measures != null) component.setMeasures(measures);
        if (filters != null) component.setFilters(filters);
        if (chartConfig != null) component.setChartConfig(chartConfig);
        if (styleConfig != null) component.setStyleConfig(styleConfig);
        if (isLocked != null) component.setIsLocked(isLocked);
        if (isHidden != null) component.setIsHidden(isHidden);

        return dashboardComponentRepository.save(component);
    }

    @Override
    @Transactional
    public void removeComponent(UUID componentId) {
        DashboardComponent component = dashboardComponentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException("组件不存在"));

        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(component.getDashboard().getId(), userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        component.setDeleted(true);
        dashboardComponentRepository.save(component);
    }

    @Override
    @Transactional
    public DashboardComponent updateComponentPosition(UUID componentId, Integer positionX, Integer positionY,
                                                    Integer width, Integer height) {
        DashboardComponent component = dashboardComponentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException("组件不存在"));

        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(component.getDashboard().getId(), userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }

        if (positionX != null) component.setPositionX(positionX);
        if (positionY != null) component.setPositionY(positionY);
        if (width != null) component.setWidth(width);
        if (height != null) component.setHeight(height);

        return dashboardComponentRepository.save(component);
    }

    @Override
    public Map<String, Object> executeComponentQuery(UUID componentId, Map<String, Object> params) {
        DashboardComponent component = dashboardComponentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException("组件不存在"));

        UUID userId = getCurrentUserId();
        if (!permissionService.canViewDashboard(component.getDashboard().getId(), userId)) {
            throw new BusinessException("没有权限查看该仪表板");
        }

        try {
            SqlGenerator.QueryRequest request = buildQueryRequest(component, params);
            return dataModelService.executeQuery(component.getDataModel().getId(), request);
        } catch (Exception e) {
            throw new BusinessException("组件查询执行失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Dashboard setTheme(UUID id, String theme) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setTheme(theme);
        return dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public Dashboard setRefreshInterval(UUID id, String refreshInterval) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setRefreshInterval(RefreshInterval.valueOf(refreshInterval));
        return dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public void pauseRefresh(UUID id) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setRefreshPaused(true);
        dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public void resumeRefresh(UUID id) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setRefreshPaused(false);
        dashboard.setConsecutiveFailures(0);
        dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public void recordRefreshResult(UUID id, boolean success) {
        Dashboard dashboard = getDashboard(id);
        dashboard.setLastRefreshAt(LocalDateTime.now());
        dashboard.setLastRefreshSuccess(success);

        if (success) {
            dashboard.setConsecutiveFailures(0);
        } else {
            dashboard.setConsecutiveFailures(dashboard.getConsecutiveFailures() + 1);
            if (dashboard.getConsecutiveFailures() >= 3) {
                dashboard.setRefreshPaused(true);
                User user = userRepository.findById(getCurrentUserId()).orElse(null);
                if (user != null && user.getRole() == RoleType.TENANT_ADMIN) {
                    // TODO: 发送通知给管理员
                }
            }
        }

        dashboardRepository.save(dashboard);
    }

    @Override
    public List<Dashboard> getDashboardsNeedingRefresh() {
        return dashboardRepository.findDashboardsNeedingRefresh();
    }

    @Override
    @Transactional
    public Dashboard updateGlobalFilters(UUID id, String filters) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canEditDashboard(id, userId)) {
            throw new BusinessException("没有权限编辑该仪表板");
        }
        Dashboard dashboard = getDashboard(id);
        dashboard.setGlobalFilters(filters);
        return dashboardRepository.save(dashboard);
    }

    @Override
    public Map<String, Object> getDashboardData(UUID id, Map<String, Object> filterParams) {
        UUID userId = getCurrentUserId();
        if (!permissionService.canViewDashboard(id, userId)) {
            throw new BusinessException("没有权限查看该仪表板");
        }

        Dashboard dashboard = getDashboardWithDetails(id);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> componentsData = new ArrayList<>();
        for (DashboardComponent component : dashboard.getComponents()) {
            if (Boolean.TRUE.equals(component.getIsHidden())) {
                continue;
            }
            try {
                Map<String, Object> compData = new HashMap<>();
                compData.put("componentId", component.getId());
                compData.put("name", component.getName());
                compData.put("chartType", component.getChartType());

                SqlGenerator.QueryRequest request = buildQueryRequest(component, filterParams);
                Map<String, Object> queryResult = dataModelService.executeQuery(
                        component.getDataModel().getId(), request);
                compData.put("data", queryResult);
                componentsData.add(compData);
            } catch (Exception e) {
                Map<String, Object> compData = new HashMap<>();
                compData.put("componentId", component.getId());
                compData.put("name", component.getName());
                compData.put("chartType", component.getChartType());
                compData.put("error", e.getMessage());
                componentsData.add(compData);
            }
        }

        result.put("dashboardId", dashboard.getId());
        result.put("name", dashboard.getName());
        result.put("theme", dashboard.getTheme());
        result.put("components", componentsData);
        result.put("lastUpdated", LocalDateTime.now().toString());

        return result;
    }

    private SqlGenerator.QueryRequest buildQueryRequest(DashboardComponent component, Map<String, Object> params) {
        SqlGenerator.QueryRequest request = new SqlGenerator.QueryRequest();

        try {
            if (component.getDimensions() != null) {
                List<Map<String, Object>> dims = objectMapper.readValue(
                        component.getDimensions(),
                        new TypeReference<List<Map<String, Object>>>() {});
                List<SqlGenerator.DimensionField> dimensions = new ArrayList<>();
                for (Map<String, Object> dim : dims) {
                    SqlGenerator.DimensionField df = new SqlGenerator.DimensionField();
                    df.setTableAlias((String) dim.get("tableAlias"));
                    df.setColumnName((String) dim.get("columnName"));
                    df.setAlias((String) dim.get("alias"));
                    df.setFunction((String) dim.get("function"));
                    df.setDateFormat((String) dim.get("dateFormat"));
                    dimensions.add(df);
                }
                request.setDimensions(dimensions);
            }

            if (component.getMeasures() != null) {
                List<Map<String, Object>> meas = objectMapper.readValue(
                        component.getMeasures(),
                        new TypeReference<List<Map<String, Object>>>() {});
                List<SqlGenerator.MeasureField> measures = new ArrayList<>();
                for (Map<String, Object> m : meas) {
                    SqlGenerator.MeasureField mf = new SqlGenerator.MeasureField();
                    mf.setTableAlias((String) m.get("tableAlias"));
                    mf.setColumnName((String) m.get("columnName"));
                    mf.setAlias((String) m.get("alias"));
                    mf.setAggregationType(AggregationType.valueOf((String) m.get("aggregationType")));
                    mf.setDistinct((Boolean) m.get("distinct"));
                    mf.setFilterCondition((String) m.get("filterCondition"));
                    mf.setExpression((String) m.get("expression"));
                    measures.add(mf);
                }
                request.setMeasures(measures);
            }

            if (component.getFilters() != null) {
                List<Map<String, Object>> fils = objectMapper.readValue(
                        component.getFilters(),
                        new TypeReference<List<Map<String, Object>>>() {});
                List<SqlGenerator.FilterCondition> filters = new ArrayList<>();
                for (Map<String, Object> f : fils) {
                    SqlGenerator.FilterCondition fc = new SqlGenerator.FilterCondition();
                    fc.setTableAlias((String) f.get("tableAlias"));
                    fc.setColumnName((String) f.get("columnName"));
                    fc.setOperator((String) f.get("operator"));
                    fc.setValue(f.get("value"));
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) f.get("values");
                    fc.setValues(values);
                    fc.setLogic((String) f.get("logic"));
                    filters.add(fc);
                }
                request.setFilters(filters);
            }

            if (component.getSorts() != null) {
                List<Map<String, Object>> sors = objectMapper.readValue(
                        component.getSorts(),
                        new TypeReference<List<Map<String, Object>>>() {});
                List<SqlGenerator.SortField> sorts = new ArrayList<>();
                for (Map<String, Object> s : sors) {
                    SqlGenerator.SortField sf = new SqlGenerator.SortField();
                    sf.setField((String) s.get("field"));
                    sf.setDirection((String) s.get("direction"));
                    sorts.add(sf);
                }
                request.setSorts(sorts);
            }
        } catch (Exception e) {
            throw new BusinessException("解析组件配置失败: " + e.getMessage());
        }

        request.setLimit(component.getMaxRows() != null ? component.getMaxRows() : 5000);

        return request;
    }
}
