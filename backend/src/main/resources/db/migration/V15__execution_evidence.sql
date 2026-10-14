CREATE TABLE activity_extension (
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    activity_id UUID NOT NULL,
    ends_on DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    granted_by UUID NOT NULL REFERENCES app_user(id),
    row_version BIGINT NOT NULL DEFAULT 0,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(work_plan_id,activity_id)
);
CREATE TABLE execution_evidence (
    id UUID PRIMARY KEY,
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    activity_id UUID NOT NULL,
    medium_index INTEGER NOT NULL CHECK(medium_index>=0),
    version_number INTEGER NOT NULL CHECK(version_number>0),
    file_id UUID NOT NULL REFERENCES stored_file(id),
    uploaded_by UUID NOT NULL REFERENCES app_user(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(work_plan_id,activity_id,medium_index,version_number)
);
CREATE INDEX evidence_document_idx ON execution_evidence(work_plan_id,activity_id,medium_index,version_number DESC);
