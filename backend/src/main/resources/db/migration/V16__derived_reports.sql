ALTER TABLE work_plan ADD COLUMN document_type VARCHAR(2) NOT NULL DEFAULT 'T1' CHECK(document_type IN ('T1','T2'));
ALTER TABLE work_plan ADD COLUMN base_plan_id UUID REFERENCES work_plan(id);
DROP INDEX work_plan_active_scope_idx;
CREATE UNIQUE INDEX work_plan_active_scope_idx ON work_plan(scope_id) WHERE archived_at IS NULL AND scope_id IS NOT NULL AND document_type='T1';
CREATE TABLE work_report (
    id UUID PRIMARY KEY REFERENCES work_plan(id),
    base_plan_id UUID NOT NULL UNIQUE REFERENCES work_plan(id),
    base_artifact_id UUID NOT NULL REFERENCES document_artifact(id),
    base_formal_version VARCHAR(20) NOT NULL,
    background TEXT NOT NULL DEFAULT '' CHECK(length(background)<=50000),
    conclusions TEXT NOT NULL DEFAULT '' CHECK(length(conclusions)<=50000),
    improvements TEXT NOT NULL DEFAULT '' CHECK(length(improvements)<=50000),
    results JSONB NOT NULL DEFAULT '[]',
    delegations JSONB NOT NULL DEFAULT '[]'
);
