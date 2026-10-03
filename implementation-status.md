# Estado de implementación productiva

Inicio: 2026-10-02. Entrega: 2026-10-03. Rama: feature/project-foundation, creada desde develop. Commit de la base autorizado el 2026-10-03; push y PR pendientes.

| Área | Estado | Evidencia |
|---|---|---|
| Documentación inicial | Disponible; pendientes institucionales registrados | docs/requirements y docs/validation. |
| Stack productivo | Seleccionado e implementado | ADR-0001, manifests y Dockerfiles. |
| Credenciales locales | Implementado | Login/logout, bcrypt, CSRF, sesión JDBC, cambio obligatorio y recuperación SMTP. |
| Usuarios y permisos | Primera entrega implementada | Bootstrap, alta, activación/desactivación, ADMIN/USER; revocación y protección último admin. |
| Grupos y pertenencias | Primera entrega implementada | Alta/listado, MEMBER/COORDINATOR, asignación/retiro, aislamiento por objeto. |
| Períodos | Alta/consulta y validación de rangos implementadas | Fechas civiles/ventanas; cierre y aplicación documental pendientes. |
| Auditoría | Eventos de identidad/administración persistidos | No existe todavía visor administrativo ni política de retención institucional. |
| Pruebas | Backend PostgreSQL y E2E de identidad ejecutados | docs/reports/2026-10-03-01-project-foundation.md. |
| CI | Workflow frontend/backend/E2E preparado | Ejecución GitHub pendiente de push. |
| T1/T2, firma, revisión, evidencias | No implementados en producto | Mockup no cuenta como entrega productiva. |
| SMTP institucional/HTTPS/operación | Pendiente | Mailpit captura correo local; Compose local no es publicación institucional. |

## Trazabilidad

FR-AUTH-001/003/004/005 cubiertos en esta entrega; FR-AUTH-002 parcialmente: contraseñas temporales para altas administrativas, sin importación masiva. FR-GRP-001/002/003 cubiertos; FR-GRP-006 parcial: MEMBER/COORDINATOR, no roles personalizados. FR-PER-001/002 cubiertos para alta/consulta; FR-PER-003 parcial hasta edición/configuración posterior. FR-ARCH-DB-001 implementado.

Administración completa, roles institucionales de revisión, búsqueda/paginación avanzada, importación y edición general aún no se entregan. Las reglas institucionales pendientes no se consideran resueltas por la existencia de tablas o pantallas.
