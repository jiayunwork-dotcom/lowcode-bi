-- ============================================================
-- Lowcode BI Platform - Database Initialization Script
-- ============================================================

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- ============================================================
-- Create Extensions
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- Tenants Table
-- ============================================================

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_connections INTEGER NOT NULL DEFAULT 20,
    settings JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_tenants_code ON tenants(code);
CREATE INDEX idx_tenants_status ON tenants(status);

-- ============================================================
-- Users Table
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    attributes JSONB,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(tenant_id, username),
    UNIQUE(tenant_id, email)
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- ============================================================
-- Data Sources Table
-- ============================================================

CREATE TABLE data_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    connection_config JSONB NOT NULL,
    connection_pool_config JSONB,
    is_encrypted BOOLEAN NOT NULL DEFAULT TRUE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_data_sources_tenant_id ON data_sources(tenant_id);
CREATE INDEX idx_data_sources_type ON data_sources(type);

-- ============================================================
-- CSV Uploads Table
-- ============================================================

CREATE TABLE csv_uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    column_count INTEGER NOT NULL,
    row_count INTEGER NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    columns_metadata JSONB NOT NULL,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    error_message TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_csv_uploads_tenant_id ON csv_uploads(tenant_id);
CREATE INDEX idx_csv_uploads_data_source_id ON csv_uploads(data_source_id);
CREATE INDEX idx_csv_uploads_status ON csv_uploads(upload_status);

-- ============================================================
-- Table Metadata Table
-- ============================================================

CREATE TABLE table_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    description TEXT,
    schema_name VARCHAR(50),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, data_source_id, name)
);

CREATE INDEX idx_table_metadata_tenant_id ON table_metadata(tenant_id);
CREATE INDEX idx_table_metadata_data_source_id ON table_metadata(data_source_id);

-- ============================================================
-- Column Metadata Table
-- ============================================================

CREATE TABLE column_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    table_id UUID NOT NULL REFERENCES table_metadata(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    data_type VARCHAR(50) NOT NULL,
    column_type VARCHAR(20) NOT NULL DEFAULT 'DIMENSION',
    is_nullable BOOLEAN NOT NULL DEFAULT TRUE,
    is_primary_key BOOLEAN NOT NULL DEFAULT FALSE,
    precision INTEGER,
    scale INTEGER,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, table_id, name)
);

CREATE INDEX idx_column_metadata_tenant_id ON column_metadata(tenant_id);
CREATE INDEX idx_column_metadata_table_id ON column_metadata(table_id);
CREATE INDEX idx_column_metadata_type ON column_metadata(column_type);

-- ============================================================
-- Table Relationships Table
-- ============================================================

CREATE TABLE table_relationships (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    source_table_id UUID NOT NULL REFERENCES table_metadata(id),
    target_table_id UUID NOT NULL REFERENCES table_metadata(id),
    relationship_type VARCHAR(20) NOT NULL,
    join_condition JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, source_table_id, target_table_id)
);

CREATE INDEX idx_table_relationships_tenant_id ON table_relationships(tenant_id);
CREATE INDEX idx_table_relationships_source ON table_relationships(source_table_id);
CREATE INDEX idx_table_relationships_target ON table_relationships(target_table_id);

-- ============================================================
-- Calculated Fields Table
-- ============================================================

