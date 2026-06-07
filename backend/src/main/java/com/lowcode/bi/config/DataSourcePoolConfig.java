package com.lowcode.bi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DataSourcePoolConfig {

    @Value("${app.datasource.tenant.max-connections:20}")
    private int maxConnectionsPerTenant;

    @Value("${app.datasource.tenant.queue-timeout:10000}")
    private int queueTimeout;

    @Value("${app.datasource.tenant.connection-wait-timeout:10000}")
    private int connectionWaitTimeout;

    private final Map<UUID, TenantConnectionPool> tenantPools = new ConcurrentHashMap<>();
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, Semaphore> connectionSemaphores = new ConcurrentHashMap<>();

    public static class TenantConnectionPool {
        private final UUID tenantId;
        private final Semaphore semaphore;
        private int activeConnections = 0;
        private final int maxConnections;
        private final int queueTimeout;

        public TenantConnectionPool(UUID tenantId, int maxConnections, int queueTimeout) {
            this.tenantId = tenantId;
            this.maxConnections = maxConnections;
            this.queueTimeout = queueTimeout;
            this.semaphore = new Semaphore(maxConnections, true);
        }

        public boolean acquire() throws InterruptedException {
            return semaphore.tryAcquire(queueTimeout, TimeUnit.MILLISECONDS);
        }

        public void release() {
            semaphore.release();
            decrementActive();
        }

        public synchronized void incrementActive() {
            activeConnections++;
        }

        public synchronized void decrementActive() {
            activeConnections = Math.max(0, activeConnections - 1);
        }

        public synchronized int getActiveConnections() {
            return activeConnections;
        }

        public int getAvailablePermits() {
            return semaphore.availablePermits();
        }
    }

    public TenantConnectionPool getTenantPool(UUID tenantId) {
        return tenantPools.computeIfAbsent(tenantId,
                k -> new TenantConnectionPool(tenantId, maxConnectionsPerTenant, queueTimeout));
    }

    public void registerDataSource(String dataSourceKey, HikariDataSource dataSource) {
        dataSources.put(dataSourceKey, dataSource);
        connectionSemaphores.put(dataSourceKey, new Semaphore(dataSource.getMaximumPoolSize(), true));
    }

    public void unregisterDataSource(String dataSourceKey) {
        HikariDataSource dataSource = dataSources.remove(dataSourceKey);
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        connectionSemaphores.remove(dataSourceKey);
    }

    public HikariDataSource getDataSource(String dataSourceKey) {
        return dataSources.get(dataSourceKey);
    }

    public boolean acquireConnection(String dataSourceKey, UUID tenantId) throws InterruptedException {
        TenantConnectionPool tenantPool = getTenantPool(tenantId);
        if (!tenantPool.acquire()) {
            throw new RuntimeException("服务繁忙，请稍后再试。租户连接池已满，已等待" + queueTimeout + "毫秒。");
        }

        Semaphore semaphore = connectionSemaphores.get(dataSourceKey);
        if (semaphore == null) {
            tenantPool.release();
            throw new RuntimeException("数据源不存在或已关闭");
        }

        if (!semaphore.tryAcquire(connectionWaitTimeout, TimeUnit.MILLISECONDS)) {
            tenantPool.release();
            throw new RuntimeException("服务繁忙，数据源连接池已满，请稍后再试。");
        }

        tenantPool.incrementActive();
        return true;
    }

    public void releaseConnection(String dataSourceKey, UUID tenantId) {
        Semaphore semaphore = connectionSemaphores.get(dataSourceKey);
        if (semaphore != null) {
            semaphore.release();
        }
        TenantConnectionPool tenantPool = tenantPools.get(tenantId);
        if (tenantPool != null) {
            tenantPool.release();
        }
    }

    public void closeAllPools() {
        dataSources.values().forEach(ds -> {
            if (!ds.isClosed()) {
                ds.close();
            }
        });
        dataSources.clear();
        connectionSemaphores.clear();
        tenantPools.clear();
    }

    public HikariConfig createHikariConfig(
            String driverClassName,
            String jdbcUrl,
            String username,
            String password,
            int maxPoolSize,
            int minIdle,
            int connectionTimeout,
            int readTimeout,
            Map<String, String> properties) {

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        if (properties != null) {
            properties.forEach(config::addDataSourceProperty);
        }

        config.addDataSourceProperty("socketTimeout", String.valueOf(readTimeout));
        config.addDataSourceProperty("connectTimeout", String.valueOf(connectionTimeout));

        return config;
    }
}
