package com.lowcode.bi.service;

import com.lowcode.bi.entity.PreAggregation;
import com.lowcode.bi.query.SqlGenerator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PreAggregationService {

    PreAggregation createPreAggregation(UUID dataModelId, String name, String description,
                                        String targetTable, List<SqlGenerator.DimensionField> dimensions,
                                        List<SqlGenerator.MeasureField> measures,
                                        List<SqlGenerator.FilterCondition> filters,
                                        String cronExpression, String refreshStrategy,
                                        String incrementalColumn);

    PreAggregation updatePreAggregation(UUID id, String name, String description,
                                        String cronExpression, Boolean isEnabled);

    void deletePreAggregation(UUID id);

    PreAggregation getPreAggregation(UUID id);

    List<PreAggregation> getPreAggregationsByDataModel(UUID dataModelId);

    List<PreAggregation> getPreAggregationsByTenant();

    PreAggregation executePreAggregation(UUID id);

    void pausePreAggregation(UUID id, String reason);

    void resumePreAggregation(UUID id);

    Map<String, Object> getPreAggregationStatus(UUID id);

    String generatePreAggregationSql(UUID dataModelId,
                                     List<SqlGenerator.DimensionField> dimensions,
                                     List<SqlGenerator.MeasureField> measures,
                                     List<SqlGenerator.FilterCondition> filters);

    void createTargetTable(PreAggregation preAggregation);

    void checkAndPauseFailedAggregations();
}
