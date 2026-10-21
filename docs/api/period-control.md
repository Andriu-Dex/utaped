# Períodos, asignaciones y archivo histórico

Entrega 2026-10-12. Fuente: Plan Maestro §§3.9, 9, 29, 52–53. Credenciales locales, sesión y CSRF habituales. Las fechas usan America/Guayaquil; la indicación `is_current` no cierra ni activa flujos automáticamente.

## Configuración y activación

Los flujos generales publicados por grupo son plantillas reutilizables. Una asignación selecciona una revisión inmutable para un ámbito grupo/carrera/período y un tipo T1/T2. No existe fallback silencioso al flujo general cuando falta la asignación.

| Ruta | Permiso y comportamiento |
|---|---|
| `GET /api/admin/scopes/{scope}/workflows` | ADMIN; revisiones asignadas y versiones de concurrencia. |
| `PUT /api/admin/scopes/{scope}/workflows/{type}` | ADMIN; `{revisionId,rowVersion}`. La revisión debe corresponder al grupo y tipo del ámbito. Ausencia inicial equivale a versión 0. |
| `PUT /api/admin/scopes/{scope}/workflows` | ADMIN; `{assignments:[{type,revisionId,rowVersion}]}`. Una o dos asignaciones distintas, guardadas en una transacción; fallo de una revierte todas. |
| `POST /api/admin/periods/{period}/activate-workflows` | ADMIN; `{rowVersion}`. Exige principal y T1/T2 con ejecutores activos, individuales, sin duplicados ni autorrevisión en cada ámbito activo del grupo activo. |

Activar resuelve y congela los participantes de las revisiones seleccionadas, registra actor/fecha e incrementa `academic_period.row_version`. Después se rechazan cambios de asignación y nuevos ámbitos en ese período. Publicar otra plantilla general prepara futuras asignaciones; no sustituye el flujo activado.

La firma de nuevos artefactos por ámbito requiere activación. Los borradores y las previsualizaciones pueden prepararse antes. Un órgano colegiado sin ejecutores individuales no se convierte en un aprobador ficticio. No se define aquí una política de sustitución ante bajas: se conserva la identidad congelada y los controles vigentes de cuenta/firmante.

## Cierre

| Ruta | Permiso y comportamiento |
|---|---|
| `GET /api/admin/periods/{period}/control` | ADMIN; estado, conteos T1/T2, fecha/actor/motivo del cierre y tamaño/hash del archivo interno. Sin lectura implícita de PDFs. |
| `POST /api/admin/periods/{period}/close` | ADMIN; `{rowVersion,reason}`. ACTIVE→CLOSED con motivo obligatorio y versión actual. Repetir cierre devuelve 409. |
| `GET /api/periods/{period}/archive` | Cuenta autenticada con documentos autorizados en el período cerrado. Devuelve ZIP privado, `Cache-Control: no-store`, `Content-Disposition` y `X-Content-SHA256` del ZIP entregado. Sin documentos autorizados: 404, incluso para ADMIN. |

El cierre toma un bloqueo exclusivo del período. Las operaciones documentales, la organización del ámbito y la política de feriados toman un bloqueo compartido antes de escribir. Una operación ya iniciada finaliza antes del archivo; una posterior encuentra CLOSED y no puede cambiar el contenido.

Se archivan las versiones documentales vigentes sin purgar y se conservan sus estados, tareas, observaciones, firmas y evidencias. Un IN_REVIEW conserva sus tareas pero deja de admitir decisiones, firmas o nuevas observaciones. Los aprobados mantienen los mismos bytes y su descarga final. No se inventa reapertura, aprobación automática ni cancelación de tareas.

## Contenido e integridad del archivo

El ZIP interno incluye metadatos del período y, por ID documental, su manifiesto, matriz/T2, artefactos y snapshots, rondas/tareas/observaciones, firmas públicas, evidencias/versiones, prórrogas, asignaciones y auditoría. Conserva las versiones de archivos, incluidos preliminares y anexos retirados; no contiene contraseñas, sesiones ni material privado de firma. Los nombres de entradas se construyen con UUIDs, sin rutas aportadas por usuarios.

El almacenamiento comprueba tamaño y SHA-256 de cada archivo leído. Si falta un archivo, falla su integridad o se supera el límite, el cierre se revierte y el período sigue ACTIVE. No se entrega un archivo parcial como cierre correcto.

La descarga vuelve a autorizar cada documento y filtra las entradas del ZIP interno. ADMIN no obtiene contenido privado solo por su rol. Para documentos por ámbito se entregan PDFs finales aprobados, evidencias y anexos vigentes; el ZIP no evita el bloqueo de descargas preliminares. Cada `download.json` enumera los archivos incluidos; el `manifest.json` conserva el registro histórico completo de ese documento, por lo que puede referenciar archivos que esta descarga no incluye. Los documentos anteriores sin ámbito conservan sus permisos de descarga previos. El hash del archivo interno administrativo y el del ZIP filtrado son diferentes por diseño.

`PERIOD_ARCHIVE_MAX_BYTES` limita el tamaño sin comprimir, incluyendo metadatos y referencias repetidas: 128 MiB inicialmente, rango técnico 1–512 MiB. La generación actual se realiza en memoria; grandes volúmenes requieren exportación operativa por streaming y dimensionamiento. No es una política institucional de retención.

## Migración y acciones del operador

V19 añade estado ACTIVE/CLOSED, control de concurrencia, activación, asignaciones y archivo privado. Los períodos existentes quedan ACTIVE, sin activación automática. Los ámbitos existentes reciben inicialmente la revisión general publicada disponible. No se alteran snapshots ni firmas históricos.

1. Reiniciar/reconstruir el backend para aplicar Flyway y actualizar el frontend. No reiniciar datos.
2. Revisar principales e integrantes de los ámbitos.
3. Publicar plantillas T1/T2; en Períodos, elegir las revisiones para cada ámbito y guardar asignaciones.
4. Activar los flujos del período antes de nuevas firmas. Revisar la selección antes de confirmar: no hay desbloqueo estructural.
5. Al cerrar, revisar los conteos pendientes y confirmar un motivo. Descargar el historial autorizado desde Documentos, filtrando por el período cerrado.

Pendientes: `PENDIENTE_REAL_RETENCION`, respaldo/restauración operacional, permisos contextuales de Decano/Subdecano, sustitución de ejecutores y política de reapertura. El ZIP histórico de aplicación no equivale a un respaldo completo restaurable de PostgreSQL.
