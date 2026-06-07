package com.lowcode.bi.dto;

import com.lowcode.bi.common.enums.SystemMessageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SystemMessageResponse {
    private UUID id;
    private SystemMessageType messageType;
    private String title;
    private String content;
    private String relatedType;
    private UUID relatedId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private Long unreadCount;
}
