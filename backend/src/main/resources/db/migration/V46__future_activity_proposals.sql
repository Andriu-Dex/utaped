-- Immutable source identifiers remain as custody references after an authorized period purge.
CREATE TABLE activity_proposal (
 id uuid PRIMARY KEY, source_document_id uuid NOT NULL, source_artifact_id uuid NOT NULL, source_pdf_hash varchar(64) NOT NULL,
 group_id uuid NOT NULL REFERENCES institutional_group(id), career_id uuid REFERENCES academic_career(id), source_title varchar(200) NOT NULL, source_formal_version varchar(30) NOT NULL, source_period_ends_on date NOT NULL,
 title varchar(500) NOT NULL, created_by uuid NOT NULL REFERENCES app_user(id), request_key uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','ACCEPTED','REJECTED')), row_version bigint NOT NULL DEFAULT 0,
 catalog_id uuid REFERENCES activity_catalog(id), resolved_by uuid REFERENCES app_user(id), resolved_at timestamptz, resolution_note varchar(1000), UNIQUE(created_by,request_key)
);
