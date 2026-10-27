-- Preserve legacy bindings and signed bytes; new operations record public certificate metadata separately.
ALTER TABLE signed_document_artifact ALTER COLUMN binding_id DROP NOT NULL;
ALTER TABLE signed_document_artifact ALTER COLUMN signer_name TYPE TEXT;
ALTER TABLE signed_document_artifact ADD COLUMN actor_name VARCHAR(160);
ALTER TABLE signed_document_artifact ADD COLUMN certificate_subject TEXT;
ALTER TABLE signed_document_artifact ADD COLUMN certificate_issuer TEXT;
ALTER TABLE signed_document_artifact ADD COLUMN certificate_serial TEXT;
ALTER TABLE signed_document_artifact ADD COLUMN certificate_fingerprint CHAR(64);
ALTER TABLE signed_document_artifact ADD COLUMN certificate_valid_until TIMESTAMPTZ;
UPDATE signed_document_artifact SET actor_name=signer_name;
ALTER TABLE review_task ADD COLUMN signature_actor_name VARCHAR(160);
ALTER TABLE review_task ADD COLUMN certificate_subject TEXT;
ALTER TABLE review_task ADD COLUMN certificate_issuer TEXT;
ALTER TABLE review_task ADD COLUMN certificate_serial TEXT;
ALTER TABLE review_task ADD COLUMN certificate_fingerprint CHAR(64);
ALTER TABLE review_task ADD COLUMN certificate_valid_until TIMESTAMPTZ;
