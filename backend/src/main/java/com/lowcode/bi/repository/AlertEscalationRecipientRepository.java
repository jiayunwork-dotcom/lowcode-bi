package com.lowcode.bi.repository;

import com.lowcode.bi.entity.AlertEscalationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertEscalationRecipientRepository extends JpaRepository<AlertEscalationRecipient, UUID> {

    List<AlertEscalationRecipient> findByAlertRuleId(UUID ruleId);

    Optional<AlertEscalationRecipient> findByAlertRuleIdAndUserId(UUID ruleId, UUID userId);

    void deleteByAlertRuleId(UUID ruleId);
}
