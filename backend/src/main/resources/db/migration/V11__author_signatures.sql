CREATE TABLE signing_certificate_binding (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    fingerprint CHAR(64) NOT NULL CHECK(fingerprint ~ '^[a-f0-9]{64}$'),
    verification_note VARCHAR(500) NOT NULL,
    verified_by UUID NOT NULL REFERENCES app_user(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX signing_binding_user_active ON signing_certificate_binding(user_id) WHERE active;
CREATE UNIQUE INDEX signing_binding_fingerprint_active ON signing_certificate_binding(fingerprint) WHERE active;
ALTER TABLE document_artifact ADD CONSTRAINT artifact_document_unique UNIQUE(id,work_plan_id);
CREATE TABLE signed_document_artifact (
    id UUID PRIMARY KEY,
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    artifact_id UUID NOT NULL,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    binding_id UUID NOT NULL REFERENCES signing_certificate_binding(id),
    file_id UUID NOT NULL REFERENCES stored_file(id),
    request_key UUID NOT NULL,
    source_row_version BIGINT NOT NULL,
    input_hash CHAR(64) NOT NULL,
    output_hash CHAR(64) NOT NULL,
    signer_name VARCHAR(160) NOT NULL,
    signed_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY(artifact_id,work_plan_id) REFERENCES document_artifact(id,work_plan_id),
    UNIQUE(actor_id,request_key),
    UNIQUE(artifact_id,actor_id)
);
CREATE INDEX signed_artifact_document_idx ON signed_document_artifact(work_plan_id,signed_at DESC);
