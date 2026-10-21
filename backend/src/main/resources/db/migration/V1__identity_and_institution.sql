CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email)),
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    system_role VARCHAR(20) NOT NULL CHECK (system_role IN ('ADMIN','USER')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE institutional_group (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    group_type VARCHAR(30) NOT NULL CHECK (group_type IN ('COMMISSION','UNIT','CLUB','OTHER')),
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE membership (
    user_id UUID NOT NULL REFERENCES app_user(id),
    group_id UUID NOT NULL REFERENCES institutional_group(id),
    membership_role VARCHAR(30) NOT NULL CHECK (membership_role IN ('MEMBER','COORDINATOR')),
    PRIMARY KEY (user_id, group_id)
);
CREATE INDEX membership_group_idx ON membership(group_id);
CREATE TABLE academic_period (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    preparation_starts_on DATE NOT NULL,
    preparation_ends_on DATE NOT NULL,
    review_starts_on DATE NOT NULL,
    review_ends_on DATE NOT NULL,
    CHECK (starts_on <= ends_on),
    CHECK (preparation_starts_on <= preparation_ends_on),
    CHECK (review_starts_on <= review_ends_on)
);
CREATE TABLE password_reset (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX password_reset_user_idx ON password_reset(user_id);
CREATE TABLE auth_throttle (
    key_hash VARCHAR(64) PRIMARY KEY,
    attempts INTEGER NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES app_user(id),
    action VARCHAR(80) NOT NULL,
    target_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_event_time_idx ON audit_event(occurred_at);
