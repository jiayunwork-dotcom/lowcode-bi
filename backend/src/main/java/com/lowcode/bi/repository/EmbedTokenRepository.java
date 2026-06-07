package com.lowcode.bi.repository;

import com.lowcode.bi.entity.EmbedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmbedTokenRepository extends JpaRepository<EmbedToken, UUID> {

    List<EmbedToken> findByTenantId(UUID tenantId);

    List<EmbedToken> findByDashboardId(UUID dashboardId);

    Optional<EmbedToken> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<EmbedToken> findByToken(String token);

    @Query("SELECT et FROM EmbedToken et WHERE et.tenant.id = :tenantId AND et.dashboard.id = :dashboardId " +
           "AND et.isActive = true AND et.deleted = false ORDER BY et.createdAt DESC")
    List<EmbedToken> findActiveByDashboardId(
            @Param("tenantId") UUID tenantId,
            @Param("dashboardId") UUID dashboardId);

    @Query("SELECT et FROM EmbedToken et WHERE et.token = :token AND et.isActive = true " +
           "AND et.expiresAt > :now AND et.deleted = false")
    Optional<EmbedToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE EmbedToken et SET et.isActive = false WHERE et.id = :id AND et.tenant.id = :tenantId")
    int revokeToken(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Modifying
    @Transactional
    @Query("UPDATE EmbedToken et SET et.currentUses = et.currentUses + 1, " +
           "et.lastUsedAt = :now, et.lastUsedIp = :ip WHERE et.id = :id")
    int incrementUsage(@Param("id") UUID id, @Param("now") LocalDateTime now, @Param("ip") String ip);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmbedToken et WHERE et.expiresAt < :cutoffTime")
    int deleteExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmbedToken et WHERE et.dashboardId = :dashboardId")
    int deleteByDashboardId(@Param("dashboardId") UUID dashboardId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmbedToken et WHERE et.tenantId = :tenantId")
    int deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(et) FROM EmbedToken et WHERE et.tenant.id = :tenantId AND et.isActive = true")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
}
