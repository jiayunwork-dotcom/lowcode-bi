-- ============================================================
-- Alert & Subscription System Schema
-- ============================================================

-- ============================================================
-- Alert Rules Table
-- ============================================================
CREATE TABLE alert_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_model_id UUID NOT NULL REFERENCES data_models(id) ON DELETE CASCADE,
    measure_id UUID NOT NULL REFERENCES measures(id) ON DELETE CASCADE,
    measure_name VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    trigger_type VARCHAR(32) NOT NULL,
    operator VARCHAR(32) NOT NULL,
    threshold DECIMAL(20,6) NOT NULL,
    check_interval VARCHAR(32) NOT NULL,
    silence_period INTEGER NOT NULL DEFAULT 300,
    severity VARCHAR(32) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_triggered_at TIMESTAMP,
    last_checked_at TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_rules_tenant_id ON alert_rules(tenant_id);
CREATE INDEX idx_alert_rules_data_model_id ON alert_rules(data_model_id);
CREATE INDEX idx_alert_rules_measure_id ON alert_rules(measure_id);
CREATE INDEX idx_alert_rules_is_enabled ON alert_rules(is_enabled);
CREATE INDEX idx_alert_rules_status ON alert_rules(status);
CREATE INDEX idx_alert_rules_severity ON alert_rules(severity);

-- ============================================================
-- Alert Notification Channels Table
-- ============================================================
CREATE TABLE alert_notification_channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    channel_type VARCHAR(32) NOT NULL,
    config JSONB NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_channels_tenant_id ON alert_notification_channels(tenant_id);
CREATE INDEX idx_alert_channels_rule_id ON alert_notification_channels(alert_rule_id);
CREATE INDEX idx_alert_channels_type ON alert_notification_channels(channel_type);

-- ============================================================
-- Alert Subscriptions Table
-- ============================================================
CREATE TABLE alert_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_subscribed BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, alert_rule_id, user_id)
);

CREATE INDEX idx_alert_subs_tenant_id ON alert_subscriptions(tenant_id);
CREATE INDEX idx_alert_subs_rule_id ON alert_subscriptions(alert_rule_id);
CREATE INDEX idx_alert_subs_user_id ON alert_subscriptions(user_id);
CREATE INDEX idx_alert_subs_is_subscribed ON alert_subscriptions(is_subscribed);

-- ============================================================
-- Alert Events Table
-- ============================================================
CREATE TABLE alert_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    trigger_value DECIMAL(20,6) NOT NULL,
    threshold DECIMAL(20,6) NOT NULL,
    previous_value DECIMAL(20,6),
    change_percent DECIMAL(10,4),
    severity VARCHAR(32) NOT NULL,
    event_status VARCHAR(32) NOT NULL DEFAULT 'FIRING',
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    acknowledged_at TIMESTAMP,
    acknowledged_by UUID REFERENCES users(id),
    is_recovered BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_value DECIMAL(20,6),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_events_tenant_id ON alert_events(tenant_id);
CREATE INDEX idx_alert_events_rule_id ON alert_events(alert_rule_id);
CREATE INDEX idx_alert_events_status ON alert_events(event_status);
CREATE INDEX idx_alert_events_severity ON alert_events(severity);
CREATE INDEX idx_alert_events_triggered_at ON alert_events(triggered_at);
CREATE INDEX idx_alert_events_is_recovered ON alert_events(is_recovered);

-- ============================================================
-- Alert Notifications Table (Notification Send Log)
-- ============================================================
CREATE TABLE alert_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    alert_event_id UUID NOT NULL REFERENCES alert_events(id) ON DELETE CASCADE,
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    channel_type VARCHAR(32) NOT NULL,
    recipient VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    error_message VARCHAR(1024),
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_notifs_tenant_id ON alert_notifications(tenant_id);
CREATE INDEX idx_alert_notifs_event_id ON alert_notifications(alert_event_id);
CREATE INDEX idx_alert_notifs_rule_id ON alert_notifications(alert_rule_id);
CREATE INDEX idx_alert_notifs_status ON alert_notifications(status);
CREATE INDEX idx_alert_notifs_created_at ON alert_notifications(created_at);

-- ============================================================
-- System Messages Table (Internal Messages)
-- ============================================================
CREATE TABLE system_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type VARCHAR(32) NOT NULL,
    title VARCHAR(256) NOT NULL,
    content TEXT,
    related_type VARCHAR(64),
    related_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_messages_tenant_id ON system_messages(tenant_id);
CREATE INDEX idx_system_messages_user_id ON system_messages(user_id);
CREATE INDEX idx_system_messages_is_read ON system_messages(is_read);
CREATE INDEX idx_system_messages_created_at ON system_messages(created_at);
CREATE INDEX idx_system_messages_type ON system_messages(message_type);

-- ============================================================
-- Email Queue Table
-- ============================================================
CREATE TABLE email_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    to_email VARCHAR(256) NOT NULL,
    subject VARCHAR(512) NOT NULL,
    body TEXT NOT NULL,
    is_html BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    error_message VARCHAR(1024),
    retry_count INTEGER NOT NULL DEFAULT 0,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_queue_tenant_id ON email_queue(tenant_id);
CREATE INDEX idx_email_queue_status ON email_queue(status);
CREATE INDEX idx_email_queue_created_at ON email_queue(created_at);
CREATE INDEX idx_email_queue_priority ON email_queue(priority);

-- ============================================================
-- Add updated_at triggers for new tables
-- ============================================================
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'alert_rules', 'alert_notification_channels', 'alert_subscriptions',
        'alert_events', 'alert_notifications', 'system_messages', 'email_queue'
    ] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS update_%s_updated_at ON %I', t, t);
        EXECUTE format('CREATE TRIGGER update_%s_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END $$;
