package com.lowcode.bi.controller;

import com.lowcode.bi.common.enums.DatabaseType;
import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.TableMetadata;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.DataSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/data-sources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

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
}
