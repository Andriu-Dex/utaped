-- Delivery is independent of signing: an SMTP outage must not undo a valid signature.
CREATE TABLE signature_mail_delivery (
    notification_id uuid PRIMARY KEY REFERENCES personal_notification(id) ON DELETE CASCADE,
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at timestamptz NOT NULL DEFAULT now(),
    delivered_at timestamptz,
    last_error_code varchar(40)
);
CREATE INDEX signature_mail_pending ON signature_mail_delivery(available_at)
    WHERE delivered_at IS NULL;
