package com.lowcode.bi.controller;

import com.lowcode.bi.common.enums.AlertEventStatus;
import com.lowcode.bi.common.enums.AlertSeverity;
import com.lowcode.bi.dto.*;
import com.lowcode.bi.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService alertRuleService;

    @GetMapping("/rules")
    public ResponseEntity<Page<AlertRuleResponse>> getRules(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertRuleService.getRules(pageable));
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<AlertRuleResponse> getRule(@PathVariable UUID id) {
        return ResponseEntity.ok(alertRuleService.getRule(id));
    }

    @PostMapping("/rules")
    public ResponseEntity<AlertRuleResponse> createRule(@RequestBody AlertRuleCreateRequest request) {
        return ResponseEntity.ok(alertRuleService.createRule(request));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRuleResponse> updateRule(
            @PathVariable UUID id,
            @RequestBody AlertRuleUpdateRequest request) {
        return ResponseEntity.ok(alertRuleService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        alertRuleService.deleteRule(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rules/{id}/enable")
    public ResponseEntity<AlertRuleResponse> enableRule(@PathVariable UUID id) {
        return ResponseEntity.ok(alertRuleService.enableRule(id));
    }

    @PostMapping("/rules/{id}/disable")
    public ResponseEntity<AlertRuleResponse> disableRule(@PathVariable UUID id) {
        return ResponseEntity.ok(alertRuleService.disableRule(id));
    }

    @GetMapping("/rules/data-model/{dataModelId}")
    public ResponseEntity<List<AlertRuleResponse>> getRulesByDataModel(
            @PathVariable UUID dataModelId) {
        return ResponseEntity.ok(alertRuleService.getRulesByDataModel(dataModelId));
    }

    @GetMapping("/rules/subscribed")
    public ResponseEntity<Page<AlertRuleResponse>> getSubscribedRules(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertRuleService.getSubscribedRules(pageable));
    }

    @PostMapping("/rules/{id}/subscribe")
    public ResponseEntity<Map<String, Boolean>> subscribe(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> request) {
        boolean subscribed = request.getOrDefault("subscribed", true);
        alertRuleService.subscribe(id, subscribed);
        return ResponseEntity.ok(Map.of("subscribed", subscribed));
    }

    @GetMapping("/rules/{id}/subscribed")
    public ResponseEntity<Map<String, Boolean>> isSubscribed(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("subscribed", alertRuleService.isSubscribed(id)));
    }

    @GetMapping("/rules/{id}/events")
    public ResponseEntity<Page<AlertEventResponse>> getEventsByRule(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertRuleService.getEventsByRule(id, pageable));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<AlertEventResponse>> getEvents(
            @RequestParam(required = false) AlertEventStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertRuleService.getEvents(status, severity, pageable));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<AlertEventResponse> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(alertRuleService.getEvent(id));
    }

    @PostMapping("/events/{id}/acknowledge")
    public ResponseEntity<AlertEventResponse> acknowledgeEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(alertRuleService.acknowledgeEvent(id));
    }

    @GetMapping("/statistics")
    public ResponseEntity<AlertStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(alertRuleService.getStatistics());
    }
}
