-- Correct only the known FISEI reference; other institutional units retain their names.
UPDATE institutional_unit
SET name='Facultad de Ingeniería en Sistemas, Electrónica e Industrial', row_version=row_version+1
WHERE id='d091bc85-0823-4977-8b45-493cb466f858'
  AND name='FISEI — Universidad Técnica de Ambato';

-- Existing signed artifacts/snapshots are immutable. Only unsigned drafts are corrected.
UPDATE work_plan p
SET institutional_unit='Facultad de Ingeniería en Sistemas, Electrónica e Industrial',
    row_version=row_version+1, updated_at=now()
WHERE p.document_state='DRAFT' AND p.institutional_unit_type='ACADEMIC'
  AND p.institutional_unit='FISEI — Universidad Técnica de Ambato'
  AND NOT EXISTS (SELECT 1 FROM signed_document_artifact s WHERE s.work_plan_id=p.id);
