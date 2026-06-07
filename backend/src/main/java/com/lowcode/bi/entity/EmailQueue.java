package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "email_queue")
public class EmailQueue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "to_email", length = 256, nullable = false)
    private String toEmail;

    @Column(name = "subject", length = 512, nullable = false)
    private String subject;

    @Column(name = "body", columnDefinition = "text", nullable = false)
    private String body;

    @Column(name = "is_html", nullable = false)
    private Boolean isHtml = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
}
