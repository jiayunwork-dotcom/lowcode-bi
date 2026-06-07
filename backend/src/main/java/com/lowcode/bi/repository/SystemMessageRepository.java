package com.lowcode.bi.repository;

import com.lowcode.bi.entity.SystemMessage;
import com.lowcode.bi.common.enums.SystemMessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemMessageRepository extends JpaRepository<SystemMessage, UUID> {

    Optional<SystemMessage> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<SystemMessage> findByUserIdAndTenantId(UUID userId, UUID tenantId, Pageable pageable);

    Page<SystemMessage> findByUserIdAndTenantIdAndIsRead(UUID userId, UUID tenantId, boolean isRead, Pageable pageable);

    Page<SystemMessage> findByUserIdAndTenantIdAndMessageType(UUID userId, UUID tenantId, SystemMessageType type, Pageable pageable);

    long countByUserIdAndTenantIdAndIsReadFalse(UUID userId, UUID tenantId);

    List<SystemMessage> findByUserIdAndTenantIdAndIsReadFalse(UUID userId, UUID tenantId);
}
