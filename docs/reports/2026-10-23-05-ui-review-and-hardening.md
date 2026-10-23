# Revisión de mejoras de interfaz y estabilidad

Fecha: 2026-10-23. Rama: `fix/institutional-document-spacing`.

## Evaluación

Se revisaron los cambios staged del propietario en listados documentales, información general, selección de elaboradores, matriz/actividades, formularios administrativos, iconos y centro de notificaciones. Se conserva su propuesta visual: mayor jerarquía, botones agrupados, estados compactos, acciones iconográficas con nombres accesibles, fila de actividad seleccionada y configuración bajo demanda. La tabla consolidada evita mantener abiertos todos los formularios y reduce la longitud del paso 3.

El conjunto se considera adecuado para continuar las pruebas manuales, tras corregir los problemas descritos abajo. No se cambian los formatos PDF institucionales ni sus datos, firmas o artefactos.

## Correcciones realizadas durante la revisión

- El editor de actividad utilizaba un overlay sin semántica/modalidad, confinamiento de foco ni bloqueo del fondo. Se convirtió en diálogo nativo con título accesible, foco inicial/restaurado, teclado y bloqueo del scroll. Cerrar/Listo conserva los cambios en el borrador local; quitar mantiene confirmación.
- Se corrigió el contenedor de desplazamiento del modal: el pie podía interceptar controles de medios de verificación al desplazarlos hasta el final.
- El cierre del detalle estaba dentro del fieldset deshabilitado en solo lectura. Se separaron los controles de cierre de los campos editables para poder consultar y salir sin habilitar modificaciones.
- Antes de guardar desde el modal se comprueban fechas presentes y ordenadas, se conserva el borrador ante errores y se dirige al detalle que requiere atención. Se mantiene el bloqueo para avanzar con cero actividades.
- Los botones Guardar borrador del editor general podían activarse únicamente por cambios pendientes de coautores. Ese guardado recargaba los elaboradores y podía descartar su selección. Se condicionan al contenido y se bloquea la edición general mientras hay otro formulario de autoría/ciclo de vida pendiente, evitando guardados cruzados.
- Las filas de notificaciones eran clicables mediante un div sin acceso equivalente por teclado. Se utilizan botones con nombre accesible, bloqueo durante peticiones y destino no disponible deshabilitado.
- La campana expone expansión/destino y el panel tiene nombre/rol de diálogo no modal. Apertura por teclado, cierre con Escape, botón, clic exterior o salida del foco; restauración de foco al cerrar explícitamente. Un fallo al abrir un destino mantiene el aviso visible.
- Se restituyen los filtros Todas/Sin leer/Leídas, que habían quedado sin controles aunque continuaban en el estado. La lista vacía describe el filtro seleccionado y una recuperación correcta limpia errores anteriores.
- Se reemplazan variables CSS inexistentes (`--primary`, `--blue-300`) por tokens definidos y se respeta la preferencia de movimiento reducido. No se agregan estilos específicos de celulares.

## Verificación

- Lint y compilación TypeScript/Vite aprobados, sin errores de tipos ni avisos de lint. Permanece el aviso conocido de bundle superior a 500 kB (569,37 kB; 149,68 kB gzip).
- Primera ronda: 16 pruebas existentes aprobadas. Al ampliar la cobertura se detectaron el solapamiento del pie del modal y la protección incompleta del formulario general; ambos se corrigieron.
- Ronda final: **17 pruebas de aceptación aprobadas**. Incluye T1 → devolución/revisión → firmas → evidencias → T2 → versión formal, matriz vacía/incompleta, conservación tras error, roles/permisos, selección de coautores, modal en solo lectura, Escape/restauración de foco y notificaciones con filtros/teclado/destino no disponible.
- La prueba de coautores verifica que Guardar borrador y los campos generales no se pueden usar mientras su selección está pendiente. La de matriz prueba un detalle servido como solo lectura sin habilitar fechas ni bloquear el cierre.
- Inspección visual de capturas reales en PC y tablet oscura: `activity-modal-desktop.png`, `activity-modal-tablet-dark.png`, `notifications-desktop.png`, `notifications-tablet-dark.png` y resumen de matriz. Sin desbordamiento horizontal de la página en tablet; la tabla conserva su propio desplazamiento.
- `git diff HEAD --check` aprobado. Entornos `utaped-acceptance` desechables eliminados tras cada ronda. No se tocaron los datos manuales ni se ejecutaron pruebas backend, al no cambiar servicios/contratos.

Evidencias ignoradas: `backend/target/ui-review-acceptance.log`, `ui-review-final.log`, `ui-review-verified.log` y `frontend/test-results/`. La validación corresponde al árbol de trabajo integrado, incluyendo las mejoras del propietario y las correcciones de esta revisión.

## Entrega

Commits de código: `b49e43a` (espacios documentales y formularios) y `54110a0` (notificaciones). El reporte, su índice y el estado de implementación se registran en un commit documental separado. Sin push ni pull request. No requiere variables de entorno, migraciones ni cambios de datos del entorno manual. Con Vite, recargar la aplicación; un frontend Docker compilado requiere reconstrucción.
