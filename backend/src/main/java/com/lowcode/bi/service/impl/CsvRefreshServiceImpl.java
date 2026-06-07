package com.lowcode.bi.service.impl;

import com.lowcode.bi.common.enums.ColumnDataType;
import com.lowcode.bi.common.enums.DatabaseType;
import com.lowcode.bi.common.enums.RefreshInterval;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.ColumnMetadata;
import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.TableMetadata;
import com.lowcode.bi.entity.Tenant;
import com.lowcode.bi.repository.ColumnMetadataRepository;
import com.lowcode.bi.repository.DataSourceRepository;
import com.lowcode.bi.repository.TableMetadataRepository;
import com.lowcode.bi.repository.TenantRepository;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.CsvRefreshService;
import com.lowcode.bi.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvRefreshServiceImpl implements CsvRefreshService {

    private final DataSourceRepository dataSourceRepository;
    private final TableMetadataRepository tableMetadataRepository;
    private final ColumnMetadataRepository columnMetadataRepository;
    private final TenantRepository tenantRepository;
    private final DataSourceService dataSourceService;

    @Override
    @Transactional
    public void configureRefresh(UUID dataSourceId, RefreshInterval interval, String directory) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        if (dataSource.getDatabaseType() != DatabaseType.CSV) {
            throw new BusinessException("只有CSV类型数据源支持自动刷新");
        }

        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new BusinessException("目录不存在或不是有效目录");
        }

        dataSource.setCsvRefreshInterval(interval);
        dataSource.setCsvRefreshDirectory(directory);
        dataSourceRepository.save(dataSource);

        log.info("CSV自动刷新配置成功: dataSourceId={}, interval={}, directory={}",
                dataSourceId, interval, directory);
    }

    @Override
    @Async
    @Transactional
    public void executeRefresh(UUID dataSourceId) {
        UUID tenantId = TenantContext.getTenantId();
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(dataSourceId, tenantId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        if (dataSource.getCsvRefreshInProgress()) {
            log.warn("CSV刷新已在进行中: dataSourceId={}", dataSourceId);
            return;
        }

        try {
            dataSource.setCsvRefreshInProgress(true);
            dataSource.setCsvLastRefreshStatus("REFRESHING");
            dataSourceRepository.save(dataSource);

            checkAndRefreshCsvFile(dataSource);

            dataSource.setCsvLastRefreshStatus("SUCCESS");
            dataSource.setCsvLastRefreshError(null);
            dataSource.setCsvLastImportTime(LocalDateTime.now());

            log.info("CSV刷新成功: dataSourceId={}", dataSourceId);

        } catch (Exception e) {
            log.error("CSV刷新失败: dataSourceId={}", dataSourceId, e);
            dataSource.setCsvLastRefreshStatus("FAILED");
            dataSource.setCsvLastRefreshError(e.getMessage());
            throw new BusinessException("CSV刷新失败: " + e.getMessage());
        } finally {
            dataSource.setCsvRefreshInProgress(false);
            dataSourceRepository.save(dataSource);
        }
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void executeAllDueRefreshes() {
        log.debug("开始检查需要自动刷新的CSV数据源");

        List<DataSource> csvDataSources = dataSourceRepository.findByDatabaseType(DatabaseType.CSV);

        for (DataSource dataSource : csvDataSources) {
            if (dataSource.getCsvRefreshInterval() == null
                    || dataSource.getCsvRefreshInterval() == RefreshInterval.OFF
                    || dataSource.getCsvRefreshInterval() == RefreshInterval.MANUAL) {
                continue;
            }

            if (dataSource.getCsvRefreshInProgress()) {
                continue;
            }

            try {
                TenantContext.setTenantId(dataSource.getTenant().getId());
                checkAndRefreshCsvFile(dataSource);
            } catch (Exception e) {
                log.error("自动刷新CSV失败: dataSourceId={}", dataSource.getId(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Override
    public void triggerManualRefresh(UUID dataSourceId) {
        executeRefresh(dataSourceId);
    }

    @Override
    @Transactional
    public void checkAndRefreshCsvFile(DataSource dataSource) {
        dataSource.setCsvRefreshInProgress(true);
        dataSource.setCsvLastRefreshStatus("REFRESHING");
        dataSourceRepository.save(dataSource);

        try {
            String refreshDirectory = dataSource.getCsvRefreshDirectory();
            String csvFileName = dataSource.getCsvFileName();

            if (refreshDirectory == null || csvFileName == null) {
                throw new BusinessException("刷新目录或文件名未配置");
            }

            Path filePath = Paths.get(refreshDirectory, csvFileName);
            if (!Files.exists(filePath)) {
                throw new BusinessException("CSV文件不存在: " + filePath);
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            LocalDateTime fileModifiedTime = LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(),
                    ZoneId.systemDefault()
            );

            LocalDateTime lastImportTime = dataSource.getCsvLastImportTime();
            if (lastImportTime != null && !fileModifiedTime.isAfter(lastImportTime)) {
                log.debug("CSV文件未修改，跳过刷新: dataSourceId={}", dataSource.getId());
                dataSource.setCsvLastRefreshStatus("SUCCESS");
                return;
            }

            log.info("检测到CSV文件更新，开始刷新: dataSourceId={}, file={}",
                    dataSource.getId(), filePath);

            File csvFile = filePath.toFile();
            processCsvFile(csvFile, dataSource);

            dataSource.setCsvLastImportTime(LocalDateTime.now());
            dataSource.setCsvLastRefreshStatus("SUCCESS");
            dataSource.setCsvLastRefreshError(null);
            dataSource.setCsvFilePath(filePath.toString());
            dataSource.setCsvFileSize(csvFile.length());

            log.info("CSV刷新完成: dataSourceId={}", dataSource.getId());

        } catch (Exception e) {
            log.error("CSV刷新失败: dataSourceId={}", dataSource.getId(), e);
            dataSource.setCsvLastRefreshStatus("FAILED");
            dataSource.setCsvLastRefreshError(e.getMessage());
            throw new BusinessException("CSV刷新失败: " + e.getMessage());
        } finally {
            dataSource.setCsvRefreshInProgress(false);
            dataSourceRepository.save(dataSource);
        }
    }

    private void processCsvFile(File csvFile, DataSource dataSource) throws IOException {
        byte[] bytes = Files.readAllBytes(csvFile.toPath());
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

            for (int i = 0; i < headers.size(); i++) {
                columnTypes.add(ColumnDataType.STRING);
            }

            int count = 0;
            for (CSVRecord record : parser) {
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

        String sanitizedFileName = csvFile.getName();
        TableMetadata table = tableMetadataRepository
                .findByDataSourceAndSchemaAndTable(dataSource.getId(), null, sanitizedFileName)
                .orElse(new TableMetadata());

        table.setDataSource(dataSource);
        table.setSchemaName(null);
        table.setTableName(sanitizedFileName);
        table.setDisplayName(csvFile.getName());
        table.setDescription("CSV文件自动刷新: " + csvFile.getName());
        table.setIsView(false);
        table.setRowCount((long) rows.size());
        table.setSizeBytes(csvFile.length());
        table.setLastRefreshedAt(LocalDateTime.now());

        TableMetadata savedTable = tableMetadataRepository.save(table);

        List<ColumnMetadata> existingColumns = columnMetadataRepository.findByTableId(savedTable.getId());
        columnMetadataRepository.deleteAll(existingColumns);

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
        }

        try {
            Long.parseLong(trimmed);
            return ColumnDataType.LONG;
        } catch (NumberFormatException e) {
        }

        try {
            Double.parseDouble(trimmed);
            return ColumnDataType.DOUBLE;
        } catch (NumberFormatException e) {
        }

        try {
            java.time.LocalDate.parse(trimmed);
            return ColumnDataType.DATE;
        } catch (Exception e) {
        }

        try {
            java.time.LocalDateTime.parse(trimmed);
            return ColumnDataType.DATETIME;
        } catch (Exception e) {
        }

        return ColumnDataType.STRING;
    }

    private boolean isNumericType(ColumnDataType dataType) {
        return dataType == ColumnDataType.INTEGER
                || dataType == ColumnDataType.LONG
                || dataType == ColumnDataType.DOUBLE
                || dataType == ColumnDataType.DECIMAL;
    }
}
