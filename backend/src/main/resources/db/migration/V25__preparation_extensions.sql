ALTER TABLE organizational_scope ADD COLUMN preparation_extended_until DATE;
ALTER TABLE organizational_scope ADD COLUMN preparation_extension_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE preparation_extension_history (
    id UUID PRIMARY KEY,
    scope_id UUID NOT NULL REFERENCES organizational_scope(id),
    old_deadline DATE NOT NULL,
    new_deadline DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    authorized_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX preparation_extension_history_scope_idx ON preparation_extension_history(scope_id, created_at DESC);
