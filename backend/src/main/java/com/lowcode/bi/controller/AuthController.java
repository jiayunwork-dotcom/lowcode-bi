package com.lowcode.bi.controller;

import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.entity.Tenant;
import com.lowcode.bi.entity.User;
import com.lowcode.bi.repository.TenantRepository;
import com.lowcode.bi.repository.UserRepository;
import com.lowcode.bi.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String fullUsername = request.getUsername() + "@" + request.getTenantCode();

        Tenant tenant = tenantRepository.findByCodeAndDeletedFalse(request.getTenantCode())
                .orElseThrow(() -> new BusinessException("租户不存在"));

        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new BusinessException("租户已被禁用");
        }

        if (tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("租户已过期");
        }

        User user = userRepository.findByUsernameAndTenantIdAndDeletedFalse(request.getUsername(), tenant.getId())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("用户已被禁用");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(fullUsername, request.getPassword())
        );

        String token = jwtUtil.generateToken(
                user.getId(),
                tenant.getId(),
                user.getUsername(),
                user.getRole().name()
        );

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(request.getIp());
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400);
        response.put("user", buildUserInfo(user, tenant));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-tenant")
    public ResponseEntity<Map<String, Object>> registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        if (tenantRepository.existsByCode(request.getTenantCode())) {
            throw new BusinessException("租户编码已存在");
        }

        Tenant tenant = new Tenant();
        tenant.setCode(request.getTenantCode());
        tenant.setName(request.getTenantName());
        tenant.setDescription(request.getTenantDescription());
        tenant.setStatus("ACTIVE");
        tenant.setMaxUsers(100);
        tenant.setMaxDataSources(20);
        tenant.setMaxDashboards(100);
        tenant.setStorageLimitMb(10240L);
        tenant = tenantRepository.save(tenant);

        User adminUser = new User();
        adminUser.setTenant(tenant);
        adminUser.setUsername(request.getAdminUsername());
        adminUser.setEmail(request.getAdminEmail());
        adminUser.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
        adminUser.setFullName(request.getAdminFullName());
        adminUser.setRole(com.lowcode.bi.common.enums.RoleType.TENANT_ADMIN);
        adminUser.setStatus("ACTIVE");
        adminUser.setIsSystemAdmin(false);
        adminUser = userRepository.save(adminUser);

        String token = jwtUtil.generateToken(
                adminUser.getId(),
                tenant.getId(),
                adminUser.getUsername(),
                adminUser.getRole().name()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400);
        response.put("user", buildUserInfo(adminUser, tenant));
        response.put("tenant", Map.of(
                "id", tenant.getId(),
                "code", tenant.getCode(),
                "name", tenant.getName()
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        UUID userId = com.lowcode.bi.security.TenantContext.getUserId();
        UUID tenantId = com.lowcode.bi.security.TenantContext.getTenantId();

        if (userId == null || tenantId == null) {
            throw new BusinessException("未登录");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("租户不存在"));

        return ResponseEntity.ok(buildUserInfo(user, tenant));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of("message", "登出成功"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken() {
        UUID userId = com.lowcode.bi.security.TenantContext.getUserId();
        UUID tenantId = com.lowcode.bi.security.TenantContext.getTenantId();
        String username = com.lowcode.bi.security.TenantContext.getUsername();
        String role = com.lowcode.bi.security.TenantContext.getRole();

        if (userId == null || tenantId == null) {
            throw new BusinessException("未登录");
        }

        String newToken = jwtUtil.generateToken(userId, tenantId, username, role);

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildUserInfo(User user, Tenant tenant) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("role", user.getRole().name());
        userInfo.put("status", user.getStatus());
        userInfo.put("isSystemAdmin", user.getIsSystemAdmin());
        userInfo.put("lastLoginAt", user.getLastLoginAt());
        userInfo.put("preferences", user.getPreferences());

        Map<String, Object> tenantInfo = new HashMap<>();
        tenantInfo.put("id", tenant.getId());
        tenantInfo.put("code", tenant.getCode());
        tenantInfo.put("name", tenant.getName());
        tenantInfo.put("logoUrl", tenant.getLogoUrl());
        tenantInfo.put("status", tenant.getStatus());

        userInfo.put("tenant", tenantInfo);

        return userInfo;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "租户编码不能为空")
        private String tenantCode;

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String ip;
    }

    @Data
    public static class RegisterTenantRequest {
        @NotBlank(message = "租户编码不能为空")
        private String tenantCode;

        @NotBlank(message = "租户名称不能为空")
        private String tenantName;

        private String tenantDescription;

        @NotBlank(message = "管理员用户名不能为空")
        private String adminUsername;

        @NotBlank(message = "管理员邮箱不能为空")
        private String adminEmail;

        @NotBlank(message = "管理员密码不能为空")
        private String adminPassword;

        private String adminFullName;
    }
}
