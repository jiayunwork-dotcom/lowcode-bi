package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.Dashboard;
import com.lowcode.bi.entity.EmbedToken;
import com.lowcode.bi.entity.Tenant;
import com.lowcode.bi.entity.User;
import com.lowcode.bi.repository.DashboardRepository;
import com.lowcode.bi.repository.EmbedTokenRepository;
import com.lowcode.bi.repository.UserRepository;
import com.lowcode.bi.security.JwtUtil;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.EmbedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbedServiceImpl implements EmbedService {

    private final EmbedTokenRepository embedTokenRepository;
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.embed.default-validity-seconds:86400}")
    private int defaultValiditySeconds;

    @Value("${app.embed.max-validity-seconds:2592000}")
    private int maxValiditySeconds;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Transactional
    public EmbedToken createEmbedToken(UUID dashboardId, String name, String description,
                                       Integer validitySeconds,
                                       Boolean hideTitle, Boolean hideToolbar, Boolean hideFilters,
                                       Boolean hideTabs, Boolean enableFullscreen, Boolean enableExport,
                                       Boolean enableDrilldown, Boolean enableFilterInteraction,
                                       Map<String, Object> defaultFilters, String defaultTabId,
                                       List<Map<String, Object>> rowPermissionRules,
                                       List<String> allowedDomains, Integer maxUses,
                                       String theme, String locale,
                                       String iframeWidth, String iframeHeight,
                                       String borderStyle, String customCss) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        Dashboard dashboard = dashboardRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        int validity = validitySeconds != null ? validitySeconds : defaultValiditySeconds;
        if (validity > maxValiditySeconds) {
            throw new BusinessException("有效期不能超过" + (maxValiditySeconds / 86400) + "天");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(validity);

        Map<String, Object> jwtClaims = new HashMap<>();
        jwtClaims.put("type", "embed");
        jwtClaims.put("dashboardId", dashboardId.toString());
        jwtClaims.put("tenantId", tenantId.toString());
        jwtClaims.put("userId", userId.toString());
        jwtClaims.put("hideTitle", hideTitle != null ? hideTitle : false);
        jwtClaims.put("hideToolbar", hideToolbar != null ? hideToolbar : true);
        jwtClaims.put("hideFilters", hideFilters != null ? hideFilters : true);
        jwtClaims.put("hideTabs", hideTabs != null ? hideTabs : false);
        jwtClaims.put("enableFullscreen", enableFullscreen != null ? enableFullscreen : false);
        jwtClaims.put("enableExport", enableExport != null ? enableExport : false);
        jwtClaims.put("enableDrilldown", enableDrilldown != null ? enableDrilldown : false);
        jwtClaims.put("enableFilterInteraction", enableFilterInteraction != null ? enableFilterInteraction : false);
        jwtClaims.put("theme", theme != null ? theme : "light");
        jwtClaims.put("locale", locale != null ? locale : "zh-CN");

        if (defaultTabId != null) {
            jwtClaims.put("defaultTabId", defaultTabId);
        }
        if (defaultFilters != null) {
            jwtClaims.put("defaultFilters", defaultFilters);
        }
        if (rowPermissionRules != null) {
            jwtClaims.put("rowPermissionRules", rowPermissionRules);
        }

        String token = jwtUtil.generateToken(
                user.getUsername(),
                tenantId.toString(),
                "VIEWER",
                jwtClaims,
                validity
        );

        EmbedToken embedToken = new EmbedToken();
        embedToken.setDashboard(dashboard);
        embedToken.setTenant(dashboard.getTenant());
        embedToken.setCreatedBy(user);
        embedToken.setName(name);
        embedToken.setDescription(description);
        embedToken.setToken(token);
        embedToken.setTokenType("JWT");
        embedToken.setExpiresAt(expiresAt);
        embedToken.setValiditySeconds(validity);
        embedToken.setIsActive(true);

        embedToken.setHideTitle(hideTitle != null ? hideTitle : false);
        embedToken.setHideToolbar(hideToolbar != null ? hideToolbar : true);
        embedToken.setHideFilters(hideFilters != null ? hideFilters : true);
        embedToken.setHideTabs(hideTabs != null ? hideTabs : false);
        embedToken.setEnableFullscreen(enableFullscreen != null ? enableFullscreen : false);
        embedToken.setEnableExport(enableExport != null ? enableExport : false);
        embedToken.setEnableDrilldown(enableDrilldown != null ? enableDrilldown : false);
        embedToken.setEnableFilterInteraction(enableFilterInteraction != null ? enableFilterInteraction : false);

        try {
            if (defaultFilters != null) {
                embedToken.setDefaultFilters(objectMapper.writeValueAsString(defaultFilters));
            }
            if (rowPermissionRules != null) {
                embedToken.setRowPermissionRules(objectMapper.writeValueAsString(rowPermissionRules));
            }
            if (allowedDomains != null) {
                embedToken.setAllowedDomains(objectMapper.writeValueAsString(allowedDomains));
            }
        } catch (Exception e) {
            throw new BusinessException("序列化配置失败: " + e.getMessage());
        }

        embedToken.setDefaultTabId(defaultTabId);
        embedToken.setMaxUses(maxUses);
        embedToken.setTheme(theme != null ? theme : "light");
        embedToken.setLocale(locale != null ? locale : "zh-CN");
        embedToken.setIframeWidth(iframeWidth != null ? iframeWidth : "100%");
        embedToken.setIframeHeight(iframeHeight != null ? iframeHeight : "600px");
        embedToken.setBorderStyle(borderStyle);
        embedToken.setCustomCss(customCss);

        return embedTokenRepository.save(embedToken);
    }

    @Override
    public EmbedToken getEmbedToken(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return embedTokenRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("嵌入令牌不存在"));
    }

    @Override
    public List<EmbedToken> getEmbedTokensByDashboard(UUID dashboardId) {
        UUID tenantId = TenantContext.getTenantId();
        dashboardRepository.findByIdAndTenantId(dashboardId, tenantId)
                .orElseThrow(() -> new BusinessException("仪表板不存在"));
        return embedTokenRepository.findActiveByDashboardId(tenantId, dashboardId);
    }

    @Override
    public List<EmbedToken> getEmbedTokensByTenant() {
        UUID tenantId = TenantContext.getTenantId();
        return embedTokenRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void revokeEmbedToken(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        int updated = embedTokenRepository.revokeToken(id, tenantId);
        if (updated == 0) {
            throw new BusinessException("嵌入令牌不存在或已撤销");
        }
    }

    @Override
    @Transactional
    public void deleteEmbedToken(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        EmbedToken token = embedTokenRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("嵌入令牌不存在"));
        embedTokenRepository.delete(token);
    }

    @Override
    @Transactional
    public EmbedToken validateToken(String token, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        EmbedToken embedToken = embedTokenRepository.findValidToken(token, now)
                .orElseThrow(() -> new BusinessException("无效或已过期的嵌入令牌"));

        if (embedToken.getMaxUses() != null
                && embedToken.getCurrentUses() >= embedToken.getMaxUses()) {
            throw new BusinessException("嵌入令牌已达到最大使用次数");
        }

        if (embedToken.getAllowedDomains() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> allowedDomains = objectMapper.readValue(
                        embedToken.getAllowedDomains(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                if (!allowedDomains.isEmpty() && clientIp != null) {
                    boolean allowed = allowedDomains.stream()
                            .anyMatch(domain -> clientIp.contains(domain)
                                    || domain.equals("*")
                                    || domain.equals(clientIp));
                    if (!allowed) {
                        throw new BusinessException("访问域名不在白名单中");
                    }
                }
            } catch (Exception e) {
                log.warn("验证域名白名单失败: {}", e.getMessage());
            }
        }

        incrementUsage(embedToken.getId(), clientIp);

        return embedToken;
    }

    @Override
    public String generateEmbedCode(UUID id) {
        EmbedToken token = getEmbedToken(id);
        String embedUrl = generateEmbedUrl(id);

        StringBuilder code = new StringBuilder();
        code.append("<!-- 低代码BI仪表板嵌入代码 -->\n");
        code.append("<iframe\n");
        code.append("    src=\"").append(embedUrl).append("\"\n");
        code.append("    width=\"").append(token.getIframeWidth()).append("\"\n");
        code.append("    height=\"").append(token.getIframeHeight()).append("\"\n");

        if (token.getBorderStyle() != null) {
            code.append("    style=\"border: ").append(token.getBorderStyle()).append(";\"\n");
        } else {
            code.append("    style=\"border: none;\"\n");
        }

        code.append("    frameborder=\"0\"\n");
        code.append("    allowfullscreen").append(token.getEnableFullscreen() ? "" : "=\"false\"").append("\n");
        code.append("    loading=\"lazy\"\n");
        code.append("    referrerpolicy=\"no-referrer-when-downgrade\"\n");
        code.append("></iframe>");

        if (token.getCustomCss() != null) {
            code.append("\n\n<!-- 自定义样式 -->\n");
            code.append("<style>\n").append(token.getCustomCss()).append("\n</style>");
        }

        return code.toString();
    }

    @Override
    public String generateEmbedUrl(UUID id) {
        EmbedToken token = getEmbedToken(id);
        return frontendUrl + "/embed/dashboard/" + token.getDashboard().getId()
                + "?token=" + token.getToken();
    }

    @Override
    public Map<String, Object> getEmbedConfig(UUID id) {
        EmbedToken token = getEmbedToken(id);

        Map<String, Object> config = new HashMap<>();
        config.put("id", token.getId());
        config.put("name", token.getName());
        config.put("dashboardId", token.getDashboard().getId());
        config.put("dashboardName", token.getDashboard().getName());
        config.put("embedUrl", generateEmbedUrl(id));
        config.put("embedCode", generateEmbedCode(id));
        config.put("hideTitle", token.getHideTitle());
        config.put("hideToolbar", token.getHideToolbar());
        config.put("hideFilters", token.getHideFilters());
        config.put("hideTabs", token.getHideTabs());
        config.put("enableFullscreen", token.getEnableFullscreen());
        config.put("enableExport", token.getEnableExport());
        config.put("enableDrilldown", token.getEnableDrilldown());
        config.put("enableFilterInteraction", token.getEnableFilterInteraction());
        config.put("theme", token.getTheme());
        config.put("locale", token.getLocale());
        config.put("iframeWidth", token.getIframeWidth());
        config.put("iframeHeight", token.getIframeHeight());
        config.put("expiresAt", token.getExpiresAt());
        config.put("validitySeconds", token.getValiditySeconds());

        return config;
    }

    @Override
    public Map<String, Object> getTokenStats(UUID id) {
        EmbedToken token = getEmbedToken(id);

        Map<String, Object> stats = new HashMap<>();
        stats.put("id", token.getId());
        stats.put("name", token.getName());
        stats.put("isActive", token.getIsActive());
        stats.put("currentUses", token.getCurrentUses());
        stats.put("maxUses", token.getMaxUses());
        stats.put("lastUsedAt", token.getLastUsedAt());
        stats.put("lastUsedIp", token.getLastUsedIp());
        stats.put("createdAt", token.getCreatedAt());
        stats.put("expiresAt", token.getExpiresAt());
        stats.put("remainingSeconds",
                java.time.Duration.between(LocalDateTime.now(), token.getExpiresAt()).getSeconds());

        return stats;
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        int deleted = embedTokenRepository.deleteExpiredTokens(cutoffTime);
        log.info("清理了{}个过期的嵌入令牌", deleted);
    }

    @Override
    @Transactional
    public void incrementUsage(UUID id, String clientIp) {
        embedTokenRepository.incrementUsage(id, LocalDateTime.now(), clientIp);
    }
}
