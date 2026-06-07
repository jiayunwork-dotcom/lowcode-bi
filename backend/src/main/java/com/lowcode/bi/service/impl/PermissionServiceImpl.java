package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.enums.RoleType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.expression.ExpressionParser;
import com.lowcode.bi.repository.*;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private DashboardPermissionRepository dashboardPermissionRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataModelRepository dataModelRepository;

    @Autowired
    private ModelTableRepository modelTableRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Autowired
    private ExpressionParser expressionParser;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID getCurrentTenantId() {
        return TenantContext.getCurrentTenantId();
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    @Override
    public boolean canViewDashboard(UUID dashboardId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return true;
        }

        Optional<DashboardPermission> permission = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        return permission.map(dp -> Boolean.TRUE.equals(dp.getCanView())).orElse(false);
    }

    @Override
    public boolean canEditDashboard(UUID dashboardId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return true;
        }

        Optional<DashboardPermission> permission = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        return permission.map(dp -> Boolean.TRUE.equals(dp.getCanEdit())).orElse(false);
    }

    @Override
    public boolean canDeleteDashboard(UUID dashboardId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return true;
        }

        Optional<DashboardPermission> permission = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        return permission.map(dp -> Boolean.TRUE.equals(dp.getCanDelete())).orElse(false);
    }

    @Override
    public boolean canShareDashboard(UUID dashboardId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return true;
        }

        Optional<DashboardPermission> permission = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        return permission.map(dp -> Boolean.TRUE.equals(dp.getCanShare())).orElse(false);
    }

    @Override
    public boolean canExportDashboard(UUID dashboardId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return true;
        }

        Optional<DashboardPermission> permission = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        return permission.map(dp -> Boolean.TRUE.equals(dp.getCanExport())).orElse(false);
    }

    @Override
    public boolean isDashboardOwner(UUID dashboardId, UUID userId) {
        Optional<DashboardPermission> permission = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        return permission.map(dp -> Boolean.TRUE.equals(dp.getIsOwner())).orElse(false);
    }

    @Override
    @Transactional
    public DashboardPermission grantPermission(UUID dashboardId, UUID userId,
                                               Boolean canView, Boolean canEdit,
                                               Boolean canDelete, Boolean canShare,
                                               Boolean canExport, Boolean isOwner) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        if (!isDashboardOwner(dashboardId, currentUserId) &&
                !canEditDashboard(dashboardId, currentUserId)) {
            throw new BusinessException("没有权限分配仪表板权限");
        }

        Dashboard dashboard = dashboardRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));

        User user = findUserByIdAndTenantId(userId, tenantId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Optional<DashboardPermission> existing = dashboardPermissionRepository
                .findByDashboardIdAndUserId(dashboardId, userId);

        DashboardPermission permission;
        if (existing.isPresent()) {
            permission = existing.get();
        } else {
            permission = new DashboardPermission();
            permission.setDashboard(dashboard);
            permission.setUser(user);
        }

        if (canView != null) permission.setCanView(canView);
        if (canEdit != null) permission.setCanEdit(canEdit);
        if (canDelete != null) permission.setCanDelete(canDelete);
        if (canShare != null) permission.setCanShare(canShare);
        if (canExport != null) permission.setCanExport(canExport);
        if (isOwner != null) permission.setIsOwner(isOwner);

        if (Boolean.TRUE.equals(isOwner)) {
            permission.setCanView(true);
            permission.setCanEdit(true);
            permission.setCanDelete(true);
            permission.setCanShare(true);
            permission.setCanExport(true);
        }

        return dashboardPermissionRepository.save(permission);
    }

    @Override
    @Transactional
    public DashboardPermission updatePermission(UUID permissionId,
                                                Boolean canView, Boolean canEdit,
                                                Boolean canDelete, Boolean canShare,
                                                Boolean canExport) {
        UUID currentUserId = getCurrentUserId();

        DashboardPermission permission = dashboardPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("权限不存在"));

        if (!isDashboardOwner(permission.getDashboard().getId(), currentUserId) &&
                !canEditDashboard(permission.getDashboard().getId(), currentUserId)) {
            throw new BusinessException("没有权限修改仪表板权限");
        }

        if (canView != null) permission.setCanView(canView);
        if (canEdit != null) permission.setCanEdit(canEdit);
        if (canDelete != null) permission.setCanDelete(canDelete);
        if (canShare != null) permission.setCanShare(canShare);
        if (canExport != null) permission.setCanExport(canExport);

        return dashboardPermissionRepository.save(permission);
    }

    @Override
    @Transactional
    public void revokePermission(UUID permissionId) {
        UUID currentUserId = getCurrentUserId();

        DashboardPermission permission = dashboardPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("权限不存在"));

        if (Boolean.TRUE.equals(permission.getIsOwner())) {
            throw new BusinessException("不能撤销所有者权限");
        }

        if (!isDashboardOwner(permission.getDashboard().getId(), currentUserId) &&
                !canEditDashboard(permission.getDashboard().getId(), currentUserId)) {
            throw new BusinessException("没有权限撤销仪表板权限");
        }

        permission.setDeleted(true);
        dashboardPermissionRepository.save(permission);
    }

    @Override
    public List<DashboardPermission> getDashboardPermissions(UUID dashboardId) {
        UUID currentUserId = getCurrentUserId();
        if (!canViewDashboard(dashboardId, currentUserId)) {
            throw new BusinessException("没有权限查看仪表板权限");
        }
        return dashboardPermissionRepository.findByDashboardId(dashboardId);
    }

    @Override
    public List<DashboardPermission> getUserPermissions(UUID userId) {
        UUID currentUserId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        if (!currentUserId.equals(userId)) {
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            if (currentUser.getRole() != RoleType.TENANT_ADMIN) {
                throw new BusinessException("没有权限查看其他用户的权限");
            }
        }

        return dashboardPermissionRepository.findByUserId(userId);
    }

    @Override
    public String getRowPermissionFilter(UUID userId, UUID dataModelId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return null;
        }

        String filters = user.getRowPermissionFilters();
        if (!StringUtils.hasText(filters)) {
            return null;
        }

        try {
            List<Map<String, Object>> rules = objectMapper.readValue(filters,
                    new TypeReference<List<Map<String, Object>>>() {});

            Set<String> availableFields = getAvailableFields(dataModelId);
            StringBuilder whereClause = new StringBuilder();

            for (int i = 0; i < rules.size(); i++) {
                Map<String, Object> rule = rules.get(i);
                String field = (String) rule.get("field");
                String operator = (String) rule.get("operator");
                Object value = rule.get("value");

                if (!availableFields.contains(field)) {
                    continue;
                }

                if (i > 0) {
                    String logic = (String) rule.get("logic");
                    whereClause.append(" ").append(logic != null ? logic : "AND").append(" ");
                }

                String tableAlias = extractTableAlias(field, dataModelId);
                String columnName = extractColumnName(field);

                whereClause.append(tableAlias).append(".").append(columnName);
                whereClause.append(" ").append(operator).append(" ");

                if (value instanceof String) {
                    whereClause.append("?");
                } else if (value instanceof Collection) {
                    whereClause.append("(");
                    Collection<?> values = (Collection<?>) value;
                    for (int j = 0; j < values.size(); j++) {
                        if (j > 0) whereClause.append(", ");
                        whereClause.append("?");
                    }
                    whereClause.append(")");
                } else {
                    whereClause.append("?");
                }
            }

            return whereClause.length() > 0 ? whereClause.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> validateRowPermissionRules(String rules) {
        if (!StringUtils.hasText(rules)) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> ruleList = objectMapper.readValue(rules,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<Map<String, Object>> errors = new ArrayList<>();

            for (int i = 0; i < ruleList.size(); i++) {
                Map<String, Object> rule = ruleList.get(i);
                Map<String, Object> error = new HashMap<>();

                String field = (String) rule.get("field");
                String operator = (String) rule.get("operator");
                Object value = rule.get("value");

                if (!StringUtils.hasText(field)) {
                    error.put("index", i);
                    error.put("message", "字段名不能为空");
                    errors.add(error);
                    continue;
                }

                if (!StringUtils.hasText(operator)) {
                    error.put("index", i);
                    error.put("message", "操作符不能为空");
                    errors.add(error);
                    continue;
                }

                Set<String> validOperators = new HashSet<>(Arrays.asList(
                        "=", "!=", "<>", ">", "<", ">=", "<=",
                        "LIKE", "IN", "NOT IN", "BETWEEN", "IS NULL", "IS NOT NULL"
                ));

                if (!validOperators.contains(operator.toUpperCase())) {
                    error.put("index", i);
                    error.put("message", "不支持的操作符: " + operator);
                    errors.add(error);
                    continue;
                }

                if (!Arrays.asList("IS NULL", "IS NOT NULL").contains(operator.toUpperCase())
                        && value == null) {
                    error.put("index", i);
                    error.put("message", "操作值不能为空");
                    errors.add(error);
                }

                ExpressionParser.ExpressionValidationResult validationResult =
                        expressionParser.validate(field + " " + operator, Collections.emptySet());
                if (!validationResult.isValid()) {
                    error.put("index", i);
                    error.put("message", "规则包含非法字符");
                    errors.add(error);
                }
            }

            return errors;
        } catch (Exception e) {
            throw new BusinessException("行权限规则格式错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public User updateUserRole(UUID userId, String role) {
        UUID currentUserId = getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (currentUser.getRole() != RoleType.TENANT_ADMIN &&
                !Boolean.TRUE.equals(currentUser.getIsSystemAdmin())) {
            throw new BusinessException("只有管理员可以修改用户角色");
        }

        User user = findUserByIdAndTenantId(userId, getCurrentTenantId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        RoleType newRole = RoleType.valueOf(role);
        if (newRole == RoleType.TENANT_ADMIN &&
                currentUser.getRole() != RoleType.TENANT_ADMIN &&
                !Boolean.TRUE.equals(currentUser.getIsSystemAdmin())) {
            throw new BusinessException("没有权限设置管理员角色");
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserRowPermissions(UUID userId, String rowPermissionFilters) {
        UUID currentUserId = getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (currentUser.getRole() != RoleType.TENANT_ADMIN &&
                !Boolean.TRUE.equals(currentUser.getIsSystemAdmin())) {
            throw new BusinessException("只有管理员可以配置行级数据权限");
        }

        User user = findUserByIdAndTenantId(userId, getCurrentTenantId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(rowPermissionFilters)) {
            List<Map<String, Object>> errors = validateRowPermissionRules(rowPermissionFilters);
            if (!errors.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("行权限规则验证失败: ");
                for (Map<String, Object> error : errors) {
                    errorMsg.append("规则").append(error.get("index")).append(": ")
                            .append(error.get("message")).append("; ");
                }
                throw new BusinessException(errorMsg.toString());
            }
        }

        user.setRowPermissionFilters(rowPermissionFilters);
        return userRepository.save(user);
    }

    @Override
    public List<UUID> getViewableDashboardIds(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return dashboardRepository.findByTenantId(getCurrentTenantId())
                    .stream()
                    .map(Dashboard::getId)
                    .collect(Collectors.toList());
        }

        return dashboardPermissionRepository.findViewableDashboardIdsByUserId(userId);
    }

    @Override
    public List<UUID> getEditableDashboardIds(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.TENANT_ADMIN || Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            return dashboardRepository.findByTenantId(getCurrentTenantId())
                    .stream()
                    .map(Dashboard::getId)
                    .collect(Collectors.toList());
        }

        return dashboardPermissionRepository.findEditableDashboardIdsByUserId(userId);
    }

    @Override
    public void checkTenantAdmin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() != RoleType.TENANT_ADMIN && !Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            throw new BusinessException("需要管理员权限");
        }
    }

    @Override
    public void checkEditorRole(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getRole() == RoleType.VIEWER && !Boolean.TRUE.equals(user.getIsSystemAdmin())) {
            throw new BusinessException("需要编辑者权限");
        }
    }

    @Override
    public void ensureTenantIsolation(UUID entityTenantId) {
        UUID currentTenantId = getCurrentTenantId();
        if (entityTenantId == null || !entityTenantId.equals(currentTenantId)) {
            throw new BusinessException("租户隔离违规");
        }
    }

    private Set<String> getAvailableFields(UUID dataModelId) {
        Set<String> fields = new HashSet<>();
        List<ModelTable> tables = modelTableRepository.findByDataModelId(dataModelId);

        for (ModelTable table : tables) {
            if (!table.getIsEnabled()) continue;
            if (table.getTableMetadata() != null && table.getTableMetadata().getColumns() != null) {
                for (ColumnMetadata col : table.getTableMetadata().getColumns()) {
                    fields.add(table.getAlias() + "." + col.getColumnName());
                    fields.add(col.getColumnName());
                }
            }
        }
        return fields;
    }

    private String extractTableAlias(String field, UUID dataModelId) {
        if (field.contains(".")) {
            return field.substring(0, field.indexOf("."));
        }

        List<ModelTable> tables = modelTableRepository.findByDataModelId(dataModelId);
        for (ModelTable table : tables) {
            if (table.getIsEnabled() && table.getTableMetadata() != null
                    && table.getTableMetadata().getColumns() != null) {
                for (ColumnMetadata col : table.getTableMetadata().getColumns()) {
                    if (field.equals(col.getColumnName())) {
                        return table.getAlias();
                    }
                }
            }
        }
        return "t";
    }

    private String extractColumnName(String field) {
        if (field.contains(".")) {
            return field.substring(field.indexOf(".") + 1);
        }
        return field;
    }

    private User findUserByIdAndTenantId(UUID userId, UUID tenantId) {
        return userRepository.findById(userId)
                .filter(u -> u.getTenant().getId().equals(tenantId) && !u.getDeleted())
                .orElse(null);
    }
}
