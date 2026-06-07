package com.lowcode.bi.security;

import com.lowcode.bi.entity.User;
import com.lowcode.bi.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String tokenType = jwtUtil.getTokenType(token);
            UUID tenantId = jwtUtil.getTenantIdFromToken(token);

            if ("embed".equals(tokenType)) {
                handleEmbedToken(request, token, tenantId);
            } else {
                handleAccessToken(request, token, tenantId);
            }

        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleAccessToken(HttpServletRequest request, String token, UUID tenantId) {
        UUID userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("租户不匹配");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("用户已被禁用");
        }

        TenantContext.TenantContextData contextData = new TenantContext.TenantContextData(
                tenantId,
                userId,
                username,
                role,
                user.getRowPermissionFilters(),
                user.getIsSystemAdmin(),
                false
        );
        TenantContext.set(contextData);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(authority)
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleEmbedToken(HttpServletRequest request, String token, UUID tenantId) {
        java.util.Map<String, Object> permissions = jwtUtil.getEmbedPermissions(token);

        TenantContext.TenantContextData contextData = new TenantContext.TenantContextData(
                tenantId,
                null,
                "embed_user",
                "VIEWER",
                permissions.get("rowFilters") != null ? permissions.get("rowFilters").toString() : null,
                false,
                true
        );
        TenantContext.set(contextData);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_VIEWER");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "embed_user",
                null,
                Collections.singletonList(authority)
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/")
                || path.startsWith("/public/")
                || path.startsWith("/embed/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }
}
