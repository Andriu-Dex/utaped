# Edición documental y motivos de solo lectura

Los recursos de documento, matriz y anexos incorporan `readOnlyReason`, calculado por el mismo criterio del servidor que determina `editable`. No cambia las autorizaciones de escritura ni permite editar documentos firmados.

| Código | Motivo |
|---|---|
| `ARCHIVED` | Documento archivado |
| `PERIOD_CLOSED` | Período cerrado |
| `NOT_PRINCIPAL` | El usuario no es el responsable principal |
| `IN_REVIEW` | Documento enviado a revisión |
| `APPROVED` | Documento aprobado |
| `DOCUMENT_LOCKED` | Estado documental no editable distinto de los anteriores |
| `SIGNED` | Firma de elaboración ya registrada sobre la revisión actual |
| `PREPARATION_NOT_STARTED` / `PREPARATION_ENDED` | Ventana de elaboración T1 aún no iniciada o finalizada |
| `PERIOD_NOT_STARTED` / `PERIOD_ENDED` | Período de elaboración del Informe aún no iniciado o finalizado |

`null` indica que el documento es editable. Cuando coinciden varias restricciones, se devuelve la primera según el orden de la tabla. Los límites de fechas son inclusivos; T1 considera la extensión efectiva del ámbito y la zona horaria institucional. La configuración de exigencia de fechas sigue vigente.

El frontend muestra el aviso junto al formulario, sin una alerta flotante permanente. General, contenido, matriz, anexos e informes comparten sus textos. Un código desconocido o ausente usa un mensaje general, sin inventar un vencimiento. Los permisos siguen dependiendo del servidor, no del aviso.

Una devolución permite corregir dentro de las condiciones vigentes; no concede por sí sola una extensión ni modifica la versión formal o los artefactos previamente firmados.
