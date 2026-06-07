package com.lowcode.bi.controller;

import com.lowcode.bi.entity.*;
import com.lowcode.bi.expression.ExpressionParser;
import com.lowcode.bi.query.SqlGenerator;
import com.lowcode.bi.service.DataModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/models")
public class DataModelController {

    @Autowired
    private DataModelService dataModelService;

    @PostMapping
    public ResponseEntity<DataModel> createModel(@RequestBody Map<String, Object> request) {
        UUID dataSourceId = UUID.fromString((String) request.get("dataSourceId"));
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        DataModel model = dataModelService.createModel(dataSourceId, name, description);
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataModel> getModel(@PathVariable UUID id) {
        DataModel model = dataModelService.getModel(id);
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<DataModel> getModelWithDetails(@PathVariable UUID id) {
        DataModel model = dataModelService.getModelWithDetails(id);
        return ResponseEntity.ok(model);
    }

    @GetMapping
    public ResponseEntity<List<DataModel>> listModels() {
        List<DataModel> models = dataModelService.listModels();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/dataSource/{dataSourceId}")
    public ResponseEntity<List<DataModel>> listModelsByDataSource(@PathVariable UUID dataSourceId) {
        List<DataModel> models = dataModelService.listModelsByDataSource(dataSourceId);
        return ResponseEntity.ok(models);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataModel> updateModel(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String status = (String) request.get("status");
        DataModel model = dataModelService.updateModel(id, name, description, status);
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteModel(@PathVariable UUID id) {
        dataModelService.deleteModel(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<DataModel> publishModel(@PathVariable UUID id) {
        DataModel model = dataModelService.publishModel(id);
        return ResponseEntity.ok(model);
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<DataModel> copyModel(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        String newName = (String) request.get("newName");
        DataModel model = dataModelService.copyModel(id, newName);
        return ResponseEntity.ok(model);
    }

    @GetMapping("/templates")
    public ResponseEntity<List<DataModel>> listTemplates() {
        List<DataModel> templates = dataModelService.listTemplates();
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates/{templateId}/create")
    public ResponseEntity<DataModel> createFromTemplate(@PathVariable UUID templateId, @RequestBody Map<String, Object> request) {
        String newName = (String) request.get("newName");
        DataModel model = dataModelService.createFromTemplate(templateId, newName);
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}/available-tables")
    public ResponseEntity<List<TableMetadata>> getAvailableTables(@PathVariable UUID id) {
        List<TableMetadata> tables = dataModelService.getAvailableTables(id);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Map<String, Object>> getModelPreview(@PathVariable UUID id) {
        Map<String, Object> preview = dataModelService.getModelPreview(id);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/{id}/tables")
    public ResponseEntity<ModelTable> addTable(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        UUID tableMetadataId = UUID.fromString((String) request.get("tableMetadataId"));
        String alias = (String) request.get("alias");
        String displayName = (String) request.get("displayName");
        Boolean isFactTable = (Boolean) request.get("isFactTable");
        ModelTable table = dataModelService.addTable(id, tableMetadataId, alias, displayName, isFactTable);
        return ResponseEntity.ok(table);
    }

    @PutMapping("/tables/{tableId}")
    public ResponseEntity<ModelTable> updateTable(@PathVariable UUID tableId, @RequestBody Map<String, Object> request) {
        String displayName = (String) request.get("displayName");
        Integer positionX = request.get("positionX") != null ? ((Number) request.get("positionX")).intValue() : null;
        Integer positionY = request.get("positionY") != null ? ((Number) request.get("positionY")).intValue() : null;
        String filterCondition = (String) request.get("filterCondition");
        ModelTable table = dataModelService.updateTable(tableId, displayName, positionX, positionY, filterCondition);
        return ResponseEntity.ok(table);
    }

    @DeleteMapping("/tables/{tableId}")
    public ResponseEntity<Map<String, String>> removeTable(@PathVariable UUID tableId) {
        dataModelService.removeTable(tableId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/relations")
    public ResponseEntity<ModelRelation> addRelation(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        UUID leftTableId = UUID.fromString((String) request.get("leftTableId"));
        UUID rightTableId = UUID.fromString((String) request.get("rightTableId"));
        String leftColumn = (String) request.get("leftColumn");
        String rightColumn = (String) request.get("rightColumn");
        String relationType = (String) request.get("relationType");
        String joinType = (String) request.get("joinType");
        String description = (String) request.get("description");
        Integer weight = request.get("weight") != null ? ((Number) request.get("weight")).intValue() : null;
        ModelRelation relation = dataModelService.addRelation(id, leftTableId, rightTableId,
                leftColumn, rightColumn, relationType, joinType, description, weight);
        return ResponseEntity.ok(relation);
    }

    @PutMapping("/relations/{relationId}")
    public ResponseEntity<ModelRelation> updateRelation(@PathVariable UUID relationId, @RequestBody Map<String, Object> request) {
        Boolean isEnabled = (Boolean) request.get("isEnabled");
        String joinType = (String) request.get("joinType");
        Integer weight = request.get("weight") != null ? ((Number) request.get("weight")).intValue() : null;
        ModelRelation relation = dataModelService.updateRelation(relationId, isEnabled, joinType, weight);
        return ResponseEntity.ok(relation);
    }

    @DeleteMapping("/relations/{relationId}")
    public ResponseEntity<Map<String, String>> removeRelation(@PathVariable UUID relationId) {
        dataModelService.removeRelation(relationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/calculated-fields")
    public ResponseEntity<CalculatedField> addCalculatedField(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String displayName = (String) request.get("displayName");
        String expression = (String) request.get("expression");
        String dataType = (String) request.get("dataType");
        Boolean isDimension = (Boolean) request.get("isDimension");
        Boolean isMeasure = (Boolean) request.get("isMeasure");
        String formatPattern = (String) request.get("formatPattern");
        String unit = (String) request.get("unit");
        CalculatedField field = dataModelService.addCalculatedField(id, name, displayName,
                expression, dataType, isDimension, isMeasure, formatPattern, unit);
        return ResponseEntity.ok(field);
    }

    @PutMapping("/calculated-fields/{fieldId}")
    public ResponseEntity<CalculatedField> updateCalculatedField(@PathVariable UUID fieldId, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String displayName = (String) request.get("displayName");
        String expression = (String) request.get("expression");
        String dataType = (String) request.get("dataType");
        String formatPattern = (String) request.get("formatPattern");
        String unit = (String) request.get("unit");
        Integer position = request.get("position") != null ? ((Number) request.get("position")).intValue() : null;
        CalculatedField field = dataModelService.updateCalculatedField(fieldId, name, displayName,
                expression, dataType, formatPattern, unit, position);
        return ResponseEntity.ok(field);
    }

    @DeleteMapping("/calculated-fields/{fieldId}")
    public ResponseEntity<Map<String, String>> removeCalculatedField(@PathVariable UUID fieldId) {
        dataModelService.removeCalculatedField(fieldId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/validate-expression")
    public ResponseEntity<ExpressionParser.ExpressionValidationResult> validateExpression(
            @PathVariable UUID id, @RequestBody Map<String, Object> request) {
        String expression = (String) request.get("expression");
        ExpressionParser.ExpressionValidationResult result = dataModelService.validateExpression(expression, id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/measures")
    public ResponseEntity<Measure> addMeasure(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String displayName = (String) request.get("displayName");
        String tableAlias = (String) request.get("tableAlias");
        String columnName = (String) request.get("columnName");
        String aggregationType = (String) request.get("aggregationType");
        Boolean isDistinct = (Boolean) request.get("isDistinct");
        String filterCondition = (String) request.get("filterCondition");
        String expression = (String) request.get("expression");
        String formatPattern = (String) request.get("formatPattern");
        String unit = (String) request.get("unit");
        Integer decimalPlaces = request.get("decimalPlaces") != null ? ((Number) request.get("decimalPlaces")).intValue() : null;
        Measure measure = dataModelService.addMeasure(id, name, displayName, tableAlias, columnName,
                aggregationType, isDistinct, filterCondition, expression, formatPattern, unit, decimalPlaces);
        return ResponseEntity.ok(measure);
    }

    @PutMapping("/measures/{measureId}")
    public ResponseEntity<Measure> updateMeasure(@PathVariable UUID measureId, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String displayName = (String) request.get("displayName");
        String filterCondition = (String) request.get("filterCondition");
        String expression = (String) request.get("expression");
        String formatPattern = (String) request.get("formatPattern");
        String unit = (String) request.get("unit");
        Integer decimalPlaces = request.get("decimalPlaces") != null ? ((Number) request.get("decimalPlaces")).intValue() : null;
        Integer position = request.get("position") != null ? ((Number) request.get("position")).intValue() : null;
        Measure measure = dataModelService.updateMeasure(measureId, name, displayName,
                filterCondition, expression, formatPattern, unit, decimalPlaces, position);
        return ResponseEntity.ok(measure);
    }

    @DeleteMapping("/measures/{measureId}")
    public ResponseEntity<Map<String, String>> removeMeasure(@PathVariable UUID measureId) {
        dataModelService.removeMeasure(measureId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/hierarchies")
    public ResponseEntity<DimensionHierarchy> addDimensionHierarchy(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String displayName = (String) request.get("displayName");
        String description = (String) request.get("description");
        String hierarchyType = (String) request.get("hierarchyType");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> levels = (List<Map<String, Object>>) request.get("levels");
        DimensionHierarchy hierarchy = dataModelService.addDimensionHierarchy(id, name, displayName,
                description, hierarchyType, levels);
        return ResponseEntity.ok(hierarchy);
    }

    @PutMapping("/hierarchies/{hierarchyId}")
    public ResponseEntity<DimensionHierarchy> updateDimensionHierarchy(@PathVariable UUID hierarchyId, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String displayName = (String) request.get("displayName");
        String description = (String) request.get("description");
        Integer position = request.get("position") != null ? ((Number) request.get("position")).intValue() : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> levels = (List<Map<String, Object>>) request.get("levels");
        DimensionHierarchy hierarchy = dataModelService.updateDimensionHierarchy(hierarchyId, name, displayName,
                description, position, levels);
        return ResponseEntity.ok(hierarchy);
    }

    @DeleteMapping("/hierarchies/{hierarchyId}")
    public ResponseEntity<Map<String, String>> removeDimensionHierarchy(@PathVariable UUID hierarchyId) {
        dataModelService.removeDimensionHierarchy(hierarchyId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate-sql")
    public ResponseEntity<SqlGenerator.GeneratedSql> generateQuery(@PathVariable UUID id, @RequestBody SqlGenerator.QueryRequest request) {
        SqlGenerator.GeneratedSql sql = dataModelService.generateQuery(id, request);
        return ResponseEntity.ok(sql);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeQuery(@PathVariable UUID id, @RequestBody SqlGenerator.QueryRequest request) {
        Map<String, Object> result = dataModelService.executeQuery(id, request);
        return ResponseEntity.ok(result);
    }
}
