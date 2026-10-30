# Carga de reportes: rendimiento y actualización

## Diagnóstico y medición

La pantalla solicitaba grupos, períodos y cálculo del reporte mediante un Promise.all cada vez que se cambiaban filtros o se actualizaba. Cualquier respuesta lenta retrasaba todo el contenido. Además, se eliminaba la visualización anterior incluso al consultar de nuevo exactamente los mismos filtros.

Se midió ReportingService.get en PostgreSQL aislado usando el mismo escenario de tres actividades con responsables/evidencias y cuatro consultas consecutivas. Antes: [130, 110, 104, 105] ms. Después de deshabilitar JIT para la transacción de lectura: [6, 1, 1, 1] ms. Los resultados completos permanecieron idénticos entre lecturas. Estas cifras son del fixture local; no representan latencia HTTP, tiempos del navegador, dimensionamiento institucional ni una garantía para cualquier volumen. No se reprodujeron todavía los 2–3 segundos observados por el propietario en su navegador.

## Backend

ReportingService usa SET LOCAL jit = off en la transacción de lectura: evita compilar consultas interactivas complejas cuyo costo de compilación no se amortiza. No modifica PostgreSQL globalmente ni deja una configuración de sesión permanente en conexiones del pool. La exportación CSV también tiene transacción de lectura explícita para que el cálculo interno mantenga el mismo alcance transaccional. El panel administrativo reutiliza el mismo cálculo y recibe la mejora.

Se conservan SQL de métricas, ACL, filtros, prórrogas, resultados declarados, fórmulas, CSV y separación de agregados administrativos frente a detalles privados. No se añadió caché ni índices especulativos.

## Frontend

- Catálogos cargados independientemente del reporte, sin solicitarlos de nuevo por filtros/Actualizar.
- El reporte puede aparecer aunque los catálogos estén pendientes; fallo de filtros con reintento propio.
- Primera carga y cambio de filtros: skeleton accesible con tarjetas y gráficos, claro/oscuro y tablet.
- Actualizar los mismos filtros: datos conservados y estado «Actualizando reporte…», exportación suspendida mientras carga.
- Fallo de actualización: advertencia explícita de que se conserva la última consulta y botón de reintento.
- Snapshot asociado a identidad de consulta (contexto administrativo y filtros). Nunca se presentan datos de filtros distintos como actuales; respuestas tardías descartadas al cambiar consulta/desmontar.
- Errores de exportación separados de errores de consulta para no anunciar incorrectamente un fallo de actualización.

## Verificación

Ocho pruebas ReportingIntegrationTests aprobadas: indicadores, versiones, privacidad, prórrogas, históricos, administración, CSV y estabilidad de resultados. Cuatro pruebas de aceptación aprobadas: reportería/CSV, panel administrativo, acceso agregado y carga con respuestas controladas. El escenario nuevo retiene catálogos para comprobar carga independiente, falla la actualización conservando datos, cambia filtros ocultando el snapshot previo, falla/reintenta sin reutilizar datos de filtros distintos y comprueba que no se recargan catálogos.

Typecheck y lint aprobados; build aprobado con advertencia previa de tamaño del bundle. Captura del skeleton en tablet oscura revisada. No se usan umbrales temporales frágiles en CI. Comprobación adicional del restablecimiento de JIT al devolver la conexión al pool y actualización del entorno manual registradas al cierre.

Cierre: prueba dirigida de estabilidad/restablecimiento de JIT aprobada. Entorno manual actualizado con start-development.ps1 -Web, conservando datos y sin iniciar Vite. Sin commits ni push en esta entrega.
