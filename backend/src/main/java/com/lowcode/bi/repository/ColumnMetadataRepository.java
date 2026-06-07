package com.lowcode.bi.repository;

import com.lowcode.bi.entity.ColumnMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, UUID> {

    @Query("SELECT c FROM ColumnMetadata c WHERE c.table.id = :tableId AND c.deleted = false ORDER BY c.position ASC")
    List<ColumnMetadata> findByTableId(@Param("tableId") UUID tableId);

    @Query("SELECT c FROM ColumnMetadata c WHERE c.table.id = :tableId AND c.columnName = :columnName AND c.deleted = false")
    Optional<ColumnMetadata> findByTableIdAndColumnName(@Param("tableId") UUID tableId, @Param("columnName") String columnName);

    @Query("SELECT c FROM ColumnMetadata c WHERE c.table.id = :tableId AND c.isDimension = true AND c.deleted = false ORDER BY c.position ASC")
    List<ColumnMetadata> findDimensionsByTableId(@Param("tableId") UUID tableId);

    @Query("SELECT c FROM ColumnMetadata c WHERE c.table.id = :tableId AND c.isMeasure = true AND c.deleted = false ORDER BY c.position ASC")
    List<ColumnMetadata> findMeasuresByTableId(@Param("tableId") UUID tableId);

    @Query("SELECT COUNT(c) FROM ColumnMetadata c WHERE c.table.id = :tableId AND c.deleted = false")
    long countByTableId(@Param("tableId") UUID tableId);
}