CREATE TABLE calculated_fields (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    name VARCHAR(100) NOT NULL,
    expression TEXT NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    description TEXT,
    is_aggregate BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_calculated_fields_tenant_id ON calculated_fields(tenant_id);
CREATE INDEX idx_calculated_fields_data_source_id ON calculated_fields(data_source_id);

-- ============================================================
-- Measures Table
-- ============================================================

CREATE TABLE measures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    name VARCHAR(100) NOT NULL,
    column_id UUID REFERENCES column_metadata(id),
    aggregation_type VARCHAR(20) NOT NULL,
    filter_condition TEXT,
    description TEXT,
    format_pattern VARCHAR(50),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_measures_tenant_id ON measures(tenant_id);
CREATE INDEX idx_measures_data_source_id ON measures(data_source_id);
CREATE INDEX idx_measures_column_id ON measures(column_id);

-- ============================================================
-- Dimension Hierarchies Table
-- ============================================================

CREATE TABLE dimension_hierarchies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    name VARCHAR(100) NOT NULL,
    levels JSONB NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dimension_hierarchies_tenant_id ON dimension_hierarchies(tenant_id);
CREATE INDEX idx_dimension_hierarchies_data_source_id ON dimension_hierarchies(data_source_id);

-- ============================================================
-- Dashboards Table
-- ============================================================

CREATE TABLE dashboards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    layout_config JSONB NOT NULL,
    theme VARCHAR(20) NOT NULL DEFAULT 'light',
    auto_refresh_interval INTEGER DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_dashboards_tenant_id ON dashboards(tenant_id);
CREATE INDEX idx_dashboards_created_by ON dashboards(created_by);
CREATE INDEX idx_dashboards_is_published ON dashboards(is_published);
CREATE INDEX idx_dashboards_is_template ON dashboards(is_template);

-- ============================================================
-- Dashboard Components Table
-- ============================================================

CREATE TABLE dashboard_components (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    dashboard_id UUID NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    tab_id VARCHAR(50) NOT NULL,
    component_type VARCHAR(50) NOT NULL,
    title VARCHAR(100),
    position_config JSONB NOT NULL,
    data_config JSONB NOT NULL,
    style_config JSONB,
    interaction_config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dashboard_components_tenant_id ON dashboard_components(tenant_id);
CREATE INDEX idx_dashboard_components_dashboard_id ON dashboard_components(dashboard_id);
CREATE INDEX idx_dashboard_components_tab_id ON dashboard_components(tab_id);

-- ============================================================
-- Dashboard Permissions Table
-- ============================================================

CREATE TABLE dashboard_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    dashboard_id UUID NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, dashboard_id, user_id)
);

CREATE INDEX idx_dashboard_permissions_tenant_id ON dashboard_permissions(tenant_id);
CREATE INDEX idx_dashboard_permissions_dashboard_id ON dashboard_permissions(dashboard_id);
CREATE INDEX idx_dashboard_permissions_user_id ON dashboard_permissions(user_id);

-- ============================================================
-- Row Permission Rules Table
-- ============================================================

CREATE TABLE row_permission_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    value_type VARCHAR(20) NOT NULL,
    value VARCHAR(255) NOT NULL,
    user_roles VARCHAR(50)[] NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_row_permission_rules_tenant_id ON row_permission_rules(tenant_id);
CREATE INDEX idx_row_permission_rules_table ON row_permission_rules(table_name);
CREATE INDEX idx_row_permission_rules_enabled ON row_permission_rules(is_enabled);

-- ============================================================
-- Schedule Configs Table
-- ============================================================

CREATE TABLE schedule_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    dashboard_id UUID NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    cron_expression VARCHAR(100) NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
    email_subject VARCHAR(255) NOT NULL,
    email_body TEXT,
    recipients VARCHAR(255)[] NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_paused BOOLEAN NOT NULL DEFAULT FALSE,
    last_execution_at TIMESTAMP,
    last_execution_status VARCHAR(20),
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schedule_configs_tenant_id ON schedule_configs(tenant_id);
CREATE INDEX idx_schedule_configs_dashboard_id ON schedule_configs(dashboard_id);
CREATE INDEX idx_schedule_configs_enabled ON schedule_configs(is_enabled);

-- ============================================================
-- Schedule Execution Logs Table
-- ============================================================

CREATE TABLE schedule_execution_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    schedule_id UUID NOT NULL REFERENCES schedule_configs(id) ON DELETE CASCADE,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    duration_ms BIGINT NOT NULL,
    error_message TEXT,
    screenshot_path VARCHAR(255),
    email_sent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_schedule_execution_logs_tenant_id ON schedule_execution_logs(tenant_id);
