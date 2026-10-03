# Estado de implementación productiva

Inicio: 2026-10-02. Actualización: 2026-10-03. Base productiva fusionada a develop mediante PR #1 (commit e6ec287). Entrega actual en feature/work-plan-drafts, creada desde develop; sin commit ni publicación.

| Área | Estado | Evidencia |
|---|---|---|
| Documentación inicial | Disponible; pendientes institucionales registrados | docs/requirements y docs/validation. |
| Stack productivo | Seleccionado e implementado | ADR-0001, manifests y Dockerfiles. |
| Credenciales locales | Implementado | Login/logout, bcrypt, CSRF, sesión JDBC, cambio obligatorio y recuperación SMTP. |
| Usuarios y permisos | Primera entrega implementada | Bootstrap, alta, activación/desactivación, ADMIN/USER; revocación y protección último admin. |
| Grupos y pertenencias | Primera entrega implementada | Alta/listado, MEMBER/COORDINATOR, asignación/retiro, aislamiento por objeto. |
| Períodos | Alta/consulta, rangos y ventana de elaboración de borradores | Fechas civiles inclusivas en zona configurable; cierre definitivo/reapertura pendientes. |
| Auditoría | Eventos de identidad/administración persistidos | No existe todavía visor administrativo ni política de retención institucional. |
| Pruebas | Backend PostgreSQL y E2E de identidad ejecutados | docs/reports/2026-10-03-01-project-foundation.md. |
| CI | Workflow frontend/backend/E2E disponible | Base publicada y fusionada; cambios actuales verificados localmente, ejecución remota pendiente de publicación. |
| Borradores T1 | Primera entrega parcial implementada | Creación, información general, justificación/objetivo, lista filtrada/paginada, persistencia, aislamiento y concurrencia. |
| T1 completo/T2, firma, revisión, evidencias | Pendientes en producto | Actividades, matriz, anexos y artefactos oficiales aún no entregados; mockup no cuenta como entrega productiva. |
| SMTP institucional/HTTPS/operación | Pendiente | Mailpit captura correo local; Compose local no es publicación institucional. |

## Trazabilidad

FR-AUTH-001/003/004/005 cubiertos en esta entrega; FR-AUTH-002 parcialmente: contraseñas temporales para altas administrativas, sin importación masiva. FR-GRP-001/002/003 cubiertos; FR-GRP-006 parcial: MEMBER/COORDINATOR, no roles personalizados. FR-PER-001/002 cubiertos para alta/consulta; FR-PER-003 parcial hasta edición/configuración posterior. FR-ARCH-DB-001 implementado.

Administración completa, roles institucionales de revisión, búsqueda/paginación avanzada, importación y edición general aún no se entregan. Las reglas institucionales pendientes no se consideran resueltas por la existencia de tablas o pantallas.

## Entrega de borradores T1

Commit de esta entrega autorizado el 2026-10-03. El nuevo agente de inspección de proyecto/Trello está registrado como requisito, sin implementación ni cambios al esquema documental: [alcance y propuestas](docs/requirements/project-advisor-agent.md).

FR-T1-001 parcial: identidad autenticada, grupo y período; unidad y carrera son texto manual mientras no exista catálogo institucional. FR-T1-004 registra fecha civil al crear; FR-T1-005 depende de finalización futura. FR-T1-CONT-001/002 disponen de un campo persistido por documento, permitiendo valores incompletos en borrador. No se afirma que el asistente T1 completo esté implementado.

Q-001 y Q-002 siguen pendientes: no se redefine el número de pasos; hay dos secciones de edición inicial. Unicidad por docente/grupo/período configurable y deshabilitada provisionalmente. La idempotencia de una solicitud no equivale a unicidad institucional.

Validación: frontend build/lint, 15 pruebas backend con PostgreSQL aislado y E2E ampliado con Vite y Nginx/Docker. Reporte: [borradores T1](docs/reports/2026-10-03-03-work-plan-drafts.md). Próxima entrega propuesta: catálogo de actividades, responsables y matriz T1, tras resolver decisiones de estructura/cardinalidad.
