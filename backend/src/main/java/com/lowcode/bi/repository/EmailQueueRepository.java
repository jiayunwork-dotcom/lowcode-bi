package com.lowcode.bi.repository;

import com.lowcode.bi.entity.EmailQueue;
import com.lowcode.bi.common.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID> {

    @Query("SELECT e FROM EmailQueue e WHERE e.status = :status AND e.retryCount < :maxRetries " +
           "ORDER BY e.priority DESC, e.createdAt ASC")
    List<EmailQueue> findPendingEmails(@Param("status") NotificationStatus status,
                                        @Param("maxRetries") int maxRetries,
                                        Pageable pageable);

    List<EmailQueue> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime cutoffTime);
}
