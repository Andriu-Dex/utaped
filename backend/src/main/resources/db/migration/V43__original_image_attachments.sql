ALTER TABLE document_attachment ADD COLUMN original_file_id uuid REFERENCES stored_file(id);
ALTER TABLE document_attachment ADD COLUMN original_media_type varchar(30);
ALTER TABLE document_attachment ADD CONSTRAINT attachment_original_pair CHECK ((original_file_id IS NULL)=(original_media_type IS NULL));
