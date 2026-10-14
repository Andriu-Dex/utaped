CREATE TABLE academic_career (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE organizational_scope (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES institutional_group(id),
    period_id UUID NOT NULL REFERENCES academic_period(id),
    career_id UUID REFERENCES academic_career(id),
    principal_user_id UUID REFERENCES app_user(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version BIGINT NOT NULL DEFAULT 0,
    UNIQUE NULLS NOT DISTINCT (group_id, period_id, career_id)
);
CREATE TABLE scope_member (
    scope_id UUID NOT NULL REFERENCES organizational_scope(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    membership_role VARCHAR(30) NOT NULL CHECK (membership_role IN ('MEMBER','COORDINATOR')),
    position_label VARCHAR(160) NOT NULL DEFAULT '',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (scope_id, user_id)
);
ALTER TABLE work_plan ADD COLUMN scope_id UUID REFERENCES organizational_scope(id);
ALTER TABLE work_plan ADD COLUMN created_by UUID REFERENCES app_user(id);
UPDATE work_plan SET created_by=teacher_id;
ALTER TABLE work_plan ALTER COLUMN created_by SET NOT NULL;
-- Compatibility for trusted legacy SQL imports; API creation always supplies the actor.
CREATE FUNCTION work_plan_creator_default() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.created_by IS NULL THEN NEW.created_by := NEW.teacher_id; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER work_plan_creator_default BEFORE INSERT ON work_plan FOR EACH ROW EXECUTE FUNCTION work_plan_creator_default();
ALTER TABLE work_plan ADD COLUMN archived_at TIMESTAMPTZ;
ALTER TABLE work_plan ADD COLUMN review_round INTEGER NOT NULL DEFAULT 1 CHECK (review_round>0);
CREATE UNIQUE INDEX work_plan_active_scope_idx ON work_plan(scope_id) WHERE archived_at IS NULL AND scope_id IS NOT NULL;
CREATE TABLE document_access (
    work_plan_id UUID NOT NULL REFERENCES work_plan(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by UUID REFERENCES app_user(id),
    PRIMARY KEY (work_plan_id,user_id)
);
CREATE INDEX scope_member_user_idx ON scope_member(user_id,scope_id) WHERE active;
CREATE INDEX document_access_user_idx ON document_access(user_id,work_plan_id);
