package com.lowcode.bi.repository;

import com.lowcode.bi.entity.QueryCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryCacheRepository extends JpaRepository<QueryCache, UUID> {

    Optional<QueryCache> findByCacheKey(String cacheKey);

    Optional<QueryCache> findByCacheKeyAndExpireAtAfter(String cacheKey, LocalDateTime now);

    @Query("SELECT qc FROM QueryCache qc WHERE qc.tenantId = :tenantId AND qc.userId = :userId AND qc.cacheKey = :cacheKey AND qc.expireAt > :now")
    Optional<QueryCache> findValidCache(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                                        @Param("cacheKey") String cacheKey, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryCache qc WHERE qc.expireAt <= :now")
    int deleteExpiredCache(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryCache qc WHERE qc.dataSourceId = :dataSourceId")
    int deleteByDataSourceId(@Param("dataSourceId") UUID dataSourceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryCache qc WHERE qc.dataModelId = :dataModelId")
    int deleteByDataModelId(@Param("dataModelId") UUID dataModelId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryCache qc WHERE qc.dashboardId = :dashboardId")
    int deleteByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryCache qc WHERE qc.tenantId = :tenantId")
    int deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(qc) FROM QueryCache qc WHERE qc.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
