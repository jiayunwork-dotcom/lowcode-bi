package com.lowcode.bi.repository;

import com.lowcode.bi.entity.ModelRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModelRelationRepository extends JpaRepository<ModelRelation, UUID> {

    @Query("SELECT mr FROM ModelRelation mr WHERE mr.dataModel.id = :dataModelId AND mr.deleted = false ORDER BY mr.weight ASC")
    List<ModelRelation> findByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Query("SELECT mr FROM ModelRelation mr WHERE (mr.leftTable.id = :tableId OR mr.rightTable.id = :tableId) AND mr.deleted = false")
    List<ModelRelation> findByTableId(@Param("tableId") UUID tableId);

    @Query("SELECT mr FROM ModelRelation mr WHERE mr.dataModel.id = :dataModelId AND mr.isEnabled = true AND mr.deleted = false ORDER BY mr.weight ASC")
    List<ModelRelation> findEnabledByDataModelId(@Param("dataModelId") UUID dataModelId);
}
