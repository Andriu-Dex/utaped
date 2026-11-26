-- Existing artifacts and review outcomes remain unchanged.
ALTER TABLE work_plan DROP CONSTRAINT work_plan_document_state_check;
ALTER TABLE work_plan ADD CONSTRAINT work_plan_document_state_check CHECK (document_state IN ('DRAFT','IN_REVIEW','APPROVED','VALIDATED'));
ALTER TABLE document_review DROP CONSTRAINT document_review_status_check;
ALTER TABLE document_review ADD CONSTRAINT document_review_status_check CHECK (status IN ('IN_REVIEW','RETURNED','APPROVED','VALIDATED'));
ALTER TABLE personal_notification DROP CONSTRAINT personal_notification_kind_check;
ALTER TABLE personal_notification ADD CONSTRAINT personal_notification_kind_check CHECK(kind IN
 ('MEMBERSHIP_ASSIGNED','MEMBERSHIP_REMOVED','T1_PREVIEW_GENERATED','T2_PREVIEW_GENERATED','REVIEW_TASK_ASSIGNED','DOCUMENT_RETURNED','DOCUMENT_APPROVED','DOCUMENT_VALIDATED','ACTIVITY_EXTENSION_GRANTED','DOCUMENT_ARCHIVED','ACTIVITY_DEADLINE_NEAR','EVIDENCE_MISSING','ADMIN_PASSWORD_RESET','COORDINATOR_PASSWORD_RESET','AUTHOR_SIGNATURE_REQUESTED'));
