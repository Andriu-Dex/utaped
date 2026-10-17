# Limpieza de ejemplos y referencias institucionales

Fecha: 2026-10-17. Rama: fix/document-creation-options.

## Decisión del propietario

Se autoriza reemplazar los registros actuales porque pertenecen al desarrollo y son ejemplos. Los archivos de docs/templates son fuentes institucionales oficiales. La excepción confirmada es el tamaño Carta de una referencia: T1/T2 deben seguir en A4. Esta autorización no convierte las asignaciones ficticias en cargos oficiales ni habilita una limpieza futura de producción.

## Alcance

Se reconstruye exclusivamente la base manual utaped-sample/utaped_dev desde las referencias curadas y trazables de templates. Se conservan los siete usuarios dev y sus hashes de contraseña; se retiran los demás usuarios y los escenarios/documentos operativos de ejemplo. Se reconstruyen grupos, carreras, período, ámbitos y catálogos oficiales con los mismos identificadores estables de semilla. Se borran sesiones y habrá que iniciar sesión de nuevo.

No se ejecuta ResetData, que regeneraría también cuentas. La nueva herramienta scripts/clean-development-data.py tiene previsualización por defecto y requiere --apply, DB/proyecto/perfil exactos y respaldo emparejado válido. Rechaza cambios de metadata posteriores al respaldo y reinicia solo los servicios que estaban activos. No modifica migraciones Flyway ni lógica productiva de documentos.

## Respaldo y almacenamiento

Respaldo privado ignorado por Git: backups/pre-cleanup-2026-10-17. Incluye PostgreSQL, documentos e imágenes de aplicación. Verificación terminada correctamente: 122 documentos y 302 archivos, migración 22. Se esperó a su finalización antes de ejecutar la limpieza efectiva.

Los archivos del volumen no se purgan físicamente: los archivos antiguos sin referencias no aparecen en la aplicación y también se conservan en el respaldo. No se borran carpetas de templates ni archivos oficiales. No se modifican otras bases/proyectos Docker.

## Resultado y comprobaciones

- Antes: 46 usuarios y 122 documentos de ejemplo. Después: 7 usuarios dev, 0 documentos; hashes de contraseña conservados, comprobados en memoria sin registrarlos.
- Referencias reconstruidas: 3 carreras, 23 grupos, 1 período Julio–Diciembre 2026 (01/07/2026–31/12/2026), 30 ámbitos, 55 actividades y 26 elementos de catálogo (15 recursos y 11 medios).
- Validación exacta por identificador/nombre de todas las carreras, grupos y actividades frente al JSON oficial curado. Las 30 combinaciones activas de grupo/carrera/período coinciden con la fuente; no quedan grupos «Ámbito recuperación» ni «Unidad creación».
- Backend CSRF: HTTP 200. Permanecen activos solo db/backend/mail de utaped-sample; no se inició el frontend Docker ni las copias antiguas.
- Los flujos T1/T2 de ejemplo se regeneran para revisión manual, sin activar automáticamente el período ni registrar firmas/aprobaciones institucionales.

Se ejecutaron 11 pruebas Python de operaciones/limpieza, sin fallos, incluyendo rechazo de base ajena, ausencia de perfil semilla, respaldo inválido y datos cambiados antes de TRUNCATE. Son pruebas con fixtures locales; la aplicación de la limpieza se valida además sobre el entorno de desarrollo autorizado.

Se comprobó DOCUMENT_PAGE_FORMAT=A4 en el backend y la configuración A4 de T1/T2. No se alteró su maquetación. No se ejecutó una nueva batería frontend/backend porque no se cambió código de la aplicación: el cambio es una operación de datos, un comando administrativo local y documentación.

El archivo frontend/pnpm-lock.yaml ya estaba sin seguimiento al comenzar; se conserva sin incluirlo en el commit. No se hizo push ni se actualizó el PR.
