# Personas ficticias realistas en desarrollo

Fecha: 2026-10-17. Rama: `feature/realistic-development-identities`, creada desde develop después del merge del PR #13.

## Solicitud y cambios

El propietario solicita una presentación verosímil en responsables y demás datos del escenario, conservando su carácter de ejemplo. Se sustituyen las siete etiquetas de cuenta «Desarrollo + rol» por nombres completos ficticios; la correspondencia se documenta en [entorno de desarrollo](../development/sample-environment.md).

Los cargos semilla pasan a Responsable principal, Coordinador del ámbito y Docente integrante. Los flujos nuevos usan Revisión documental y Aprobación del documento. No se reescriben revisiones históricas de flujos existentes. Carreras, período Julio–Diciembre 2026, comisiones, recursos, medios y actividades conservan los datos oficiales ya curados de templates. No se precargan documentos ni firmas atribuidos a autoridades reales.

La implementación permanece en el perfil development-seed, limitado a la base utaped_dev. Nuevas cuentas reciben los nombres realistas. Para cuentas existentes se exige ID, usuario, correo y nombres genéricos originales coincidentes antes de actualizar. Los cargos solo se sustituyen cuando conservan el literal semilla original. Los cambios incrementan sus versiones; personalizaciones administrativas y contraseñas se conservan. No se añade una migración global ni una variable de entorno.

## Aplicación en el entorno manual

Se realizó un respaldo conjunto verificado antes de la actualización, en backend/target/backups/2026-10-17-before-realistic-identities (ignorado por Git), con un documento, cero archivos privados referenciados y Flyway V23. Incluye base, volumen e imágenes; no se hizo un reset.

Se reconstruyó/inició el backend con scripts/start-development.ps1, manteniendo el modo Vite y los tres servicios diarios. Se verificaron las siete cuentas con sus nombres nuevos y cero cargos genéricos pendientes. Una comparación por huellas antes/después confirmó intactos credenciales y estado de acceso, permisos de miembros, filas de documentos y artefactos. No se publicaron contraseñas, hashes privados ni archivos de configuración en Git.

Para ver los cambios basta recargar la aplicación. Se utilizan los mismos dev.admin/dev.principal/etc. y las mismas contraseñas. Cualquier nombre almacenado dentro de un artefacto firmado conserva sus bytes originales.

## Validaciones

- DevelopmentSeedTests: dos pruebas aprobadas en PostgreSQL desechable, con rechazo de base incorrecta, creación idempotente, actualización de nombres/cargos antiguos, conservación de credenciales y respeto de personalizaciones.
- Lint y build TypeScript/Vite aprobados. Permanece la advertencia conocida de bundle de 510,84 kB; no se cambió código visual en esta entrega.
- Aceptación del circuito: 14 escenarios aprobados, incluidos permisos por rol, T1/T2, firma, devolución, evidencias y versiones. Repetición focalizada de los dos escenarios de referencia aprobada, con capturas de responsables en PC claro y tablet oscuro, sin desbordamiento horizontal. Se usó el mismo lanzador aislado, filtrando el archivo de pruebas para esa repetición.
- Revisión visual de ambas capturas: nombres completos legibles y distribución correcta en las dos presentaciones.
- Verificación del respaldo y de invariantes del entorno manual aprobada.

Los ensayos automatizados se ejecutan en proyectos desechables; la actualización manual autorizada se limita a los perfiles/cargos de ejemplo. No se altera la base institucional ni Prototipos. Los nombres ficticios no representan nombramientos oficiales. Las revisiones de flujos anteriores conservan su denominación histórica.

## Fuentes y entrega

Solicitud actual del propietario; AGENTS.md productivo; requisitos y estado vigentes; semillas DevelopmentSeed/DevelopmentReference; documentación de procedencia institucional. Comprobaciones: Maven `-Dtest=DevelopmentSeedTests` mediante compose.test.yml; scripts/test-acceptance.ps1; repetición Playwright focalizada en institutional-reference.spec.ts dentro del mismo arranque/limpieza de aceptación; npm run lint y npm run build. Logs, capturas y respaldo permanecen ignorados. Commit local tras completar las comprobaciones; sin push ni PR en esta entrega.
