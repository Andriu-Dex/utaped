# Almacenamiento y respaldo documental

Compose monta document-data en `/app/runtime-data`; backend utiliza `/app/runtime-data/documents`. Sin Docker, DOCUMENT_STORAGE_ROOT permite carpeta privada; valor por defecto `./runtime-data/documents`. runtime-data está ignorado por Git. Nunca servir esta carpeta con Nginx ni versionar PDFs de usuarios.

DOCUMENT_MAX_FILE_BYTES, DOCUMENT_MAX_REQUEST_BYTES, DOCUMENT_MAX_TOTAL_BYTES, DOCUMENT_MAX_ATTACHMENTS y DOCUMENT_MAX_ATTACHMENT_PAGES ajustan límites técnicos. A4 es la decisión del proyecto; REFERENCE permite comparar dimensiones originales. Para aumentar carga sobre 12 MB, ajustar también client_max_body_size en frontend/nginx.conf y reconstruir.

Docker incluye Liberation Sans y DejaVu Sans; instalarlas al ejecutar sin Docker. No necesita Word, LibreOffice ni conversión externa.

## Respaldo consistente

Actualización 2026-10-12: se entregan `scripts/operations.py` para respaldo conjunto DB/volumen/imágenes, validación y restauración en destinos nuevos; se comprobó con 57 documentos y 156 archivos ficticios. [Herramientas, comandos y límites](../operations/production-readiness.md). Los pasos manuales siguientes quedan como explicación del procedimiento, no como ausencia de herramientas.

Este procedimiento todavía no constituye un programador de respaldos ni un RPO/RTO institucional.

1. Identificar proyecto Compose y volúmenes reales; no confundir pruebas con institución.
2. Detener backend y frontend para bloquear escrituras, conservando db activo. No usar down -v.
3. Exportar PostgreSQL mediante pg_dump en formato custom a destino protegido. En PowerShell copiar un archivo creado dentro del contenedor para evitar recodificar bytes con redirección textual.
4. Archivar document-data con contenedor de respaldo confiable, origen de solo lectura y conservación de permisos. Registrar fecha, versión, migración y hashes de ambos archivos.
5. Proteger/cifrar respaldo y secretos por separado. Comprobar ambos archivos y reiniciar servicios.

DB y archivos son un único conjunto de respaldo. La DB sola no recupera PDFs.

## Restauración

Probar primero en entorno aislado con escrituras detenidas: pg_restore para DB, archivo del volumen para documentos y permisos del usuario utaped. Usar la misma versión de aplicativo y verificar Flyway antes de actualizar. Autenticar propietario, descargar anexos/artefactos históricos, verificar hashes y visualizar páginas.

Probar recuperación completa con datos institucionales autorizados antes de habilitar operación. Se ensayó restauración técnica completa con datos de ejemplo, Flyway/conteos, hashes de archivos, acceso HTTPS y descarga de un PDF firmado con los mismos bytes. Este ensayo no acredita los insumos ni la política de operación institucional.

## Retención

Anexos quitados y artefactos anteriores se conservan; no se regeneran al consultar. Retención legal, baja definitiva y acceso tras retirar pertenencia requieren validación. Hoy retirar pertenencia revoca el acceso según permiso existente.

Una interrupción abrupta entre escritura y commit puede dejar bytes sin metadata. Rollback normal los elimina; no hay limpieza automática de huérfanos. Reconciliar UUIDs con stored_file y proteger referencias antes de considerar cualquier eliminación. Nunca borrar volúmenes para reparar errores rutinarios.
