package com.lowcode.bi.controller;

import com.lowcode.bi.common.enums.DatabaseType;
import com.lowcode.bi.dto.CsvColumnTypeUpdateRequest;
import com.lowcode.bi.dto.CsvPreviewResponse;
import com.lowcode.bi.dto.CsvRefreshConfigRequest;
import com.lowcode.bi.dto.DataLineageResponse;
import com.lowcode.bi.dto.FileChunkUploadRequest;
import com.lowcode.bi.dto.FileChunkUploadResponse;
import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.TableMetadata;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.CsvRefreshService;
import com.lowcode.bi.service.DataSourceService;
import com.lowcode.bi.service.FileChunkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/data-sources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final FileChunkService fileChunkService;
    private final CsvRefreshService csvRefreshService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<DataSource>> list() {
        return ResponseEntity.ok(dataSourceService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<DataSource> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(dataSourceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<DataSource> create(@Valid @RequestBody DataSource dataSource) {
        return ResponseEntity.ok(dataSourceService.create(dataSource));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<DataSource> update(@PathVariable UUID id, @Valid @RequestBody DataSource dataSource) {
        return ResponseEntity.ok(dataSourceService.update(id, dataSource));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dataSourceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody DataSource dataSource) {
        boolean success = dataSourceService.testConnection(dataSource);
        Map<String, Object> result = Map.of(
                "success", success,
                "message", success ? "连接成功" : "连接失败"
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> testConnectionById(@PathVariable UUID id) {
        boolean success = dataSourceService.testConnectionById(id);
        Map<String, Object> result = Map.of(
                "success", success,
                "message", success ? "连接成功" : "连接失败"
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/sync-metadata")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<List<TableMetadata>> syncMetadata(@PathVariable UUID id) {
        return ResponseEntity.ok(dataSourceService.syncMetadata(id));
    }

    @GetMapping("/{id}/tables")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<List<TableMetadata>> getTables(@PathVariable UUID id) {
        return ResponseEntity.ok(dataSourceService.getTables(id));
    }

    @GetMapping("/tables/{tableId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<TableMetadata> getTable(@PathVariable UUID tableId) {
        return ResponseEntity.ok(dataSourceService.getTable(tableId));
    }

    @PostMapping("/{id}/upload-csv")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> uploadCsv(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(dataSourceService.uploadCsv(id, file));
    }

    @PostMapping("/preview-csv")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<List<Map<String, Object>>> previewCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(dataSourceService.previewCsv(file, limit));
    }

    @GetMapping("/jdbc-url")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> buildJdbcUrl(
            @RequestParam DatabaseType type,
            @RequestParam String host,
            @RequestParam int port,
            @RequestParam String database,
            @RequestParam(defaultValue = "false") boolean useSsl) {
        String url = dataSourceService.buildJdbcUrl(type, host, port, database, useSsl);
        String driver = dataSourceService.getDriverClassName(type);
        return ResponseEntity.ok(Map.of(
                "jdbcUrl", url,
                "driverClassName", driver
        ));
    }

    @GetMapping("/pool-status")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getConnectionPoolStatus() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(dataSourceService.getConnectionPoolStatus(tenantId));
    }

    @PostMapping("/preview-csv-enhanced")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<CsvPreviewResponse> previewCsvEnhanced(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(dataSourceService.previewCsvEnhanced(file, limit));
    }

    @PostMapping("/update-column-types")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> updateColumnTypes(
            @Valid @RequestBody CsvColumnTypeUpdateRequest request) {
        dataSourceService.updateColumnTypes(
                request.getDataSourceId(),
                request.getTableId(),
                request.getColumnTypes()
        );
        return ResponseEntity.ok(Map.of("success", true, "message", "列类型更新成功"));
    }

    @PostMapping("/upload-chunk")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<FileChunkUploadResponse> uploadChunk(
            @RequestParam("fileId") String fileId,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkNumber") Integer chunkNumber,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("chunkSize") Long chunkSize,
            @RequestParam("totalSize") Long totalSize,
            @RequestParam("file") MultipartFile file) {
        FileChunkUploadRequest request = new FileChunkUploadRequest();
        request.setFileId(fileId);
        request.setFileName(fileName);
        request.setChunkNumber(chunkNumber);
        request.setTotalChunks(totalChunks);
        request.setChunkSize(chunkSize);
        request.setTotalSize(totalSize);
        request.setFile(file);
        return ResponseEntity.ok(fileChunkService.uploadChunk(request));
    }

    @GetMapping("/check-chunk")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> checkChunk(
            @RequestParam("fileId") String fileId,
            @RequestParam("chunkNumber") int chunkNumber) {
        boolean exists = fileChunkService.checkChunkExists(fileId, chunkNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/{id}/merge-chunks")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> mergeChunks(
            @PathVariable UUID id,
            @RequestParam("fileId") String fileId,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("totalSize") long totalSize) {
        UUID tenantId = TenantContext.getTenantId();
        File mergedFile = fileChunkService.mergeChunks(fileId, fileName, totalChunks, tenantId);
        Map<String, Object> result = dataSourceService.uploadCsvFromMergedFile(
                id, mergedFile, fileName, totalSize);
        fileChunkService.cleanupChunks(fileId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/configure-refresh")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> configureRefresh(
            @Valid @RequestBody CsvRefreshConfigRequest request) {
        csvRefreshService.configureRefresh(
                request.getDataSourceId(),
                request.getRefreshInterval(),
                request.getRefreshDirectory()
        );
        return ResponseEntity.ok(Map.of("success", true, "message", "刷新配置保存成功"));
    }

    @PostMapping("/{id}/refresh-now")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> triggerManualRefresh(@PathVariable UUID id) {
        csvRefreshService.triggerManualRefresh(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "刷新任务已启动"));
    }

    @GetMapping("/{id}/lineage")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<DataLineageResponse> getDataLineage(@PathVariable UUID id) {
        return ResponseEntity.ok(dataSourceService.getDataLineage(id));
    }
}
