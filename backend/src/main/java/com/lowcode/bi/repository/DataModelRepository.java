package com.lowcode.bi.repository;

import com.lowcode.bi.entity.DataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataModelRepository extends JpaRepository<DataModel, UUID> {

    @Query("SELECT dm FROM DataModel dm WHERE dm.tenant.id = :tenantId AND dm.deleted = false")
    List<DataModel> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT dm FROM DataModel dm WHERE dm.tenant.id = :tenantId AND dm.id = :id AND dm.deleted = false")
    Optional<DataModel> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT dm FROM DataModel dm WHERE dm.dataSource.id = :dataSourceId AND dm.deleted = false")
    List<DataModel> findByDataSourceId(@Param("dataSourceId") UUID dataSourceId);

    @Query("SELECT dm FROM DataModel dm WHERE dm.tenant.id = :tenantId AND dm.isTemplate = true AND dm.deleted = false")
    List<DataModel> findTemplatesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT dm FROM DataModel dm WHERE dm.isTemplate = true AND dm.deleted = false")
    List<DataModel> findAllTemplates();

    @Query("SELECT COUNT(dm) FROM DataModel dm WHERE dm.tenant.id = :tenantId AND dm.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantIdAndDeletedFalse(String name, UUID tenantId);
}
