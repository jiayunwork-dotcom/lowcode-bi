package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tenants", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_code", columnNames = {"code"})
})
public class Tenant extends BaseEntity {

    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "status", length = 32, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "max_users")
    private Integer maxUsers = 100;

    @Column(name = "max_data_sources")
    private Integer maxDataSources = 20;

    @Column(name = "max_dashboards")
    private Integer maxDashboards = 100;

    @Column(name = "storage_limit_mb")
    private Long storageLimitMb = 10240L;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "settings", columnDefinition = "json")
    private String settings;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DataSource> dataSources = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Dashboard> dashboards = new HashSet<>();
}
