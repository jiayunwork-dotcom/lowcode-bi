package com.lowcode.bi.service;

import com.lowcode.bi.common.enums.SystemMessageType;
import com.lowcode.bi.dto.SystemMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SystemMessageService {
    Page<SystemMessageResponse> getMessages(SystemMessageType type, Boolean isRead, Pageable pageable);
    SystemMessageResponse getMessage(UUID id);
    void markAsRead(UUID id);
    void markAllAsRead();
    long getUnreadCount();
}
