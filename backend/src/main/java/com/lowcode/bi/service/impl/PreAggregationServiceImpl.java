package com.lowcode.bi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowcode.bi.common.exception.BusinessException;
import com.lowcode.bi.config.DataSourcePoolConfig;
import com.lowcode.bi.entity.DataModel;
import com.lowcode.bi.entity.DataSource;
import com.lowcode.bi.entity.PreAggregation;
import com.lowcode.bi.entity.Tenant;
import com.lowcode.bi.query.SqlGenerator;
import com.lowcode.bi.query.SqlQueryEngine;
import com.lowcode.bi.repository.DataModelRepository;
import com.lowcode.bi.repository.PreAggregationRepository;
import com.lowcode.bi.security.TenantContext;
import com.lowcode.bi.service.PreAggregationService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreAggregationServiceImpl implements PreAggregationService {

    private final PreAggregationRepository preAggregationRepository;
    private final DataModelRepository dataModelRepository;
    private final SqlGenerator sqlGenerator;
    private final SqlQueryEngine sqlQueryEngine;
    private final DataSourcePoolConfig poolConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.pre-aggregation.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    @Override
    @Transactional
    public PreAggregation createPreAggregation(UUID dataModelId, String name, String description,
                                               String targetTable, List<SqlGenerator.DimensionField> dimensions,
                                               List<SqlGenerator.MeasureField> measures,
                                               List<SqlGenerator.FilterCondition> filters,
                                               String cronExpression, String refreshStrategy,
                                               String incrementalColumn) {
        UUID tenantId = TenantContext.getTenantId();

        DataModel dataModel = dataModelRepository.findByIdAndTenantId(dataModelId, tenantId)
                .orElseThrow(() -> new BusinessException("数据模型不存在"));

        String aggregationSql = generatePreAggregationSql(dataModelId, dimensions, measures, filters);

        PreAggregation preAggregation = new PreAggregation();
        preAggregation.setDataModel(dataModel);
        preAggregation.setTenant(dataModel.getTenant());
        preAggregation.setName(name);
        preAggregation.setDescription(description);
        preAggregation.setTargetTable(targetTable);

        try {
            preAggregation.setDimensions(objectMapper.writeValueAsString(dimensions));
            preAggregation.setMeasures(objectMapper.writeValueAsString(measures));
            preAggregation.setFilters(filters != null ? objectMapper.writeValueAsString(filters) : null);
        } catch (Exception e) {
            throw new BusinessException("序列化配置失败: " + e.getMessage());
        }

        preAggregation.setAggregationSql(aggregationSql);
        preAggregation.setCronExpression(cronExpression);
        preAggregation.setRefreshStrategy(refreshStrategy != null ? refreshStrategy : "FULL");
        preAggregation.setIncrementalColumn(incrementalColumn);
        preAggregation.setIsEnabled(true);
        preAggregation.setIsPaused(false);

        preAggregation = preAggregationRepository.save(preAggregation);

        try {
            createTargetTable(preAggregation);
        } catch (Exception e) {
            log.warn("创建预聚合表失败，稍后自动重试: {}", e.getMessage());
        }

        return preAggregation;
    }

    @Override
    @Transactional
    public PreAggregation updatePreAggregation(UUID id, String name, String description,
                                               String cronExpression, Boolean isEnabled) {
        UUID tenantId = TenantContext.getTenantId();
        PreAggregation preAggregation = preAggregationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("预聚合不存在"));

        if (name != null) preAggregation.setName(name);
        if (description != null) preAggregation.setDescription(description);
        if (cronExpression != null) preAggregation.setCronExpression(cronExpression);
        if (isEnabled != null) preAggregation.setIsEnabled(isEnabled);

        return preAggregationRepository.save(preAggregation);
    }

    @Override
    @Transactional
    public void deletePreAggregation(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        PreAggregation preAggregation = preAggregationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("预聚合不存在"));

        dropTargetTable(preAggregation);
        preAggregationRepository.delete(preAggregation);
    }

    @Override
    public PreAggregation getPreAggregation(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return preAggregationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("预聚合不存在"));
    }

    @Override
    public List<PreAggregation> getPreAggregationsByDataModel(UUID dataModelId) {
        UUID tenantId = TenantContext.getTenantId();
        dataModelRepository.findByIdAndTenantId(dataModelId, tenantId)
                .orElseThrow(() -> new BusinessException("数据模型不存在"));
        return preAggregationRepository.findByDataModelId(dataModelId);
    }

    @Override
    public List<PreAggregation> getPreAggregationsByTenant() {
        UUID tenantId = TenantContext.getTenantId();
        return preAggregationRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional
    public PreAggregation executePreAggregation(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        PreAggregation preAggregation = preAggregationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("预聚合不存在"));

        if (preAggregation.getIsPaused()) {
            throw new BusinessException("预聚合已暂停，请先恢复");
        }

        long startTime = System.currentTimeMillis();
        preAggregation.setLastRefreshAt(LocalDateTime.now());

        try {
            String insertSql;
            if ("INCREMENTAL".equals(preAggregation.getRefreshStrategy())
                    && preAggregation.getIncrementalColumn() != null
                    && preAggregation.getLastRefreshAt() != null) {
                insertSql = buildIncrementalInsertSql(preAggregation);
            } else {
                insertSql = buildFullInsertSql(preAggregation);
            }

            SqlQueryEngine.QueryRequest request = new SqlQueryEngine.QueryRequest();
            request.setDataSourceId(preAggregation.getDataModel().getDataSource().getId());
            request.setSql(insertSql);
            request.setUseCache(false);
            request.setApplyRowPermissions(false);
            request.setTimeout(300);

            SqlQueryEngine.QueryResult result = sqlQueryEngine.executeQuery(request);

            preAggregation.setLastRefreshStatus("SUCCESS");
            preAggregation.setLastRefreshMessage("预聚合执行成功");
            preAggregation.setLastRefreshRowCount(result.getTotalRows());
            preAggregation.setConsecutiveFailures(0);

        } catch (Exception e) {
            log.error("预聚合执行失败: {}", e.getMessage(), e);
            preAggregation.setLastRefreshStatus("FAILED");
            preAggregation.setLastRefreshMessage("执行失败: " + e.getMessage());
            preAggregation.setConsecutiveFailures(preAggregation.getConsecutiveFailures() + 1);
        }

        preAggregation.setLastRefreshDurationMs(System.currentTimeMillis() - startTime);

        if (preAggregation.getConsecutiveFailures() >= maxConsecutiveFailures) {
            preAggregation.setIsPaused(true);
            preAggregation.setPauseReason("连续失败" + maxConsecutiveFailures + "次，自动暂停");
        }

        return preAggregationRepository.save(preAggregation);
    }

    @Override
    @Transactional
    public void pausePreAggregation(UUID id, String reason) {
        UUID tenantId = TenantContext.getTenantId();
        preAggregationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("预聚合不存在"));
        preAggregationRepository.pausePreAggregation(id, reason != null ? reason : "手动暂停");
    }

    @Override
    @Transactional
    public void resumePreAggregation(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        preAggregationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException("预聚合不存在"));
        preAggregationRepository.resumePreAggregation(id);
    }

    @Override
    public Map<String, Object> getPreAggregationStatus(UUID id) {
        PreAggregation preAggregation = getPreAggregation(id);

        Map<String, Object> status = new HashMap<>();
        status.put("id", preAggregation.getId());
        status.put("name", preAggregation.getName());
        status.put("isEnabled", preAggregation.getIsEnabled());
        status.put("isPaused", preAggregation.getIsPaused());
        status.put("pauseReason", preAggregation.getPauseReason());
        status.put("lastRefreshAt", preAggregation.getLastRefreshAt());
        status.put("lastRefreshStatus", preAggregation.getLastRefreshStatus());
        status.put("lastRefreshMessage", preAggregation.getLastRefreshMessage());
        status.put("lastRefreshRowCount", preAggregation.getLastRefreshRowCount());
        status.put("lastRefreshDurationMs", preAggregation.getLastRefreshDurationMs());
        status.put("consecutiveFailures", preAggregation.getConsecutiveFailures());
        status.put("hitCount", preAggregation.getHitCount());
        status.put("lastHitAt", preAggregation.getLastHitAt());
        status.put("targetTable", preAggregation.getTargetTable());

        return status;
    }

    @Override
    public String generatePreAggregationSql(UUID dataModelId,
                                            List<SqlGenerator.DimensionField> dimensions,
                                            List<SqlGenerator.MeasureField> measures,
                                            List<SqlGenerator.FilterCondition> filters) {
        UUID tenantId = TenantContext.getTenantId();
        DataModel dataModel = dataModelRepository.findByIdAndTenantId(dataModelId, tenantId)
                .orElseThrow(() -> new BusinessException("数据模型不存在"));

        SqlGenerator.QueryRequest request = new SqlGenerator.QueryRequest();
        request.setDimensions(dimensions);
        request.setMeasures(measures);
        request.setFilters(filters);

        SqlGenerator.GeneratedSql generated = sqlGenerator.generateQuery(dataModel, request);
        return generated.getSql();
    }

    @Override
    public void createTargetTable(PreAggregation preAggregation) {
        DataSource dataSource = preAggregation.getDataModel().getDataSource();
        String dataSourceKey = "datasource_" + dataSource.getId();
        HikariDataSource hikariDataSource = poolConfig.getDataSource(dataSourceKey);

        if (hikariDataSource == null) {
            throw new BusinessException("数据源连接池未初始化");
        }

        String tableName = preAggregation.getTargetTable();
        String createSql = buildCreateTableSql(preAggregation, dataSource);

        try (Connection conn = hikariDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            log.info("预聚合表创建成功: {}", tableName);
        } catch (Exception e) {
            throw new BusinessException("创建预聚合表失败: " + e.getMessage());
        }
    }

    private void dropTargetTable(PreAggregation preAggregation) {
        DataSource dataSource = preAggregation.getDataModel().getDataSource();
        String dataSourceKey = "datasource_" + dataSource.getId();
        HikariDataSource hikariDataSource = poolConfig.getDataSource(dataSourceKey);

        if (hikariDataSource == null) {
            return;
        }

        String dropSql = "DROP TABLE IF EXISTS " + preAggregation.getTargetTable();

        try (Connection conn = hikariDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropSql);
            log.info("预聚合表删除成功: {}", preAggregation.getTargetTable());
        } catch (Exception e) {
            log.warn("删除预聚合表失败: {}", e.getMessage());
        }
    }

    private String buildCreateTableSql(PreAggregation preAggregation, DataSource dataSource) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(preAggregation.getTargetTable()).append(" (");

        try {
            List<Map<String, Object>> dimensions = objectMapper.readValue(
                    preAggregation.getDimensions(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            List<Map<String, Object>> measures = objectMapper.readValue(
                    preAggregation.getMeasures(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            for (int i = 0; i < dimensions.size(); i++) {
                if (i > 0) sb.append(", ");
                String alias = (String) dimensions.get(i).get("alias");
                if (alias == null) alias = "dim_" + (i + 1);
                sb.append(alias).append(" VARCHAR(255)");
            }

            for (int i = 0; i < measures.size(); i++) {
                sb.append(", ");
                String alias = (String) measures.get(i).get("alias");
                if (alias == null) alias = "measure_" + (i + 1);
                sb.append(alias).append(" DECIMAL(20,6)");
            }

            sb.append(", pre_agg_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            sb.append(")");

            if (dataSource.getDatabaseType().name().equals("CLICKHOUSE")) {
                sb.append(" ENGINE = MergeTree() ORDER BY (");
                for (int i = 0; i < dimensions.size(); i++) {
                    if (i > 0) sb.append(", ");
                    String alias = (String) dimensions.get(i).get("alias");
                    if (alias == null) alias = "dim_" + (i + 1);
                    sb.append(alias);
                }
                sb.append(")");
            }

        } catch (Exception e) {
            throw new BusinessException("解析预聚合配置失败: " + e.getMessage());
        }

        return sb.toString();
    }

    private String buildFullInsertSql(PreAggregation preAggregation) {
        return "INSERT INTO " + preAggregation.getTargetTable() + " " +
               "SELECT *, CURRENT_TIMESTAMP FROM (" + preAggregation.getAggregationSql() + ") t";
    }

    private String buildIncrementalInsertSql(PreAggregation preAggregation) {
        String incrementalColumn = preAggregation.getIncrementalColumn();
        LocalDateTime lastRefresh = preAggregation.getLastRefreshAt();

        String baseSql = preAggregation.getAggregationSql();
        String whereCondition = incrementalColumn + " > '" + lastRefresh + "'";

        if (baseSql.toUpperCase().contains(" WHERE ")) {
            baseSql = baseSql.replaceFirst("(?i) WHERE ", " WHERE " + whereCondition + " AND ");
        } else if (baseSql.toUpperCase().contains(" GROUP BY ")) {
            baseSql = baseSql.replaceFirst("(?i) GROUP BY ", " WHERE " + whereCondition + " GROUP BY ");
        } else {
            baseSql = baseSql + " WHERE " + whereCondition;
        }

        return "INSERT INTO " + preAggregation.getTargetTable() + " " +
               "SELECT *, CURRENT_TIMESTAMP FROM (" + baseSql + ") t";
    }

    @Override
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void checkAndPauseFailedAggregations() {
        List<PreAggregation> failed = preAggregationRepository.findNeedPause(maxConsecutiveFailures);
        for (PreAggregation pa : failed) {
            if (!pa.getIsPaused()) {
                pa.setIsPaused(true);
                pa.setPauseReason("连续失败" + maxConsecutiveFailures + "次，自动暂停");
                preAggregationRepository.save(pa);
                log.warn("预聚合{}因连续失败已自动暂停", pa.getName());
            }
        }
    }
}
