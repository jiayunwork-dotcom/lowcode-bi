package com.lowcode.bi.repository;

import com.lowcode.bi.entity.CalculatedField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalculatedFieldRepository extends JpaRepository<CalculatedField, UUID> {

    @Query("SELECT cf FROM CalculatedField cf WHERE cf.dataModel.id = :dataModelId AND cf.deleted = false ORDER BY cf.position ASC")
    List<CalculatedField> findByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Query("SELECT cf FROM CalculatedField cf WHERE cf.dataModel.id = :dataModelId AND cf.name = :name AND cf.deleted = false")
    Optional<CalculatedField> findByDataModelIdAndName(@Param("dataModelId") UUID dataModelId, @Param("name") String name);

    @Query("SELECT cf FROM CalculatedField cf WHERE cf.dataModel.id = :dataModelId AND cf.isDimension = true AND cf.deleted = false ORDER BY cf.position ASC")
    List<CalculatedField> findDimensionsByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Query("SELECT cf FROM CalculatedField cf WHERE cf.dataModel.id = :dataModelId AND cf.isMeasure = true AND cf.deleted = false ORDER BY cf.position ASC")
    List<CalculatedField> findMeasuresByDataModelId(@Param("dataModelId") UUID dataModelId);

    boolean existsByDataModelIdAndNameAndDeletedFalse(UUID dataModelId, String name);
}
