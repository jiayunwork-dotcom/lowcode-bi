package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_tables", uniqueConstraints = {
    @UniqueConstraint(name = "uk_model_table_alias", columnNames = {"data_model_id", "alias"})
})
public class ModelTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_metadata_id", nullable = false)
    private TableMetadata tableMetadata;

    @Column(name = "alias", length = 64, nullable = false)
    private String alias;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "is_fact_table", nullable = false)
    private Boolean isFactTable = false;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "filter_condition", length = 1024)
    private String filterCondition;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "custom_sql", columnDefinition = "text")
    private String customSql;
}
