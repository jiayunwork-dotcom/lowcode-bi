package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.enums.AggregationType;
import com.lowcode.bi.common.enums.ColumnDataType;
import com.lowcode.bi.common.enums.RelationType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.expression.ExpressionParser;
import com.lowcode.bi.query.SqlGenerator;
import com.lowcode.bi.query.SqlQueryEngine;
import com.lowcode.bi.repository.*;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.DataModelService;
import com.lowcode.bi.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataModelServiceImpl implements DataModelService {

    @Autowired
    private DataModelRepository dataModelRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Autowired
    private ModelTableRepository modelTableRepository;

    @Autowired
    private ModelRelationRepository modelRelationRepository;

    @Autowired
    private CalculatedFieldRepository calculatedFieldRepository;

    @Autowired
    private MeasureRepository measureRepository;

    @Autowired
    private DimensionHierarchyRepository dimensionHierarchyRepository;

    @Autowired
    private HierarchyLevelRepository hierarchyLevelRepository;

    @Autowired
    private ExpressionParser expressionParser;

    @Autowired
    private SqlGenerator sqlGenerator;

    @Autowired
    private SqlQueryEngine sqlQueryEngine;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID getCurrentTenantId() {
        return TenantContext.getCurrentTenantId();
    }

    @Override
    @Transactional
    public DataModel createModel(UUID dataSourceId, String name, String description) {
        UUID tenantId = getCurrentTenantId();

        if (dataModelRepository.existsByNameAndTenantIdAndDeletedFalse(name, tenantId)) {
            throw new BusinessException("模型名称已存在");
        }

        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        DataModel model = new DataModel();
        model.setDataSource(dataSource);
        model.setTenant(tenant);
        model.setName(name);
        model.setDescription(description);
        model.setStatus("DRAFT");
        model.setIsTemplate(false);

        return dataModelRepository.save(model);
    }

    @Override
    public DataModel getModel(UUID id) {
        UUID tenantId = getCurrentTenantId();
        return dataModelRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("数据模型不存在"));
    }

    @Override
    public DataModel getModelWithDetails(UUID id) {
        DataModel model = getModel(id);
        model.getModelTables().size();
        model.getModelRelations().size();
        model.getCalculatedFields().size();
        model.getMeasures().size();
        model.getDimensionHierarchies().size();
        return model;
    }

    @Override
    public List<DataModel> listModels() {
        UUID tenantId = getCurrentTenantId();
        return dataModelRepository.findByTenantId(tenantId);
    }

    @Override
    public List<DataModel> listModelsByDataSource(UUID dataSourceId) {
        return dataModelRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    @Transactional
    public DataModel updateModel(UUID id, String name, String description, String status) {
        DataModel model = getModel(id);

        if (name != null && !name.equals(model.getName())) {
            UUID tenantId = getCurrentTenantId();
            if (dataModelRepository.existsByNameAndTenantIdAndDeletedFalse(name, tenantId)) {
                throw new BusinessException("模型名称已存在");
            }
            model.setName(name);
        }

        if (description != null) {
            model.setDescription(description);
        }
        if (status != null) {
            model.setStatus(status);
        }

        return dataModelRepository.save(model);
    }

    @Override
    @Transactional
    public void deleteModel(UUID id) {
        DataModel model = getModel(id);
        model.setDeleted(true);
        dataModelRepository.save(model);
    }

    @Override
    @Transactional
    public ModelTable addTable(UUID modelId, UUID tableMetadataId, String alias, String displayName, Boolean isFactTable) {
        DataModel model = getModel(modelId);

        if (modelTableRepository.existsByDataModelIdAndAliasAndDeletedFalse(modelId, alias)) {
            throw new BusinessException("表别名已存在");
        }

        TableMetadata tableMetadata = tableMetadataRepository.findById(tableMetadataId)
                .orElseThrow(() -> new BusinessException("表元数据不存在"));

        ModelTable modelTable = new ModelTable();
        modelTable.setDataModel(model);
        modelTable.setTableMetadata(tableMetadata);
        modelTable.setAlias(alias);
        modelTable.setDisplayName(displayName != null ? displayName : tableMetadata.getTableName());
        modelTable.setIsFactTable(isFactTable != null ? isFactTable : false);
        modelTable.setIsEnabled(true);

        return modelTableRepository.save(modelTable);
    }

    @Override
    @Transactional
    public ModelTable updateTable(UUID tableId, String displayName, Integer positionX, Integer positionY, String filterCondition) {
        ModelTable table = modelTableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException("模型表不存在"));

        if (displayName != null) {
            table.setDisplayName(displayName);
        }
        if (positionX != null) {
            table.setPositionX(positionX);
        }
        if (positionY != null) {
            table.setPositionY(positionY);
        }
        if (filterCondition != null) {
            table.setFilterCondition(filterCondition);
        }

        return modelTableRepository.save(table);
    }

    @Override
    @Transactional
    public void removeTable(UUID tableId) {
        ModelTable table = modelTableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException("模型表不存在"));

        List<ModelRelation> relations = modelRelationRepository.findByTableId(tableId);
        for (ModelRelation relation : relations) {
            relation.setDeleted(true);
            modelRelationRepository.save(relation);
        }

        table.setDeleted(true);
        modelTableRepository.save(table);
    }

    @Override
    @Transactional
    public ModelRelation addRelation(UUID modelId, UUID leftTableId, UUID rightTableId,
                                   String leftColumn, String rightColumn, String relationType,
                                   String joinType, String description, Integer weight) {
        DataModel model = getModel(modelId);

        ModelTable leftTable = modelTableRepository.findById(leftTableId)
                .orElseThrow(() -> new BusinessException("左表不存在"));
        ModelTable rightTable = modelTableRepository.findById(rightTableId)
                .orElseThrow(() -> new BusinessException("右表不存在"));

        ModelRelation relation = new ModelRelation();
        relation.setDataModel(model);
        relation.setLeftTable(leftTable);
        relation.setRightTable(rightTable);
        relation.setLeftColumn(leftColumn);
        relation.setRightColumn(rightColumn);
        relation.setRelationType(RelationType.valueOf(relationType));
        relation.setJoinType(joinType != null ? joinType : "INNER");
        relation.setDescription(description);
        relation.setWeight(weight != null ? weight : 100);
        relation.setIsEnabled(true);

        return modelRelationRepository.save(relation);
    }

    @Override
    @Transactional
    public ModelRelation updateRelation(UUID relationId, Boolean isEnabled, String joinType, Integer weight) {
        ModelRelation relation = modelRelationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException("关联关系不存在"));

        if (isEnabled != null) {
            relation.setIsEnabled(isEnabled);
        }
        if (joinType != null) {
            relation.setJoinType(joinType);
        }
        if (weight != null) {
            relation.setWeight(weight);
        }

        return modelRelationRepository.save(relation);
    }

    @Override
    @Transactional
    public void removeRelation(UUID relationId) {
        ModelRelation relation = modelRelationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException("关联关系不存在"));
        relation.setDeleted(true);
        modelRelationRepository.save(relation);
    }

    @Override
    @Transactional
    public CalculatedField addCalculatedField(UUID modelId, String name, String displayName,
                                              String expression, String dataType, Boolean isDimension,
                                              Boolean isMeasure, String formatPattern, String unit) {
        DataModel model = getModel(modelId);

        if (calculatedFieldRepository.existsByDataModelIdAndNameAndDeletedFalse(modelId, name)) {
            throw new BusinessException("计算字段名称已存在");
        }

        ExpressionParser.ExpressionValidationResult validationResult = validateExpression(expression, modelId);
        if (!validationResult.isValid()) {
            StringBuilder errorMsg = new StringBuilder("表达式验证失败: ");
            for (ExpressionParser.ExpressionError error : validationResult.getErrors()) {
                errorMsg.append(error.getMessage()).append("; ");
            }
            throw new BusinessException(errorMsg.toString());
        }

        CalculatedField field = new CalculatedField();
        field.setDataModel(model);
        field.setName(name);
        field.setDisplayName(displayName != null ? displayName : name);
        field.setExpression(expression);
        field.setExpressionParsed(expressionParser.parseToSql(expression, new HashMap<>()));
        field.setDataType(ColumnDataType.valueOf(dataType));
        field.setIsDimension(isDimension != null ? isDimension : true);
        field.setIsMeasure(isMeasure != null ? isMeasure : false);
        field.setFormatPattern(formatPattern);
        field.setUnit(unit);
        field.setIsVisible(true);
        field.setIsValid(true);
        field.setPosition(calculatedFieldRepository.findByDataModelId(modelId).size() + 1);

        try {
            field.setDependencies(objectMapper.writeValueAsString(validationResult.getReferencedFields()));
        } catch (Exception e) {
            field.setDependencies("[]");
        }

        return calculatedFieldRepository.save(field);
    }

    @Override
    @Transactional
    public CalculatedField updateCalculatedField(UUID fieldId, String name, String displayName,
                                               String expression, String dataType, String formatPattern,
                                               String unit, Integer position) {
        CalculatedField field = calculatedFieldRepository.findById(fieldId)
                .orElseThrow(() -> new BusinessException("计算字段不存在"));

        if (name != null && !name.equals(field.getName())) {
            if (calculatedFieldRepository.existsByDataModelIdAndNameAndDeletedFalse(field.getDataModel().getId(), name)) {
                throw new BusinessException("计算字段名称已存在");
            }
            field.setName(name);
        }

        if (displayName != null) {
            field.setDisplayName(displayName);
        }
        if (expression != null) {
            ExpressionParser.ExpressionValidationResult validationResult = validateExpression(expression, field.getDataModel().getId());
            if (!validationResult.isValid()) {
                StringBuilder errorMsg = new StringBuilder("表达式验证失败: ");
                for (ExpressionParser.ExpressionError error : validationResult.getErrors()) {
                    errorMsg.append(error.getMessage()).append("; ");
                }
                throw new BusinessException(errorMsg.toString());
            }
            field.setExpression(expression);
            field.setExpressionParsed(expressionParser.parseToSql(expression, new HashMap<>()));
            field.setIsValid(true);
            field.setErrorMessage(null);
            try {
                field.setDependencies(objectMapper.writeValueAsString(validationResult.getReferencedFields()));
            } catch (Exception e) {
                    field.setDependencies("[]");
                }
            }
        }
        if (dataType != null) {
            field.setDataType(ColumnDataType.valueOf(dataType));
        }
        if (formatPattern != null) {
            field.setFormatPattern(formatPattern);
        }
        if (unit != null) {
            field.setUnit(unit);
        }
        if (position != null) {
            field.setPosition(position);
        }

        return calculatedFieldRepository.save(field);
    }

    @Override
    @Transactional
    public void removeCalculatedField(UUID fieldId) {
        CalculatedField field = calculatedFieldRepository.findById(fieldId)
                .orElseThrow(() -> new BusinessException("计算字段不存在"));
        field.setDeleted(true);
        calculatedFieldRepository.save(field);
    }

    @Override
    public ExpressionParser.ExpressionValidationResult validateExpression(String expression, UUID modelId) {
        Set<String> availableFields = new HashSet<>();

        if (modelId != null) {
            DataModel model = getModel(modelId);
            for (ModelTable table : model.getModelTables()) {
                if (table.getIsEnabled() && table.getTableMetadata() != null && table.getTableMetadata().getColumns() != null) {
                    for (ColumnMetadata col : table.getTableMetadata().getColumns()) {
                        availableFields.add(table.getAlias() + "." + col.getColumnName());
                        availableFields.add(col.getColumnName());
                    }
                }
            }
            for (CalculatedField cf : model.getCalculatedFields()) {
                if (cf.getIsValid()) {
                    availableFields.add(cf.getName());
                }
            }
        }

        return expressionParser.validate(expression, availableFields);
    }

    @Override
    @Transactional
    public Measure addMeasure(UUID modelId, String name, String displayName,
                            String tableAlias, String columnName, String aggregationType,
                            Boolean isDistinct, String filterCondition, String expression,
                            String formatPattern, String unit, Integer decimalPlaces) {
        DataModel model = getModel(modelId);

        if (measureRepository.existsByDataModelIdAndNameAndDeletedFalse(modelId, name)) {
            throw new BusinessException("度量名称已存在");
        }

        if (expression != null && !expression.trim().isEmpty()) {
            ExpressionParser.ExpressionValidationResult validationResult = validateExpression(expression, modelId);
            if (!validationResult.isValid()) {
                StringBuilder errorMsg = new StringBuilder("表达式验证失败: ");
                for (ExpressionParser.ExpressionError error : validationResult.getErrors()) {
                    errorMsg.append(error.getMessage()).append("; ");
                }
                throw new BusinessException(errorMsg.toString());
            }
        }

        if (filterCondition != null && !filterCondition.trim().isEmpty()) {
            ExpressionParser.ExpressionValidationResult validationResult = validateExpression(filterCondition, modelId);
            if (!validationResult.isValid()) {
                StringBuilder errorMsg = new StringBuilder("过滤条件验证失败: ");
                for (ExpressionParser.ExpressionError error : validationResult.getErrors()) {
                    errorMsg.append(error.getMessage()).append("; ");
                }
                throw new BusinessException(errorMsg.toString());
            }
        }

        Measure measure = new Measure();
        measure.setDataModel(model);
        measure.setName(name);
        measure.setDisplayName(displayName != null ? displayName : name);
        measure.setTableAlias(tableAlias);
        measure.setColumnName(columnName);
        measure.setAggregationType(AggregationType.valueOf(aggregationType));
        measure.setIsDistinct(isDistinct != null ? isDistinct : false);
        measure.setFilterCondition(filterCondition);
        measure.setExpression(expression);
        measure.setFormatPattern(formatPattern);
        measure.setUnit(unit);
        measure.setDecimalPlaces(decimalPlaces);
        measure.setIsVisible(true);
        measure.setPosition(measureRepository.findByDataModelId(modelId).size() + 1);

        return measureRepository.save(measure);
    }

    @Override
    @Transactional
    public Measure updateMeasure(UUID measureId, String name, String displayName,
                               String filterCondition, String expression, String formatPattern,
                               String unit, Integer decimalPlaces, Integer position) {
        Measure measure = measureRepository.findById(measureId)
                .orElseThrow(() -> new BusinessException("度量不存在"));

        if (name != null && !name.equals(measure.getName())) {
            if (measureRepository.existsByDataModelIdAndNameAndDeletedFalse(measure.getDataModel().getId(), name)) {
                throw new BusinessException("度量名称已存在");
            }
            measure.setName(name);
        }

        if (displayName != null) {
            measure.setDisplayName(displayName);
        }
        if (expression != null) {
            if (!expression.trim().isEmpty()) {
                ExpressionParser.ExpressionValidationResult validationResult = validateExpression(expression, measure.getDataModel().getId());
                if (!validationResult.isValid()) {
                    StringBuilder errorMsg = new StringBuilder("表达式验证失败: ");
                    for (ExpressionParser.ExpressionError error : validationResult.getErrors()) {
                        errorMsg.append(error.getMessage()).append("; ");
                    }
                    throw new BusinessException(errorMsg.toString());
                }
            }
            measure.setExpression(expression);
        }
        if (filterCondition != null) {
            if (!filterCondition.trim().isEmpty()) {
                ExpressionParser.ExpressionValidationResult validationResult = validateExpression(filterCondition, measure.getDataModel().getId());
                if (!validationResult.isValid()) {
                    StringBuilder errorMsg = new StringBuilder("过滤条件验证失败: ");
                    for (ExpressionParser.ExpressionError error : validationResult.getErrors()) {
                        errorMsg.append(error.getMessage()).append("; ");
                    }
                    throw new BusinessException(errorMsg.toString());
                }
            }
            measure.setFilterCondition(filterCondition);
        }
        if (formatPattern != null) {
            measure.setFormatPattern(formatPattern);
        }
        if (unit != null) {
            measure.setUnit(unit);
        }
        if (decimalPlaces != null) {
            measure.setDecimalPlaces(decimalPlaces);
        }
        if (position != null) {
            measure.setPosition(position);
        }

        return measureRepository.save(measure);
    }

    @Override
    @Transactional
    public void removeMeasure(UUID measureId) {
        Measure measure = measureRepository.findById(measureId)
                .orElseThrow(() -> new BusinessException("度量不存在"));
        measure.setDeleted(true);
        measureRepository.save(measure);
    }

    @Override
    @Transactional
    public DimensionHierarchy addDimensionHierarchy(UUID modelId, String name, String displayName,
                                                    String description, String hierarchyType,
                                                    List<Map<String, Object>> levels) {
        DataModel model = getModel(modelId);

        if (dimensionHierarchyRepository.existsByDataModelIdAndNameAndDeletedFalse(modelId, name)) {
            throw new BusinessException("维度层级名称已存在");
        }

        DimensionHierarchy hierarchy = new DimensionHierarchy();
        hierarchy.setDataModel(model);
        hierarchy.setName(name);
        hierarchy.setDisplayName(displayName != null ? displayName : name);
        hierarchy.setDescription(description);
        hierarchy.setHierarchyType(hierarchyType != null ? hierarchyType : "STANDARD");
        hierarchy.setIsVisible(true);
        hierarchy.setPosition(dimensionHierarchyRepository.findByDataModelId(modelId).size() + 1);

        hierarchy = dimensionHierarchyRepository.save(hierarchy);

        if (levels != null && !levels.isEmpty()) {
            for (int i = 0; i < levels.size(); i++) {
                Map<String, Object> levelMap = levels.get(i);
                HierarchyLevel level = new HierarchyLevel();
                level.setHierarchy(hierarchy);
                level.setLevel(i + 1);
                level.setColumnName((String) levelMap.get("columnName"));
                level.setDisplayName((String) levelMap.get("displayName"));
                level.setTableAlias((String) levelMap.get("tableAlias"));
                level.setFunction((String) levelMap.get("function"));
                hierarchyLevelRepository.save(level);
            }
        }

        return hierarchy;
    }

    @Override
    @Transactional
    public DimensionHierarchy updateDimensionHierarchy(UUID hierarchyId, String name, String displayName,
                                               String description, Integer position,
                                               List<Map<String, Object>> levels) {
        DimensionHierarchy hierarchy = dimensionHierarchyRepository.findById(hierarchyId)
                .orElseThrow(() -> new BusinessException("维度层级不存在"));

        if (name != null && !name.equals(hierarchy.getName())) {
            if (dimensionHierarchyRepository.existsByDataModelIdAndNameAndDeletedFalse(
                    hierarchy.getDataModel().getId(), name)) {
                throw new BusinessException("维度层级名称已存在");
            }
            hierarchy.setName(name);
        }

        if (displayName != null) {
            hierarchy.setDisplayName(displayName);
        }
        if (description != null) {
            hierarchy.setDescription(description);
        }
        if (position != null) {
            hierarchy.setPosition(position);
        }

        if (levels != null) {
            List<HierarchyLevel> existingLevels = hierarchyLevelRepository.findByHierarchyId(hierarchyId);
            for (HierarchyLevel level : existingLevels) {
                level.setDeleted(true);
                hierarchyLevelRepository.save(level);
            }

            for (int i = 0; i < levels.size(); i++) {
                Map<String, Object> levelMap = levels.get(i);
                HierarchyLevel level = new HierarchyLevel();
                level.setHierarchy(hierarchy);
                level.setLevel(i + 1);
                level.setColumnName((String) levelMap.get("columnName"));
                level.setDisplayName((String) levelMap.get("displayName"));
                level.setTableAlias((String) levelMap.get("tableAlias"));
                level.setFunction((String) levelMap.get("function"));
                hierarchyLevelRepository.save(level);
            }
        }

        return dimensionHierarchyRepository.save(hierarchy);
    }

    @Override
    @Transactional
    public void removeDimensionHierarchy(UUID hierarchyId) {
        DimensionHierarchy hierarchy = dimensionHierarchyRepository.findById(hierarchyId)
                .orElseThrow(() -> new BusinessException("维度层级不存在"));

        List<HierarchyLevel> levels = hierarchyLevelRepository.findByHierarchyId(hierarchyId);
        for (HierarchyLevel level : levels) {
            level.setDeleted(true);
            hierarchyLevelRepository.save(level);
        }

        hierarchy.setDeleted(true);
        dimensionHierarchyRepository.save(hierarchy);
    }

    @Override
    public SqlGenerator.GeneratedSql generateQuery(UUID modelId, SqlGenerator.QueryRequest request) {
        DataModel model = getModelWithDetails(modelId);
        return sqlGenerator.generateQuery(model, request);
    }

    @Override
    public Map<String, Object> executeQuery(UUID modelId, SqlGenerator.QueryRequest request) {
        DataModel model = getModel(modelId);
        SqlGenerator.GeneratedSql generatedSql = generateQuery(modelId, request);

        try {
            return sqlQueryEngine.executeQuery(
                    model.getDataSource().getId(),
                    generatedSql.getSql(),
                    generatedSql.getParameters(),
                    request.getLimit() != null ? request.getLimit() : 1000
            );
        } catch (Exception e) {
            throw new BusinessException("查询执行失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DataModel publishModel(UUID id) {
        DataModel model = getModel(id);

        if (CollectionUtils.isEmpty(model.getModelTables())) {
            throw new BusinessException("模型至少需要添加一个表");
        }

        model.setStatus("PUBLISHED");
        model.setPublishedAt(LocalDateTime.now());
        model.setPublishedBy(TenantContext.getCurrentUsername());

        return dataModelRepository.save(model);
    }

    @Override
    @Transactional
    public DataModel copyModel(UUID id, String newName) {
        DataModel source = getModelWithDetails(id);
        UUID tenantId = getCurrentTenantId();

        if (dataModelRepository.existsByNameAndTenantIdAndDeletedFalse(newName, tenantId)) {
            throw new BusinessException("模型名称已存在");
        }

        DataModel copy = new DataModel();
        copy.setDataSource(source.getDataSource());
        copy.setTenant(source.getTenant());
        copy.setName(newName);
        copy.setDescription(source.getDescription() + " (副本)");
        copy.setStatus("DRAFT");
        copy.setIsTemplate(false);
        copy = dataModelRepository.save(copy);

        Map<UUID, ModelTable> tableMap = new HashMap<>();
        for (ModelTable table : source.getModelTables()) {
            ModelTable tableCopy = new ModelTable();
            tableCopy.setDataModel(copy);
            tableCopy.setTableMetadata(table.getTableMetadata());
            tableCopy.setAlias(table.getAlias());
            tableCopy.setDisplayName(table.getDisplayName());
            tableCopy.setIsFactTable(table.getIsFactTable());
            tableCopy.setPositionX(table.getPositionX());
            tableCopy.setPositionY(table.getPositionY());
            tableCopy.setFilterCondition(table.getFilterCondition());
            tableCopy.setIsEnabled(table.getIsEnabled());
            tableCopy = modelTableRepository.save(tableCopy);
            tableMap.put(table.getId(), tableCopy);
        }

        for (ModelRelation relation : source.getModelRelations()) {
            ModelRelation relationCopy = new ModelRelation();
            relationCopy.setDataModel(copy);
            relationCopy.setLeftTable(tableMap.get(relation.getLeftTable().getId()));
            relationCopy.setRightTable(tableMap.get(relation.getRightTable().getId()));
            relationCopy.setLeftColumn(relation.getLeftColumn());
            relationCopy.setRightColumn(relation.getRightColumn());
            relationCopy.setRelationType(relation.getRelationType());
            relationCopy.setJoinType(relation.getJoinType());
            relationCopy.setIsEnabled(relation.getIsEnabled());
            relationCopy.setDescription(relation.getDescription());
            relationCopy.setWeight(relation.getWeight());
            modelRelationRepository.save(relationCopy);
        }

        for (CalculatedField field : source.getCalculatedFields()) {
            CalculatedField fieldCopy = new CalculatedField();
            fieldCopy.setDataModel(copy);
            fieldCopy.setName(field.getName());
            fieldCopy.setDisplayName(field.getDisplayName());
            fieldCopy.setDescription(field.getDescription());
            fieldCopy.setDataType(field.getDataType());
            fieldCopy.setExpression(field.getExpression());
            fieldCopy.setExpressionParsed(field.getExpressionParsed());
            fieldCopy.setIsDimension(field.getIsDimension());
            fieldCopy.setIsMeasure(field.getIsMeasure());
            fieldCopy.setFormatPattern(field.getFormatPattern());
            fieldCopy.setUnit(field.getUnit());
            fieldCopy.setIsVisible(field.getIsVisible());
            fieldCopy.setPosition(field.getPosition());
            fieldCopy.setDependencies(field.getDependencies());
            fieldCopy.setIsValid(field.getIsValid());
            calculatedFieldRepository.save(fieldCopy);
        }

        for (Measure measure : source.getMeasures()) {
            Measure measureCopy = new Measure();
            measureCopy.setDataModel(copy);
            measureCopy.setName(measure.getName());
            measureCopy.setDisplayName(measure.getDisplayName());
            measureCopy.setDescription(measure.getDescription());
            measureCopy.setTableAlias(measure.getTableAlias());
            measureCopy.setColumnName(measure.getColumnName());
            measureCopy.setAggregationType(measure.getAggregationType());
            measureCopy.setIsDistinct(measure.getIsDistinct());
            measureCopy.setFilterCondition(measure.getFilterCondition());
            measureCopy.setExpression(measure.getExpression());
            measureCopy.setFormatPattern(measure.getFormatPattern());
            measureCopy.setUnit(measure.getUnit());
            measureCopy.setDecimalPlaces(measure.getDecimalPlaces());
            measureCopy.setIsVisible(measure.getIsVisible());
            measureCopy.setPosition(measure.getPosition());
            measureCopy.setIsPreAggregated(measure.getIsPreAggregated());
            measureRepository.save(measureCopy);
        }

        return copy;
    }

    @Override
    public List<DataModel> listTemplates() {
        return dataModelRepository.findAllTemplates();
    }

    @Override
    @Transactional
    public DataModel createFromTemplate(UUID templateId, String newName) {
        return copyModel(templateId, newName);
    }

    @Override
    public List<TableMetadata> getAvailableTables(UUID modelId) {
        DataModel model = getModel(modelId);
        return tableMetadataRepository.findByDataSourceId(model.getDataSource().getId());
    }

    @Override
    public Map<String, Object> getModelPreview(UUID modelId) {
        DataModel model = getModelWithDetails(modelId);
        Map<String, Object> preview = new HashMap<>();

        List<Map<String, Object>> tables = new ArrayList<>();
        for (ModelTable table : model.getModelTables()) {
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("id", table.getId());
            tableInfo.put("alias", table.getAlias());
            tableInfo.put("displayName", table.getDisplayName());
            tableInfo.put("tableName", table.getTableMetadata().getTableName());
            tableInfo.put("isFactTable", table.getIsFactTable());
            tableInfo.put("positionX", table.getPositionX());
            tableInfo.put("positionY", table.getPositionY());

            List<Map<String, Object>> columns = new ArrayList<>();
            if (table.getTableMetadata().getColumns() != null) {
                for (ColumnMetadata col : table.getTableMetadata().getColumns()) {
                    Map<String, Object> colInfo = new HashMap<>();
                    colInfo.put("name", col.getColumnName());
                    colInfo.put("displayName", col.getDisplayName());
                    colInfo.put("dataType", col.getDataType());
                    columns.add(colInfo);
                }
            }
            tableInfo.put("columns", columns);
            tables.add(tableInfo);
        }
        preview.put("tables", tables);

        List<Map<String, Object>> relations = new ArrayList<>();
        for (ModelRelation relation : model.getModelRelations()) {
            Map<String, Object> relInfo = new HashMap<>();
            relInfo.put("id", relation.getId());
            relInfo.put("leftTableAlias", relation.getLeftTable().getAlias());
            relInfo.put("rightTableAlias", relation.getRightTable().getAlias());
            relInfo.put("leftColumn", relation.getLeftColumn());
            relInfo.put("rightColumn", relation.getRightColumn());
            relInfo.put("relationType", relation.getRelationType());
            relInfo.put("joinType", relation.getJoinType());
            relInfo.put("isEnabled", relation.getIsEnabled());
            relations.add(relInfo);
        }
        preview.put("relations", relations);

        List<Map<String, Object>> calculatedFields = new ArrayList<>();
        for (CalculatedField field : model.getCalculatedFields()) {
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("id", field.getId());
            fieldInfo.put("name", field.getName());
            fieldInfo.put("displayName", field.getDisplayName());
            fieldInfo.put("expression", field.getExpression());
            fieldInfo.put("dataType", field.getDataType());
            fieldInfo.put("isDimension", field.getIsDimension());
            fieldInfo.put("isMeasure", field.getIsMeasure());
            calculatedFields.add(fieldInfo);
        }
        preview.put("calculatedFields", calculatedFields);

        List<Map<String, Object>> measures = new ArrayList<>();
        for (Measure measure : model.getMeasures()) {
            Map<String, Object> measureInfo = new HashMap<>();
            measureInfo.put("id", measure.getId());
            measureInfo.put("name", measure.getName());
            measureInfo.put("displayName", measure.getDisplayName());
            measureInfo.put("tableAlias", measure.getTableAlias());
            measureInfo.put("columnName", measure.getColumnName());
            measureInfo.put("aggregationType", measure.getAggregationType());
            measures.add(measureInfo);
        }
        preview.put("measures", measures);

        List<Map<String, Object>> hierarchies = new ArrayList<>();
        for (DimensionHierarchy hierarchy : model.getDimensionHierarchies()) {
            Map<String, Object> hierInfo = new HashMap<>();
            hierInfo.put("id", hierarchy.getId());
            hierInfo.put("name", hierarchy.getName());
            hierInfo.put("displayName", hierarchy.getDisplayName());
            hierInfo.put("hierarchyType", hierarchy.getHierarchyType());

            List<Map<String, Object>> levels = new ArrayList<>();
            for (HierarchyLevel level : hierarchy.getLevels()) {
                Map<String, Object> levelInfo = new HashMap<>();
                levelInfo.put("level", level.getLevel());
                levelInfo.put("columnName", level.getColumnName());
                levelInfo.put("displayName", level.getDisplayName());
                levelInfo.put("tableAlias", level.getTableAlias());
                levelInfo.put("function", level.getFunction());
                levels.add(levelInfo);
            }
            hierInfo.put("levels", levels);
            hierarchies.add(hierInfo);
        }
        preview.put("hierarchies", hierarchies);

        return preview;
    }
}
