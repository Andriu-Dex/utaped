CREATE TABLE work_plan (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES app_user(id),
    group_id UUID NOT NULL REFERENCES institutional_group(id),
    period_id UUID NOT NULL REFERENCES academic_period(id),
    request_key UUID NOT NULL,
    creation_title VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    institutional_unit VARCHAR(200) NOT NULL DEFAULT '',
    career VARCHAR(200) NOT NULL DEFAULT '',
    justification TEXT NOT NULL DEFAULT '',
    objective TEXT NOT NULL DEFAULT '',
    document_state VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (document_state IN ('DRAFT')),
    formal_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    row_version BIGINT NOT NULL DEFAULT 0,
    preparation_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (teacher_id, request_key),
    CHECK (char_length(justification) <= 50000),
    CHECK (char_length(objective) <= 50000)
);
CREATE INDEX work_plan_owner_updated_idx ON work_plan(teacher_id, updated_at DESC, id);
CREATE INDEX work_plan_scope_idx ON work_plan(teacher_id, group_id, period_id);
