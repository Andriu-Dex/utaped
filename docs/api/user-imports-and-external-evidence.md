# Importación de usuarios y evidencias externas

Entrega del 2026-10-12. Decisiones funcionales: Plan Maestro §§6, 7, 40 y 44. Complementa [acceso local](local-access.md), [matriz](work-plan-matrix.md), [circuito documental](document-circuit.md) y [cierre](period-control.md).

## Importación administrativa

Rutas bajo `/api/admin/user-imports`, sesión ADMIN y CSRF en mutaciones:

| Ruta | Método | Resultado |
|---|---|---|
| `/` | POST multipart `file` | Lote temporal y prevalidación por fila; no crea cuentas. |
| `/{id}` | GET | Solo el administrador que preparó el lote puede consultarlo. |
| `/{id}/commit` | POST | Creación atómica y CSV privado con credenciales temporales; solo una ejecución. |

CSV UTF-8, con BOM opcional, separador coma o punto y coma, comillas estándar y hasta 1 MiB / 500 filas de datos. Encabezados obligatorios: `email,username,first_names,last_names`. Opcionales: `scope_id,membership_role,position_label`. No se aceptan columnas desconocidas, duplicadas, contraseñas o privilegios ADMIN. Se normalizan correo y usuario; nombres y apellidos se conservan. Identificador de acceso ASCII de hasta 100 caracteres con letras minúsculas, números, punto, guion o guion bajo.

Se rechazan cuentas existentes, incluso inactivas: no hay actualización implícita. Los duplicados del archivo y campos inválidos aparecen por fila. Un lote con errores no puede confirmarse. Al confirmar se comprueban nuevamente unicidad, referencias y período: conflictos o cierre abortan toda la transacción. Las restricciones únicas y el bloqueo del lote impiden duplicación concurrente.

Se crean únicamente cuentas USER activas, con contraseña aleatoria independiente, hash Argon2id y cambio obligatorio. La pertenencia opcional requiere un `scope_id` válido de grupo/ámbito activo y período abierto, con `membership_role` MEMBER o COORDINATOR. El ID puede seleccionarse/copiarse desde Carreras y ámbitos. Esta pertenencia conserva auditoría e historial; no designa principal, concede acceso documental ni autoriza recuperación de cuentas. La importación delegada a coordinadores sigue pendiente.

El resultado contiene `username,email,temporary_password`; se neutraliza el inicio de fórmula en correos exportados. Respuesta `Cache-Control: no-store`, `Pragma: no-cache` y descarga explícita. No se guardan contraseñas en tablas de lote, auditoría ni almacenamiento del navegador. El administrador debe conservar privadamente el archivo descargado y entregar cada credencial a su titular. Una segunda confirmación devuelve 409 y no recupera contraseñas. Si la respuesta se pierde después del commit, consultar el lote y usar restablecimiento administrativo, sin reimportar.

Prevalidación temporal: 30 minutos. Filas personales se eliminan al confirmar y, si caducan sin confirmar, en una limpieza técnica cada diez minutos. Permanecen ID, propietario, nombre saneado, hash del archivo, fechas y conteo, además de auditoría. No se almacena el archivo fuente. Son límites técnicos iniciales, no una política institucional de retención. Errores principales: 400 formato; 403 permiso; 404 propietario/recurso; 409 conflicto/repetición; 410 caducado; 413 tamaño/filas.

## Tipo de medio de verificación

El catálogo MEANS incorpora `evidenceKind: PDF | URL`, por defecto PDF. RESOURCE solo admite PDF como valor técnico. La selección de matriz toma el tipo desde el servidor y lo conserva junto con etiqueta y referencia de catálogo; cambios posteriores del catálogo no cambian medios ya guardados. `Otro` mantiene PDF. Matrices históricas sin el campo se interpretan como PDF.

## Evidencias externas

`POST /api/work-plans/{documentId}/execution/activities/{activityId}/media/{mediumIndex}/reference` recibe:

```json
{"expectedVersion":0,"title":"Acta de seguimiento","url":"https://repositorio.example.invalid/acta"}
```

Requiere medio URL, T1 aprobado, acceso explícito, responsable asignado, plazo personal vigente y período abierto. Un PDF no puede sustituirse por URL y viceversa. Se usan los mismos IDs documentales explícitos y controles de concurrencia de la evidencia PDF. Título obligatorio, hasta 200 caracteres; URL absoluta HTTPS, hasta 2048, sin credenciales en el host, espacios o controles. No se consulta ni descarga el sitio desde el servidor. El responsable evita URLs con secretos; el navegador abre en otra pestaña sin acceso a `opener` ni envío de referrer.

Las versiones de un medio forman una secuencia única; reemplazar crea otra fila, conservando autor/fecha/referencia anteriores. URL no tiene `file_id`, hash, páginas ni contenido descargable de PDF. GET de ejecución expone `evidenceKind,externalUrl,referenceTitle`; PDF mantiene sus campos originales. Un medio cubierto por URL cuenta en cobertura y avisos de pendientes, sin atribuir validación del contenido remoto ni porcentaje físico.

El T2 congela ID y versión de referencia, título y URL en su snapshot; los muestra como texto en medios/anexos. Solo PDFs se anexan como páginas. Cambiar una evidencia del Plan no modifica bytes ni referencias de un T2 firmado. Cierre histórico conserva todas las referencias en los manifiestos del ZIP, sin intentar incluir archivos inexistentes ni descargar recursos externos.

## Persistencia y límites

Flyway V21: `user_import_batch`, tipo del catálogo, tipo/referencia de `execution_evidence` y restricción de payload PDF/URL. La nueva metadata de referencias vacía se omite del snapshot para conservar hashes de documentos previos sin enlaces.

Fuera de alcance: comprobar disponibilidad/identidad/integridad remota, importar XLSX, actualizar usuarios por CSV, altas delegadas, compartir credenciales por correo y otros tipos documentales oficiales aún no proporcionados. No se garantiza rendimiento de lotes máximos antes de pruebas de carga.
