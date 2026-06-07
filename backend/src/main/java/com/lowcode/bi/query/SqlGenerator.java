package com.lowcode.bi.query;

import com.lowcode.bi.common.enums.AggregationType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.*;
import com.lowcode.bi.expression.ExpressionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SqlGenerator {

    @Autowired
    private ExpressionParser expressionParser;

    public static class QueryRequest {
        private List<DimensionField> dimensions;
        private List<MeasureField> measures;
        private List<FilterCondition> filters;
        private List<SortField> sorts;
        private Integer limit;
        private Integer offset;

        public List<DimensionField> getDimensions() { return dimensions; }
        public void setDimensions(List<DimensionField> dimensions) { this.dimensions = dimensions; }
        public List<MeasureField> getMeasures() { return measures; }
        public void setMeasures(List<MeasureField> measures) { this.measures = measures; }
        public List<FilterCondition> getFilters() { return filters; }
        public void setFilters(List<FilterCondition> filters) { this.filters = filters; }
        public List<SortField> getSorts() { return sorts; }
        public void setSorts(List<SortField> sorts) { this.sorts = sorts; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        public Integer getOffset() { return offset; }
        public void setOffset(Integer offset) { this.offset = offset; }
    }

    public static class DimensionField {
        private String tableAlias;
        private String columnName;
        private String alias;
        private String function;
        private String dateFormat;
        private Integer level;

        public String getTableAlias() { return tableAlias; }
        public void setTableAlias(String tableAlias) { this.tableAlias = tableAlias; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        public String getFunction() { return function; }
        public void setFunction(String function) { this.function = function; }
        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
    }

    public static class MeasureField {
        private String tableAlias;
        private String columnName;
        private String alias;
        private AggregationType aggregationType;
        private Boolean distinct;
        private String filterCondition;
        private String expression;

        public String getTableAlias() { return tableAlias; }
        public void setTableAlias(String tableAlias) { this.tableAlias = tableAlias; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        public AggregationType getAggregationType() { return aggregationType; }
        public void setAggregationType(AggregationType aggregationType) { this.aggregationType = aggregationType; }
        public Boolean getDistinct() { return distinct; }
        public void setDistinct(Boolean distinct) { this.distinct = distinct; }
        public String getFilterCondition() { return filterCondition; }
        public void setFilterCondition(String filterCondition) { this.filterCondition = filterCondition; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
    }

    public static class FilterCondition {
        private String tableAlias;
        private String columnName;
        private String operator;
        private Object value;
        private List<Object> values;
        private String expression;
        private String logic;

        public String getTableAlias() { return tableAlias; }
        public void setTableAlias(String tableAlias) { this.tableAlias = tableAlias; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        public List<Object> getValues() { return values; }
        public void setValues(List<Object> values) { this.values = values; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getLogic() { return logic; }
        public void setLogic(String logic) { this.logic = logic; }
    }

    public static class SortField {
        private String field;
        private String direction;

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
    }

    public static class GeneratedSql {
        private String sql;
        private List<Object> parameters;
        private Map<String, String> columnMappings;

        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public List<Object> getParameters() { return parameters; }
        public void setParameters(List<Object> parameters) { this.parameters = parameters; }
        public Map<String, String> getColumnMappings() { return columnMappings; }
        public void setColumnMappings(Map<String, String> columnMappings) { this.columnMappings = columnMappings; }
    }

    public GeneratedSql generateQuery(DataModel dataModel, QueryRequest request) {
        GeneratedSql result = new GeneratedSql();
        List<Object> parameters = new ArrayList<>();
        Map<String, String> columnMappings = new HashMap<>();

        Set<ModelTable> requiredTables = new HashSet<>();
        Map<String, String> fieldMappings = new HashMap<>();

        buildFieldMappings(dataModel, fieldMappings);

        if (!CollectionUtils.isEmpty(request.getDimensions())) {
            for (DimensionField dim : request.getDimensions()) {
                ModelTable table = findTableByAlias(dataModel, dim.getTableAlias());
                if (table != null) {
                    requiredTables.add(table);
                }
            }
        }

        if (!CollectionUtils.isEmpty(request.getMeasures())) {
            for (MeasureField measure : request.getMeasures()) {
                ModelTable table = findTableByAlias(dataModel, measure.getTableAlias());
                if (table != null) {
                    requiredTables.add(table);
                }
            }
        }

        if (!CollectionUtils.isEmpty(request.getFilters())) {
            for (FilterCondition filter : request.getFilters()) {
                ModelTable table = findTableByAlias(dataModel, filter.getTableAlias());
                if (table != null) {
                    requiredTables.add(table);
                }
            }
        }

        if (requiredTables.isEmpty()) {
            throw new BusinessException("至少需要选择一个维度或度量");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        List<String> selectParts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getDimensions())) {
            for (int i = 0; i < request.getDimensions().size(); i++) {
                DimensionField dim = request.getDimensions().get(i);
                String columnExpr = buildDimensionExpression(dim);
                String alias = dim.getAlias() != null ? dim.getAlias() : "dim_" + (i + 1);
                selectParts.add(columnExpr + " AS " + alias);
                columnMappings.put(alias, dim.getTableAlias() + "." + dim.getColumnName());
            }
        }

        if (!CollectionUtils.isEmpty(request.getMeasures())) {
            for (int i = 0; i < request.getMeasures().size(); i++) {
                MeasureField measure = request.getMeasures().get(i);
                String columnExpr = buildMeasureExpression(measure, fieldMappings);
                String alias = measure.getAlias() != null ? measure.getAlias() : "measure_" + (i + 1);
                selectParts.add(columnExpr + " AS " + alias);
                columnMappings.put(alias, measure.getTableAlias() + "." + measure.getColumnName());
            }
        }

        sql.append(String.join(", ", selectParts));

        sql.append(" FROM ");
        List<String> fromParts = buildFromClause(dataModel, requiredTables);
        sql.append(String.join(" ", fromParts));

        List<String> whereParts = buildWhereClause(request, parameters);
        if (!CollectionUtils.isEmpty(whereParts)) {
            sql.append(" WHERE ").append(String.join(" AND ", whereParts));
        }

        if (!CollectionUtils.isEmpty(request.getDimensions())) {
            sql.append(" GROUP BY ");
            List<String> groupByParts = new ArrayList<>();
            for (int i = 0; i < request.getDimensions().size(); i++) {
                groupByParts.add(String.valueOf(i + 1));
            }
            sql.append(String.join(", ", groupByParts));
        }

        if (!CollectionUtils.isEmpty(request.getSorts())) {
            sql.append(" ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (SortField sort : request.getSorts()) {
                orderParts.add(sort.getField() + " " + sort.getDirection());
            }
            sql.append(String.join(", ", orderParts));
        }

        if (request.getLimit() != null) {
            sql.append(" LIMIT ").append(request.getLimit());
        }
        if (request.getOffset() != null) {
            sql.append(" OFFSET ").append(request.getOffset());
        }

        result.setSql(sql.toString());
        result.setParameters(parameters);
        result.setColumnMappings(columnMappings);

        return result;
    }

    private void buildFieldMappings(DataModel dataModel, Map<String, String> fieldMappings) {
        for (ModelTable table : dataModel.getModelTables()) {
            if (!table.getIsEnabled()) continue;
            TableMetadata tableMetadata = table.getTableMetadata();
            if (tableMetadata != null && tableMetadata.getColumns() != null) {
                for (ColumnMetadata col : tableMetadata.getColumns()) {
                    String fieldKey = table.getAlias() + "." + col.getColumnName();
                    String sqlField = table.getAlias() + "." + col.getColumnName();
                    fieldMappings.put(fieldKey, sqlField);
                    fieldMappings.put(col.getColumnName(), sqlField);
                }
            }
        }

        for (CalculatedField cf : dataModel.getCalculatedFields()) {
            if (!cf.getIsValid()) continue;
            String parsedSql = expressionParser.parseToSql(cf.getExpression(), fieldMappings);
            fieldMappings.put(cf.getName(), "(" + parsedSql + ")");
        }
    }

    private String buildDimensionExpression(DimensionField dim) {
        String column = dim.getTableAlias() + "." + dim.getColumnName();
        if (dim.getFunction() != null) {
            switch (dim.getFunction().toUpperCase()) {
                case "YEAR":
                    return "YEAR(" + column + ")";
                case "QUARTER":
                    return "QUARTER(" + column + ")";
                case "MONTH":
                    return "DATE_FORMAT(" + column + ", '%Y-%m')";
                case "DAY":
                    return "DATE_FORMAT(" + column + ", '%Y-%m-%d')";
                case "HOUR":
                    return "DATE_FORMAT(" + column + ", '%Y-%m-%d %H:00')";
                default:
                    return column;
            }
        }
        if (dim.getDateFormat() != null) {
            return "DATE_FORMAT(" + column + ", '" + dim.getDateFormat() + "')";
        }
        return column;
    }

    private String buildMeasureExpression(MeasureField measure, Map<String, String> fieldMappings) {
        AggregationType aggType = measure.getAggregationType();
        String column;

        if (measure.getExpression() != null && !measure.getExpression().trim().isEmpty()) {
            column = expressionParser.parseToSql(measure.getExpression(), fieldMappings);
        } else {
            column = measure.getTableAlias() + "." + measure.getColumnName();
        }

        String distinct = Boolean.TRUE.equals(measure.getDistinct()) ? "DISTINCT " : "";
        String baseExpr;

        switch (aggType) {
            case SUM:
                baseExpr = "SUM(" + distinct + column + ")";
                break;
            case AVG:
                baseExpr = "AVG(" + distinct + column + ")";
                break;
            case COUNT:
                baseExpr = "COUNT(" + distinct + column + ")";
                break;
            case COUNT_DISTINCT:
                baseExpr = "COUNT(DISTINCT " + column + ")";
                break;
            case MAX:
                baseExpr = "MAX(" + column + ")";
                break;
            case MIN:
                baseExpr = "MIN(" + column + ")";
                break;
            default:
                baseExpr = column;
        }

        if (measure.getFilterCondition() != null && !measure.getFilterCondition().trim().isEmpty()) {
            String parsedFilter = expressionParser.parseToSql(measure.getFilterCondition(), fieldMappings);
            baseExpr = "SUM(CASE WHEN " + parsedFilter + " THEN " + column + " ELSE 0 END)";
        }

        return baseExpr;
    }

    private List<String> buildFromClause(DataModel dataModel, Set<ModelTable> requiredTables) {
        List<String> fromParts = new ArrayList<>();
        List<ModelTable> tables = new ArrayList<>(requiredTables);

        if (tables.isEmpty()) {
            return fromParts;
        }

        ModelTable firstTable = tables.get(0);
        fromParts.add(firstTable.getTableMetadata().getTableName() + " " + firstTable.getAlias());

        Set<ModelTable> joinedTables = new HashSet<>();
        joinedTables.add(firstTable);

        List<ModelRelation> relations = dataModel.getModelRelations().stream()
                .filter(ModelRelation::getIsEnabled)
                .sorted(Comparator.comparing(ModelRelation::getWeight))
                .collect(Collectors.toList());

        boolean added = true;
        while (added) {
            added = false;
            for (ModelRelation relation : relations) {
                ModelTable left = relation.getLeftTable();
                ModelTable right = relation.getRightTable();

                if (!requiredTables.contains(left) && !requiredTables.contains(right)) {
                    continue;
                }

                if (joinedTables.contains(left) && joinedTables.contains(right)) {
                    continue;
                }

                if (joinedTables.contains(left) && requiredTables.contains(right)) {
                    String joinClause = buildJoinClause(relation, right);
                    fromParts.add(joinClause);
                    joinedTables.add(right);
                    added = true;
                } else if (joinedTables.contains(right) && requiredTables.contains(left)) {
                    String joinClause = buildJoinClause(relation, left);
                    fromParts.add(joinClause);
                    joinedTables.add(left);
                    added = true;
                }
            }
        }

        for (ModelTable table : tables) {
            if (!joinedTables.contains(table)) {
                fromParts.add(", " + table.getTableMetadata().getTableName() + " " + table.getAlias());
            }
        }

        return fromParts;
    }

    private String buildJoinClause(ModelRelation relation, ModelTable table) {
        String joinType = relation.getJoinType() != null ? relation.getJoinType() + " JOIN" : "INNER JOIN";
        String tableName = table.getTableMetadata().getTableName() + " " + table.getAlias();
        String onClause = relation.getLeftTable().getAlias() + "." + relation.getLeftColumn() +
                " = " + relation.getRightTable().getAlias() + "." + relation.getRightColumn();
        return joinType + " " + tableName + " ON " + onClause;
    }

    private List<String> buildWhereClause(QueryRequest request, List<Object> parameters) {
        List<String> whereParts = new ArrayList<>();

        if (CollectionUtils.isEmpty(request.getFilters())) {
            return whereParts;
        }

        for (FilterCondition filter : request.getFilters()) {
            StringBuilder condition = new StringBuilder();

            if (filter.getExpression() != null && !filter.getExpression().trim().isEmpty()) {
                condition.append(filter.getExpression());
                whereParts.add(condition.toString());
                continue;
            }

            String column = filter.getTableAlias() + "." + filter.getColumnName();
            String operator = filter.getOperator() != null ? filter.getOperator().toUpperCase() : "=";

            switch (operator) {
                case "=":
                case "!=":
                case "<>":
                case ">":
                case "<":
                case ">=":
                case "<=":
                    condition.append(column).append(" ").append(operator).append(" ?");
                    parameters.add(filter.getValue());
                    break;
                case "LIKE":
                    condition.append(column).append(" LIKE ?");
                    parameters.add("%" + filter.getValue() + "%");
                    break;
                case "IN":
                    condition.append(column).append(" IN (");
                    List<Object> values = filter.getValues();
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) condition.append(", ");
                        condition.append("?");
                        parameters.add(values.get(i));
                    }
                    condition.append(")");
                    break;
                case "NOT IN":
                    condition.append(column).append(" NOT IN (");
                    values = filter.getValues();
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) condition.append(", ");
                        condition.append("?");
                        parameters.add(values.get(i));
                    }
                    condition.append(")");
                    break;
                case "BETWEEN":
                    condition.append(column).append(" BETWEEN ? AND ?");
                    values = filter.getValues();
                    parameters.add(values.get(0));
                    parameters.add(values.get(1));
                    break;
                case "IS NULL":
                    condition.append(column).append(" IS NULL");
                    break;
                case "IS NOT NULL":
                    condition.append(column).append(" IS NOT NULL");
                    break;
                default:
                    condition.append(column).append(" = ?");
                    parameters.add(filter.getValue());
            }

            if (filter.getLogic() != null) {
                condition.insert(0, filter.getLogic() + " ");
            }

            whereParts.add(condition.toString());
        }

        return whereParts;
    }

    private ModelTable findTableByAlias(DataModel dataModel, String alias) {
        if (alias == null || dataModel.getModelTables() == null) {
            return null;
        }
        return dataModel.getModelTables().stream()
                .filter(t -> alias.equals(t.getAlias()))
                .findFirst()
                .orElse(null);
    }
}
