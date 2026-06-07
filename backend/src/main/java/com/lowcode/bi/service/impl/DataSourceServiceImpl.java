package com.lowcode.bi.service.impl;

import com.lowcode.bi.common.enums.ColumnDataType;
import com.lowcode.bi.common.enums.DatabaseType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.config.DataSourcePoolConfig;
import com.lowcode.bi.config.EncryptionConfig;
import com.lowcode.bi.entity.ColumnMetadata;
import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.TableMetadata;
import com.lowcode.bi.entity.Tenant;
import com.lowcode.bi.repository.ColumnMetadataRepository;
import com.lowcode.bi.repository.DataSourceRepository;
import com.lowcode.bi.repository.TableMetadataRepository;
import com.lowcode.bi.repository.TenantRepository;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.DataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final TableMetadataRepository tableMetadataRepository;
    private final ColumnMetadataRepository columnMetadataRepository;
    private final TenantRepository tenantRepository;
    private final DataSourcePoolConfig poolConfig;
    private final EncryptionConfig encryptionConfig;

    @Value("${app.export.csv.max-columns:500}")
    private int maxCsvColumns;

    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private String maxFileSize;

    private static final Map<String, String> DRIVER_CLASS_MAP = new ConcurrentHashMap<>();

    static {
        DRIVER_CLASS_MAP.put(DatabaseType.MYSQL.name(), "com.mysql.cj.jdbc.Driver");
        DRIVER_CLASS_MAP.put(DatabaseType.POSTGRESQL.name(), "org.postgresql.Driver");
        DRIVER_CLASS_MAP.put(DatabaseType.CLICKHOUSE.name(), "com.clickhouse.jdbc.ClickHouseDriver");
    }

    @Override
    public List<DataSource> list() {
        UUID tenantId = TenantContext.getTenantId();
        return dataSourceRepository.findByTenantId(tenantId);
    }

    @Override
    public DataSource getById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        dataSource.setPassword(null);
        return dataSource;
    }

    @Override
    @Transactional
    public DataSource create(DataSource dataSource) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findByIdAndNotDeleted(tenantId)
                .orElseThrow(() -> new BusinessException("租户不存在"));

        long currentCount = dataSourceRepository.countByTenantId(tenantId);
        if (tenant.getMaxDataSources() != null && currentCount >= tenant.getMaxDataSources()) {
            throw new BusinessException("已达到租户数据源数量上限: " + tenant.getMaxDataSources());
        }

        if (dataSourceRepository.existsByNameAndTenantIdAndDeletedFalse(dataSource.getName(), tenantId)) {
            throw new BusinessException("数据源名称已存在");
        }

        if (dataSource.getDatabaseType() == DatabaseType.CSV) {
            dataSource.setHost(null);
            dataSource.setPort(null);
            dataSource.setUsername(null);
            dataSource.setPasswordEncrypted(null);
        } else {
            if (dataSource.getPassword() != null && !dataSource.getPassword().isEmpty()) {
                dataSource.setPasswordEncrypted(encryptionConfig.encrypt(dataSource.getPassword()));
            }
        }

        dataSource.setTenant(tenant);
        dataSource.setStatus("ACTIVE");
        dataSource.setDeleted(false);

        DataSource saved = dataSourceRepository.save(dataSource);

        if (dataSource.getDatabaseType() != DatabaseType.CSV) {
            try {
                initializeConnectionPool(saved);
            } catch (Exception e) {
                log.warn("Failed to initialize connection pool for datasource: {}", saved.getName(), e);
            }
        }

        saved.setPassword(null);
        return saved;
    }

    @Override
    @Transactional
    public DataSource update(UUID id, DataSource dataSource) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource existing = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        if (!existing.getName().equals(dataSource.getName())
                && dataSourceRepository.existsByNameAndTenantIdAndDeletedFalse(dataSource.getName(), tenantId)) {
            throw new BusinessException("数据源名称已存在");
        }

        existing.setName(dataSource.getName());
        existing.setDescription(dataSource.getDescription());

        if (existing.getDatabaseType() != DatabaseType.CSV) {
            existing.setHost(dataSource.getHost());
            existing.setPort(dataSource.getPort());
            existing.setDatabaseName(dataSource.getDatabaseName());
            existing.setUsername(dataSource.getUsername());
            existing.setUseSsl(dataSource.getUseSsl());

            if (dataSource.getPassword() != null && !dataSource.getPassword().isEmpty()) {
                existing.setPasswordEncrypted(encryptionConfig.encrypt(dataSource.getPassword()));
            }

            existing.setMaxPoolSize(dataSource.getMaxPoolSize());
            existing.setMinIdle(dataSource.getMinIdle());
            existing.setConnectionTimeout(dataSource.getConnectionTimeout());
            existing.setReadTimeout(dataSource.getReadTimeout());
            existing.setJdbcProperties(dataSource.getJdbcProperties());
            existing.setIsOlapEnabled(dataSource.getIsOlapEnabled());
            existing.setOlapDatasourceId(dataSource.getOlapDatasourceId());

            String dataSourceKey = getDataSourceKey(existing.getId());
            poolConfig.unregisterDataSource(dataSourceKey);
            try {
                initializeConnectionPool(existing);
            } catch (Exception e) {
                log.warn("Failed to reinitialize connection pool: {}", e.getMessage());
            }
        }

        DataSource saved = dataSourceRepository.save(existing);
        saved.setPassword(null);
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        String dataSourceKey = getDataSourceKey(id);
        poolConfig.unregisterDataSource(dataSourceKey);

        dataSource.setDeleted(true);
        dataSourceRepository.save(dataSource);
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        if (dataSource.getDatabaseType() == DatabaseType.CSV) {
            return true;
        }

        String jdbcUrl = buildJdbcUrl(
                dataSource.getDatabaseType(),
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDatabaseName(),
                dataSource.getUseSsl()
        );

        String password = dataSource.getPassword();
        if (password == null && dataSource.getPasswordEncrypted() != null) {
            password = encryptionConfig.decrypt(dataSource.getPasswordEncrypted());
        }

        return testJdbcConnection(
                dataSource.getDatabaseType(),
                jdbcUrl,
                dataSource.getUsername(),
                password,
                dataSource.getConnectionTimeout()
        );
    }

    @Override
    public boolean testConnectionById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        return testConnection(dataSource);
    }

    private boolean testJdbcConnection(DatabaseType type, String jdbcUrl, String username, String password, int timeout) {
        Connection connection = null;
        try {
            Class.forName(getDriverClassName(type));
            DriverManager.setLoginTimeout(timeout / 1000);

            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("connectTimeout", String.valueOf(timeout));
            props.setProperty("socketTimeout", String.valueOf(timeout));

            connection = DriverManager.getConnection(jdbcUrl, props);

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    @Override
    @Transactional
    public List<TableMetadata> syncMetadata(UUID dataSourceId) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        if (dataSource.getDatabaseType() == DatabaseType.CSV) {
            return tableMetadataRepository.findByDataSourceId(dataSourceId);
        }

        HikariDataSource hikariDataSource = poolConfig.getDataSource(getDataSourceKey(dataSourceId));
        if (hikariDataSource == null) {
            initializeConnectionPool(dataSource);
            hikariDataSource = poolConfig.getDataSource(getDataSourceKey(dataSourceId));
        }

        List<TableMetadata> tables = new ArrayList<>();

        try (Connection conn = hikariDataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            String catalog = dataSource.getDatabaseName();
            String schemaPattern = null;
            String tableNamePattern = "%";
            String[] types = {"TABLE", "VIEW"};

            try (ResultSet rs = metaData.getTables(catalog, schemaPattern, tableNamePattern, types)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String schemaName = rs.getString("TABLE_SCHEM");
                    String tableType = rs.getString("TABLE_TYPE");
                    String remarks = rs.getString("REMARKS");

                    TableMetadata table = tableMetadataRepository
                            .findByDataSourceAndSchemaAndTable(dataSourceId, schemaName, tableName)
                            .orElse(new TableMetadata());

                    table.setDataSource(dataSource);
                    table.setSchemaName(schemaName);
                    table.setTableName(tableName);
                    table.setDisplayName(tableName);
                    table.setDescription(remarks);
                    table.setIsView("VIEW".equals(tableType));
                    table.setLastRefreshedAt(LocalDateTime.now());

                    TableMetadata savedTable = tableMetadataRepository.save(table);
                    tables.add(savedTable);

                    List<ColumnMetadata> columns = extractColumns(metaData, catalog, schemaName, tableName, savedTable);
                    savedTable.setColumns(columns);
                }
            }

            dataSource.setLastTestAt(LocalDateTime.now());
            dataSource.setLastTestResult("SUCCESS");
            dataSourceRepository.save(dataSource);

        } catch (Exception e) {
            log.error("Failed to sync metadata: {}", e.getMessage());
            throw new BusinessException("同步元数据失败: " + e.getMessage());
        }

        return tables;
    }

    private List<ColumnMetadata> extractColumns(DatabaseMetaData metaData, String catalog,
                                        String schema, String tableName, TableMetadata table) throws SQLException {

        List<ColumnMetadata> columns = new ArrayList<>();
        int position = 0;

        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int dataType = rs.getInt("DATA_TYPE");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                String isNullable = rs.getString("IS_NULLABLE");
                String remarks = rs.getString("REMARKS");
                String defaultValue = rs.getString("COLUMN_DEF");
                boolean isPrimaryKey = false;

                try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                    while (pkRs.next()) {
                        if (columnName.equals(pkRs.getString("COLUMN_NAME"))) {
                            isPrimaryKey = true;
                            break;
                        }
                    }
                }

                ColumnMetadata column = new ColumnMetadata();
                column.setTable(table);
                column.setColumnName(columnName);
                column.setDisplayName(columnName);
                column.setDescription(remarks);
                column.setPosition(position++);
                column.setDataType(mapSqlTypeToDataType(dataType, typeName));
                column.setNativeType(typeName);
                column.setPrecision(columnSize);
                column.setScale(decimalDigits);
                column.setMaxLength(columnSize);
                column.setIsNullable("YES".equals(isNullable));
                column.setIsPrimaryKey(isPrimaryKey);
                column.setDefaultValue(defaultValue);

                boolean isNumericType = isNumericType(column.getDataType());
                column.setIsDimension(!isNumericType);
                column.setIsMeasure(isNumericType);

                columns.add(columnMetadataRepository.save(column));
            }
        }

        return columns;
    }

    private ColumnDataType mapSqlTypeToDataType(int sqlType, String typeName) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.NVARCHAR, Types.NCHAR, Types.LONGVARCHAR, Types.CLOB -> ColumnDataType.STRING;
            case Types.INTEGER, Types.TINYINT, Types.SMALLINT -> ColumnDataType.INTEGER;
            case Types.BIGINT -> ColumnDataType.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> ColumnDataType.DOUBLE;
            case Types.DECIMAL, Types.NUMERIC -> ColumnDataType.DECIMAL;
            case Types.BIT, Types.BOOLEAN -> ColumnDataType.BOOLEAN;
            case Types.DATE -> ColumnDataType.DATE;
            case Types.TIME, Types.TIMESTAMP -> ColumnDataType.DATETIME;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> ColumnDataType.BINARY;
            default -> {
                if (typeName != null && typeName.toLowerCase().contains("json")) {
                    yield ColumnDataType.JSON;
                }
                yield ColumnDataType.STRING;
            }
        };
    }

    private boolean isNumericType(ColumnDataType dataType) {
        return dataType == ColumnDataType.INTEGER
                || dataType == ColumnDataType.LONG
                || dataType == ColumnDataType.DOUBLE
                || dataType == ColumnDataType.DECIMAL;
    }

    @Override
    public List<TableMetadata> getTables(UUID dataSourceId) {
        return tableMetadataRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    public TableMetadata getTable(UUID tableId) {
        return tableMetadataRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException("表不存在"));
    }

    @Override
    public String buildJdbcUrl(DatabaseType type, String host, int port, String database, boolean useSsl) {
        return switch (type) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=%b&serverTimezone=Asia/Shanghai",
                    host, port, database, useSsl);
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=%b",
                    host, port, database, useSsl);
            case CLICKHOUSE -> String.format("jdbc:ch://%s:%d/%s?ssl=%b",
                    host, port, database, useSsl);
            default -> throw new BusinessException("不支持的数据库类型: " + type);
        };
    }

    @Override
    public String getDriverClassName(DatabaseType type) {
        String driver = DRIVER_CLASS_MAP.get(type.name());
        if (driver == null) {
            throw new BusinessException("不支持的数据库类型: " + type);
        }
        return driver;
    }

    private void initializeConnectionPool(DataSource dataSource) {
        if (dataSource.getDatabaseType() == DatabaseType.CSV) {
            return;
        }

        String dataSourceKey = getDataSourceKey(dataSource.getId());

        if (poolConfig.getDataSource(dataSourceKey) != null) {
            return;
        }

        String jdbcUrl = dataSource.getConnectionUrl();
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            jdbcUrl = buildJdbcUrl(
                    dataSource.getDatabaseType(),
                    dataSource.getHost(),
                    dataSource.getPort(),
                    dataSource.getDatabaseName(),
                    dataSource.getUseSsl()
            );
        }

        String password = encryptionConfig.decrypt(dataSource.getPasswordEncrypted());

        Map<String, String> properties = new HashMap<>();
        if (dataSource.getJdbcProperties() != null && !dataSource.getJdbcProperties().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                properties = mapper.readValue(dataSource.getJdbcProperties(), Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse JDBC properties: {}", e.getMessage());
            }
        }

        HikariConfig config = poolConfig.createHikariConfig(
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

        HikariDataSource hikariDataSource = new HikariDataSource(config);
        poolConfig.registerDataSource(dataSourceKey, hikariDataSource);

        log.info("Initialized connection pool for datasource: {}", dataSource.getName());
    }

    private String getDataSourceKey(UUID dataSourceId) {
        return "datasource_" + dataSourceId.toString();
    }

    @Override
    @Transactional
    public Map<String, Object> uploadCsv(UUID dataSourceId, MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        if (dataSource.getDatabaseType() != DatabaseType.CSV) {
            throw new BusinessException("只能上传CSV文件到CSV类型数据源");
        }

        validateCsvFile(file);

        try {
            String fileName = file.getOriginalFilename();
            String sanitizedFileName = sanitizeFileName(fileName);
            Path storageDir = getCsvStoragePath(tenantId, dataSourceId);

            Files.createDirectories(storageDir);

            Path targetPath = storageDir.resolve(sanitizedFileName);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Map<String, Object> previewData = previewCsv(file, 100);

            @SuppressWarnings("unchecked")
            List<String> headers = (List<String>) previewData.get("headers");
            @SuppressWarnings("unchecked")
            List<ColumnDataType> columnTypes = (List<ColumnDataType>) previewData.get("columnTypes");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) previewData.get("rows");

            TableMetadata table = tableMetadataRepository
                    .findByDataSourceAndSchemaAndTable(dataSourceId, null, sanitizedFileName)
                    .orElse(new TableMetadata());

            table.setDataSource(dataSource);
            table.setSchemaName(null);
            table.setTableName(sanitizedFileName);
            table.setDisplayName(fileName);
            table.setDescription("CSV文件上传: " + fileName);
            table.setIsView(false);
            table.setRowCount((long) rows.size());
            table.setSizeBytes(file.getSize());
            table.setLastRefreshedAt(LocalDateTime.now());
            table.setRefreshStrategy("MANUAL");

            TableMetadata savedTable = tableMetadataRepository.save(table);

            List<ColumnMetadata> columns = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                ColumnMetadata column = new ColumnMetadata();
                column.setTable(savedTable);
                column.setColumnName(headers.get(i));
                column.setDisplayName(headers.get(i));
                column.setPosition(i);
                column.setDataType(columnTypes.get(i));
                column.setIsNullable(true);
                column.setIsDimension(!isNumericType(columnTypes.get(i)));
                column.setIsMeasure(isNumericType(columnTypes.get(i)));
                columns.add(columnMetadataRepository.save(column));
            }

            savedTable.setColumns(columns);

            dataSource.setCsvFilePath(targetPath.toString());
            dataSource.setCsvFileName(sanitizedFileName);
            dataSource.setCsvFileSize(file.getSize());
            dataSourceRepository.save(dataSource);

            Map<String, Object> result = new HashMap<>();
            result.put("tableId", savedTable.getId());
            result.put("tableName", sanitizedFileName);
            result.put("rowCount", rows.size());
            result.put("columnCount", headers.size());
            result.put("fileSize", file.getSize());
            result.put("filePath", targetPath.toString());

            return result;

        } catch (IOException e) {
            log.error("CSV upload failed: {}", e.getMessage());
            throw new BusinessException("CSV文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void validateCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("只支持CSV文件");
        }

        if (file.getSize() > 100 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过100MB");
        }
    }

    @Override
    public List<Map<String, Object>> previewCsv(MultipartFile file, int limit) {
        try {
            byte[] bytes = file.getBytes();
            Charset charset = detectCharset(bytes);

            List<String> headers = new ArrayList<>();
            List<ColumnDataType> columnTypes = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();

            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), charset);
                 CSVParser parser = CSVFormat.DEFAULT
                         .builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setIgnoreHeaderCase(true)
                         .setTrim(true)
                         .get()
                         .parse(reader)) {

                headers = parser.getHeaderNames();

                if (headers.size() > maxCsvColumns) {
                    throw new BusinessException("列数过多，请检查文件格式。最大支持" + maxCsvColumns + "列");
                }

                for (int i = 0; i < headers.size(); i++) {
                    columnTypes.add(ColumnDataType.STRING);
                }

                int count = 0;
                for (CSVRecord record : parser) {
                    if (count >= limit) break;

                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String value = record.get(i);
                        row.put(headers.get(i), value);

                        if (count < 20) {
                            ColumnDataType inferredType = inferColumnType(value);
                            if (columnTypes.get(i) == ColumnDataType.STRING && inferredType != ColumnDataType.STRING) {
                                columnTypes.set(i, inferredType);
                            } else if (columnTypes.get(i) == ColumnDataType.INTEGER && inferredType == ColumnDataType.DOUBLE) {
                                columnTypes.set(i, ColumnDataType.DOUBLE);
                            }
                        }
                    }
                    rows.add(row);
                    count++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("headers", headers);
            result.put("columnTypes", columnTypes);
            result.put("rows", rows);
            result.put("charset", charset.name());
            result.put("rowCount", rows.size());
            result.put("columnCount", headers.size());

            return result;

        } catch (Exception e) {
            log.error("CSV preview failed: {}", e.getMessage());
            throw new BusinessException("CSV文件解析失败: " + e.getMessage());
        }
    }

    private Charset detectCharset(byte[] bytes) {
        try {
            Charset utf8 = StandardCharsets.UTF_8;
            String utf8Content = new String(bytes, utf8);
            byte[] reEncoded = utf8Content.getBytes(utf8);

            boolean isUtf8 = java.util.Arrays.equals(bytes, 0, bytes.length, reEncoded, 0, reEncoded.length);

            if (isUtf8) {
                return StandardCharsets.UTF_8;
            }

            try {
                Charset gbk = Charset.forName("GBK");
                String gbkContent = new String(bytes, gbk);
                return gbk;
            } catch (Exception e) {
                return StandardCharsets.UTF_8;
            }
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private ColumnDataType inferColumnType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ColumnDataType.STRING;
        }

        String trimmed = value.trim();

        try {
            Integer.parseInt(trimmed);
            return ColumnDataType.INTEGER;
        } catch (NumberFormatException e) {
            // continue
        }

        try {
            Long.parseLong(trimmed);
            return ColumnDataType.LONG;
        } catch (NumberFormatException e) {
            // continue
        }

        try {
            Double.parseDouble(trimmed);
            return ColumnDataType.DOUBLE;
        } catch (NumberFormatException e) {
            // continue
        }

        try {
            java.time.LocalDate.parse(trimmed);
            return ColumnDataType.DATE;
        } catch (Exception e) {
            // continue
        }

        try {
            java.time.LocalDateTime.parse(trimmed);
            return ColumnDataType.DATETIME;
        } catch (Exception e) {
            // continue
        }

        return ColumnDataType.STRING;
    }

    private Path getCsvStoragePath(UUID tenantId, UUID dataSourceId) throws IOException {
        String baseDir = System.getProperty("user.home") + "/lowcode-bi/csv";
        return Path.of(baseDir, tenantId.toString(), dataSourceId.toString());
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "upload.csv";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Override
    public Map<String, Object> getConnectionPoolStatus(UUID tenantId) {
        DataSourcePoolConfig.TenantConnectionPool pool = poolConfig.getTenantPool(tenantId);

        Map<String, Object> status = new HashMap<>();
        status.put("activeConnections", pool.getActiveConnections());
        status.put("availableConnections", pool.getAvailablePermits());
        status.put("maxConnections", 20);

        List<DataSource> dataSources = dataSourceRepository.findByTenantId(tenantId);
        List<Map<String, Object>> dsStatusList = new ArrayList<>();

        for (DataSource ds : dataSources) {
            if (ds.getDatabaseType() == DatabaseType.CSV) {
                continue;
            }

            String key = getDataSourceKey(ds.getId());
            HikariDataSource hikariDs = poolConfig.getDataSource(key);

            if (hikariDs != null) {
                Map<String, Object> dsStatus = new HashMap<>();
                dsStatus.put("id", ds.getId());
                dsStatus.put("name", ds.getName());
                dsStatus.put("type", ds.getDatabaseType());
                dsStatus.put("activeConnections", hikariDs.getHikariPoolMXBean().getActiveConnections());
                dsStatus.put("idleConnections", hikariDs.getHikariPoolMXBean().getIdleConnections());
                dsStatus.put("totalConnections", hikariDs.getHikariPoolMXBean().getTotalConnections());
                dsStatus.put("threadsAwaitingConnection", hikariDs.getHikariPoolMXBean().getThreadsAwaitingConnection());
                dsStatus.put("status", ds.getStatus());

                dsStatusList.add(dsStatus);
            }
        }

        status.put("dataSources", dsStatusList);

        return status;
    }
}
