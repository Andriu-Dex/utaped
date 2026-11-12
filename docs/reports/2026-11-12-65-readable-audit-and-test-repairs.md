# Auditoría legible y reparación de pruebas

Fecha: 2026-11-12. Rama: `feature/home-review-and-document-timeline`, que reúne los commits locales del 05/11 (Inicio, bandeja de revisión, creación de documentos, previsualización y cronología) más esta entrega.

## Objetivo

La revisión del video comercial mostró que la Auditoría administrativa presentaba identificadores internos (UUID) y códigos de acción en inglés. Al verificar la rama antes de publicarla, la suite de backend tenía tres pruebas de la cronología que nunca habían podido pasar, y dos escenarios E2E desactualizados.

## Cambios

### Auditoría administrativa

- Cada evento incluye el nombre actual de su objeto (`targetLabel`: título del documento, persona, grupo, período, carrera, cargo o grupo del ámbito) y de la persona afectada (`subjectUserName`).
- La tabla muestra nombres. Ya no presenta identificadores internos; si el objeto ya no existe, indica «Registro no disponible».
- Los filtros «ID del actor» e «ID del objeto» se reemplazan por búsquedas por nombre: «Persona que realizó la acción» y «Documento o registro afectado». Los parámetros por UUID se conservan en la API para el historial del documento y usos técnicos.
- Se tradujeron las 23 acciones que el backend registraba sin etiqueta en español: Informe de ejecución, estructuras, Quipux, plazos, custodia, borrado del período, cargos, actividades futuras y feriados, entre otras.
- El CSV conserva los identificadores en columnas propias («ID objeto», «ID usuario afectado») y agrega los nombres. Las posiciones de columna cambian para quien lea por índice. Contrato: [audit-history.md](../api/audit-history.md).
- Excepción deliberada: la vista administrativa muestra el título de los documentos. Es el único dato documental expuesto a administración; no concede lectura del contenido.

### Pruebas reparadas

- `DocumentTimelineIntegrationTests`: los datos de prueba no cumplían el esquema real (faltaban `creation_title` y `preparation_date`, y se usaban columnas inexistentes de `stored_file`). El caso del colaborador suponía acceso a un Plan sin ámbito; ahora obtiene acceso real por ámbito. El caso de estados usa una cuenta administradora, porque un borrador descartado solo es visible para administración, y un T3 en lugar de un segundo T2 activo, que la base impide.
- El caso de fechas no fabricadas pasó a `ReviewExecutionIntegrationTests`, donde se construye una revisión real firmada y aprobada antes de quitar `acted_at` y `closed_at`. Las aserciones originales se conservan.
- E2E `foundation` y `workspace`: desde el 31/10 el botón «Crear documento» se oculta a quien no tiene ámbitos para crear. Las pruebas esperaban abrirlo y ver un mensaje; ahora verifican que el botón no aparece.
- No se cambió el comportamiento de `DocumentTimelineService`: con los datos corregidos, sus controles de acceso y fechas se comportan como se esperaba.

## Validaciones ejecutadas

- `scripts/test-backend.ps1`: 394 pruebas en 36 suites, sin fallos ni errores.
- `scripts/test-e2e.ps1`: 8 escenarios aprobados, incluido el filtro y la exportación de auditoría.
- Frontend: `npm run lint`, `tsc -b` y `npm run build` sin errores.

No se ejecutó `scripts/test-acceptance.ps1` ni una revisión visual en navegador de la nueva tabla de auditoría.

## Pendiente

- Volver a grabar el video comercial con esta versión: escenas de auditoría, feriados (respuesta vacía del proveedor) y nueva ronda (pantalla en carga), con datos de ejemplo más completos.
