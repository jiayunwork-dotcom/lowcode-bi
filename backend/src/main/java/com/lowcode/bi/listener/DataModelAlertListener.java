package com.lowcode.bi.listener;

import com.lowcode.bi.common.enums.AlertRuleStatus;
import com.lowcode.bi.entity.AlertRule;
import com.lowcode.bi.entity.DataModel;
import com.lowcode.bi.entity.Measure;
import com.lowcode.bi.repository.AlertRuleRepository;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataModelAlertListener {

    @Lazy
    private final AlertRuleRepository alertRuleRepository;

    @PreRemove
    public void preRemove(DataModel dataModel) {
        log.info("DataModel {} being removed, invalidating associated alert rules", dataModel.getId());
        invalidateRulesForDataModel(dataModel);
    }

    @PreUpdate
    public void preUpdate(DataModel dataModel) {
        syncMeasureNames(dataModel);
    }

    @PreRemove
    public void preRemoveMeasure(Measure measure) {
        log.info("Measure {} being removed, invalidating associated alert rules", measure.getId());
        List<AlertRule> rules = alertRuleRepository.findByMeasureIdAndTenantId(
            measure.getId(), measure.getTenant().getId());
        for (AlertRule rule : rules) {
            rule.setStatus(AlertRuleStatus.INVALID);
            rule.setIsEnabled(false);
            alertRuleRepository.save(rule);
        }
    }

    private void invalidateRulesForDataModel(DataModel dataModel) {
        List<AlertRule> rules = alertRuleRepository.findByDataModelIdAndTenantId(
            dataModel.getId(), dataModel.getTenant().getId());
        for (AlertRule rule : rules) {
            rule.setStatus(AlertRuleStatus.INVALID);
            rule.setIsEnabled(false);
            alertRuleRepository.save(rule);
            log.info("Alert rule {} invalidated due to data model deletion", rule.getId());
        }
    }

    private void syncMeasureNames(DataModel dataModel) {
        List<AlertRule> rules = alertRuleRepository.findByDataModelIdAndTenantId(
            dataModel.getId(), dataModel.getTenant().getId());

        for (AlertRule rule : rules) {
            if (rule.getMeasure() != null) {
                String currentMeasureName = rule.getMeasure().getName();
                if (!currentMeasureName.equals(rule.getMeasureName())) {
                    rule.setMeasureName(currentMeasureName);
                    alertRuleRepository.save(rule);
                    log.info("Synced measure name for rule {}: {}", rule.getId(), currentMeasureName);
                }
            }
        }
    }
}
