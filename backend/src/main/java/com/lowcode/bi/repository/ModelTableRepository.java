package com.lowcode.bi.repository;

import com.lowcode.bi.entity.ModelTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelTableRepository extends JpaRepository<ModelTable, UUID> {

    @Query("SELECT mt FROM ModelTable mt WHERE mt.dataModel.id = :dataModelId AND mt.deleted = false")
    List<ModelTable> findByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Query("SELECT mt FROM ModelTable mt WHERE mt.dataModel.id = :dataModelId AND mt.alias = :alias AND mt.deleted = false")
    Optional<ModelTable> findByDataModelIdAndAlias(@Param("dataModelId") UUID dataModelId, @Param("alias") String alias);

    @Query("SELECT mt FROM ModelTable mt WHERE mt.tableMetadata.id = :tableMetadataId AND mt.deleted = false")
    List<ModelTable> findByTableMetadataId(@Param("tableMetadataId") UUID tableMetadataId);

    @Query("SELECT mt FROM ModelTable mt WHERE mt.dataModel.id = :dataModelId AND mt.isFactTable = true AND mt.deleted = false")
    List<ModelTable> findFactTablesByDataModelId(@Param("dataModelId") UUID dataModelId);

    boolean existsByDataModelIdAndAliasAndDeletedFalse(UUID dataModelId, String alias);
}
