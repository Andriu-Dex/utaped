ALTER TABLE institutional_position ADD COLUMN can_view_history BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE formal_submission (work_plan_id UUID PRIMARY KEY REFERENCES work_plan(id), file_id UUID NOT NULL REFERENCES stored_file(id), registered_by UUID NOT NULL REFERENCES app_user(id), registered_at TIMESTAMPTZ NOT NULL DEFAULT now());
