ALTER TABLE period_archive ADD COLUMN custody_verified_at TIMESTAMPTZ;
ALTER TABLE period_archive ADD COLUMN custody_verified_by UUID REFERENCES app_user(id);
