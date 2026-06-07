package com.lowcode.bi.repository;

import com.lowcode.bi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndTenantIdAndDeletedFalse(String username, UUID tenantId);

    Optional<User> findByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.deleted = false")
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.role = 'TENANT_ADMIN' AND u.deleted = false")
    List<User> findTenantAdmins(@Param("tenantId") UUID tenantId);

    boolean existsByUsernameAndTenantIdAndDeletedFalse(String username, UUID tenantId);

    boolean existsByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.tenant.id = :tenantId AND u.deleted = false")
    Optional<User> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
