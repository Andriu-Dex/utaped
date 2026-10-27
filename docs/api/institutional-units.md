# Procedencia institucional

## Catálogo

`GET /api/institutional-units`: unidades activas para usuarios autenticados; Administración también consulta inactivas. Cada unidad expone `id`, `name`, `unit_type` (ACADEMIC o ADMINISTRATIVE), `active` y `row_version`.

`POST /api/admin/institutional-units` y `PUT /api/admin/institutional-units/{id}` reciben `name`, `unitType`, `active`, `rowVersion`. Solo ADMIN; escritura con CSRF y auditoría. La actualización requiere la versión vigente; no se puede cambiar el tipo si hay carreras o ámbitos asociados. No hay eliminación física.

## Asignaciones

Crear carrera admite `institutionalUnitId`, que debe corresponder a una unidad académica activa. Crear ámbito admite `institutionalUnitId`; una carrera debe pertenecer a esa misma unidad y estar activa. Las unidades administrativas no admiten carrera. Para compatibilidad, omitir la unidad en carreras usa FISEI; omitirla en ámbitos deriva de la carrera o usa FISEI cuando no hay carrera. Las pantallas nuevas envían la selección explícita.

Los ámbitos se identifican por grupo, período, unidad y carrera opcional. El catálogo no cambia permisos ni asigna automáticamente responsables.

## Documentos

El T1 nuevo hereda nombre de unidad, tipo y carrera del ámbito. Los campos quedan bloqueados en el editor. Los documentos antiguos sin ámbito pueden elegir del catálogo activo; los valores históricos sin cambios siguen siendo legibles. T2, T3 y nuevas versiones formales conservan la procedencia del documento base.

El nombre y tipo documentales son una instantánea: renombrar o desactivar el catálogo no reescribe documentos. El encabezado T1/T2 diferencia Unidad académica / Unidad administrativa y omite la carrera en la segunda. Los artefactos firmados no se reconstruyen. El hash omite ACADEMIC, que era el tipo implícito histórico, para conservar la compatibilidad de artefactos previos.

## Migración

V34 crea el catálogo y asocia carreras y ámbitos existentes con FISEI, equivalente al valor que antes se fijaba al crear documentos. Mantiene textos, versiones y firmas existentes. Solo se precarga FISEI; otras unidades deben registrarse desde Administración con datos confirmados. No se inventan unidades institucionales.
