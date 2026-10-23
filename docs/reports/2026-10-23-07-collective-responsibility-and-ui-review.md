# Responsables colectivos y revisión de cambios pendientes

## Correcciones

- Las semillas usan Responsable de la comisión, Responsable de la unidad, Responsable del club o Responsable del grupo según el tipo, como en Prototipos. El valor sigue siendo configurable por Administración.
- La carga corrige únicamente etiquetas antiguas iguales al nombre original del grupo y conserva las personalizadas. Se mantienen los integrantes individuales, documentos y firmas.
- La tabla muestra la etiqueta colectiva y el número de integrantes; el tooltip permite consultar los nombres. La selección parcial conserva los nombres individuales.
- Se revisaron e incorporaron las mejoras pendientes del propietario: resumen compacto de cinco columnas, rango de fechas, categorías, indicadores de configuración y encabezado simplificado del modal. Se conserva la eliminación desde la tabla y la protección de actividades obligatorias.
- El resumen ya no considera completa una configuración que solo tiene recursos o solo medios. El rango de fechas admite ajuste de línea para no invadir responsables en tablet.
- Se incluye la navegación pendiente entre Anexos, Preparación y Firma, conservando confirmaciones y errores sin descartar el formulario.

## Entorno manual

Se actualizaron 23 etiquetas antiguas en utaped-sample, mediante IDs deterministas de las semillas y coincidencia del nombre original; no se alteraron etiquetas personalizadas ni los artefactos firmados. Respaldo previo de etiquetas y versiones en backend/target/collective-labels-before.csv (ignorado). Recargar la matriz para consultar los valores nuevos; los PDF ya conservados no se reconstruyen.

## Validación

- Lint y compilación TypeScript/Vite aprobados. Advertencia previa de bundle mayor a 500 kB sin bloqueo.
- DevelopmentSeedTests: 2 pruebas aprobadas en PostgreSQL aislado, incluyendo reparación idempotente y conservación de etiquetas personalizadas.
- Suite final Playwright: 17 pruebas aprobadas, incluyendo selección parcial/colectiva, persistencia, errores de guardado, navegación secuencial T1, permisos y circuito T1/T2 completo.
- Capturas inspeccionadas del modal en PC y del resumen en tablet oscura; corregido el solapamiento del rango de fechas. La tabla conserva desplazamiento horizontal interno en tablet.
- Entornos desechables de backend y aceptación eliminados tras las verificaciones; se conserva utaped-sample.
- Cambios preparados en fix/matrix-responsibility-and-navigation para commit local, sin push ni actualización de pull request.
