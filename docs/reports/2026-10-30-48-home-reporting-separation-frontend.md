# Separación de Inicio y Reportes de gestión — frontend

## Alcance

Implementación de la decisión del propietario según `instruction.md`:
1. **Inicio** es estrictamente el espacio de trabajo personal: pendientes de acción (`PendingInbox`) y acceso directo a documentos recientes (`/api/tracking/recent`).
2. **Reportes de gestión** concentra todos los indicadores analíticos, gráficos ejecutivos, filtros de supervisión institucional y exportaciones CSV.
3. Se retiró de Inicio todo el bloque de métricas analíticas («Panorama documental», `ManagementOverview`, gráficos de estado y distribución, filtros de grupo/período para analítica y botón «Actualizar seguimiento»).
4. Se eliminó la llamada concurrente `Promise.all([/tracking, /work-plans/options, /reporting])` de Inicio; ahora consulta exclusivamente `/api/tracking/recent`.
5. Se conservó el Panel general de supervisión institucional de Administración sin transformarlo en bandeja personal.
6. Sin adición de ítems intermedios de menú «Dashboard».

## Implementación

- **`frontend/src/planning/TrackingDashboard.tsx`**:
  - Cabecera compacta integrada con kicker `ESPACIO PERSONAL`, saludo institucional `Bienvenido, {displayName}`, descripción breve orientada a retomar trabajo y botón de acceso secundario `Ver reportes de gestión` (`onOpenReports`). Se eliminó la doble cabecera anterior (antiguo banner extenso `.welcome-banner` + bloque repetitivo de bienvenida), acelerando el acceso inmediato a las acciones pendientes en tablet y escritorio.
  - Integración de `<PendingInbox onOpen={onOpen} />` en primer lugar como resumen compacto de las tareas pendientes del usuario con conteo global por categorías.
  - Tarjeta `<section className="card recent-documents-card">` en segundo lugar con hasta cinco documentos recientes:
    - Indicador de tipo documental (`Plan T1`, `Informe final T2`, `Informe de ejecución`).
    - Título del documento y versión formal.
    - Grupo y período académico asignados.
    - Badge de estado documental real derivado de `documentState` (`Borrador`, `En revisión`, `Aprobado` o `Archivado`).
    - Pill secundario `Solo lectura` cuando `!plan.editable`, sin sustituir el estado documental real.
    - Fecha y hora de última modificación legible (`Actualizado ...`).
    - Botón de acción `Abrir` que navega directamente al visor/editor del documento.
    - Enlace inferior `Ver todos los documentos ({total})` cuando hay más de 5 accesibles, o `Ver todos los documentos` hacia la lista general.
  - Carga, error y reintentos completamente independientes de la bandeja de pendientes: un fallo en recientes no oculta pendientes ni presenta conteos artificiales a 0; un fallo en pendientes no afecta a la tarjeta de recientes.
  - Estado vacío accesible con mensaje `Aún no tienes documentos disponibles.` y botón hacia `Documentos`.
  - Cancelación activa (`active = false`) en desmontaje para evitar condiciones de carrera o datos de sesiones previas.
- **`frontend/src/App.tsx`**:
  - Pasa callback `onOpenDocuments={() => navigate('documents')}` a `TrackingDashboard`.
- **`frontend/src/planning/types.ts`**:
  - Exporta `type PlanSummary = PlanPage['items'][number]`.
- **`frontend/src/planning/management-dashboard.css`**:
  - Estilos modernos para `.tracking-home-header`, `.recent-documents-card`, `.recent-card-footer`, `.recent-view-all-btn`, `.recent-error-state`, `.status-readonly`, `.tracking-item-meta`, `.tracking-item-updated`.
  - Soporte completo de tema oscuro (`:root[data-theme='dark']`) y adaptación para tablet (`@media (max-width: 820px)`).

## Validación y Pruebas Realizadas

1. **Chequeo de tipos y linters**:
   - `npm --prefix frontend run typecheck`: 0 errores.
   - `npm --prefix frontend run lint`: 0 errores y 0 advertencias (oxlint).
   - `npm --prefix frontend run build`: compilación de Vite completada correctamente.
2. **Pruebas de aceptación Playwright en entorno aislado (`scripts/test-acceptance.ps1`)**:
   - `management-reporting.spec.ts`: 6 pruebas aprobadas (9.2s):
     - `management overview distinguishes declared progress, coverage and evidence; filters and CSV remain connected` (ok).
     - `personal home requests only recent documents and pending work, avoiding analytical calls` (ok: verifica que en Inicio no se llame `/api/reporting` ni `/api/work-plans/options`, que `Indicadores de gestión` no aparezca, y desruta adecuadamente los mocks antes de navegar a Documentos para no bloquear las opciones reales).
     - `recent documents supports 0 items, 1 item, and more than 5 items without confusing total with item length` (ok: comprueba 0 documentos con navegación a Documentos y validación de su contenedor accesible, 1 documento con pill solo lectura y fecha, y >5 documentos con paginación `Ver todos los documentos (8)`).
     - `recent documents and pending work isolate errors and support local recovery` (ok: aislamiento de fallos entre pendientes y recientes con reintento local).
     - `recent documents displays real status badges for in review and approved without replacing state with read only` (ok: estados En revisión, Aprobado y Borrador sin enmascarar).
     - `administrative home includes institutional indicators without implicit private document access` (ok).
   - `work-assistance.spec.ts`: 4 pruebas aprobadas (8.7s).
   - `product-circuit.spec.ts`: 1 prueba E2E completa multi-rol aprobada (59.8s).
3. **Comprobación visual y diseño**:
   - Capturas en entorno muestra (`utaped-sample`) en escritorio claro, escritorio oscuro y tablet.
   - Sin commits ni push según las directrices del proyecto.


## Revisión de cierre

Se retiró el banner de bienvenida redundante y se integró el nombre del usuario en la cabecera compacta de Inicio. Las pruebas liberan las rutas interceptadas antes de navegar a Documentos y aceptan su estado vacío legítimo, sin exigir una tabla inexistente.

Verificación final: siete pruebas de aceptación de Inicio, Reportes y Administración aprobadas; typecheck, lint y build correctos. Captura de tablet en tema oscuro revisada. Las tres pruebas backend del contrato de recientes ya habían pasado durante su implementación, sin cambios posteriores de backend. No se repitió el circuito completo en esta revisión. Cierre con commits locales autorizado por el propietario, sin push.
