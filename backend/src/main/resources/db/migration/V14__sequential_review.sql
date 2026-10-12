ALTER TABLE work_plan DROP CONSTRAINT work_plan_document_state_check;
ALTER TABLE work_plan ADD CONSTRAINT work_plan_document_state_check CHECK (document_state IN ('DRAFT','IN_REVIEW','APPROVED'));
CREATE TABLE document_review (
    id UUID PRIMARY KEY,
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    artifact_id UUID NOT NULL,
    author_signature_id UUID NOT NULL REFERENCES signed_document_artifact(id),
    current_file_id UUID NOT NULL REFERENCES stored_file(id),
    round_number INTEGER NOT NULL CHECK(round_number>0),
    status VARCHAR(20) NOT NULL CHECK(status IN ('IN_REVIEW','RETURNED','APPROVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    FOREIGN KEY(artifact_id,work_plan_id) REFERENCES document_artifact(id,work_plan_id),
    UNIQUE(work_plan_id,round_number)
);
CREATE UNIQUE INDEX one_open_review ON document_review(work_plan_id) WHERE status='IN_REVIEW';
CREATE TABLE review_task (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES document_review(id),
    actor_id UUID NOT NULL REFERENCES app_user(id),
    actor_name VARCHAR(160) NOT NULL,
    stage_label VARCHAR(160) NOT NULL,
    stage_action VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL CHECK(position>=0),
    requires_signature BOOLEAN NOT NULL,
    signature_action VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','ACTIVE','COMPLETED','RETURNED','CANCELLED')),
    row_version BIGINT NOT NULL DEFAULT 0,
    output_file_id UUID REFERENCES stored_file(id),
    request_key UUID,
    binding_id UUID REFERENCES signing_certificate_binding(id),
    acted_at TIMESTAMPTZ,
    UNIQUE(review_id,position),
    UNIQUE(review_id,actor_id)
);
CREATE UNIQUE INDEX one_active_review_task ON review_task(review_id) WHERE status='ACTIVE';
CREATE INDEX review_task_actor_idx ON review_task(actor_id,status);
CREATE UNIQUE INDEX review_task_request_idx ON review_task(actor_id,request_key) WHERE request_key IS NOT NULL;
CREATE TABLE review_observation (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES document_review(id),
    task_id UUID NOT NULL REFERENCES review_task(id),
    actor_id UUID NOT NULL REFERENCES app_user(id),
    message VARCHAR(4000) NOT NULL CHECK(length(trim(message))>0),
    page_index INTEGER,
    x DOUBLE PRECISION,y DOUBLE PRECISION,width DOUBLE PRECISION,height DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (page_index IS NULL OR page_index>=0),
    CHECK ((x IS NULL AND y IS NULL AND width IS NULL AND height IS NULL)
      OR (page_index IS NOT NULL AND x>=0 AND y>=0 AND width>0 AND height>0 AND x+width<=1.000001 AND y+height<=1.000001))
);
ALTER TABLE personal_notification DROP CONSTRAINT personal_notification_kind_check;
ALTER TABLE personal_notification ADD CONSTRAINT personal_notification_kind_check CHECK(kind IN
  ('MEMBERSHIP_ASSIGNED','MEMBERSHIP_REMOVED','T1_PREVIEW_GENERATED','REVIEW_TASK_ASSIGNED','DOCUMENT_RETURNED','DOCUMENT_APPROVED'));
