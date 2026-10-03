# Borradores persistidos de Plan de Trabajo

Fecha: 2026-10-03. Proyecto: UTAPED productivo. Rama: `feature/work-plan-drafts`, desde `develop` actualizado. La base anterior fue fusionada mediante PR #1; esta entrega no contiene commit, push ni PR. El mockup externo permanece congelado.

## Alcance implementado

Primera entrega del módulo de planificación: creación y edición de borradores T1 con PostgreSQL, separada de firma y finalización. No representa la entrega completa del T1.

- Flyway V4 crea `work_plan` con titular, grupo, período, contenido, fecha civil, estado, versión formal y contador técnico de concurrencia.
- El elaborador se deriva de la sesión. Identidad, grupo, período y fecha no se cambian al editar. Grupo activo y pertenencia se verifican en el servidor por documento explícito; ADMIN no permite acceder a borradores ajenos.
- Alta idempotente mediante UUID de solicitud; reintentos no crean duplicados ni repiten auditoría. La solicitud original permanece identificable después de editar el título.
- Lista propia paginada y filtrada por título/grupo/período. Los resultados contienen resúmenes, sin cargar ni transferir los textos extensos del documento.
- Información general, unidad/carrera manuales, justificación y objetivo guardados en servidor. Campos incompletos permitidos en borrador; título obligatorio y límites de tamaño validados en API.
- Control de concurrencia optimista: un guardado obsoleto recibe 409. La interfaz conserva sus cambios locales, permite comparar y cargar la versión actual con confirmación. No sobrescribe silenciosamente.
- Confirmación al abandonar ediciones pendientes por navegación o cierre de sesión; protección al recargar/cerrar la pestaña. Los borradores no se guardan en localStorage. Una expiración de sesión puede perder cambios todavía no guardados.
- Ventana inclusiva de elaboración en zona configurable, lectura fuera de ventana y auditoría transaccional de alta/guardado. El contador técnico no altera la versión formal `1.0` ni crea rondas de revisión.

Contrato: [API documental](../api/work-plan-drafts.md). Estado: [implementation-status.md](../../implementation-status.md).

## Validaciones ejecutadas

| Validación | Resultado |
|---|---|
| `npm run build` | TypeScript y compilación Vite correctos. |
| `npm run lint` | Sin errores de oxlint. |
| Suite Maven mediante `compose.test.yml` | 15 pruebas: 12 de base, 1 de contexto y 2 documentales; sin fallos. PostgreSQL aislado `utaped_test`. |
| Playwright con Vite | Flujo ampliado completo aprobado. |
| Playwright con Nginx/Docker | Mismo flujo aprobado sobre compilación servida. |
| Inspección visual móvil | Formulario legible, navegación y contenidos correctos; comprobación de ausencia de desbordamiento horizontal. |
| `git diff --check` | Sin errores de espacios. |

Pruebas documentales: persistencia entre sesiones, aislamiento de dos documentos, idempotencia incluso después de cambiar el título, edición obsoleta sin alteración, auditoría, filtros, permisos de propietario, denegación a terceros/admin, CSRF, entradas inválidas, cierre de ventana y revocación de pertenencia.

E2E conserva el flujo anterior de acceso/administración/recuperación y añade creación, guardado, recarga, cancelación de navegación con cambios pendientes y conflicto entre pestañas con comparación y recarga explícita. Las capturas y salidas de prueba son artefactos locales ignorados, no archivos de entrega.

## Decisiones pendientes y límites

Q-001: aún no se define seis/siete pasos del asistente completo. Las dos secciones actuales corresponden únicamente al alcance entregado.

Q-002: unicidad institucional sin confirmar. `PLANNING_SINGLE_PLAN_PER_SCOPE=false` permite temporalmente varios borradores; `true` exige uno por docente/grupo/período. No se crea una regla definitiva ni excepciones. Debe resolverse antes de completar el flujo documental.

Unidad y carrera no se derivan de catálogos todavía (FR-T1-001/002/003 parciales o pendientes). Fecha registrada al crear (FR-T1-004); congelación al finalizar pendiente. Q-006/Q-011 permanecen abiertos respecto a reaperturas y política institucional de fechas. No existen actividades, matriz, anexos, PDF oficial, firma, revisión, evidencias ni T2 productivos en esta entrega.

## Siguiente implementación

Catálogo de actividades y su selección por Plan, responsables individuales/colectivos y matriz T1, manteniendo identificadores explícitos y concurrencia. Resolver primero estructura del asistente y cardinalidad institucional. Revisar esta entrega y realizar su commit/PR hacia develop cuando se autorice.
