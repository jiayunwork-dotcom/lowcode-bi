-- ============================================================
-- Add CSV auto-refresh columns to data_sources table
-- ============================================================

ALTER TABLE data_sources 
ADD COLUMN IF NOT EXISTS csv_refresh_interval VARCHAR(32) DEFAULT 'MANUAL';

ALTER TABLE data_sources 
ADD COLUMN IF NOT EXISTS csv_refresh_directory VARCHAR(512);

ALTER TABLE data_sources 
ADD COLUMN IF NOT EXISTS csv_last_import_time TIMESTAMP;

ALTER TABLE data_sources 
ADD COLUMN IF NOT EXISTS csv_last_refresh_status VARCHAR(32);

ALTER TABLE data_sources 
ADD COLUMN IF NOT EXISTS csv_last_refresh_error VARCHAR(1024);

ALTER TABLE data_sources 
ADD COLUMN IF NOT EXISTS csv_refresh_in_progress BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_data_sources_csv_refresh_interval ON data_sources(csv_refresh_interval);
CREATE INDEX IF NOT EXISTS idx_data_sources_csv_refresh_status ON data_sources(csv_last_refresh_status);
