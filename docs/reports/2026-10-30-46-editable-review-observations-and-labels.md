# Observaciones de revisión editables, normalización de etiquetas y archivo de custodia

Fecha: 2026-10-30.
Estado: implementado, verificado y documentado; sin commits ni push según las instrucciones vigentes.
Rama: `fix/integral-product-audit`.

## 1. Alcance y objetivo

Implementar la gestión completa de observaciones de revisión (creación, edición y retiro lógico propio) en el flujo de revisión documental de UTAPED, normalizar la presentación de las etiquetas heredadas de ejemplo sin mutar la base de datos, y garantizar la protección de datos en la interfaz y en el archivo histórico de custodia institucional.

Se resolvieron los tres aspectos críticos identificados:
1. **Preservación estricta de página asociada:** Al editar observaciones vinculadas a una página (con o sin resaltado), la navegación de lectura en el visor PDF no reasigna la página de la observación; se preserva `associatedPage` hasta que el usuario decida reasignarla explícitamente («Asociar a página actual» o trazado de una nueva zona).
2. **Detección de cambios y protección al cancelar:** Convertir una observación de página en observación general activa inmediatamente la detección de cambios pendientes (`isFormDirty` y activación del botón «Guardar cambios»). «Cancelar edición» evalúa si existen modificaciones respecto al estado base y solicita confirmación mediante `confirmAction` antes de descartar cualquier texto o geometría.
3. **Preservación de historial en custodia de período:** La tabla `review_observation_history` se incorpora formalmente en `documentMetadata` (`observationHistory`), garantizando que cada `manifest.json` del ZIP de custodia de período contenga el historial completo de cambios previos y retiros antes de la purga física.

## 2. Modificaciones técnicas

### Backend y base de datos
- **Migración Flyway `V37__editable_review_observations.sql`:**
  - Añade columnas `updated_at`, `deleted_at` y `deleted_by` a `review_observation` con restricción de coherencia en eliminaciones lógicas.
  - Crea la tabla `review_observation_history` con `operation` (`UPDATE`, `DELETE`), estados serializados en JSONB (`before_state`, `after_state`), marcas de tiempo y claves foráneas en cascada.
  - Índices parciales para consultas eficientes de observaciones activas (`WHERE deleted_at IS NULL`).
- **`ReviewService.java` y `ReviewController.java`:**
  - Añade `PUT /api/reviews/tasks/{taskId}/observations/{observationId}` y `DELETE /api/reviews/tasks/{taskId}/observations/{observationId}?rowVersion={rowVersion}`.
  - Reutilización de `lockTask` para verificar sesión, pertenencia de tarea, ronda activa (`IN_REVIEW`), tarea activa (`ACTIVE`) y concurrencia optimista (`rowVersion`).
  - Extracción y reutilización de validación geométrica (`validateGeometry`) para coordenadas normalizadas `[0, 1]`, límites finitos y páginas válidas.
  - Registro de auditoría `REVIEW_OBSERVATION_UPDATED` y `REVIEW_OBSERVATION_DELETED`.
  - Normalización en presentación mediante `ReviewService.displayStageLabel(...)` para `REVIEW` («Revisión de ejemplo» → «Revisión documental») y `APPROVE` («Aprobación de ejemplo» → «Aprobación del documento»).
- **`PendingWorkService.java`:** Incorpora `stage_action` y envuelve la etiqueta en `displayStageLabel`.
- **`PeriodArchiveService.java`:** Incorpora `observationHistory` en `documentMetadata` para el archivo ZIP de custodia y ordena la eliminación previa de `review_observation_history` durante el purgado del período.

### Frontend
- **`workflowLabels.ts`:** Función centralizada `displayStageLabel`.
- **`ReviewWorkspace.tsx`:**
  - Modelo de observación extendido (`updated_at`, `can_edit`, `can_delete`).
  - Estado `associatedPage` independiente del número de página actualmente visualizado (`page`), garantizando que hojear el PDF mientras se edita no altere la página vinculada a la nota.
  - Detección precisa de suciedad en formulario (`isFormDirty`) contrastando mensaje, página propuesta y geometría contra el estado base (`baselineMessage`, `baselinePage`, `baselineRegion`), cubriendo transiciones a observación general.
  - Diálogo de confirmación asíncrono con salvaguarda de datos antes de cancelar la edición o descartar borradores.
  - Acciones iconográficas de editar y eliminar protegidas por `can_edit` y `can_delete`, badge discreto «Editada» y estilos completos para tema claro y tema oscuro en `review.css`.

## 3. Pruebas y verificación

- **Pruebas backend dirigidas (`ReviewExecutionIntegrationTests`):**
  - `editableObservationsLifecycleAndAudit`: ciclo completo de creación, edición de texto y geometría, verificación de `updated_at`, registro en historial con `before_state`/`after_state`, detección de conflicto de versión (409), retiro lógico (404 en reintentos posteriores) y auditoría.
  - `observationValidationAndGeometryConstraints`: páginas fuera de rango, geometrías incompletas/inválidas, aceptación de observación general y conversión de página sin zona.
  - `withdrawnObservationDoesNotBlockOrJustifyReturn`: verifica que notas retiradas no justifican una devolución (`RETURN`) ni bloquean `COMPLETE`.
  - `observationPermissionsAndIsolationBetweenDocuments`: no permite edición/retiro por parte de administradores no asignados ni autores, ni tras el cierre de etapa/decisión.
  - `stageLabelPresentationNormalization`: normalización estricta de las dos denominaciones heredadas.
  - `observationHistoryPreservedInPeriodCustodyArchive`: comprobación de que el ZIP de custodia contiene `manifest.json` con `observationHistory`, `before_state` y `after_state`.
  - Resultado: **Aprobadas sin errores.**
- **Pruebas de aceptación Playwright (`review-signing-draft.spec.ts`):**
  - Preparación de firma protegida y navegación sticky (conservada).
  - CRUD de observaciones y normalización de etiquetas de revisión heredadas en bandeja y lista de etapas.
  - Preservación de página asociada al navegar por el visor, detección de dirty al convertir a general y confirmación requerida al pulsar «Cancelar edición».
  - Resultado: **3 pruebas de navegador aprobadas.**
- **Comprobación estática frontend:**
  - `npm run typecheck`: 0 errores.
  - `npm run lint` (oxlint): 0 errores y 0 advertencias.
  - `npm run build`: empaquetado Vite correcto.
- **Entorno manual `utaped-sample`:**
  - Migración V37 validada y aplicada automáticamente al iniciar.
  - Backend y bundle de frontend actualizados en los contenedores de desarrollo.


## Revisión de cierre

La cancelación desde el contenedor ActionForm y Escape utiliza una sola confirmación: el contenedor confirma antes de limpiar el borrador. El botón interno «Cancelar edición» conserva su confirmación propia. Se añadió una regresión de navegador que comprueba rechazo conservando texto y aceptación cerrando el formulario sin un segundo diálogo para ambas vías.

Validación del código de revisión previo: 49 pruebas backend sin fallos. Tras conservar el historial en el archivo del período, también pasó la prueba dirigida `observationHistoryPreservedInPeriodCustodyArchive`. Las mejoras visuales de inicio y reportería pasaron tres pruebas de aceptación. En el cierre se verificaron build y lint del frontend.

Resultado de cierre: las tres pruebas de `review-signing-draft.spec.ts` pasaron, incluyendo Cancelar/Escape con confirmación única y conservación del texto al rechazar. `git diff --check` sin incidencias.
