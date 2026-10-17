CREATE TABLE institutional_document_template (
    document_type VARCHAR(10) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    justification TEXT NOT NULL DEFAULT '',
    row_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    CHECK (char_length(justification) <= 50000)
);

INSERT INTO institutional_document_template (document_type, name, description, justification)
VALUES (
    'T1',
    'Plan de Trabajo (Formato T1)',
    'Texto predeterminado cargado automáticamente en el campo de Justificación al crear un nuevo Plan de Trabajo.',
    'El presente Plan de Trabajo se formula en cumplimiento del Estatuto de la Universidad Técnica de Ambato, el Reglamento de Carrera y Escalafón del Personal Académico del Sistema de Educación Superior y los lineamientos académicos de la Facultad de Ingeniería en Sistemas, Electrónica e Industrial (FISEI). Su propósito es articular, planificar y ejecutar de manera eficiente las actividades académicas, pedagógicas, de tutoría, investigación formativa y vinculación con la sociedad correspondientes al presente período académico, garantizando el rigor formativo y la calidad institucional.'
);
