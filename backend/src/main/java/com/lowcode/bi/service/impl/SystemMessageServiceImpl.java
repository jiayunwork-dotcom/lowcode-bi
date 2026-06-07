package com.lowcode.bi.service.impl;

import com.lowcode.bi.common.enums.SystemMessageType;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.dto.SystemMessageResponse;
import com.lowcode.bi.entity.SystemMessage;
import com.lowcode.bi.repository.SystemMessageRepository;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMessageServiceImpl implements SystemMessageService {

    private final SystemMessageRepository systemMessageRepository;

    @Override
    public Page<SystemMessageResponse> getMessages(SystemMessageType type, Boolean isRead, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SystemMessage> messages;
        if (isRead != null) {
            messages = systemMessageRepository.findByUserIdAndTenantIdAndIsRead(
                userId, tenantId, isRead, sortedPageable);
        } else if (type != null) {
            messages = systemMessageRepository.findByUserIdAndTenantIdAndMessageType(
                userId, tenantId, type, sortedPageable);
        } else {
            messages = systemMessageRepository.findByUserIdAndTenantId(
                userId, tenantId, sortedPageable);
        }

        long unreadCount = getUnreadCount();
        return messages.map(msg -> toResponse(msg, unreadCount));
    }

    @Override
    public SystemMessageResponse getMessage(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        SystemMessage message = systemMessageRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("消息不存在"));

        if (!message.getUser().getId().equals(userId)) {
            throw new BusinessException("无权限访问此消息");
        }

        long unreadCount = getUnreadCount();
        return toResponse(message, unreadCount);
    }

    @Override
    @Transactional
    public void markAsRead(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        SystemMessage message = systemMessageRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new BusinessException("消息不存在"));

        if (!message.getUser().getId().equals(userId)) {
            throw new BusinessException("无权限操作此消息");
        }

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            systemMessageRepository.save(message);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        var unread = systemMessageRepository.findByUserIdAndTenantIdAndIsReadFalse(
            userId, tenantId);

        for (SystemMessage msg : unread) {
            msg.setIsRead(true);
            msg.setReadAt(LocalDateTime.now());
            systemMessageRepository.save(msg);
        }
    }

    @Override
    public long getUnreadCount() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        return systemMessageRepository.countByUserIdAndTenantIdAndIsReadFalse(userId, tenantId);
    }

    private SystemMessageResponse toResponse(SystemMessage message, long unreadCount) {
        SystemMessageResponse response = new SystemMessageResponse();
        response.setId(message.getId());
        response.setMessageType(message.getMessageType());
        response.setTitle(message.getTitle());
        response.setContent(message.getContent());
        response.setRelatedType(message.getRelatedType());
        response.setRelatedId(message.getRelatedId());
        response.setIsRead(message.getIsRead());
        response.setReadAt(message.getReadAt());
        response.setCreatedAt(message.getCreatedAt());
        response.setUnreadCount(unreadCount);
        return response;
    }
}
