# Modelo conceptual de datos

Entrega 2026-10-03: V6 implementa StoredFile (clave privada/hash/tamaño/páginas), DocumentAttachment (Plan/orden/requestKey/baja lógica) y DocumentArtifact (Plan/actor/snapshot/hash/plantilla/páginas/slots). Los binarios se conservan en volumen local privado. Los artefactos actuales son previsualizaciones sin firma; no implementan SignatureRecord ni ReviewRound. [ADR-0002](../architecture/adr/0002-t1-document-preparation.md).

Propuesta inicial; no contiene DDL ni esquema físico definitivo.

| Entidad | Relaciones e integridad |
|---|---|
| User / InstitutionalGroup / Membership | Pertenencias múltiples, roles y vigencia. |
| AcademicPeriod | Ventanas/fechas y documentos; cierre pendiente. |
| Document / WorkPlan / Report | Identidades independientes; Informe referencia Plan cuando deriva. |
| PlannedActivity / ActivityResponsible | Plan, actividades y responsables individuales. |
| Resource / VerificationMethod | Catálogos/selecciones y descripción de Otro. |
| Attachment | Documento, orden y numeración; distinto de evidencia. |
| TemplateVersion / TemplateSnapshot | Configuración fijada; cambios no retroactivos. |
| DocumentArtifact / DocumentPage / SignatureSlot | Composición, páginas/slots reales, contenido firmado inmutable. |
| WorkflowDefinition / WorkflowSnapshot / ReviewRound / StageAssignment | Configuración aplicada, rondas y participantes identificados. |
| ReviewObservation / ReviewDecision | Autor, ronda/artefacto, anclaje de zona; historia congelada. |
| SignatureRecord | Firmante/artefacto/etapa; jamás contraseña o certificado privado. |
| Evidence / EvidenceVersion / EvidenceReview | Actividad/medio, PDF vigente único e historia. |
| StoredFile | Ubicación, tipo, tamaño, integridad y acceso. |
| Notification / AuditEvent | Destinatario/actor, objeto y evento; retención explícita. |

Antes de SQL: precisar cardinalidades, obligatoriedad, unicidad docente/grupo/período, vigencia, bajas y retención. Cambiar miembros/catálogos/plantillas no debe alterar historia firmada. Ubicación de binarios pendiente.
