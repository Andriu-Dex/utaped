# Entrega ampliada — Flujos, administración y trazabilidad

Fecha: 2026-10-03. Rama: feature/workflow-administration-audit desde develop, merge 68119c4 del PR #4, con CI previo correcto. Trabajo organizado en bloques verificados y commits separados; un único push/PR al finalizar. Revisión y fusión reservadas al propietario.

## Bloque 1 — Configuración de flujos

Flyway V7 y módulo administrativo para definiciones T1/T2 independientes por grupo. Etapas ordenadas, personas múltiples, roles de pertenencia y órganos colegiados sin firmante individual cuando no requieren firma personal. Borrador editable, validación de destinatarios, concurrencia, revisión técnica inmutable, historial y deshabilitación. Ninguna configuración concede acceso a archivos ni inicia revisión/firma documental.

Validación del bloque: frontend build/lint; 25 pruebas backend con PostgreSQL aislado, sin fallos; dos escenarios Playwright con Nginx/Docker, incluido flujo administrativo, falta de destinatarios, orden, historia y separación T1/T2. Q-003/Q-007/Q-018 siguen pendientes para ejecutar transiciones institucionales. [API](../api/workflow-configuration.md).


## Bloque 2 — Directorio y perfiles

Flyway V8: contadores técnicos de usuarios/grupos. Búsqueda y paginación, edición de nombre/permiso, filtros de acceso, edición y activación de grupos, preservación de documentos/pertenencias y rechazo de conflictos. Protección del último administrador y revocación de sesiones al cambiar permisos. Selectores de integrantes y participantes buscan más allá del límite anterior de 200 usuarios. Formularios conservan cambios ante error y protegen navegación.

Validación: frontend build/lint (tres avisos de sincronización de estado desde propiedades, sin errores); 28 pruebas backend sin fallos, incluidas paginación con 220 registros, concurrencia, CSRF, revocación y aislamiento por grupo inactivo; dos E2E con Nginx/Docker, incluidos edición, navegación con cambios pendientes, filtros y desactivación/reactivación. [API](../api/administrative-directory.md).

## Bloque 3 — Auditoría e historial documental

Flyway V9 añade nombres capturados para eventos nuevos e índices de consulta. Visor ADMIN con filtros por acción/actor/objeto/instante y paginación. Historial T1 por identificador explícito, autorizado por propietario y pertenencia vigente; no incluye acciones de otros documentos ni autenticación. ADMIN no concede acceso al historial/archivo de un Plan ajeno. Eventos previos indican que muestran nombre actual; no se inventa su identidad histórica.

El visor no expone contenido, contraseñas, certificados ni correos. No agrega edición/purga de registros ni política institucional de retención. La historia de acciones es distinta de versión formal, revisión de configuración y ronda documental. [API](../api/audit-history.md).

Pruebas backend: 31 casos totales con PostgreSQL aislado, cero fallos; filtros temporales inclusivos, límites/paginación estable, entradas inválidas, autorización, nombres históricos y retiro de pertenencia. Se corrigió una prueba que enviaba dos parámetros size diferentes; el servidor aplicaba correctamente el primero. Frontend build/lint pasan sin avisos, tras eliminar sincronizaciones innecesarias en efectos.

## Alcance pendiente

Ejecución institucional de revisión, firma real, observaciones/devoluciones, T2, evidencias y agente código/Trello no forman parte de esta entrega. Q-003/Q-007/Q-018 no se resuelven por configurar etapas. Q-006 permanece pendiente; no se introducen reaperturas ni prórrogas. Mockup congelado sin modificaciones.

## Commits y publicación

- 9be9cf7: configuración versionada de flujos.
- 8390517: directorio y perfiles.
- Tercer commit: auditoría, historial, comprobaciones y documentación final; identificado en el historial de la rama y en el PR.

Push único de feature/workflow-administration-audit y PR hacia develop al finalizar verificaciones. El propietario conserva revisión y fusión; main no se modifica.

Verificación final: dos escenarios E2E ampliados con Nginx/Docker, incluidos historial del Plan, auditoría filtrada/sin resultados, directorio, perfiles, flujos y regresión de PDF inmutable. Revisión visual de historial móvil y auditoría de escritorio; el historial móvil usa filas adaptadas para evitar nombres/acciones recortados. Controles accesibles explícitos evitan ambigüedades entre nombres de acciones y campos administrativos. Las capturas y reportes de pruebas permanecen ignorados, al igual que secretos y datos de ejecución.
