ALTER TABLE work_plan ADD COLUMN attachments_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE work_plan ADD COLUMN privacy_notice_enabled BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE stored_file (
    id UUID PRIMARY KEY,
    storage_key UUID NOT NULL UNIQUE,
    sha256 CHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK(size_bytes > 0),
    original_name VARCHAR(200) NOT NULL,
    page_count INTEGER NOT NULL CHECK(page_count > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE document_attachment (
    id UUID PRIMARY KEY,
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    request_key UUID NOT NULL,
    file_id UUID NOT NULL REFERENCES stored_file(id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL CHECK(sort_order >= 0),
    removed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(work_plan_id,request_key)
);
CREATE INDEX document_attachment_plan_idx ON document_attachment(work_plan_id,removed_at,sort_order);
CREATE TABLE document_artifact (
    id UUID PRIMARY KEY,
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    actor_id UUID NOT NULL REFERENCES app_user(id),
    source_row_version BIGINT NOT NULL,
    source_hash CHAR(64) NOT NULL,
    template_version VARCHAR(100) NOT NULL,
    file_id UUID NOT NULL REFERENCES stored_file(id),
    snapshot JSONB NOT NULL,
    pages JSONB NOT NULL CHECK(jsonb_typeof(pages)='array'),
    signature_slots JSONB NOT NULL CHECK(jsonb_typeof(signature_slots)='array'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(work_plan_id,source_hash,template_version)
);
CREATE INDEX document_artifact_plan_idx ON document_artifact(work_plan_id,created_at DESC,id);
