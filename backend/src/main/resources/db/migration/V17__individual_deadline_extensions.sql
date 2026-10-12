CREATE TABLE deadline_extension (
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    activity_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id),
    ends_on DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    granted_by UUID NOT NULL REFERENCES app_user(id),
    row_version BIGINT NOT NULL DEFAULT 0,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(work_plan_id,activity_id,user_id)
);
CREATE TABLE deadline_extension_history (
    id UUID PRIMARY KEY,
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    activity_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id),
    old_deadline DATE NOT NULL,
    new_deadline DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    authorized_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO deadline_extension(work_plan_id,activity_id,user_id,ends_on,reason,granted_by,row_version,granted_at)
SELECT e.work_plan_id,e.activity_id,u.id,e.ends_on,e.reason,e.granted_by,e.row_version,e.granted_at
FROM activity_extension e JOIN work_plan_matrix m ON m.work_plan_id=e.work_plan_id
CROSS JOIN LATERAL jsonb_array_elements(m.activities) a
CROSS JOIN LATERAL jsonb_array_elements_text(a->'responsibleIds') r
JOIN app_user u ON u.id=r.value::uuid WHERE (a->>'id')::uuid=e.activity_id;
INSERT INTO deadline_extension_history(id,work_plan_id,activity_id,user_id,old_deadline,new_deadline,reason,authorized_by,created_at)
SELECT gen_random_uuid(),e.work_plan_id,e.activity_id,e.user_id,(a->>'endsOn')::date,e.ends_on,e.reason,e.granted_by,e.granted_at
FROM deadline_extension e JOIN work_plan_matrix m ON m.work_plan_id=e.work_plan_id
CROSS JOIN LATERAL jsonb_array_elements(m.activities) a WHERE (a->>'id')::uuid=e.activity_id;
ALTER TABLE personal_notification DROP CONSTRAINT personal_notification_kind_check;
ALTER TABLE personal_notification ADD CONSTRAINT personal_notification_kind_check CHECK(kind IN
 ('MEMBERSHIP_ASSIGNED','MEMBERSHIP_REMOVED','T1_PREVIEW_GENERATED','T2_PREVIEW_GENERATED','REVIEW_TASK_ASSIGNED','DOCUMENT_RETURNED','DOCUMENT_APPROVED','ACTIVITY_EXTENSION_GRANTED','DOCUMENT_ARCHIVED'));
