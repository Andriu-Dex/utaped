# Circuito documental T1 y T2

Entrega de desarrollo del 2026-10-12. Complementa los contratos anteriores; prevalece en los nuevos documentos por ámbito. Todas las rutas tienen prefijo `/api`, sesión local y CSRF en mutaciones. IDs de documento, tarea y artefacto son explícitos. Un administrador no obtiene acceso implícito a PDFs privados.

## Organización y acceso

- `GET /careers`, `GET /organization/scopes`, `GET /organization/scopes/{id}/members`.
- Administración: `POST /admin/careers`, `POST /admin/organization/scopes`, `PUT /admin/organization/scopes/{id}/members`, `PUT /admin/organization/scopes/{id}/principal`.
- Un ámbito identifica grupo, período y carrera opcional. La carrera nula representa alcance de facultad. Su responsable principal debe ser integrante activo. Pertenencias y designaciones usan `rowVersion`; una edición obsoleta responde 409.
- Crear T1 requiere `scopeId`, grupo/período coincidentes, titularidad y ventana abierta. La unicidad de un T1 activo por ámbito se aplica siempre, independientemente de la bandera de compatibilidad de borradores individuales antiguos. El acceso queda concedido explícitamente a los integrantes iniciales. Un integrante incorporado después no obtiene todos los documentos anteriores. Una transferencia de titularidad concede acceso explícito al nuevo principal y conserva el historial.
- Integrantes iniciales pueden leer; solo el principal vigente edita el borrador. Los responsables de actividades cargan sus evidencias. Asignaciones de revisión conceden acceso al documento y, para T2, al Plan base necesario para revisarlo.
- Una pertenencia general que todavía tiene pertenencias por período no se elimina desde la pantalla de grupos: debe actualizarse desde Carreras y ámbitos.

## Firma y revisión

Los contratos de artefactos y firma del elaborador se conservan. En documentos por ámbito, la firma correcta inicia la primera tarea de revisión dentro de la misma transacción. La configuración y los participantes se congelan en el artefacto. No se inventa representante para un órgano colegiado; una etapa sin ejecutor individual explícito bloquea el envío.

| Ruta | Resultado |
|---|---|
| `GET /reviews/tasks` | Tareas propias activas e históricas; límite técnico actual de 200. |
| `GET /work-plans/{doc}/reviews` | Rondas del documento autorizado. |
| `GET /reviews/{id}` | Estado, etapas, observaciones, hash y páginas reales. |
| `GET /reviews/{id}/pages/{page}` | PNG del PDF firmado vigente; índice base cero. |
| `GET /reviews/{id}/content` | PDF final aprobado, con autorización. |
| `POST /reviews/tasks/{id}/observations` | Observación con `rowVersion`, `message` y página opcional. Resaltado: `x`, `y`, `width`, `height` normalizados a 0–1 y contenidos en la página. |
| `POST /reviews/tasks/{id}/decisions` | `rowVersion`, `action` COMPLETE/RETURN, `message`, `requestKey`. COMPLETE no sustituye una firma obligatoria. |
| `POST /reviews/tasks/{id}/signing` | Firma incremental; parámetros `rowVersion`, `inputHash`, `requestKey`; mismo cuerpo binario temporal de firma personal. |

Solo actúa la persona de la tarea activa. Todas las etapas se completan secuencialmente. Una devolución exige observación, conserva el PDF y firmas anteriores, cancela las tareas restantes y devuelve el documento a DRAFT con una ronda nueva. No aumenta su versión formal. Reenviar exige un artefacto y firma nuevos. La aprobación habilita la descarga final; no se exporta un borrador por la ruta de documento aprobado.

La firma comprueba identidad mediante huella pública vinculada, vigencia, CMS e integridad de todas las firmas acumuladas. No acredita cadena emisora, revocación, PAdES institucional ni sellado de tiempo. Contenedor y contraseña se liberan del proceso y no se guardan. La respuesta exitosa de firma de revisión tiene cuerpo vacío.

## Ejecución y prórrogas individuales

