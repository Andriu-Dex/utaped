# Borradores de Plan de Trabajo

Primera entrega de planificación. Sesión local y CSRF para POST/PUT, con las mismas reglas de la base productiva. El servidor deriva la identidad del usuario autenticado; ningún cliente elige al elaborador. ADMIN no habilita lectura ni edición de documentos ajenos en este módulo.

| Método | Ruta bajo `/api` | Contrato |
|---|---|---|
| GET | `/work-plans/options` | Grupos de pertenencia activos, períodos con indicador editable y políticas configuradas. |
| GET | `/work-plans` | Lista propia; `page` desde 0, `size` 1–50 (20 predeterminado), `groupId`, `periodId`, `query` por título hasta 200 caracteres. |
| POST | `/work-plans` | `groupId`, `periodId`, `requestKey` UUID y `title` hasta 200 caracteres. Devuelve el borrador creado. |
| GET | `/work-plans/{targetDocId}` | Documento propio por identificador explícito. |
| PUT | `/work-plans/{targetDocId}` | `rowVersion`, `title`, `institutionalUnit`, `career`, `justification`, `objective`; todos presentes. |

Título obligatorio; unidad/carrera hasta 200 caracteres y contenido hasta 50 000 por campo. Contenido incompleto permitido en borrador. El cuerpo de respuesta incluye identificadores, nombres contextuales, estado `DRAFT`, versión formal `1.0`, fecha civil de elaboración, fechas de creación/actualización, `rowVersion` y `editable`.

`rowVersion` controla concurrencia técnica: cada guardado lo incrementa. No es versión formal ni ronda de revisión. Una edición con revisión obsoleta recibe 409 y no altera el documento. No existe sobrescritura forzada. El frontend conserva los cambios locales, permite comparar y cargar la versión del servidor con confirmación de descarte.

Reintentar POST con igual identidad y `requestKey` devuelve el mismo documento sin otra auditoría. La clave identifica la solicitud original (grupo, período y título de creación), incluso después de editar el título. Reutilizarla con otro contenido de creación devuelve 409.

Sin pertenencia activa, GET/PUT devuelven 404 y POST 403. Fuera de ventana de elaboración inclusiva, el documento se consulta en solo lectura y POST/PUT devuelven 403. Fechas calculadas en `America/Guayaquil`, configurable. La política no crea prórrogas ni reaperturas.

Configuración: `PLANNING_SINGLE_PLAN_PER_SCOPE=false` provisional mientras Q-002 esté pendiente; `true` limita un Plan por docente/grupo/período. `PLANNING_ENFORCE_PREPARATION_WINDOW=true`; `PLANNING_TIME_ZONE=America/Guayaquil`. No se interpreta esta configuración como aprobación institucional de reglas pendientes.

Persistencia Flyway V4; auditoría `WORK_PLAN_CREATED` y `WORK_PLAN_DRAFT_SAVED`, sin copiar contenido documental al evento. No hay eliminación, firma, artefactos PDF, envío a revisión, actividades ni T2 en esta entrega.
