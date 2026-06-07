package com.lowcode.bi.entity;

import com.lowcode.bi.common.BaseEntity;
import com.lowcode.bi.common.enums.SystemMessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "system_messages")
public class SystemMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 32, nullable = false)
    private SystemMessageType messageType;

    @Column(name = "title", length = 256, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "related_type", length = 64)
    private String relatedType;

    @Column(name = "related_id")
    private UUID relatedId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
