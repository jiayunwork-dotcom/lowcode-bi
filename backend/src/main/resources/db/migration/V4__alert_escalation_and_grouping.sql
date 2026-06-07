-- ============================================================
-- Alert Escalation Policy and Grouping Enhancements
-- ============================================================

-- ============================================================
-- Add escalation columns to alert_rules
-- ============================================================
ALTER TABLE alert_rules 
ADD COLUMN escalation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN escalation_threshold INTEGER NOT NULL DEFAULT 3,
ADD COLUMN consecutive_trigger_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN escalation_level INTEGER NOT NULL DEFAULT 0,
ADD COLUMN current_severity VARCHAR(32) NOT NULL DEFAULT 'WARNING';

-- ============================================================
-- Alert Escalation Recipients Table
-- ============================================================
CREATE TABLE alert_escalation_recipients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, alert_rule_id, user_id)
);

CREATE INDEX idx_alert_escalation_recipients_tenant_id ON alert_escalation_recipients(tenant_id);
CREATE INDEX idx_alert_escalation_recipients_rule_id ON alert_escalation_recipients(alert_rule_id);
CREATE INDEX idx_alert_escalation_recipients_user_id ON alert_escalation_recipients(user_id);

-- ============================================================
-- Initialize current_severity from existing severity
-- ============================================================
UPDATE alert_rules SET current_severity = severity WHERE current_severity IS NULL;

-- ============================================================
-- Add updated_at trigger for new table
-- ============================================================
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['alert_escalation_recipients'] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS update_%s_updated_at ON %I', t, t);
        EXECUTE format('CREATE TRIGGER update_%s_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END $$;
