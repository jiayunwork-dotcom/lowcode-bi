package com.lowcode.bi.service;

import com.lowcode.bi.entity.*;
import com.lowcode.bi.expression.ExpressionParser;
import com.lowcode.bi.query.SqlGenerator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DataModelService {

    DataModel createModel(UUID dataSourceId, String name, String description);

    DataModel getModel(UUID id);

    DataModel getModelWithDetails(UUID id);

    List<DataModel> listModels();

    List<DataModel> listModelsByDataSource(UUID dataSourceId);

    DataModel updateModel(UUID id, String name, String description, String status);

    void deleteModel(UUID id);

    ModelTable addTable(UUID modelId, UUID tableMetadataId, String alias, String displayName, Boolean isFactTable);

    ModelTable updateTable(UUID tableId, String displayName, Integer positionX, Integer positionY, String filterCondition);

    void removeTable(UUID tableId);

    ModelRelation addRelation(UUID modelId, UUID leftTableId, UUID rightTableId,
                              String leftColumn, String rightColumn, String relationType,
                              String joinType, String description, Integer weight);

    ModelRelation updateRelation(UUID relationId, Boolean isEnabled, String joinType, Integer weight);

    void removeRelation(UUID relationId);

    CalculatedField addCalculatedField(UUID modelId, String name, String displayName,
                                       String expression, String dataType, Boolean isDimension,
                                       Boolean isMeasure, String formatPattern, String unit);

    CalculatedField updateCalculatedField(UUID fieldId, String name, String displayName,
                                          String expression, String dataType, String formatPattern,
                                          String unit, Integer position);

    void removeCalculatedField(UUID fieldId);

    ExpressionParser.ExpressionValidationResult validateExpression(String expression, UUID modelId);

    Measure addMeasure(UUID modelId, String name, String displayName,
                       String tableAlias, String columnName, String aggregationType,
                       Boolean isDistinct, String filterCondition, String expression,
                       String formatPattern, String unit, Integer decimalPlaces);

    Measure updateMeasure(UUID measureId, String name, String displayName,
                          String filterCondition, String expression, String formatPattern,
                          String unit, Integer decimalPlaces, Integer position);

    void removeMeasure(UUID measureId);

    DimensionHierarchy addDimensionHierarchy(UUID modelId, String name, String displayName,
                                             String description, String hierarchyType,
                                             List<Map<String, Object>> levels);

    DimensionHierarchy updateDimensionHierarchy(UUID hierarchyId, String name, String displayName,
                                                String description, Integer position,
                                                List<Map<String, Object>> levels);

    void removeDimensionHierarchy(UUID hierarchyId);

    SqlGenerator.GeneratedSql generateQuery(UUID modelId, SqlGenerator.QueryRequest request);

    Map<String, Object> executeQuery(UUID modelId, SqlGenerator.QueryRequest request);

    DataModel publishModel(UUID id);

    DataModel copyModel(UUID id, String newName);

    List<DataModel> listTemplates();

    DataModel createFromTemplate(UUID templateId, String newName);

    List<TableMetadata> getAvailableTables(UUID modelId);

    Map<String, Object> getModelPreview(UUID modelId);
}
