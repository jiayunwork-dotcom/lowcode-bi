package com.lowcode.bi.controller;

import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.EmbedToken;
import com.lowcode.bi.service.EmbedService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/embed")
@RequiredArgsConstructor
public class EmbedController {

    private final EmbedService embedService;

    @PostMapping("/token")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> createEmbedToken(@RequestBody Map<String, Object> request) {
        try {
            UUID dashboardId = UUID.fromString((String) request.get("dashboardId"));
            String name = (String) request.get("name");
            String description = (String) request.get("description");

            Integer validitySeconds = request.get("validitySeconds") != null ?
                    ((Number) request.get("validitySeconds")).intValue() : null;

            Boolean hideTitle = (Boolean) request.get("hideTitle");
            Boolean hideToolbar = (Boolean) request.get("hideToolbar");
            Boolean hideFilters = (Boolean) request.get("hideFilters");
            Boolean hideTabs = (Boolean) request.get("hideTabs");
            Boolean enableFullscreen = (Boolean) request.get("enableFullscreen");
            Boolean enableExport = (Boolean) request.get("enableExport");
            Boolean enableDrilldown = (Boolean) request.get("enableDrilldown");
            Boolean enableFilterInteraction = (Boolean) request.get("enableFilterInteraction");

            @SuppressWarnings("unchecked")
            Map<String, Object> defaultFilters = request.get("defaultFilters") != null ?
                    (Map<String, Object>) request.get("defaultFilters") : null;

            String defaultTabId = (String) request.get("defaultTabId");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rowPermissionRules = request.get("rowPermissionRules") != null ?
                    (List<Map<String, Object>>) request.get("rowPermissionRules") : null;

            @SuppressWarnings("unchecked")
            List<String> allowedDomains = request.get("allowedDomains") != null ?
                    (List<String>) request.get("allowedDomains") : null;

            Integer maxUses = request.get("maxUses") != null ?
                    ((Number) request.get("maxUses")).intValue() : null;

            String theme = (String) request.get("theme");
            String locale = (String) request.get("locale");
            String iframeWidth = (String) request.get("iframeWidth");
            String iframeHeight = (String) request.get("iframeHeight");
            String borderStyle = (String) request.get("borderStyle");
            String customCss = (String) request.get("customCss");

            EmbedToken token = embedService.createEmbedToken(
                    dashboardId, name, description,
                    validitySeconds,
                    hideTitle, hideToolbar, hideFilters,
                    hideTabs, enableFullscreen, enableExport,
                    enableDrilldown, enableFilterInteraction,
                    defaultFilters, defaultTabId,
                    rowPermissionRules, allowedDomains, maxUses,
                    theme, locale,
                    iframeWidth, iframeHeight,
                    borderStyle, customCss
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", token);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Create embed token error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "创建嵌入令牌失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/token/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getEmbedToken(@PathVariable UUID id) {
        try {
            EmbedToken token = embedService.getEmbedToken(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", token);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get embed token error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取嵌入令牌失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/dashboard/{dashboardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getEmbedTokensByDashboard(@PathVariable UUID dashboardId) {
        try {
            List<EmbedToken> tokens = embedService.getEmbedTokensByDashboard(dashboardId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tokens);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get embed tokens error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取嵌入令牌列表失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/token")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getEmbedTokensByTenant() {
        try {
            List<EmbedToken> tokens = embedService.getEmbedTokensByTenant();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tokens);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get embed tokens error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取嵌入令牌列表失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/token/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> revokeEmbedToken(@PathVariable UUID id) {
        try {
            embedService.revokeEmbedToken(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "嵌入令牌已撤销");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Revoke embed token error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "撤销嵌入令牌失败: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/token/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> deleteEmbedToken(@PathVariable UUID id) {
        try {
            embedService.deleteEmbedToken(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "嵌入令牌已删除");

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Delete embed token error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "删除嵌入令牌失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/token/{id}/code")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getEmbedCode(@PathVariable UUID id) {
        try {
            String code = embedService.generateEmbedCode(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("code", code);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get embed code error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取嵌入代码失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/token/{id}/url")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getEmbedUrl(@PathVariable UUID id) {
        try {
            String url = embedService.generateEmbedUrl(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("url", url);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get embed url error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取嵌入链接失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/token/{id}/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getEmbedConfig(@PathVariable UUID id) {
        try {
            Map<String, Object> config = embedService.getEmbedConfig(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get embed config error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取嵌入配置失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/token/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public ResponseEntity<?> getTokenStats(@PathVariable UUID id) {
        try {
            Map<String, Object> stats = embedService.getTokenStats(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Get token stats error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/token/validate")
    public ResponseEntity<?> validateToken(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String token = request.get("token");
            String clientIp = getClientIp(httpRequest);

            EmbedToken embedToken = embedService.validateToken(token, clientIp);

            Map<String, Object> config = new HashMap<>();
            config.put("dashboardId", embedToken.getDashboard().getId());
            config.put("hideTitle", embedToken.getHideTitle());
            config.put("hideToolbar", embedToken.getHideToolbar());
            config.put("hideFilters", embedToken.getHideFilters());
            config.put("hideTabs", embedToken.getHideTabs());
            config.put("enableFullscreen", embedToken.getEnableFullscreen());
            config.put("enableExport", embedToken.getEnableExport());
            config.put("enableDrilldown", embedToken.getEnableDrilldown());
            config.put("enableFilterInteraction", embedToken.getEnableFilterInteraction());
            config.put("theme", embedToken.getTheme());
            config.put("locale", embedToken.getLocale());
            config.put("defaultTabId", embedToken.getDefaultTabId());
            config.put("defaultFilters", embedToken.getDefaultFilters());
            config.put("rowPermissionRules", embedToken.getRowPermissionRules());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", true);
            response.put("config", config);

            return ResponseEntity.ok(response);

        } catch (BusinessException e) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Validate token error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "验证令牌失败: " + e.getMessage()
            ));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
