ALTER TABLE institutional_group ADD COLUMN collective_label VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE academic_period ADD COLUMN restrict_holiday_endpoints BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE planning_catalog (
    id UUID PRIMARY KEY,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('RESOURCE','MEANS')),
    label VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(kind,label)
);
CREATE TABLE activity_catalog (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES institutional_group(id),
    title VARCHAR(500) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('POA','IMPROVEMENT_PLAN','IMPROVEMENT_ACTION','OTHER')),
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX activity_catalog_group_idx ON activity_catalog(group_id);
CREATE TABLE planning_holiday (
    holiday_date DATE PRIMARY KEY,
    label VARCHAR(200) NOT NULL
);
CREATE TABLE work_plan_matrix (
    work_plan_id UUID PRIMARY KEY REFERENCES work_plan(id),
    source VARCHAR(500) NOT NULL DEFAULT '',
    activities JSONB NOT NULL DEFAULT '[]'::jsonb CHECK(jsonb_typeof(activities)='array')
);