CREATE INDEX idx_schedule_execution_logs_schedule_id ON schedule_execution_logs(schedule_id);
CREATE INDEX idx_schedule_execution_logs_executed_at ON schedule_execution_logs(executed_at);

-- ============================================================
-- Query Cache Table
-- ============================================================

CREATE TABLE query_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    query_hash VARCHAR(64) NOT NULL,
    query_sql TEXT NOT NULL,
    query_params JSONB,
    result_data JSONB,
    expires_at TIMESTAMP NOT NULL,
    hit_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, user_id, query_hash)
);

CREATE INDEX idx_query_cache_tenant_id ON query_cache(tenant_id);
CREATE INDEX idx_query_cache_user_id ON query_cache(user_id);
CREATE INDEX idx_query_cache_expires_at ON query_cache(expires_at);

-- ============================================================
-- Pre-aggregation Configs Table
-- ============================================================

CREATE TABLE pre_aggregation_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    name VARCHAR(100) NOT NULL,
    source_table VARCHAR(100) NOT NULL,
    target_table VARCHAR(100) NOT NULL,
    aggregation_sql TEXT NOT NULL,
    schedule_cron VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMP,
    last_run_status VARCHAR(20),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pre_aggregation_configs_tenant_id ON pre_aggregation_configs(tenant_id);
CREATE INDEX idx_pre_aggregation_configs_enabled ON pre_aggregation_configs(is_enabled);

-- ============================================================
-- SQL Queries Table
-- ============================================================

CREATE TABLE sql_queries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    data_source_id UUID NOT NULL REFERENCES data_sources(id),
    name VARCHAR(100) NOT NULL,
    query_sql TEXT NOT NULL,
    parameters JSONB,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sql_queries_tenant_id ON sql_queries(tenant_id);
CREATE INDEX idx_sql_queries_data_source_id ON sql_queries(data_source_id);
CREATE INDEX idx_sql_queries_created_by ON sql_queries(created_by);

-- ============================================================
-- Embedded Tokens Table
-- ============================================================

CREATE TABLE embedded_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    dashboard_id UUID NOT NULL REFERENCES dashboards(id),
    token_hash VARCHAR(64) NOT NULL,
    permissions JSONB,
    expires_at TIMESTAMP NOT NULL,
    domain_whitelist VARCHAR(255)[],
    hide_filters BOOLEAN NOT NULL DEFAULT FALSE,
    hide_title BOOLEAN NOT NULL DEFAULT FALSE,
    hide_toolbar BOOLEAN NOT NULL DEFAULT FALSE,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, token_hash)
);

CREATE INDEX idx_embedded_tokens_tenant_id ON embedded_tokens(tenant_id);
CREATE INDEX idx_embedded_tokens_dashboard_id ON embedded_tokens(dashboard_id);
CREATE INDEX idx_embedded_tokens_expires_at ON embedded_tokens(expires_at);

-- ============================================================
-- Triggers for updated_at columns
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'tenants', 'users', 'data_sources', 'table_metadata',
        'column_metadata', 'calculated_fields', 'measures',
        'dimension_hierarchies', 'dashboards', 'dashboard_components',
        'row_permission_rules', 'schedule_configs', 'pre_aggregation_configs',
        'sql_queries'
    ] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS update_%s_updated_at ON %I', t, t);
        EXECUTE format('CREATE TRIGGER update_%s_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END $$;

-- ============================================================
-- Insert Default Data
-- ============================================================

INSERT INTO tenants (id, name, code, status, max_connections)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Tenant', 'default', 'ACTIVE', 20)
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (id, tenant_id, username, email, password_hash, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    'ACTIVE'
)
ON CONFLICT (tenant_id, username) DO NOTHING;
