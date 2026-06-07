package com.lowcode.bi.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<TenantContextData> CONTEXT = new ThreadLocal<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantContextData {
        private UUID tenantId;
        private UUID userId;
        private String username;
        private String role;
        private String rowPermissionFilters;
        private boolean isSystemAdmin;
        private boolean isEmbedToken;
    }

    public static void set(TenantContextData data) {
        CONTEXT.set(data);
    }

    public static TenantContextData get() {
        return CONTEXT.get();
    }

    public static UUID getTenantId() {
        TenantContextData data = CONTEXT.get();
        return data != null ? data.getTenantId() : null;
    }

    public static UUID getUserId() {
        TenantContextData data = CONTEXT.get();
        return data != null ? data.getUserId() : null;
    }

    public static String getUsername() {
        TenantContextData data = CONTEXT.get();
        return data != null ? data.getUsername() : null;
    }

    public static String getRole() {
        TenantContextData data = CONTEXT.get();
        return data != null ? data.getRole() : null;
    }

    public static String getRowPermissionFilters() {
        TenantContextData data = CONTEXT.get();
        return data != null ? data.getRowPermissionFilters() : null;
    }

    public static boolean isSystemAdmin() {
        TenantContextData data = CONTEXT.get();
        return data != null && data.isSystemAdmin();
    }

    public static boolean isEmbedToken() {
        TenantContextData data = CONTEXT.get();
        return data != null && data.isEmbedToken();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }
}
