# 2026-11-05 — Línea de Tiempo Documental y Trazabilidad Cronológica de Hitos

## Objetivo
Implementar la **Fase 2 (Línea de tiempo documental)** especificada en el plan de mejoras institucionales, proporcionando a los usuarios de UTAPED una cronología unificada, legible y estructurada de los acontecimientos clave del ciclo de vida del documento (elaboración, firmas de autor, envíos a revisión, devoluciones con observaciones, aprobaciones formales, nuevas versiones formales e informes derivados), respetando estrictamente el aislamiento por documento y los controles de acceso institucionales en el servidor.

## Cambios realizados

### Backend (Spring Boot / Java 21)
- `backend/src/main/java/ec/edu/uta/utaped/planning/DocumentTimelineService.java`:
  - Servicio transaccional de lectura (`@Transactional(readOnly = true)`) que reúne y estructura cronológicamente los acontecimientos del documento:
    1. **Elaboración**: Registro del inicio del borrador a partir de `audit_event` o metadatos de creación del plan.
    2. **Firmas de elaboración**: Firmas electrónicas de autores registradas en `signed_document_artifact` con orden y nombre del firmante.
    3. **Rondas de revisión y etapas**: Envíos de ronda desde `document_review`, tareas actuadas desde `review_task` (devoluciones `RETURNED` y firmas/revisiones completadas `COMPLETED`).
    4. **Aprobación formal**: Cierre exitoso del flujo de revisión (`status = 'APPROVED'`).
    5. **Versiones formales**: Trazabilidad de nuevas versiones formales aprobadas (`work_plan.formal_version`, `previous_version_id`).
    6. **Informes derivados**: Para Planes T1, enlace directo con informes finales (T2) o de ejecución (T3) generados; para Informes (T2/T3), enlace con su Plan de Trabajo base.
    7. **Archivado formal**: Registro del estado y motivo cuando el documento ha sido archivado.
  - Ordenación estable configurable (`asc` cronológico o `desc` inverso).
  - Conversión segura y robusta de marcas temporales con zona horaria institucional (`America/Guayaquil`).
  - Verificación rigurosa de control de acceso previo (`plans.get(targetDocId, email)`), impidiendo cualquier fuga de información a usuarios no autorizados.
- `backend/src/main/java/ec/edu/uta/utaped/planning/DocumentTimelineController.java`:
  - Controlador REST que expone el endpoint seguro `GET /api/work-plans/{targetDocId}/timeline?order=asc|desc`.
- `backend/src/test/java/ec/edu/uta/utaped/DocumentTimelineIntegrationTests.java`:
  - Suite de pruebas de integración con PostgreSQL en contenedor que valida:
    - Creación de hitos en documentos nuevos.
    - Rechazo de acceso a usuarios no autorizados (`ResponseStatusException`).
    - Inversión estable del orden cronológico (`asc` vs `desc`).
    - Aislamiento estricto de eventos entre documentos distintos.

### Frontend (React / TypeScript / Vite)
- `frontend/src/planning/DocumentTimeline.tsx`:
  - Componente de trazabilidad con cabecera institucional según el Sistema de Diseño Moderno UTAPED (`.plan-wizard-header`, `.plan-back-btn` con `<Icon name="arrowLeft" size={20} />`).
  - Selector de pestañas integrado entre «Línea de tiempo» (hitos humanos) y «Auditoría técnica» (`AuditViewer` integrado para consultas administrativas).
  - Botón interactivo para alternar orden cronológico («Más antiguos primero» / «Más recientes primero»).
  - Filtros por categoría de acontecimientos: Todos, Firmas, Revisiones, Versiones e Informes vinculados.
  - Pistas visuales semánticas por tipo de nodo (iconos y colores para creación, firmas, rondas, devoluciones, aprobaciones y versiones).
  - Acciones contextuales directas: botón «Examinar ronda» para inspeccionar observaciones con `ReviewInspector` y botón «Abrir documento» para informes derivados o plan base.
  - Estados accesibles de carga, reintento de error y lista vacía.
- `frontend/src/planning/document-timeline.css`:
  - Estilos específicos de pista vertical (`.document-timeline-track`), nodos y tarjetas de acontecimiento.
  - Compatibilidad completa con modo oscuro institucional (`:root[data-theme='dark']`) y contrastes de color semánticos.
- `frontend/src/planning/DocumentFollowUp.tsx`:
  - Incorporada la opción «Línea de tiempo documental» en el menú desplegable de seguimiento.
- Integración en espacios de trabajo documental:
  - `frontend/src/planning/WorkPlanEditor.tsx`: Soporte para la sección `timeline`.
  - `frontend/src/planning/ReportEditor.tsx`: Soporte para el panel `timeline` y acceso directo en la barra de estado.
  - `frontend/src/planning/ExecutionReportEditor.tsx`: Soporte para el panel `timeline` y acceso directo en la barra de estado.
  - `frontend/src/planning/DocumentConsultation.tsx`: Soporte para el modo `timeline` en la barra de herramientas de consulta.

## Validaciones ejecutadas
- **Pruebas de Integración Backend (`DocumentTimelineIntegrationTests`):**
  - Ejecutada la suite completa en contenedor aislado `utaped-tests` con PostgreSQL 18.
  - 4 pruebas específicas superadas exitosamente (100% aprobadas, código de salida 0).
- **Linter Frontend (`oxlint`):**
  - 0 errores, 0 advertencias en 136 archivos analizados con 116 reglas activas.
- **Compilación TypeScript y Empaquetado Vite (`tsc -b && vite build`):**
  - Tipos comprobados sin discrepancias y empaquetado completado limpiamente (código de salida 0).
- **Despliegue y Verificación en Contenedores Locales:**
  - Reconstruida e iniciada la imagen `utaped-backend` en `utaped-backend-1`. Estado verificado: `UP` en `/actuator/health`.
  - Sincronizados los estáticos compilados en `utaped-frontend-1` (`http://127.0.0.1:64430`).
