CREATE TABLE institutional_position (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    career_specific BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version BIGINT NOT NULL DEFAULT 0
);

-- One holder per position, period and career (career NULL for faculty-wide positions such as Dean).
CREATE TABLE position_assignment (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL REFERENCES academic_period(id),
    position_id UUID NOT NULL REFERENCES institutional_position(id),
    career_id UUID REFERENCES academic_career(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    assigned_by UUID NOT NULL REFERENCES app_user(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT (period_id, position_id, career_id)
);
CREATE INDEX position_assignment_user_idx ON position_assignment(user_id, period_id);
