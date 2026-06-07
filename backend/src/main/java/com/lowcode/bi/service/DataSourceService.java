package com.lowcode.bi.service;

import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.TableMetadata;
import com.lowcode.bi.common.enums.DatabaseType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DataSourceService {

    List<DataSource> list();

    DataSource getById(UUID id);

    DataSource create(DataSource dataSource);

    DataSource update(UUID id, DataSource dataSource);

    void delete(UUID id);

    boolean testConnection(DataSource dataSource);

    boolean testConnectionById(UUID id);

    List<TableMetadata> syncMetadata(UUID dataSourceId);

    List<TableMetadata> getTables(UUID dataSourceId);

    TableMetadata getTable(UUID tableId);

    String buildJdbcUrl(DatabaseType type, String host, int port, String database, boolean useSsl);

    String getDriverClassName(DatabaseType type);

    Map<String, Object> uploadCsv(UUID dataSourceId, MultipartFile file);

    void validateCsvFile(MultipartFile file);

    List<Map<String, Object>> previewCsv(MultipartFile file, int limit);

    Map<String, Object> getConnectionPoolStatus(UUID tenantId);
}
