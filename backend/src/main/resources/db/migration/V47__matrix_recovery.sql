CREATE TABLE work_plan_matrix_recovery (
    work_plan_id UUID NOT NULL REFERENCES work_plan(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    base_version BIGINT NOT NULL CHECK (base_version >= 0),
    revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0),
    content JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (work_plan_id, user_id)
);
