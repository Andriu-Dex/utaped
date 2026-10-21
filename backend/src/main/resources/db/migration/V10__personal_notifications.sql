CREATE TABLE personal_notification (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES app_user(id),
    source_event_id UUID NOT NULL REFERENCES audit_event(id),
    kind VARCHAR(30) NOT NULL CHECK(kind IN ('MEMBERSHIP_ASSIGNED','MEMBERSHIP_REMOVED','T1_PREVIEW_GENERATED')),
    target_type VARCHAR(20) NOT NULL CHECK(target_type IN ('GROUP','WORK_PLAN')),
    target_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ,
    UNIQUE(source_event_id,recipient_id)
);
CREATE INDEX notification_recipient_time_idx ON personal_notification(recipient_id,created_at DESC,id);
CREATE INDEX notification_unread_idx ON personal_notification(recipient_id) WHERE read_at IS NULL;
