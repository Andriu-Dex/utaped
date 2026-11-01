ALTER TABLE review_task ADD COLUMN slot_actor_id UUID REFERENCES app_user(id);

CREATE TABLE review_task_reassignment (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES review_task(id),
    previous_actor_id UUID NOT NULL REFERENCES app_user(id),
    previous_actor_name VARCHAR(160) NOT NULL,
    actor_id UUID NOT NULL REFERENCES app_user(id),
    actor_name VARCHAR(160) NOT NULL,
    authorized_by UUID NOT NULL REFERENCES app_user(id),
    authorized_by_name VARCHAR(160) NOT NULL,
    reason VARCHAR(1000) NOT NULL CHECK(length(trim(reason))>0),
    previous_row_version BIGINT NOT NULL,
    request_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(task_id,request_key)
);
CREATE INDEX review_task_reassignment_task_idx ON review_task_reassignment(task_id,created_at);

CREATE TABLE plan_copy_batch (
    request_key UUID PRIMARY KEY,
    source_document_id UUID NOT NULL REFERENCES work_plan(id),
    actor_id UUID NOT NULL REFERENCES app_user(id),
    source_row_version BIGINT NOT NULL,
    target_scope_ids JSONB NOT NULL,
    document_ids JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
