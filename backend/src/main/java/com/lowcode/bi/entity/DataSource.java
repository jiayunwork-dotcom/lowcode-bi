package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.DatabaseType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "data_sources", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_datasource_name", columnNames = {"tenant_id", "name"})
})
public class DataSource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "database_type", length = 32, nullable = false)
    private DatabaseType databaseType;

    @Column(name = "host", length = 256)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "database_name", length = 128)
    private String databaseName;

    @Column(name = "username", length = 128)
    private String username;

    @Column(name = "password_encrypted", length = 512)
    private String passwordEncrypted;

    @Column(name = "connection_url", length = 1024)
    private String connectionUrl;

    @Column(name = "jdbc_properties", columnDefinition = "json")
    private String jdbcProperties;

    @Column(name = "csv_file_path", length = 512)
    private String csvFilePath;

    @Column(name = "csv_file_name", length = 256)
    private String csvFileName;

    @Column(name = "csv_file_size")
    private Long csvFileSize;

    @Column(name = "max_pool_size", nullable = false)
    private Integer maxPoolSize = 20;

    @Column(name = "min_idle", nullable = false)
    private Integer minIdle = 5;

    @Column(name = "connection_timeout", nullable = false)
    private Integer connectionTimeout = 30000;

    @Column(name = "read_timeout", nullable = false)
    private Integer readTimeout = 60000;

    @Column(name = "status", length = 32, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "last_test_at")
    private java.time.LocalDateTime lastTestAt;

    @Column(name = "last_test_result", length = 32)
    private String lastTestResult;

    @Column(name = "is_olap_enabled", nullable = false)
    private Boolean isOlapEnabled = false;

    @Column(name = "olap_datasource_id")
    private java.util.UUID olapDatasourceId;

    @Column(name = "use_ssl", nullable = false)
    private Boolean useSsl = false;

    @Column(name = "pool_config", columnDefinition = "json")
    private String poolConfig;

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DataModel> dataModels = new HashSet<>();

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TableMetadata> tables = new HashSet<>();

    @Transient
    private String password;
}
