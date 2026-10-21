ALTER TABLE audit_event ADD COLUMN subject_user_id UUID REFERENCES app_user(id);
