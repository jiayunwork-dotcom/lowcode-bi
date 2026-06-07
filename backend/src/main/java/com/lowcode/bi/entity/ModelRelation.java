package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.RelationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "model_relations")
public class ModelRelation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_model_id", nullable = false)
    private DataModel dataModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_table_id", nullable = false)
    private ModelTable leftTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "right_table_id", nullable = false)
    private ModelTable rightTable;

    @Column(name = "left_column", length = 128, nullable = false)
    private String leftColumn;

    @Column(name = "right_column", length = 128, nullable = false)
    private String rightColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", length = 32, nullable = false)
    private RelationType relationType;

    @Column(name = "join_type", length = 32, nullable = false)
    private String joinType = "INNER";

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "weight", nullable = false)
    private Integer weight = 100;
}
