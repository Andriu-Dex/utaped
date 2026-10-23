CREATE TABLE institutional_unit (
 id UUID PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE,
 unit_type VARCHAR(20) NOT NULL CHECK(unit_type IN ('ACADEMIC','ADMINISTRATIVE')),
 active BOOLEAN NOT NULL DEFAULT true, row_version BIGINT NOT NULL DEFAULT 0
);
INSERT INTO institutional_unit(id,name,unit_type) VALUES ('d091bc85-0823-4977-8b45-493cb466f858','FISEI — Universidad Técnica de Ambato','ACADEMIC');
ALTER TABLE academic_career ADD COLUMN institutional_unit_id UUID NOT NULL DEFAULT 'd091bc85-0823-4977-8b45-493cb466f858' REFERENCES institutional_unit(id);
ALTER TABLE organizational_scope ADD COLUMN institutional_unit_id UUID NOT NULL DEFAULT 'd091bc85-0823-4977-8b45-493cb466f858' REFERENCES institutional_unit(id);
ALTER TABLE organizational_scope DROP CONSTRAINT organizational_scope_group_id_period_id_career_id_key;
ALTER TABLE organizational_scope ADD CONSTRAINT scope_provenance_unique UNIQUE NULLS NOT DISTINCT(group_id,period_id,institutional_unit_id,career_id);
ALTER TABLE work_plan ADD COLUMN institutional_unit_type VARCHAR(20) NOT NULL DEFAULT 'ACADEMIC' CHECK(institutional_unit_type IN ('ACADEMIC','ADMINISTRATIVE'));
