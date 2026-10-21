-- The archive row outlives its ZIP: after a purge only the inventory and hash remain.
ALTER TABLE period_archive ALTER COLUMN file_id DROP NOT NULL;
ALTER TABLE period_archive ADD COLUMN archive_sha256 CHAR(64);
ALTER TABLE period_archive ADD COLUMN archive_size BIGINT;
ALTER TABLE period_archive ADD COLUMN custody_downloaded_at TIMESTAMPTZ;
ALTER TABLE period_archive ADD COLUMN custody_downloaded_by UUID REFERENCES app_user(id);
ALTER TABLE period_archive ADD COLUMN purged_at TIMESTAMPTZ;
ALTER TABLE period_archive ADD COLUMN purged_by UUID REFERENCES app_user(id);
ALTER TABLE period_archive ADD COLUMN purge_reason VARCHAR(1000);
ALTER TABLE period_archive ADD COLUMN purge_inventory JSONB;
UPDATE period_archive a SET archive_sha256=f.sha256,archive_size=f.size_bytes FROM stored_file f WHERE f.id=a.file_id;
ALTER TABLE period_archive ALTER COLUMN archive_sha256 SET NOT NULL;
ALTER TABLE period_archive ALTER COLUMN archive_size SET NOT NULL;
