CREATE TABLE user_import_batch (
    id UUID PRIMARY KEY,
    created_by UUID NOT NULL REFERENCES app_user(id),
    source_name VARCHAR(200) NOT NULL,
    source_sha256 CHAR(64) NOT NULL,
    rows JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT now()+interval '30 minutes',
    committed_at TIMESTAMPTZ,
    imported_count INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX user_import_owner_idx ON user_import_batch(created_by,created_at DESC);
ALTER TABLE planning_catalog ADD COLUMN evidence_kind VARCHAR(10) NOT NULL DEFAULT 'PDF' CHECK(evidence_kind IN ('PDF','URL'));
ALTER TABLE planning_catalog ADD CONSTRAINT resource_not_external CHECK(kind='MEANS' OR evidence_kind='PDF');
ALTER TABLE execution_evidence ALTER COLUMN file_id DROP NOT NULL;
ALTER TABLE execution_evidence ADD COLUMN evidence_kind VARCHAR(10) NOT NULL DEFAULT 'PDF';
ALTER TABLE execution_evidence ADD COLUMN external_url VARCHAR(2048);
ALTER TABLE execution_evidence ADD COLUMN reference_title VARCHAR(200);
ALTER TABLE execution_evidence ADD CONSTRAINT evidence_payload_check CHECK(
    (evidence_kind='PDF' AND file_id IS NOT NULL AND external_url IS NULL AND reference_title IS NULL)
    OR (evidence_kind='URL' AND file_id IS NULL AND external_url IS NOT NULL AND reference_title IS NOT NULL));
