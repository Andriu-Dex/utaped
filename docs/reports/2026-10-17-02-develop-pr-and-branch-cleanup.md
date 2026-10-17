# Publicación hacia develop y limpieza de ramas

Fecha: 2026-10-17. Rama: fix/document-creation-options. Autorización expresa del propietario para push/PR y retirada de ramas temporales acumuladas. El propietario conserva la revisión y fusión; no se fusiona ni se modifica main.

## Entrega para revisión

Selector Plan/Informe con nombres propuestos; formularios por acción y modal accesible de creación; confirmaciones compartidas y toasts; mejoras visuales del propietario; restauración de documento/paso en URL y protección de cambios; referencias oficiales Julio–Diciembre 2026; entorno diario único con aceptación automatizada aislada; limpieza respaldada de ejemplos conservando siete cuentas y contraseñas. A4 se mantiene. La previsualización simultánea queda para una rama posterior, después de revisar/fusionar esta entrega.

Validación previamente ejecutada y documentada en los reportes de implementación: 139 pruebas backend, ocho E2E generales y once de aceptación; cinco guardas Node de aislamiento; lint/build frontend. Tras el comando de limpieza: once pruebas Python de operaciones/guardas, respaldo verificado de 122 documentos/302 archivos, referencias comparadas por IDs/nombres y API CSRF HTTP 200. No se repiten pruebas funcionales por estos cambios documentales de cierre. La ejecución remota de CI deberá consultarse en el PR, sin anticipar su resultado.

## Ramas remotas retiradas

Cada punta estaba contenida en origin/develop y no tenía PR abierto. La eliminación utilizó el SHA esperado como lease para rechazar cambios remotos concurrentes.

- feature/modern-workspace-access
- feature/project-foundation
- feature/signature-review-preparation
- feature/t1-document-preparation
- feature/tracking-notifications-audit-export
- feature/visible-author-signing
- feature/work-plan-activities
- feature/work-plan-drafts
- feature/workflow-administration-audit

Se conserva agent/8b075407-abf0-4dbd-904c-127b57d98087-v2 por el PR #11 abierto; también main, develop y la rama actual hasta su fusión. No se borraron ramas locales, datos de la aplicación ni commits. Se habilita la opción GitHub de eliminar automáticamente las ramas después de fusionarlas, para evitar otra acumulación.

frontend/pnpm-lock.yaml permanece sin seguimiento y fuera de la entrega, tal como se encontró. El PR solicita revisión hacia develop; la fusión es manual por el propietario.
