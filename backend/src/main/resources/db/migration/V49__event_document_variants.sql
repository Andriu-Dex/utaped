ALTER TABLE work_plan ADD COLUMN document_variant VARCHAR(20) NOT NULL DEFAULT 'GENERAL' CHECK (document_variant IN ('GENERAL','EVENT'));
DROP INDEX work_plan_active_scope_idx;
CREATE UNIQUE INDEX work_plan_active_scope_idx ON work_plan(scope_id) WHERE archived_at IS NULL AND scope_id IS NOT NULL AND document_type='T1' AND document_variant='GENERAL';
CREATE TABLE event_document (
    work_plan_id UUID PRIMARY KEY REFERENCES work_plan(id),
    data JSONB NOT NULL,
    CHECK (jsonb_typeof(data) = 'object')
);
