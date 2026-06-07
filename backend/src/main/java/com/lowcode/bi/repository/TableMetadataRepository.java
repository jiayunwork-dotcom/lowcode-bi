package com.lowcode.bi.repository;

import com.lowcode.bi.entity.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, UUID> {

    @Query("SELECT t FROM TableMetadata t WHERE t.dataSource.id = :dataSourceId AND t.deleted = false")
    List<TableMetadata> findByDataSourceId(@Param("dataSourceId") UUID dataSourceId);

    @Query("SELECT t FROM TableMetadata t WHERE t.dataSource.id = :dataSourceId AND t.schemaName = :schema AND t.tableName = :table AND t.deleted = false")
    Optional<TableMetadata> findByDataSourceAndSchemaAndTable(
            @Param("dataSourceId") UUID dataSourceId,
            @Param("schema") String schemaName,
            @Param("table") String tableName);

    @Query("SELECT t FROM TableMetadata t WHERE t.dataSource.id = :dataSourceId AND t.isView = :isView AND t.deleted = false")
    List<TableMetadata> findByDataSourceIdAndIsView(@Param("dataSourceId") UUID dataSourceId, @Param("isView") boolean isView);
}
