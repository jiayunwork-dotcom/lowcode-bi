package com.lowcode.bi.repository;

import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.common.enums.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

    @Query("SELECT ds FROM DataSource ds WHERE ds.tenant.id = :tenantId AND ds.deleted = false")
    List<DataSource> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT ds FROM DataSource ds WHERE ds.tenant.id = :tenantId AND ds.id = :id AND ds.deleted = false")
    Optional<DataSource> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT ds FROM DataSource ds WHERE ds.tenant.id = :tenantId AND ds.name = :name AND ds.deleted = false")
    Optional<DataSource> findByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);

    @Query("SELECT ds FROM DataSource ds WHERE ds.tenant.id = :tenantId AND ds.databaseType = :type AND ds.deleted = false")
    List<DataSource> findByTenantIdAndType(@Param("tenantId") UUID tenantId, @Param("type") DatabaseType type);

    @Query("SELECT COUNT(ds) FROM DataSource ds WHERE ds.tenant.id = :tenantId AND ds.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantIdAndDeletedFalse(String name, UUID tenantId);

    @Query("SELECT ds FROM DataSource ds WHERE ds.databaseType = :type AND ds.deleted = false")
    List<DataSource> findByDatabaseType(@Param("type") DatabaseType type);
}
