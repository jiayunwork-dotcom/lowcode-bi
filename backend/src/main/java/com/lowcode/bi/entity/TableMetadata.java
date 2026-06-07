package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.ColumnDataType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "table_metadata")
public class TableMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSource dataSource;

    @Column(name = "schema_name", length = 128)
    private String schemaName;

    @Column(name = "table_name", length = 128, nullable = false)
    private String tableName;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "is_view", nullable = false)
    private Boolean isView = false;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ColumnMetadata> columns = new ArrayList<>();

    @Column(name = "refresh_strategy", length = 32)
    private String refreshStrategy = "MANUAL";

    @Column(name = "last_refreshed_at")
    private java.time.LocalDateTime lastRefreshedAt;

    @Column(name = "sync_config", columnDefinition = "json")
    private String syncConfig;
}