`GET /work-plans/{doc}/execution` devuelve actividades, medios, versiones de evidencia, responsables, fechas personales y cobertura. Se ejecuta un T1 aprobado y no archivado.

`POST /work-plans/{doc}/execution/activities/{activity}/media/{medium}?expectedVersion=N` recibe multipart `file`, exactamente un PDF por medio, con versiones conservadas. `GET /work-plans/{doc}/execution/evidence/{id}/content` descarga la evidencia autorizada. Solo un responsable asignado puede subir/reemplazar antes de su fecha límite; el índice de medio comienza en cero. Los límites y validación PDF aprovechan el almacenamiento privado existente.

`PUT /work-plans/{doc}/execution/activities/{activity}/extension` recibe `rowVersion`, `userId`, `endsOn`, `reason`. Solo un coordinador activo del ámbito puede concederla a un responsable activo de esa actividad. La fecha original no se cambia. El máximo técnico inicial es tres días calendario adicionales, limitado también por el período; la política definitiva sigue pendiente. Se conserva historial y notificación al destinatario. La cobertura de evidencias no se convierte automáticamente en porcentaje físico.

## Informe derivado

- `POST /work-plans/{id}/report`: crea o devuelve el Informe de un Plan aprobado, como documento independiente. Solo el principal del ámbito puede elaborarlo.
- `GET /reports/{id}`: documento, datos T2, actividades y resumen de evidencias.
- `PUT /reports/{id}`: `rowVersion`, `title`, `background`, `conclusions`, `improvements`, `results`, `delegations`. Cada resultado contiene `activityId`, `percentage` y `observations`. El porcentaje se declara explícitamente entre 0 y 100; no se infiere de archivos. Los IDs de resultados corresponden exactamente a la matriz base.
- La ventana técnica inicial para T2 es el período académico completo; se mantiene explícita hasta ratificación del plazo institucional de Informes.
- Previsualizar exige narrativas y resultados; si falta evidencia se requiere explicarlo. El T2 usa la plantilla oficial A4, la matriz del artefacto base aprobado y referencias a sus anexos/evidencias. Su firma y revisión siguen el mismo motor con identidad propia. No cambia el Plan base.

## Versiones, archivo y reportes

`GET /work-plans/{doc}/versions` devuelve la familia autorizada. `POST /work-plans/{doc}/archive` y `POST /work-plans/{doc}/versions` reciben `rowVersion` y `reason`.

El principal puede archivar sin purgar. No se archiva durante revisión ni un Plan con Informe activo. Una actualización formal de un documento aprobado archiva la versión anterior y crea otro ID, DRAFT y ronda 1 con versión mayor incrementada. Conserva contenidos y referencias a anexos, sin copiar firmas ni evidencias de ejecución. Un T2 mantiene una familia lógica por Plan, con una sola versión activa. Un borrador T2 archivado puede reemplazarse por otro ID dentro de la misma familia, conservando su número formal; esto no representa una actualización de un documento aprobado. El cierre de período y exportación ZIP siguen pendientes.

`GET /reporting` y `GET /reporting/export` aceptan `groupId`, `periodId` y `archived` (false por defecto). ADMIN ve totales institucionales, sin permiso nuevo sobre PDFs. Otros actores ven agregados de documentos autorizados. El CSV protege los valores interpretables como fórmulas. Los indicadores cuentan documentos, estados y cobertura documental; no representan una evaluación personal ni avance físico agregado.

## Compatibilidad

Flyway V13–V18 conserva documentos previos y agrega ámbitos, revisión, evidencias, T2, prórrogas individuales y familias formales. La creación individual antigua se habilita solo en fixtures de pruebas, no en el perfil normal. Las rutas existentes conservan IDs y permisos de sus documentos históricos. JDBC y contratos actuales siguen vigentes; no se declara una migración a JPA/OpenAPI que todavía no ocurrió.

## Períodos y flujos temporales (V19)

Las nuevas firmas por ámbito requieren asignaciones T1/T2 y activación explícita de los flujos del período. CLOSED bloquea las operaciones de escritura y conserva documentos y revisiones. El archivo histórico no sustituye los controles de descarga de preliminares. [Contrato de control y archivo](period-control.md).
