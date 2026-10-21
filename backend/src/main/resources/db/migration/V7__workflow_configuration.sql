CREATE TABLE workflow_configuration (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES institutional_group(id),
    document_type VARCHAR(2) NOT NULL CHECK(document_type IN ('T1','T2')),
    row_version BIGINT NOT NULL DEFAULT 0,
    draft JSONB NOT NULL,
    current_revision_id UUID,
    UNIQUE(group_id,document_type)
);
CREATE TABLE workflow_revision (
    id UUID PRIMARY KEY,
    configuration_id UUID NOT NULL REFERENCES workflow_configuration(id),
    revision_number INTEGER NOT NULL CHECK(revision_number>0),
    definition JSONB NOT NULL,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    configured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(configuration_id,revision_number)
);
ALTER TABLE workflow_configuration ADD CONSTRAINT workflow_current_revision_fk FOREIGN KEY(current_revision_id) REFERENCES workflow_revision(id);
