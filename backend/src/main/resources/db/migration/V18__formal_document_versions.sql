ALTER TABLE work_plan ADD COLUMN previous_version_id UUID UNIQUE REFERENCES work_plan(id);
ALTER TABLE work_plan ADD COLUMN version_reason VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE work_plan ADD COLUMN archive_reason VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE work_plan ADD COLUMN archived_by UUID REFERENCES app_user(id);
ALTER TABLE work_report DROP CONSTRAINT work_report_base_plan_id_key;
ALTER TABLE work_report ADD COLUMN previous_version_id UUID UNIQUE REFERENCES work_report(id);
CREATE UNIQUE INDEX report_family_root_idx ON work_report(base_plan_id) WHERE previous_version_id IS NULL;
CREATE UNIQUE INDEX active_report_family_idx ON work_plan(base_plan_id) WHERE document_type='T2' AND archived_at IS NULL;
