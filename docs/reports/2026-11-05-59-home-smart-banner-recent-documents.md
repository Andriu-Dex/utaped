# 2026-11-05 — Rediseño de Inicio: Banner Inteligente y Documentos Recientes

## Objetivo
Optimizar la pantalla de Inicio (`/`) para eliminar el espacio en blanco improductivo cuando un usuario no tiene tareas pendientes (`total === 0`), convirtiendo la sección de pendientes en una barra de estado delgada y discreta («✓ Al día: No tienes tareas pendientes. Puedes continuar con tus documentos recientes.») y otorgando protagonismo inmediato a la sección de **Documentos recientes** en la zona superior activa (*above the fold*).

## Cambios realizados
- `frontend/src/planning/PendingInbox.tsx`:
  - Incorporado renderizado condicional cuando `total === 0 && !expanded`. Muestra un banner compacto (`section.card.pending-banner-cleared`) con icono de verificación, mensaje institucional y botón «Ver filtros».
  - Mantiene la capacidad de desplegar manualmente las categorías mediante el estado interactivo `expanded`, agregando el botón «Contraer aviso» en la cabecera cuando está desplegado.
  - Expansión automática hacia la tarjeta completa si existen tareas pendientes (`total > 0`).
- `frontend/src/planning/TrackingDashboard.tsx`:
  - Restablecida la distribución de ancho completo vertical para mantener la jerarquía de lectura natural en PC y tabletas.
  - Eliminado el contenedor temporal de rejilla dividida de dos columnas tras la evaluación de prototipado.
- `frontend/src/planning/work-assistance.css`:
  - Estilos dedicados para `.pending-banner-cleared`, `.pending-banner-main`, `.pending-banner-text`, `.pending-banner-actions`, `.pending-banner-expand-btn` y `.pending-collapse-btn`.
  - Soporte completo para modo oscuro (`:root[data-theme='dark']`) con tonos semánticos verdes suaves (`rgba(16, 185, 129, 0.08)` y borde `rgba(16, 185, 129, 0.25)`).
- `frontend/src/planning/management-dashboard.css`:
  - Ajuste de márgenes superiores en `.recent-documents-card` para situar la lista de documentos recientes inmediatamente debajo del banner compacto sin requerir desplazamiento.

## Validaciones ejecutadas
- **Linter Frontend (`oxlint`):** 0 errores, 0 advertencias en 133 archivos analizados con 116 reglas activas.
- **Compilación TypeScript y empaquetado Vite:** `tsc -b && vite build` ejecutado exitosamente (código 0).
- **Despliegue local y sincronización:** Actualizados los archivos compilados en el contenedor en ejecución `utaped-frontend-1` (`127.0.0.1:64430`).
- **Verificación visual y responsiva:**
  - Modo claro en PC de escritorio (1440x900).
  - Modo oscuro en PC de escritorio (1440x900).
  - Estado expandido interactivo con filtros visibles.
  - Modo oscuro en tableta vertical (820x1180).
- **Pruebas automatizadas Playwright:** Verificada la navegación y autenticación sin regresiones.

