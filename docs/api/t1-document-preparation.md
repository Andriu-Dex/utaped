# API — Anexos y preparación T1

Rutas bajo `/api/work-plans/{targetDocId}`. Requieren sesión, propietario activo y pertenencia activa. ADMIN no tiene acceso documental implícito. Mutaciones requieren CSRF; IDs de otro Plan no permiten acceder a archivos.

## Anexos

| Método y ruta relativa | Contrato |
|---|---|
| GET /attachments | Configuración, rowVersion, editable, límites e items con etiquetas, título, descripción, tamaño, páginas y SHA-256. |
| PUT /attachments/settings | `{rowVersion,enabled,privacyNoticeEnabled}`. No desactiva anexos existentes. |
| POST /attachments | multipart file; parámetros rowVersion, requestKey UUID y title. PDF validado y privado. |
| PUT /attachments/{attachmentId} | `{rowVersion,title,description}`. |
| DELETE /attachments/{attachmentId}?rowVersion=n | Baja lógica y renumeración sin cambiar artefactos anteriores. |
| PUT /attachments/order | `{rowVersion,attachmentIds}`: exactamente todos los IDs, sin repetidos. |
| GET /attachments/{attachmentId}/content | Bytes originales, application/pdf, descarga privada no-store. |

Etiquetas Anexo A/B/.../AA derivadas del orden. Anexos, medios y evidencias son entidades distintas; las evidencias de ejecución siguen pendientes. rowVersion se comparte con información general/matriz y aumenta al editar. No es versión formal.

Errores: 400 campos/PDF inválidos; 403 solo lectura/CSRF; 404 objeto fuera del alcance; 409 concurrencia, reutilización de requestKey o integridad fallida; 413 límites; 503 almacenamiento no disponible.

## Artefactos

| Método y ruta relativa | Contrato |
|---|---|
| GET /artifacts/readiness | `{rowVersion,ready,blockers:[{section,message}]}`; preparación para previsualizar. |
| GET /artifacts | Artefactos propios con sourceRowVersion/sourceHash/templateVersion, páginas, espacios de elaboración y coincidencia con snapshot actual. |
| POST /artifacts | `{rowVersion}`. Valida preparación y conserva PDF inmutable; reutiliza si snapshot/motor coinciden. |
| GET /artifacts/{artifactId}/content | PDF histórico con integridad verificada; descarga privada no-store. |
| GET /artifacts/{artifactId}/pages/{pageIndex} | PNG de página conservada; índice base cero, límites y dimensiones de raster controlados. |

Exige unidad, justificación, objetivo y al menos una actividad completa guardada. Comprueba obligaciones actuales, fechas/rangos/feriados configurados y responsables/colectivos vigentes. Si anexos=Sí, exige un PDF. Fuente/carrera opcionales mientras no exista regla distinta confirmada. Nota de protección de datos seleccionada explícitamente.

pageCount deriva de pages.length. Cada página registra dimensiones, T1/ANNEX e ID del anexo; el espacio de elaboración identifica actor real y página efectiva. No indica firma aplicada.

Solo DRAFT admite nueva generación. Un borrador fuera de ventana permite previsualizar sin cambiar contenido. Generar no cambia rowVersion, formalVersion ni documentState. Firma, revisión y T2 quedan pendientes. No existen rutas públicas/estáticas al volumen.
