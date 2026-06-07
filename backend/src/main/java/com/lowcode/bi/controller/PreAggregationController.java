package com.lowcode.bi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.PreAggregation;
import com.lowcode.bi.query.SqlGenerator;
import com.lowcode.bi.service.PreAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pre-aggregation")
@RequiredArgsConstructor
public class PreAggregationController {

    private final PreAggregationService preAggregationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> createPreAggregation(@RequestBody Map<String, Object> request) {
        try {
            UUID dataModelId = UUID.fromString((String) request.get("dataModelId"));
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String targetTable = (String) request.get("targetTable");
            String cronExpression = (String) request.get("cronExpression");
            String refreshStrategy = (String) request.get("refreshStrategy");
            String incrementalColumn = (String) request.get("incrementalColumn");

            @SuppressWarnings("unchecked")
            List<SqlGenerator.DimensionField> dimensions = parseList(
                    (List<Map<String, Object>>) request.get("dimensions"),
                    SqlGenerator.DimensionField.class);

            @SuppressWarnings("unchecked")
            List<SqlGenerator.MeasureField> measures = parseList(
                    (List<Map<String, Object>>) request.get("measures"),
                    SqlGenerator.MeasureField.class);

            @SuppressWarnings("unchecked")
            List<SqlGenerator.FilterCondition> filters = request.get("filters") != null ?
                    parseList((List<Map<String, Object>>) request.get("filters"),
                            SqlGenerator.FilterCondition.class) : null;

            PreAggregation preAggregation = preAggregationService.createPreAggregation(
                    dataModelId, name, description, targetTable,
                    dimensions, measures, filters,
                    cronExpression, refreshStrategy, incrementalColumn);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preAggregation);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Create pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "创建预聚合失败: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> updatePreAggregation(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String cronExpression = (String) request.get("cronExpression");
            Boolean isEnabled = (Boolean) request.get("isEnabled");

            PreAggregation preAggregation = preAggregationService.updatePreAggregation(
                    id, name, description, cronExpression, isEnabled);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preAggregation);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Update pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "更新预聚合失败: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> deletePreAggregation(@PathVariable UUID id) {
        try {
            preAggregationService.deletePreAggregation(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "预聚合已删除");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Delete pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "删除预聚合失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getPreAggregation(@PathVariable UUID id) {
        try {
            PreAggregation preAggregation = preAggregationService.getPreAggregation(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preAggregation);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取预聚合失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/data-model/{dataModelId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getPreAggregationsByDataModel(@PathVariable UUID dataModelId) {
        try {
            List<PreAggregation> preAggregations =
                    preAggregationService.getPreAggregationsByDataModel(dataModelId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preAggregations);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get pre-aggregations error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取预聚合列表失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getPreAggregationsByTenant() {
        try {
            List<PreAggregation> preAggregations =
                    preAggregationService.getPreAggregationsByTenant();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preAggregations);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get pre-aggregations error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取预聚合列表失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> executePreAggregation(@PathVariable UUID id) {
        try {
            PreAggregation preAggregation = preAggregationService.executePreAggregation(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", preAggregation);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Execute pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "执行预聚合失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> pausePreAggregation(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : null;
            preAggregationService.pausePreAggregation(id, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "预聚合已暂停");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Pause pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "暂停预聚合失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> resumePreAggregation(@PathVariable UUID id) {
        try {
            preAggregationService.resumePreAggregation(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "预聚合已恢复");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Resume pre-aggregation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "恢复预聚合失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getPreAggregationStatus(@PathVariable UUID id) {
        try {
            Map<String, Object> status = preAggregationService.getPreAggregationStatus(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get pre-aggregation status error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取预聚合状态失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/generate-sql")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> generatePreAggregationSql(@RequestBody Map<String, Object> request) {
        try {
            UUID dataModelId = UUID.fromString((String) request.get("dataModelId"));

            @SuppressWarnings("unchecked")
            List<SqlGenerator.DimensionField> dimensions = parseList(
                    (List<Map<String, Object>>) request.get("dimensions"),
                    SqlGenerator.DimensionField.class);

            @SuppressWarnings("unchecked")
            List<SqlGenerator.MeasureField> measures = parseList(
                    (List<Map<String, Object>>) request.get("measures"),
                    SqlGenerator.MeasureField.class);

            @SuppressWarnings("unchecked")
            List<SqlGenerator.FilterCondition> filters = request.get("filters") != null ?
                    parseList((List<Map<String, Object>>) request.get("filters"),
                            SqlGenerator.FilterCondition.class) : null;

            String sql = preAggregationService.generatePreAggregationSql(
                    dataModelId, dimensions, measures, filters);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sql", sql);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Generate pre-aggregation sql error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "生成预聚合SQL失败: " + e.getMessage()
            ));
        }
    }

    private <T> List<T> parseList(List<Map<String, Object>> list, Class<T> clazz) {
        return objectMapper.convertValue(list,
                objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}
