package com.lowcode.bi.service;

import com.lowcode.bi.entity.EmbedToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EmbedService {

    EmbedToken createEmbedToken(UUID dashboardId, String name, String description,
                                 Integer validitySeconds,
                                 Boolean hideTitle, Boolean hideToolbar, Boolean hideFilters,
                                 Boolean hideTabs, Boolean enableFullscreen, Boolean enableExport,
                                 Boolean enableDrilldown, Boolean enableFilterInteraction,
                                 Map<String, Object> defaultFilters, String defaultTabId,
                                 List<Map<String, Object>> rowPermissionRules,
                                 List<String> allowedDomains, Integer maxUses,
                                 String theme, String locale,
                                 String iframeWidth, String iframeHeight,
                                 String borderStyle, String customCss);

    EmbedToken getEmbedToken(UUID id);

    List<EmbedToken> getEmbedTokensByDashboard(UUID dashboardId);

    List<EmbedToken> getEmbedTokensByTenant();

    void revokeEmbedToken(UUID id);

    void deleteEmbedToken(UUID id);

    EmbedToken validateToken(String token, String clientIp);

    String generateEmbedCode(UUID id);

    String generateEmbedUrl(UUID id);

    Map<String, Object> getEmbedConfig(UUID id);

    Map<String, Object> getTokenStats(UUID id);

    void cleanupExpiredTokens();

    void incrementUsage(UUID id, String clientIp);
}
