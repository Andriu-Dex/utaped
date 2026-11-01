# Contratos añadidos tras la reunión del 30/10/2026

Todos los endpoints mantienen sesión/CSRF y autorización en servidor. Los IDs documentales son explícitos. Migraciones V39–V46.

## Catálogos y propuestas

- `GET /api/admin/planning/activity-reference`: revisión del manifiesto, fuentes con SHA-256 y referencias oficiales. Es consulta, no importación automática.
- `POST /api/admin/planning/activity-reference/import`: selecciones explícitas de referencias por grupo y contexto. Idempotencia por fuente/grupo/período/carrera; los textos editados no se sustituyen al reimportar. Las referencias TI conservan esa restricción.
- `context` de definiciones/actividades conserva procedencia y jerarquía; el guardado de matriz deriva los datos del catálogo o snapshot previo, no confía en metadata enviada por el cliente.
- `GET/POST /api/reports/{doc}/activity-proposals`: consulta autorizada y propuesta desde T2 aprobado por su responsable. Alta con `requestKey` y `title`; congela fuente/version/hash.
- `GET /api/admin/planning/groups/{group}/activity-proposals`: pendientes intencionalmente remitidos a Administración, sin acceso implícito al PDF origen.
- `POST /api/admin/planning/activity-proposals/{id}/accept`: `rowVersion`, `periodId`, `careerId`, `title`; período activo posterior al origen. Crea definición opcional y resuelve propuesta en una transacción.
- `POST /api/admin/planning/activity-proposals/{id}/reject`: `rowVersion`, `reason`. Una propuesta resuelta no vuelve a aceptarse.

## Informe final y autoguardado

`PUT /api/reports/{doc}` admite `executionReportIds`. El servidor acepta solo cortes aprobados del mismo Plan, con acceso autorizado, y devuelve `executionReferences` con identidad/artefacto/version/hash. Ausencia del campo en clientes anteriores conserva referencias; lista vacía es selección explícita vacía. El borrador puede estar incompleto, pero firma exige al menos un corte. No agrega porcentajes automáticamente.

El autoguardado reutiliza los PUT canónicos de T1/matriz/T2/T3 con `rowVersion`; no existe una vía que omita validaciones. Conflicto409 exige resolver versión; no sobrescribir silenciosamente. Espera1,2s exclusiva del guardado; búsqueda inmediata no usa esa espera. Formularios inválidos permanecen pendientes de guardar.

La matriz admite `note`, no vacía. Clientes antiguos que omiten el campo conservan el valor. Snapshots nuevos contienen la nota; snapshots históricos sin el campo conservan su representación anterior.

## Anexos

`POST /api/work-plans/{targetDocId}/attachments` conserva contrato multipart y añade PNG/JPEG comprobados por contenido. Convierte a PDF A4 manteniendo proporción; conserva original privado. No admite otros formatos de imagen.

`POST .../attachments/generated`: `rowVersion`, `requestKey`, `title`, `kind` (`URL`/`TABLE`), `url` o `rows`. URL HTTPS sin credenciales; se inserta como referencia sin petición de red. Tabla hasta100 filas y8 columnas, encabezados hasta80 caracteres, cuerpo hasta200; encabezado repetido/paginación. Validación conserva formulario en UI.

`GET .../attachments/{attachmentId}/original` descarga el original autorizado con nombre/tipo real. `content` continúa siendo PDF de composición. Límites individuales se aplican al original y al render; total lógico usa tamaño del original. Custodia conserva ambos. Reintentos usan hash de solicitud estable, no bytes variables de PDF generado.

## Administración y reportes

- `GET /api/admin/document-structures/{type}/export` devuelve `formatVersion:1`, `documentType`, `sections`.
- `POST .../{type}/import`: `revision`, `structure`. Verifica tipo, secciones obligatorias y concurrencia; afecta a nuevos documentos, no snapshots firmados.
- `POST /api/admin/planning/holiday-suggestions`: `year`. Sin escrituras de calendario; límites y proveedor configurado.
- `POST .../holiday-suggestions/apply`: `year`, `items`, `reviewed:true`, `overwrite`. Cada propuesta incluye fecha/nombre/fuenteHTTPS; fechas existentes requieren autorización de reemplazo. No declara oficial una respuesta de IA.
- `GET /api/reporting/upcoming`: filtros grupo/período, `kind=UPCOMING|OVERDUE|PENDING_EVIDENCE`, `page`, `size`. Devuelve `items,total,page,size`; detalle siempre bajo permisos documentales, diferente de agregados institucionales. Vencimiento usa plazos individuales efectivos.
- Bandeja de revisión admite `action=REVIEW|APPROVE` (REVIEW incluye VALIDATE), conservando filtro de tarea, búsqueda y paginación.

## Acceso y compatibilidad

Restablecimiento administrativo confirma hash/cambio obligatorio/revocación antes de envío SMTP. Fallo de entrega503 no revierte ese restablecimiento; UI debe recargar cuenta antes de reintentar. La contraseña solo existe en memoria de la operación y mensaje transitorio.

El contador automático de notificaciones no renueva el marcador de actividad de sesión. El servidor invalida sesión tras su intervalo configurado sin otra petición interactiva. No es un detector de teclado/ratón: las peticiones funcionales/autoguardados sí cuentan.

Los campos nuevos opcionales de snapshots históricos se omiten al serializar cuando corresponda. No reconstruir bytes firmados para adoptar campos, catálogos, estructuras o sellos nuevos.


## Recuperación de matriz incompleta — 01/11/2026

- `GET /api/work-plans/{id}/matrix/recovery` devuelve `{recovery: null}` o `{recovery:{baseVersion,revision,content}}` para el responsable autorizado de un T1 editable. El contenido se almacena por documento y usuario, fuera de la matriz canónica y del artefacto.
- `PUT` de la misma ruta acepta `{baseVersion,revision,content}` con filas incompletas tipadas y límites. `revision:null` crea; una revisión numérica actualiza con concurrencia. Cambios del documento o del borrador producen409 sin sobrescribir.
- `DELETE .../recovery?revision=N` descarta solo la revisión indicada. El guardado canónico admite `recoveryRevision`, comprueba concurrencia y elimina el borrador confirmado dentro de la misma transacción.
- Preparación y firma se bloquean si existen cambios pendientes del responsable actual. La reasignación del responsable no expone ni bloquea el borrador privado del responsable anterior. Se confirma explícitamente antes de recuperar un borrador cuya versión base quedó antigua.
- El navegador conserva la edición durante peticiones, informa de errores y permite reintentar. Hasta que el servidor confirme el borrador, la salida conserva la protección de cambios pendientes. No se almacenan certificados ni contraseñas.
