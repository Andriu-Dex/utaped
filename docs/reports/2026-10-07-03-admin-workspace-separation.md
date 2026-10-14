# Separación del espacio administrativo

Fecha: 2026-10-07. Rama: `feature/modern-workspace-access`. Estado: cambios locales, sin commit, push ni modificación del pull request.

## Objetivo y fuentes

Aplicar la separación funcional aprobada por el propietario después de revisar las pantallas del administrador. Fuentes: `requirements.md`, `implementation-status.md`, Plan Maestro (§§46 y 60), decisiones de acceso y rediseño productivo. Prototipos permanece como referencia funcional congelada; la interfaz conserva el diseño productivo moderno.

## Implementación

El administrador inicia en **Panel general**, con contadores reales de usuarios registrados/activos, grupos activos y períodos, accesos a configuración y cinco eventos recientes de auditoría. Se reutilizan APIs existentes. No se muestran contadores ficticios de aprobaciones ni documentos personales en este inicio.

La navegación presenta módulos independientes:

| Módulo | Funcionalidad conservada |
|---|---|
| Usuarios | Altas, búsqueda paginada, perfiles, activación/desactivación y restablecimiento temporal de acceso. |
| Grupos y pertenencias | Alta/edición/activación de grupos; asignación y retiro de integrantes. |
| Períodos académicos | Alta y consulta de períodos con sus rangos. |
| Catálogo de actividades | Actividades por grupo y denominación colectiva. |
| Recursos y medios | Alta, edición y activación de catálogos. |
| Feriados y restricciones | Fechas y restricciones configurables por período. |
| Flujos de aprobación | Configuración versionada y conservación del historial; configurar no ejecuta una aprobación. |
| Auditoría | Filtros, historial paginado y exportación CSV aplicada. |
| Certificados | Vinculación/desvinculación de huellas públicas, sin certificados privados ni contraseñas. |

Cada página renderiza su módulo, en lugar de mostrar todos los formularios administrativos juntos. Notificaciones y seguridad de la cuenta permanecen disponibles.

Una cuenta ADMIN con pertenencia activa dispone de un selector **Espacio de trabajo** para entrar expresamente al espacio documental. Conserva su identidad real y los permisos de propiedad/pertenencia del servidor. Cambiar de espacio no cambia de usuario ni concede revisión/aprobación. Sin pertenencia activa, el menú administrativo no ofrece Documentos. Los usuarios institucionales conservan su inicio personal.

Se protegen cambios pendientes en altas, perfiles, flujos, catálogos, actividades, denominación colectiva, feriados, restricciones y vinculación pública de certificados. La navegación, el cambio de espacio y el cierre de sesión solicitan confirmación al descartar cambios; la recarga utiliza la advertencia nativa. Guardar un formulario no elimina la advertencia de otros formularios pendientes.

Se actualizan las pruebas E2E a la navegación nueva y al tooltip nativo `title` ya elegido por el propietario para el cambio de tema. Se preservan intactos los cuatro archivos previamente staged: `App.css`, `identity/Access.tsx`, `shared/ThemeToggle.tsx` y `styles/theme.css`.

## Validación

- `npm --prefix frontend run lint`: correcto, sin advertencias.
- `npm --prefix frontend run build`: TypeScript y Vite correctos; también compilación del contenedor frontend.
- Suite completa Playwright: **5 pruebas aprobadas (29,0 s)** contra PostgreSQL/backend/frontend reales del stack aislado. Incluye acceso local, recuperación, cambio de contraseña, catálogos, grupos/pertenencias, documentos, anexos/firma, flujos versionados, perfiles, auditoría/exportación, temas y aislamiento de permisos.
- Ampliación y repetición focalizada de `workflow.spec.ts`: **1 prueba aprobada**, incluyendo cancelación/confirmación al cambiar de espacio con un alta sin guardar, identidad conservada, acceso documental por pertenencia y regreso a Administración.
- Nuevas comprobaciones: ADMIN inicia en Panel general sin Documentos ni bienvenida personal; contador de usuarios coincide con API; los formularios de otros módulos no se renderizan; cancelación al navegar conserva el catálogo pendiente; usuario regular recibe HTTP 403 al consultar el directorio administrativo.
- Revisión visual de capturas del panel: PC 1440×1000 en claro/oscuro y tableta 820×1180 en oscuro, sin desbordamiento horizontal. Los flujos existentes también verifican temas en PC/tableta.
- `git diff --check`: correcto. Los cuatro archivos previamente staged no tienen cambios adicionales de esta entrega; no se alteró el índice.

La primera pasada encontró expectativas antiguas sobre el tooltip personalizado, reemplazado previamente por el propietario por `title`, y un nombre de etiqueta del selector de espacio que requería `aria-label` explícito. Se ajustaron las pruebas al tooltip existente y se corrigió la accesibilidad del selector; la repetición completa pasó. La selección de participantes utiliza búsqueda explícita al entrar en su módulo.

No se repitió la suite Java porque no cambió código de backend. Los E2E ejercitaron los servicios reales y las restricciones existentes.

## Alcance y operación

Sin modificaciones de backend, esquema de base de datos, migraciones, dependencias o variables de entorno. No se añaden estilos específicos para teléfonos. Se mantiene el puerto productivo configurado 64430.

La reorganización no implementa carreras/cargos, administración de plantillas, reportes globales ni ejecución del circuito institucional de revisión/aprobación; siguen pendientes del desarrollo previsto. No se presenta una funcionalidad pendiente como operativa.

Para ver los cambios si se usa Docker, reconstruir únicamente el frontend desde la raíz del proyecto:

```powershell
docker compose --profile web up -d --build --no-deps frontend
```

No se alteró el stack de trabajo del propietario. La validación se realiza en el proyecto Docker aislado `utaped-e2e`, con puertos distintos. No se realizó commit, staging, push ni actualización del PR.
