-- T3: execution report (UTA-SGC-A-1-1-P4-T2), derived from an approved Plan and numbered per Plan.
ALTER TABLE work_plan DROP CONSTRAINT work_plan_document_type_check;
ALTER TABLE work_plan ADD CONSTRAINT work_plan_document_type_check CHECK (document_type IN ('T1','T2','T3'));
ALTER TABLE workflow_configuration DROP CONSTRAINT workflow_configuration_document_type_check;
ALTER TABLE workflow_configuration ADD CONSTRAINT workflow_configuration_document_type_check CHECK (document_type IN ('T1','T2','T3'));
ALTER TABLE period_workflow_assignment DROP CONSTRAINT period_workflow_assignment_document_type_check;
ALTER TABLE period_workflow_assignment ADD CONSTRAINT period_workflow_assignment_document_type_check CHECK (document_type IN ('T1','T2','T3'));

CREATE TABLE execution_report (
    id UUID PRIMARY KEY REFERENCES work_plan(id),
    base_plan_id UUID NOT NULL REFERENCES work_plan(id),
    base_artifact_id UUID NOT NULL REFERENCES document_artifact(id),
    base_formal_version VARCHAR(20) NOT NULL,
    sequence_number INTEGER NOT NULL CHECK (sequence_number > 0),
    activity_ids JSONB NOT NULL DEFAULT '[]',
    results JSONB NOT NULL DEFAULT '[]',
    analysis TEXT NOT NULL DEFAULT '' CHECK (length(analysis) <= 50000),
    strengths TEXT NOT NULL DEFAULT '' CHECK (length(strengths) <= 20000),
    weaknesses TEXT NOT NULL DEFAULT '' CHECK (length(weaknesses) <= 20000),
    opportunities TEXT NOT NULL DEFAULT '' CHECK (length(opportunities) <= 20000),
    threats TEXT NOT NULL DEFAULT '' CHECK (length(threats) <= 20000),
    conclusions TEXT NOT NULL DEFAULT '' CHECK (length(conclusions) <= 50000),
    recommendations TEXT NOT NULL DEFAULT '' CHECK (length(recommendations) <= 50000),
    improvement_actions JSONB NOT NULL DEFAULT '[]',
    evidence_link VARCHAR(2000) NOT NULL DEFAULT ''
);
CREATE INDEX execution_report_base_idx ON execution_report(base_plan_id, sequence_number);
