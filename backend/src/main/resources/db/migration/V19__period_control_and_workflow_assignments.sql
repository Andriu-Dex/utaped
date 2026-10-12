ALTER TABLE academic_period ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','CLOSED'));
ALTER TABLE academic_period ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE academic_period ADD COLUMN workflow_locked_at TIMESTAMPTZ;
ALTER TABLE academic_period ADD COLUMN workflow_locked_by UUID REFERENCES app_user(id);
ALTER TABLE academic_period ADD COLUMN closed_at TIMESTAMPTZ;
ALTER TABLE academic_period ADD COLUMN closed_by UUID REFERENCES app_user(id);
ALTER TABLE academic_period ADD COLUMN closure_reason VARCHAR(1000);

CREATE TABLE period_workflow_assignment (
    scope_id UUID NOT NULL REFERENCES organizational_scope(id),
    document_type VARCHAR(2) NOT NULL CHECK (document_type IN ('T1','T2')),
    revision_id UUID NOT NULL REFERENCES workflow_revision(id),
    frozen_workflow JSONB,
    row_version BIGINT NOT NULL DEFAULT 1,
    assigned_by UUID REFERENCES app_user(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (scope_id,document_type)
);
-- Existing scoped data keeps its published configuration as an initial assignment.
-- Activation is explicit; existing document snapshots and signed bytes are untouched.
INSERT INTO period_workflow_assignment(scope_id,document_type,revision_id)
SELECT s.id,c.document_type,c.current_revision_id FROM organizational_scope s
JOIN workflow_configuration c ON c.group_id=s.group_id WHERE c.current_revision_id IS NOT NULL;

ALTER TABLE stored_file DROP CONSTRAINT stored_file_page_count_check;
ALTER TABLE stored_file ADD CONSTRAINT stored_file_page_count_check CHECK(page_count >= 0);
CREATE TABLE period_archive (
    period_id UUID PRIMARY KEY REFERENCES academic_period(id),
    file_id UUID NOT NULL REFERENCES stored_file(id),
    document_count INTEGER NOT NULL,
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
