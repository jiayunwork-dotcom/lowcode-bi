package com.lowcode.bi.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.enums.DatabaseType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.config.DataSourcePoolConfig;
import com.lowcode.bi.config.EncryptionConfig;
import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.QueryCache;
import com.lowcode.bi.repository.DataSourceRepository;
import com.lowcode.bi.repository.QueryCacheRepository;
import com.lowcode.bi.security.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlQueryEngine {

    private final DataSourceRepository dataSourceRepository;
    private final DataSourcePoolConfig poolConfig;
    private final QueryCacheRepository queryCacheRepository;
    private final EncryptionConfig encryptionConfig;
    private final SqlSecurityValidator sqlSecurityValidator;
    private final Executor queryExecutor;

    @Value("${app.query.timeout:60}")
    private int defaultQueryTimeout;

    @Value("${app.query.cache.default-ttl:300}")
    private int defaultCacheTtl;

    @Value("${app.query.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.query.max-rows:10000}")
    private int defaultMaxRows;

    @Value("${app.query.sample-threshold:5000}")
    private int sampleThreshold;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class QueryRequest {
        private UUID dataSourceId;
        private String sql;
        private Map<String, Object> parameters;
        private Integer maxRows;
        private Integer timeout;
        private boolean useCache = true;
        private Integer cacheTtl;
        private UUID dataModelId;
        private UUID dashboardId;
        private UUID componentId;
        private boolean applyRowPermissions = true;
        private String rowPermissionOverride;

        public UUID getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(UUID dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public Integer getMaxRows() { return maxRows; }
        public void setMaxRows(Integer maxRows) { this.maxRows = maxRows; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public boolean isUseCache() { return useCache; }
        public void setUseCache(boolean useCache) { this.useCache = useCache; }
        public Integer getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(Integer cacheTtl) { this.cacheTtl = cacheTtl; }
        public UUID getDataModelId() { return dataModelId; }
        public void setDataModelId(UUID dataModelId) { this.dataModelId = dataModelId; }
        public UUID getDashboardId() { return dashboardId; }
        public void setDashboardId(UUID dashboardId) { this.dashboardId = dashboardId; }
        public UUID getComponentId() { return componentId; }
        public void setComponentId(UUID componentId) { this.componentId = componentId; }
        public boolean isApplyRowPermissions() { return applyRowPermissions; }
        public void setApplyRowPermissions(boolean applyRowPermissions) { this.applyRowPermissions = applyRowPermissions; }
        public String getRowPermissionOverride() { return rowPermissionOverride; }
        public void setRowPermissionOverride(String rowPermissionOverride) { this.rowPermissionOverride = rowPermissionOverride; }
    }

    public static class QueryResult {
        private List<String> columns;
        private List<Class<?>> columnTypes;
        private List<Map<String, Object>> rows;
        private long totalRows;
        private boolean truncated;
        private boolean sampled;
        private long queryDurationMs;
        private boolean fromCache;
        private String sqlHash;
        private String errorMessage;

        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public List<Class<?>> getColumnTypes() { return columnTypes; }
        public void setColumnTypes(List<Class<?>> columnTypes) { this.columnTypes = columnTypes; }
        public List<Map<String, Object>> getRows() { return rows; }
        public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
        public long getTotalRows() { return totalRows; }
        public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
        public boolean isTruncated() { return truncated; }
        public void setTruncated(boolean truncated) { this.truncated = truncated; }
        public boolean isSampled() { return sampled; }
        public void setSampled(boolean sampled) { this.sampled = sampled; }
        public long getQueryDurationMs() { return queryDurationMs; }
        public void setQueryDurationMs(long queryDurationMs) { this.queryDurationMs = queryDurationMs; }
        public boolean isFromCache() { return fromCache; }
        public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
        public String getSqlHash() { return sqlHash; }
        public void setSqlHash(String sqlHash) { this.sqlHash = sqlHash; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public QueryResult executeQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(request.getDataSourceId(), tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        String processedSql = request.getSql();
        if (processedSql == null || processedSql.trim().isEmpty()) {
            throw new BusinessException("SQL语句不能为空");
        }

        processedSql = sqlSecurityValidator.validateAndSanitize(processedSql);

        sqlSecurityValidator.validateForbiddenOperations(processedSql);

        if (request.isApplyRowPermissions()) {
            processedSql = applyRowPermissions(processedSql, request.getRowPermissionOverride());
        }

        processedSql = applyParameterization(processedSql, request.getParameters());

        String sqlHash = calculateSqlHash(processedSql, request.getParameters(), tenantId, userId);

        if (cacheEnabled && request.isUseCache()) {
            QueryResult cachedResult = tryGetFromCache(sqlHash, tenantId, userId);
            if (cachedResult != null) {
                cachedResult.setFromCache(true);
                return cachedResult;
            }
        }

        String dataSourceKey = "datasource_" + dataSource.getId();
        HikariDataSource hikariDataSource = poolConfig.getDataSource(dataSourceKey);

        if (hikariDataSource == null) {
            initializeDataSource(dataSource, dataSourceKey);
            hikariDataSource = poolConfig.getDataSource(dataSourceKey);
        }

        QueryResult result = new QueryResult();
        result.setSqlHash(sqlHash);

        try {
            poolConfig.acquireConnection(dataSourceKey, tenantId);

            try (Connection conn = hikariDataSource.getConnection()) {
                int timeout = request.getTimeout() != null ? request.getTimeout() : defaultQueryTimeout;
                int maxRows = request.getMaxRows() != null ? request.getMaxRows() : defaultMaxRows;

                try (Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout(timeout);
                    stmt.setFetchSize(1000);
                    stmt.setMaxRows(maxRows + 1);

                    try (ResultSet rs = stmt.executeQuery(processedSql)) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        List<String> columns = new ArrayList<>();
                        List<Class<?>> columnTypes = new ArrayList<>();

                        for (int i = 1; i <= columnCount; i++) {
                            columns.add(metaData.getColumnLabel(i));
                            columnTypes.add(getColumnClass(metaData.getColumnType(i)));
                        }

                        result.setColumns(columns);
                        result.setColumnTypes(columnTypes);

                        List<Map<String, Object>> rows = new ArrayList<>();
                        int rowCount = 0;
                        boolean truncated = false;

                        while (rs.next()) {
                            if (rowCount >= maxRows) {
                                truncated = true;
                                break;
                            }

                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = rs.getObject(i);
                                row.put(columns.get(i - 1), value);
                            }
                            rows.add(row);
                            rowCount++;
                        }

                        boolean sampled = rowCount > sampleThreshold && dataSource.getDatabaseType() == DatabaseType.CLICKHOUSE;
                        if (sampled) {
                            rows = sampleData(rows, sampleThreshold);
                        }

                        result.setRows(rows);
                        result.setTotalRows(rowCount);
                        result.setTruncated(truncated);
                        result.setSampled(sampled);
                    }
                }
            } catch (SQLTimeoutException e) {
                throw new BusinessException("查询超时，已超过" + timeout + "秒限制");
            } catch (SQLException e) {
                log.error("Query execution error: {}", e.getMessage());
                throw new BusinessException("查询执行失败: " + e.getMessage());
            } finally {
                poolConfig.releaseConnection(dataSourceKey, tenantId);
            }

            long duration = System.currentTimeMillis() - startTime;
            result.setQueryDurationMs(duration);

            if (cacheEnabled && request.isUseCache()) {
                int ttl = request.getCacheTtl() != null ? request.getCacheTtl() : defaultCacheTtl;
                cacheResult(sqlHash, processedSql, request, result, tenantId, userId, ttl);
            }

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("查询被中断");
        }
    }

    public CompletableFuture<QueryResult> executeQueryAsync(QueryRequest request) {
        return CompletableFuture.supplyAsync(() -> executeQuery(request), queryExecutor)
                .orTimeout(request.getTimeout() != null ? request.getTimeout() + 5 : defaultQueryTimeout + 5, TimeUnit.SECONDS);
    }

    public Map<String, Object> explainQuery(UUID dataSourceId, String sql) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        sqlSecurityValidator.validateForbiddenOperations(sql);

        String explainSql;
        switch (dataSource.getDatabaseType()) {
            case POSTGRESQL, MYSQL -> explainSql = "EXPLAIN ANALYZE " + sql;
            case CLICKHOUSE -> explainSql = "EXPLAIN " + sql;
            default -> explainSql = "EXPLAIN " + sql;
        }

        String dataSourceKey = "datasource_" + dataSource.getId();
        HikariDataSource hikariDataSource = poolConfig.getDataSource(dataSourceKey);

        if (hikariDataSource == null) {
            initializeDataSource(dataSource, dataSourceKey);
            hikariDataSource = poolConfig.getDataSource(dataSourceKey);
        }

        List<Map<String, Object>> plan = new ArrayList<>();
        try {
            poolConfig.acquireConnection(dataSourceKey, tenantId);

            try (Connection conn = hikariDataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(explainSql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i));
                }

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columns.get(i - 1), rs.getString(i));
                    }
                    plan.add(row);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("执行计划获取失败: " + e.getMessage());
        } finally {
            poolConfig.releaseConnection(dataSourceKey, tenantId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("databaseType", dataSource.getDatabaseType());
        result.put("originalSql", sql);

        return result;
    }

    public List<Map<String, Object>> getTableNames(UUID dataSourceId) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        String dataSourceKey = "datasource_" + dataSource.getId();
        HikariDataSource hikariDataSource = poolConfig.getDataSource(dataSourceKey);

        if (hikariDataSource == null) {
            initializeDataSource(dataSource, dataSourceKey);
            hikariDataSource = poolConfig.getDataSource(dataSourceKey);
        }

        List<Map<String, Object>> tables = new ArrayList<>();
        try {
            poolConfig.acquireConnection(dataSourceKey, tenantId);

            try (Connection conn = hikariDataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();

                try (ResultSet rs = metaData.getTables(
                        dataSource.getDatabaseName(),
                        null,
                        "%",
                        new String[]{"TABLE", "VIEW"})) {

                    while (rs.next()) {
                        Map<String, Object> table = new LinkedHashMap<>();
                        table.put("schema", rs.getString("TABLE_SCHEM"));
                        table.put("name", rs.getString("TABLE_NAME"));
                        table.put("type", rs.getString("TABLE_TYPE"));
                        table.put("remarks", rs.getString("REMARKS"));
                        tables.add(table);
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("获取表列表失败: " + e.getMessage());
        } finally {
            poolConfig.releaseConnection(dataSourceKey, tenantId);
        }

        return tables;
    }

    public List<Map<String, Object>> getColumnNames(UUID dataSourceId, String schemaName, String tableName) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        String dataSourceKey = "datasource_" + dataSource.getId();
        HikariDataSource hikariDataSource = poolConfig.getDataSource(dataSourceKey);

        if (hikariDataSource == null) {
            initializeDataSource(dataSource, dataSourceKey);
            hikariDataSource = poolConfig.getDataSource(dataSourceKey);
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        try {
            poolConfig.acquireConnection(dataSourceKey, tenantId);

            try (Connection conn = hikariDataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();

                try (ResultSet rs = metaData.getColumns(
                        dataSource.getDatabaseName(),
                        schemaName,
                        tableName,
                        "%")) {

                    while (rs.next()) {
                        Map<String, Object> column = new LinkedHashMap<>();
                        column.put("name", rs.getString("COLUMN_NAME"));
                        column.put("type", rs.getString("TYPE_NAME"));
                        column.put("sqlType", rs.getInt("DATA_TYPE"));
                        column.put("size", rs.getInt("COLUMN_SIZE"));
                        column.put("nullable", "YES".equals(rs.getString("IS_NULLABLE")));
                        column.put("remarks", rs.getString("REMARKS"));
                        column.put("defaultValue", rs.getString("COLUMN_DEF"));
                        columns.add(column);
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("获取列列表失败: " + e.getMessage());
        } finally {
            poolConfig.releaseConnection(dataSourceKey, tenantId);
        }

        return columns;
    }

    private String applyRowPermissions(String sql, String override) {
        String filters = override != null ? override : TenantContext.getRowPermissionFilters();

        if (filters == null || filters.trim().isEmpty()) {
            return sql;
        }

        try {
            List<Map<String, Object>> filterRules = objectMapper.readValue(filters,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            if (filterRules.isEmpty()) {
                return sql;
            }

            StringBuilder whereClause = new StringBuilder();
            for (int i = 0; i < filterRules.size(); i++) {
                if (i > 0) {
                    whereClause.append(" AND ");
                }
                Map<String, Object> rule = filterRules.get(i);
                String column = (String) rule.get("column");
                String operator = (String) rule.get("operator");
                Object value = rule.get("value");

                whereClause.append(column).append(" ").append(operator).append(" ");

                if (value instanceof String) {
                    whereClause.append("'").append(escapeSql((String) value)).append("'");
                } else {
                    whereClause.append(value);
                }
            }

            String upperSql = sql.toUpperCase().trim();
            if (upperSql.contains(" WHERE ")) {
                return sql.replaceFirst("(?i) WHERE ", " WHERE " + whereClause + " AND ");
            } else if (upperSql.contains(" GROUP BY ")) {
                return sql.replaceFirst("(?i) GROUP BY ", " WHERE " + whereClause + " GROUP BY ");
            } else if (upperSql.contains(" ORDER BY ")) {
                return sql.replaceFirst("(?i) ORDER BY ", " WHERE " + whereClause + " ORDER BY ");
            } else if (upperSql.contains(" LIMIT ")) {
                return sql.replaceFirst("(?i) LIMIT ", " WHERE " + whereClause + " LIMIT ");
            } else {
                return sql + " WHERE " + whereClause;
            }

        } catch (Exception e) {
            log.warn("Failed to apply row permissions: {}", e.getMessage());
            return sql;
        }
    }

    private String applyParameterization(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }

        String result = sql;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            Object value = entry.getValue();

            String replacement;
            if (value == null) {
                replacement = "NULL";
            } else if (value instanceof String) {
                replacement = "'" + escapeSql((String) value) + "'";
            } else if (value instanceof java.util.Collection) {
                StringBuilder sb = new StringBuilder();
                sb.append("(");
                boolean first = true;
                for (Object item : (java.util.Collection<?>) value) {
                    if (!first) sb.append(",");
                    if (item instanceof String) {
                        sb.append("'").append(escapeSql((String) item)).append("'");
                    } else {
                        sb.append(item);
                    }
                    first = false;
                }
                sb.append(")");
                replacement = sb.toString();
            } else {
                replacement = value.toString();
            }

            result = result.replace(placeholder, replacement);
        }

        return result;
    }

    private String escapeSql(String input) {
        if (input == null) return null;
        return input.replace("'", "''")
                .replace("\\", "\\\\")
                .replace("\0", "");
    }

    private Class<?> getColumnClass(int sqlType) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.NVARCHAR, Types.NCHAR, Types.LONGVARCHAR, Types.CLOB -> String.class;
            case Types.INTEGER, Types.TINYINT, Types.SMALLINT -> Integer.class;
            case Types.BIGINT -> Long.class;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> Double.class;
            case Types.DECIMAL, Types.NUMERIC -> java.math.BigDecimal.class;
            case Types.BIT, Types.BOOLEAN -> Boolean.class;
            case Types.DATE -> java.sql.Date.class;
            case Types.TIME -> java.sql.Time.class;
            case Types.TIMESTAMP -> java.sql.Timestamp.class;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> byte[].class;
            default -> Object.class;
        };
    }

    private List<Map<String, Object>> sampleData(List<Map<String, Object>> rows, int targetSize) {
        if (rows.size() <= targetSize) {
            return rows;
        }

        List<Map<String, Object>> sampled = new ArrayList<>();
        int step = rows.size() / targetSize;

        for (int i = 0; i < rows.size(); i += step) {
            sampled.add(rows.get(i));
        }

        return sampled;
    }

    private QueryResult tryGetFromCache(String cacheKey, UUID tenantId, UUID userId) {
        Optional<QueryCache> cacheOpt = queryCacheRepository.findValidCache(tenantId, userId, cacheKey, LocalDateTime.now());

        if (cacheOpt.isPresent()) {
            QueryCache cache = cacheOpt.get();
            cache.setHitCount(cache.getHitCount() + 1);
            cache.setLastHitAt(LocalDateTime.now());
            queryCacheRepository.save(cache);

            try {
                QueryResult result = new QueryResult();
                result.setColumns(objectMapper.readValue(cache.getResultData(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class))
                        .stream()
                        .map(m -> (String) m.get("name"))
                        .toList());
                result.setRows(objectMapper.readValue(
                        (String) ((Map<?, ?>) objectMapper.readValue(cache.getResultData(), Map.class)).get("rows"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)));
                result.setTotalRows(cache.getRowCount());
                return result;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached result: {}", e.getMessage());
            }
        }

        return null;
    }

    private void cacheResult(String cacheKey, String sql, QueryRequest request,
                             QueryResult result, UUID tenantId, UUID userId, int ttl) {
        try {
            QueryCache cache = new QueryCache();
            cache.setCacheKey(cacheKey);
            cache.setSqlHash(calculateHash(sql));
            cache.setDataSourceId(request.getDataSourceId());
            cache.setTenantId(tenantId);
            cache.setUserId(userId);
            cache.setQuerySql(sql);
            cache.setQueryParams(request.getParameters() != null ?
                    objectMapper.writeValueAsString(request.getParameters()) : null);

            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("columns", result.getColumns().stream()
                    .map(c -> Map.of("name", c))
                    .toList());
            cacheData.put("rows", result.getRows());
            cache.setResultData(objectMapper.writeValueAsString(cacheData));

            cache.setRowCount(result.getTotalRows());
            cache.setColumnCount(result.getColumns().size());
            cache.setDataSizeBytes((long) cache.getResultData().length());
            cache.setQueryDurationMs(result.getQueryDurationMs());
            cache.setExpireAt(LocalDateTime.now().plusSeconds(ttl));
            cache.setDataModelId(request.getDataModelId());
            cache.setDashboardId(request.getDashboardId());
            cache.setComponentId(request.getComponentId());

            queryCacheRepository.save(cache);
        } catch (Exception e) {
            log.warn("Failed to cache query result: {}", e.getMessage());
        }
    }

    private String calculateSqlHash(String sql, Map<String, Object> parameters, UUID tenantId, UUID userId) {
        StringBuilder sb = new StringBuilder();
        sb.append(sql);
        sb.append("|").append(tenantId);
        sb.append("|").append(userId);
        if (parameters != null) {
            try {
                sb.append("|").append(objectMapper.writeValueAsString(parameters));
            } catch (Exception e) {
                sb.append("|").append(parameters.toString());
            }
        }
        return calculateHash(sb.toString());
    }

    private String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hash));
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private void initializeDataSource(DataSource dataSource, String dataSourceKey) {
        if (dataSource.getDatabaseType() == DatabaseType.CSV) {
            return;
        }

        String password = encryptionConfig.decrypt(dataSource.getPasswordEncrypted());
        String jdbcUrl = dataSource.getConnectionUrl() != null ? dataSource.getConnectionUrl() :
                buildJdbcUrl(dataSource.getDatabaseType(), dataSource.getHost(),
                        dataSource.getPort(), dataSource.getDatabaseName(), dataSource.getUseSsl());

        Map<String, String> properties = new HashMap<>();
        if (dataSource.getJdbcProperties() != null) {
            try {
                properties = objectMapper.readValue(dataSource.getJdbcProperties(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            } catch (Exception e) {
                // ignore
            }
        }

        com.zaxxer.hikari.HikariConfig config = poolConfig.createHikariConfig(
                getDriverClassName(dataSource.getDatabaseType()),
                jdbcUrl,
                dataSource.getUsername(),
                password,
                Math.min(dataSource.getMaxPoolSize(), 20),
                dataSource.getMinIdle(),
                dataSource.getConnectionTimeout(),
                dataSource.getReadTimeout(),
                properties
        );

        com.zaxxer.hikari.HikariDataSource hikariDataSource = new com.zaxxer.hikari.HikariDataSource(config);
        poolConfig.registerDataSource(dataSourceKey, hikariDataSource);
    }

    private String buildJdbcUrl(DatabaseType type, String host, Integer port, String database, Boolean useSsl) {
        int actualPort = port != null ? port : getDefaultPort(type);
        boolean actualUseSsl = useSsl != null && useSsl;

        return switch (type) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=%b&serverTimezone=Asia/Shanghai",
                    host, actualPort, database, actualUseSsl);
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=%b",
                    host, actualPort, database, actualUseSsl);
            case CLICKHOUSE -> String.format("jdbc:ch://%s:%d/%s?ssl=%b",
                    host, actualPort, database, actualUseSsl);
            default -> throw new BusinessException("不支持的数据库类型");
        };
    }

    private int getDefaultPort(DatabaseType type) {
        return switch (type) {
            case MYSQL -> 3306;
            case POSTGRESQL -> 5432;
            case CLICKHOUSE -> 8123;
            default -> 5432;
        };
    }

    private String getDriverClassName(DatabaseType type) {
        return switch (type) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
            case CLICKHOUSE -> "com.clickhouse.jdbc.ClickHouseDriver";
            default -> "org.postgresql.Driver";
        };
    }

    public void invalidateCache(UUID dataSourceId) {
        queryCacheRepository.deleteByDataSourceId(dataSourceId);
    }

    public void invalidateCacheByDashboard(UUID dashboardId) {
        queryCacheRepository.deleteByDashboardId(dashboardId);
    }

    public void invalidateCacheByDataModel(UUID dataModelId) {
        queryCacheRepository.deleteByDataModelId(dataModelId);
    }
}
