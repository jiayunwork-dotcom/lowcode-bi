package com.lowcode.bi.controller;

import com.lowcode.bi.common.enums.SystemMessageType;
import com.lowcode.bi.dto.SystemMessageResponse;
import com.lowcode.bi.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class SystemMessageController {

    private final SystemMessageService systemMessageService;

    @GetMapping
    public ResponseEntity<Page<SystemMessageResponse>> getMessages(
            @RequestParam(required = false) SystemMessageType type,
            @RequestParam(required = false) Boolean isRead,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(systemMessageService.getMessages(type, isRead, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemMessageResponse> getMessage(@PathVariable UUID id) {
        return ResponseEntity.ok(systemMessageService.getMessage(id));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        systemMessageService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        systemMessageService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", systemMessageService.getUnreadCount()));
    }
}
