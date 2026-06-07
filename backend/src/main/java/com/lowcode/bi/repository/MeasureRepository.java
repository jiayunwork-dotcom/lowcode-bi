package com.lowcode.bi.repository;

import com.lowcode.bi.entity.Measure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeasureRepository extends JpaRepository<Measure, UUID> {

    @Query("SELECT m FROM Measure m WHERE m.dataModel.id = :dataModelId AND m.deleted = false ORDER BY m.position ASC")
    List<Measure> findByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Query("SELECT m FROM Measure m WHERE m.dataModel.id = :dataModelId AND m.name = :name AND m.deleted = false")
    Optional<Measure> findByDataModelIdAndName(@Param("dataModelId") UUID dataModelId, @Param("name") String name);

    @Query("SELECT m FROM Measure m WHERE m.dataModel.id = :dataModelId AND m.isPreAggregated = true AND m.deleted = false")
    List<Measure> findPreAggregatedByDataModelId(@Param("dataModelId") UUID dataModelId);

    boolean existsByDataModelIdAndNameAndDeletedFalse(UUID dataModelId, String name);

    @Query("SELECT m FROM Measure m WHERE m.id = :id AND m.dataModel.tenant.id = :tenantId AND m.deleted = false")
    Optional<Measure> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
