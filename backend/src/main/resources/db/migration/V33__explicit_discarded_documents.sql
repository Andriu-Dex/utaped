ALTER TABLE work_plan ADD COLUMN discarded BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE work_plan w SET discarded=TRUE WHERE w.archived_at IS NOT NULL AND w.document_state='DRAFT' AND EXISTS(SELECT 1 FROM audit_event a WHERE a.target_id=w.id AND a.action='DOCUMENT_ARCHIVED');
