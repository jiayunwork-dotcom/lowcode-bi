package com.lowcode.bi.repository;

import com.lowcode.bi.entity.PreAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreAggregationRepository extends JpaRepository<PreAggregation, UUID> {

    List<PreAggregation> findByTenantId(UUID tenantId);

    List<PreAggregation> findByDataModelId(UUID dataModelId);

    Optional<PreAggregation> findByIdAndTenantId(UUID id, UUID tenantId);

    List<PreAggregation> findByIsEnabledTrueAndIsPausedFalse();

    @Query("SELECT pa FROM PreAggregation pa WHERE pa.isEnabled = true AND pa.isPaused = false " +
           "AND pa.dataModel.dataSource.id = :dataSourceId")
    List<PreAggregation> findActiveByDataSourceId(@Param("dataSourceId") UUID dataSourceId);

    @Query("SELECT pa FROM PreAggregation pa WHERE pa.isEnabled = true AND pa.isPaused = false " +
           "AND pa.consecutiveFailures >= :maxFailures")
    List<PreAggregation> findNeedPause(@Param("maxFailures") int maxFailures);

    @Modifying
    @Transactional
    @Query("UPDATE PreAggregation pa SET pa.isPaused = true, pa.pauseReason = :reason " +
           "WHERE pa.id = :id")
    int pausePreAggregation(@Param("id") UUID id, @Param("reason") String reason);

    @Modifying
    @Transactional
    @Query("UPDATE PreAggregation pa SET pa.isPaused = false, pa.pauseReason = null, " +
           "pa.consecutiveFailures = 0 WHERE pa.id = :id")
    int resumePreAggregation(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("DELETE FROM PreAggregation pa WHERE pa.dataModelId = :dataModelId")
    int deleteByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PreAggregation pa WHERE pa.tenantId = :tenantId")
    int deleteByTenantId(@Param("tenantId") UUID tenantId);
}
