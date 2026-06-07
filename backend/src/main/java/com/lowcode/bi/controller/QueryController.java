package com.lowcode.bi.controller;

import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.query.SqlQueryEngine;
import com.lowcode.bi.query.SqlSecurityValidator;
import com.lowcode.bi.repository.QueryCacheRepository;
import com.lowcode.bi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final SqlQueryEngine sqlQueryEngine;
    private final SqlSecurityValidator sqlSecurityValidator;
    private final QueryCacheRepository queryCacheRepository;

    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, Object> request) {
        try {
            UUID dataSourceId = UUID.fromString((String) request.get("dataSourceId"));
            String sql = (String) request.get("sql");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            Integer maxRows = request.get("maxRows") != null ?
                    ((Number) request.get("maxRows")).intValue() : null;
            Integer timeout = request.get("timeout") != null ?
                    ((Number) request.get("timeout")).intValue() : null;
            Boolean useCache = request.get("useCache") != null ?
                    (Boolean) request.get("useCache") : true;
            Integer cacheTtl = request.get("cacheTtl") != null ?
                    ((Number) request.get("cacheTtl")).intValue() : null;
            Boolean applyRowPermissions = request.get("applyRowPermissions") != null ?
                    (Boolean) request.get("applyRowPermissions") : true;

            SqlQueryEngine.QueryRequest queryRequest = new SqlQueryEngine.QueryRequest();
            queryRequest.setDataSourceId(dataSourceId);
            queryRequest.setSql(sql);
            queryRequest.setParameters(parameters);
            queryRequest.setMaxRows(maxRows);
            queryRequest.setTimeout(timeout);
            queryRequest.setUseCache(useCache);
            queryRequest.setCacheTtl(cacheTtl);
            queryRequest.setApplyRowPermissions(applyRowPermissions);

            if (request.get("dataModelId") != null) {
                queryRequest.setDataModelId(UUID.fromString((String) request.get("dataModelId")));
            }
            if (request.get("dashboardId") != null) {
                queryRequest.setDashboardId(UUID.fromString((String) request.get("dashboardId")));
            }
            if (request.get("componentId") != null) {
                queryRequest.setComponentId(UUID.fromString((String) request.get("componentId")));
            }

            SqlQueryEngine.QueryResult result = sqlQueryEngine.executeQuery(queryRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Query execution error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "查询执行失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> validateSql(@RequestBody Map<String, Object> request) {
        try {
            String sql = (String) request.get("sql");

            String normalized = sqlSecurityValidator.validateAndSanitize(sql);
            sqlSecurityValidator.validateForbiddenOperations(normalized);
            List<String> parameters = sqlSecurityValidator.extractParameters(normalized);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", true);
            response.put("normalizedSql", normalized);
            response.put("parameters", parameters);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("SQL validation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "SQL验证失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/explain")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> explainQuery(@RequestBody Map<String, Object> request) {
        try {
            UUID dataSourceId = UUID.fromString((String) request.get("dataSourceId"));
            String sql = (String) request.get("sql");

            Map<String, Object> result = sqlQueryEngine.explainQuery(dataSourceId, sql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Explain query error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取执行计划失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/tables/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> getTables(@PathVariable UUID dataSourceId) {
        try {
            List<Map<String, Object>> tables = sqlQueryEngine.getTableNames(dataSourceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tables);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get tables error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取表列表失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/columns/{dataSourceId}/{tableName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> getColumns(
            @PathVariable UUID dataSourceId,
            @PathVariable String tableName,
            @RequestParam(required = false) String schemaName) {
        try {
            List<Map<String, Object>> columns = sqlQueryEngine.getColumnNames(
                    dataSourceId, schemaName, tableName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", columns);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get columns error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取列列表失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/cache/status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> getCacheStatus() {
        try {
            UUID tenantId = TenantContext.getTenantId();
            long cacheCount = queryCacheRepository.countByTenantId(tenantId);

            Map<String, Object> status = new HashMap<>();
            status.put("cacheCount", cacheCount);
            status.put("tenantId", tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get cache status error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取缓存状态失败: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/cache")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> clearCache(@RequestBody Map<String, Object> request) {
        try {
            String scope = (String) request.getOrDefault("scope", "all");

            int deleted = 0;
            switch (scope) {
                case "dataSource":
                    UUID dataSourceId = UUID.fromString((String) request.get("dataSourceId"));
                    deleted = queryCacheRepository.deleteByDataSourceId(dataSourceId);
                    break;
                case "dataModel":
                    UUID dataModelId = UUID.fromString((String) request.get("dataModelId"));
                    deleted = queryCacheRepository.deleteByDataModelId(dataModelId);
                    break;
                case "dashboard":
                    UUID dashboardId = UUID.fromString((String) request.get("dashboardId"));
                    deleted = queryCacheRepository.deleteByDashboardId(dashboardId);
                    break;
                case "all":
                default:
                    UUID tenantId = TenantContext.getTenantId();
                    deleted = queryCacheRepository.deleteByTenantId(tenantId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deleted);
            response.put("scope", scope);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Clear cache error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "清除缓存失败: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/cache/expired")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> clearExpiredCache() {
        try {
            int deleted = queryCacheRepository.deleteExpiredCache(LocalDateTime.now());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deleted);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Clear expired cache error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "清除过期缓存失败: " + e.getMessage()
            ));
        }
    }
}
