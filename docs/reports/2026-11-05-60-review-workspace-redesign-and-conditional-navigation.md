# 2026-11-05 — Rediseño de Bandeja de Revisión Documental y Navegación Condicional

## Objetivo
Resolver la colisión de estilos CSS que deformaba los filtros de la bandeja de revisión documental en bloques verticales gigantescos, eliminar títulos estáticos redundantes, incorporar un estado vacío institucional optimizado y condicionar la visibilidad del módulo «Revisión documental» en la barra lateral para que se muestre únicamente a usuarios con facultades o asignaciones activas de revisión/aprobación (Opción B de navegación).

## Diagnóstico y causas raíz
1. **Colisión de clases CSS en `.review-actions`:**
   - La regla CSS en `review.css` (línea 555) aplicaba `display: flex !important; flex-direction: column !important; width: 100% !important;` de forma global al selector `.review-actions`.
   - Dicha regla estaba pensada para los botones de acción del inspector lateral (`Tu decisión: Observar / Aprobar`), pero al no estar anidada bajo `.review-decision-card`, colisionaba con los botones de filtro de la bandeja (`Todas las intervenciones`, `Documentos por revisar`, `Documentos por aprobar`), apilándolos como enormes rectángulos que ocupaban el 100% del ancho de la pantalla.
2. **Navegación incondicional en barra lateral:**
   - `App.tsx` renderizaba el acceso a «Revisión documental» para todos los usuarios con sesión institucional activa, independientemente de si poseían roles de revisor, aprobador o si tenían tareas asignadas. Esto generaba confusión en usuarios exclusivamente creadores (como `dev.principal` / Andrés Molina), quienes veían la opción fija en el menú sin tener revisiones que atender.
3. **Inconsistencia de títulos y estado vacío pobre:**
   - La tarjeta interna mostraba un título fijo «Documentos para revisar» incluso cuando el filtro activo era «Todas las intervenciones» o «Documentos por aprobar».
   - Cuando la bandeja no contenía tareas pendientes, la presentación se limitaba a un texto plano poco informativo en lugar de una tarjeta amigable que permitiera regresar a «Documentos».

## Cambios realizados

### Backend
- `backend/src/main/java/ec/edu/uta/utaped/workflow/ReviewService.java`:
  - Ampliado el registro `InboxPage` con la propiedad booleana `canReview`.
  - Calculado `canReview` evaluando si el usuario tiene tareas activas/completadas/devueltas (`total > 0` o métricas > 0) o si su identificador figura asignado dentro de `frozen_workflow` en las asignaciones de ámbitos activos (`period_workflow_assignment`).

### Frontend
- `frontend/src/App.tsx`:
  - Incorporado el estado `hasReviewAccess` (booleano o null durante la carga inicial).
  - Al iniciar sesión o cambiar credenciales, consulta `/reviews/inbox?size=1` para determinar de manera reactiva si el usuario tiene asignaciones de revisión.
  - Condicionada la opción «Revisión documental» en la barra lateral: se oculta automáticamente para usuarios sin tareas ni facultades de revisión (como el creador principal), permaneciendo accesible para revisores y aprobadores designados.
  - Conectada la navegación hacia documentos mediante la prop `onNavigateToDocuments`.
- `frontend/src/workflow/ReviewWorkspace.tsx`:
  - Reemplazado el contenedor `.review-actions` por la estructura semántica `.review-inbox-tabs` con botones `.review-inbox-tab`.
  - Título de tarjeta dinámico: refleja reactivamente la pestaña activa (`action === 'REVIEW' ? 'Documentos por revisar' : action === 'APPROVE' ? 'Documentos por aprobar' : 'Todas las intervenciones'`).
  - Incorporado estado vacío institucional (`.review-empty-state`) con icono circular de verificación, mensaje claro («No tienes documentos pendientes de tu revisión o aprobación») y botón de acción directa «Ir a Documentos».
- `frontend/src/workflow/review.css`:
  - Aislada estrictamente la regla de botones de decisión bajo `.review-decision-card .review-actions`, erradicando la colisión de estilos.
  - Creados los estilos para el control segmentado `.review-inbox-tabs` y `.review-inbox-tab`: formato horizontal tipo píldora, elevación sutil, transición suave y compatibilidad total con modo oscuro (`:root[data-theme='dark']`).
  - Estilizado el estado vacío `.review-empty-state`, `.review-empty-icon` y `.review-empty-action` acorde al UTAPED Modern Design System.

## Validaciones ejecutadas
- **Linter Frontend (`oxlint`):** 0 errores, 0 advertencias en 133 archivos analizados.
- **Compilación TypeScript y empaquetado Vite:** `tsc -b && vite build` completado con éxito sin fallos de tipos ni advertencias.
- **Despliegue local y sincronización:** Actualizados los paquetes compilados en el contenedor Docker en ejecución `utaped-frontend-1`.
- **Verificación automatizada con Playwright:**
  - `dev.principal` (creador de documentos): verificación de que «Revisión documental» **NO aparece** en la barra lateral (`principal_home_sidebar_no_review.png`). En acceso directo por URL, muestra la bandeja limpia con pestañas horizontales y estado vacío «Al día» con botón «Ir a Documentos» (`review_inbox_redesigned_empty_light.png` y `review_inbox_redesigned_empty_dark.png`).
  - `dev.reviewer` (revisor institucional): verificación de que «Revisión documental» **SÍ aparece** en la barra lateral. Al pulsar, abre la bandeja con pestañas segmentadas horizontales, métricas KPI (3 pendientes, 2 completadas), filtro de búsqueda y la tabla con los documentos pendientes y botón «Abrir» (`reviewer_inbox_with_tasks_light.png` y `reviewer_inbox_with_tasks_dark.png`).
- **Verificación de temas y accesibilidad:**
  - Modo claro y modo oscuro validados con contraste cromático institucional y tipografía legible.
