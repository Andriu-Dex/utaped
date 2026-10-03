# Actividades y matriz T1

Fecha: 2026-10-03. Rama: `feature/work-plan-activities`, desde `develop` tras verificar PR #2 fusionado (49a71bf). Mockup externo congelado. Implementación nueva sin commit, push ni PR.

## Resultado

El Plan productivo permite configurar y guardar su matriz de actividades en PostgreSQL. El administrador configura actividades por grupo, recursos, medios, denominaciones colectivas y feriados; no se incorporan datos institucionales ficticios.

- Flyway V5: catálogos de actividades y recursos/medios, feriados, configuración colectiva/por período y matriz por `work_plan_id`.
- Actividades obligatorias seleccionadas en borradores editables, opcionales y Otra libre. Las obligatorias no se pueden quitar desde UI ni API.
- Fechas completas ordenadas dentro del período. Restricción configurable para extremos en feriados, permitiendo atravesarlos.
- Uno o varios responsables activos del grupo, Seleccionar todos y representación colectiva únicamente con todos seleccionados y denominación configurada. Se conservan identidades individuales.
- Recursos/medios de catálogo, selección múltiple y Otro con descripción obligatoria. Error inline, enfoque del campo y conservación del formulario; el conteo excluye Otro vacío.
- Fuente editable y Elaborado por derivado del grupo; resumen tabular legible. No es todavía documento A4 oficial.
- Matriz guardada de forma atómica como agregado JSONB tipado/validado, por ID explícito, con auditoría. La API conserva snapshots de los títulos/clasificaciones/obligatoriedad y etiquetas seleccionadas; cambios de catálogo no reescriben datos guardados.
- Concurrencia compartida entre matriz e información general/contenido. Un cambio obsoleto recibe 409 sin sobrescritura; comparación y recarga explícita conservan cambios locales hasta confirmar descarte.
- Protección de navegación pendiente de guardado y consulta en solo lectura fuera de ventana. En solo lectura no se añaden nuevas obligaciones desde catálogos.

Las altas/ediciones/desactivaciones de catálogos y configuración exigen ADMIN y CSRF. Leer/guardar matriz exige titularidad y pertenencia al grupo activo; ADMIN no da acceso a matrices ajenas.

## Validación

- Frontend: build TypeScript/Vite y oxlint.
- Backend: suite de 18 pruebas con PostgreSQL aislado: 12 de base, 1 contexto, 2 borradores y 3 actividades.
- Pruebas nuevas: obligatoriedad y clasificación derivadas en servidor, Otro vacío, responsables ajenos, límites de fechas, feriados en extremos y cruce permitido, permisos/CSRF, aislamiento, persistencia, snapshots ante cambios de catálogo y concurrencia con otras secciones.
- Playwright ampliado sobre Vite y compilación Nginx/Docker: administración de catálogos/obligaciones/etiqueta colectiva, responsables, Otro vacío con foco, actividad libre, guardado/reapertura, resumen y conflicto entre pestañas con preservación y recarga. Se mantiene el flujo anterior de identidad/administración/recuperación.
- Inspección visual móvil y comprobación de ausencia de desbordamiento horizontal.
- Revisión de diff y enlaces de documentación.

Durante la verificación se corrigió un nombre accesible que cambiaba al mostrar el error de Otro. Docker requirió reconstrucción de backend sin caché por una capa de extracción ausente; no fue un error de aplicación ni de migración. Capturas y salidas de pruebas son locales e ignoradas.

## Trazabilidad y pendientes

Fuentes: requirements.md, catálogo de requisitos y reglas de negocio; [contrato de matriz](../api/work-plan-matrix.md). Cobertura inicial: FR-ACT-001–006, FR-DATE-001–004, FR-RESP-001–005, FR-REC-001–005, FR-MED-001–003, FR-MAT-001/002 y UT-SEC-011.

Q-001/Q-002 continúan abiertos: no se establece el número final de pasos ni se cambia la cardinalidad provisional del Plan. No hay anexos, A4/PDF oficial, finalización, firma, revisión, evidencia PDF ni T2. No se considera el medio de verificación del Plan una evidencia de ejecución.

Las denominaciones colectivas, catálogo de feriados y política aplicable deben validarse/configurarse institucionalmente. La condición de nota de datos personales sigue pendiente. Si un responsable deja de ser aplicable, su selección no se elimina silenciosamente; debe corregirse antes de guardar otra vez. La congelación del artefacto formal y sus referencias se implementará con finalización/motor documental.

El agente interno para revisar código y Trello mantiene su documentación y sigue sin implementación. Próxima entrega recomendada: anexos T1 y contrato de almacenamiento (Q-010), seguido del motor documental con formatos oficiales validados (Q-013).
