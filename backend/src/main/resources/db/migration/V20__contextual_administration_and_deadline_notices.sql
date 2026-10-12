ALTER TABLE scope_member ADD COLUMN password_reset_authorized BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE scope_membership_history (
    id UUID PRIMARY KEY,
    scope_id UUID NOT NULL REFERENCES organizational_scope(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    membership_role VARCHAR(30) NOT NULL,
    position_label VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL,
    password_reset_authorized BOOLEAN NOT NULL,
    row_version BIGINT NOT NULL,
    changed_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    origin VARCHAR(20) NOT NULL CHECK(origin IN ('BASELINE','CHANGE'))
);
INSERT INTO scope_membership_history
SELECT gen_random_uuid(),scope_id,user_id,membership_role,position_label,active,FALSE,row_version,NULL,now(),'BASELINE' FROM scope_member;
CREATE INDEX scope_membership_history_scope_idx ON scope_membership_history(scope_id,created_at DESC,id);
CREATE TABLE scope_principal_history (
    id UUID PRIMARY KEY,
    scope_id UUID NOT NULL REFERENCES organizational_scope(id),
    previous_user_id UUID REFERENCES app_user(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    changed_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    origin VARCHAR(20) NOT NULL CHECK(origin IN ('BASELINE','CHANGE'))
);
INSERT INTO scope_principal_history(id,scope_id,user_id,origin)
SELECT gen_random_uuid(),id,principal_user_id,'BASELINE' FROM organizational_scope WHERE principal_user_id IS NOT NULL;
ALTER TABLE personal_notification DROP CONSTRAINT personal_notification_kind_check;
ALTER TABLE personal_notification ADD CONSTRAINT personal_notification_kind_check CHECK(kind IN
 ('MEMBERSHIP_ASSIGNED','MEMBERSHIP_REMOVED','T1_PREVIEW_GENERATED','T2_PREVIEW_GENERATED','REVIEW_TASK_ASSIGNED','DOCUMENT_RETURNED','DOCUMENT_APPROVED','ACTIVITY_EXTENSION_GRANTED','DOCUMENT_ARCHIVED','ACTIVITY_DEADLINE_NEAR','EVIDENCE_MISSING','ADMIN_PASSWORD_RESET','COORDINATOR_PASSWORD_RESET'));
ALTER TABLE personal_notification DROP CONSTRAINT personal_notification_target_type_check;
ALTER TABLE personal_notification ADD CONSTRAINT personal_notification_target_type_check CHECK(target_type IN ('GROUP','WORK_PLAN','ACCOUNT'));
CREATE TABLE deadline_notice_delivery (
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    activity_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id),
    deadline DATE NOT NULL,
    kind VARCHAR(30) NOT NULL CHECK(kind IN ('ACTIVITY_DEADLINE_NEAR','EVIDENCE_MISSING')),
    sent_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(work_plan_id,activity_id,user_id,deadline,kind,sent_on)
);
