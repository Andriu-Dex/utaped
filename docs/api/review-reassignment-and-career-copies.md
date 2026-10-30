# Sustitución de participantes y copia multicarrera

Entrega backend del 30/10/2026. Decisión del propietario: avanzar con el cierre funcional y reservar frontend a Antigravity. Estas operaciones no sustituyen la ratificación institucional recogida en [decisiones de cierre](../validation/client-closeout-decisions.md).

## Sustitución excepcional de participante

- GET /api/reviews/tasks/{taskId}/reassignment → opciones con rowVersion, actorId, actorName, canReassign, candidates[{id,name}] e history.
- POST misma ruta: {rowVersion,actorId,reason,requestKey} → opciones actualizadas. reason obligatorio, máximo 1000 caracteres; UUIDs y versión obligatorios.
- Sesión/CSRF habituales; GET no-store. Coordinador activo del mismo ámbito con acceso documental; ADMIN técnico no es override.
- Tarea ACTIVE o PENDING, sin decisión, ronda IN_REVIEW, período ACTIVE, documento no descartado/archivado.
- Participante no disponible: cuenta inactiva o pertenencia explícitamente inactiva en ese ámbito. Ausencia de membresía no prueba indisponibilidad: los revisores pueden ser externos a la comisión. Sustituir a alguien todavía activo por ausencia temporal no representada en estos datos requiere una política adicional; no se ofrece un bypass.
- Candidato integrante activo del ámbito, cuenta activa, sin otra tarea en esa ronda y sin autoría/responsabilidad principal del documento. No necesita acceso previo a todos los documentos: la tarea activa le concede acceso a este documento. Los pendientes futuros no se convierten en revisión anticipada.
- Bloqueo de revisión seguido del agregado documental, token rowVersion e idempotencia por taskId/requestKey. Una repetición idéntica no duplica historial ni avisos; clave reutilizada con otro contenido devuelve 409.
- Se verifica de nuevo el actor después de adquirir el bloqueo de tarea, para impedir que una petición antigua actúe tras una sustitución concurrente.

El historial conserva nombres de actor anterior, nuevo actor y coordinador autorizante, además de motivo, fecha y versión previa. No se reasignan decisiones completadas, se eliminan observaciones ni se cambia su autor. can_reassign y reassignments se incluyen por tarea en GET /api/reviews/{id}; slot_actor_id identifica la ubicación original.

El PDF firmado y el snapshot del flujo permanecen intactos. slot_actor_id conserva el actor original del slot; el sustituto firma con su propia credencial en esa ubicación y la firma incremental indica sustitución, con metadato Reason y apariencia diferenciada. La tabla de nombres originalmente firmada NO se reescribe. Esta representación debe ser aceptada institucionalmente; si exigen modificar el roster impreso, deberá prepararse y firmarse un nuevo artefacto mediante el circuito formal, no alterar el actual.

Eventos: REVIEW_TASK_REASSIGNED y, si la tarea está ACTIVE, REVIEW_TASK_ASSIGNED dirigido al sustituto. El segundo reutiliza la notificación personal ya existente. Custodia incluye reviewReassignments y purga mantiene el orden de FK.

Errores: 400 candidato/entrada inválidos; 401 sin sesión/cuenta inactiva; 403 coordinador/ámbito no autorizado; 404 tarea/documento inaccesible; 409 indisponibilidad/etapa/versión/clave incompatible. No depender de una distinción 403/404 para inferir existencia de objetos ajenos.

## Copia explícita del Plan unipersonal a otras carreras

- GET /api/work-plans/{sourceId}/career-copies → {rowVersion,canCopy,scopes:[ScopeOption]}.
- POST misma ruta: {rowVersion,requestKey,targetScopeIds:[UUID]} → {documentIds:[UUID]}.
- De uno a diez destinos, sin repetidos; GET no-store, POST con sesión/CSRF.
- Origen T1 borrador editable, ámbito con carrera y exactamente un integrante activo. Destinos de otras carreras del mismo grupo/período, también unipersonales, donde el mismo usuario puede crear. No sobrescribe Plan existente ni permite usar un aprobado como origen por este endpoint.
- Se crean solo los destinos seleccionados. No genera documentos silenciosamente por cada carrera del usuario.
- Copia justificación, objetivo, estructura/contenido de secciones y matriz guardada. Revalida responsables, fechas, catálogo y obligatoriedad. Cada actividad recibe UUID nuevo; el destino usa carrera/unidad y título derivados propios.
- Conserva la opción de nota de privacidad. No copia firmas, aprobaciones, rondas, anexos ni evidencias. Los anexos de la copia quedan deshabilitados inicialmente y pueden añadirse luego por su flujo normal.
- Sin matriz guardada no inventa actividades completas; el editor de destino podrá mostrar sus actividades obligatorias pendientes de configurar.
- Operación atómica: un destino no disponible revierte el lote entero. Actor y ámbitos se bloquean con orden estable. Source permanece sin modificación y cada copia es independiente.
- requestKey identifica el lote del usuario, origen, versión y conjunto de destinos. Repetición idéntica devuelve los mismos IDs con autorización vigente; un cambio de contenido con esa clave devuelve 409. Se conserva procedencia en plan_copy_batch y WORK_PLAN_COPIED por documento.
- La custodia contiene planCopyBatches; la purga del período elimina los registros vinculados antes del documento fuente.

Errores: 400 entrada/matriz inválida; 401 sin sesión; 403 origen no elegible/permiso; 404 documento inaccesible; 409 versión, destino ocupado o clave incompatible. La selección no puede corregirse silenciosamente omitiendo destinos fallidos.

## Datos e integración frontend

Las respuestas de opciones y DTOs usan camelCase; los mapas históricos/tareas del servicio de revisión conservan snake_case. No duplicar permisos desde el rol global ni manipular slots de firma en React.

Migración V38 nueva, sin editar V37. El historial y lotes se conservan junto a la custodia. No se almacenan credenciales privadas. Interfaz integrada en CareerCopyModal, TaskReassignmentModal, WorkPlanEditor y ReviewInspector. La aceptación institucional sigue pendiente.
