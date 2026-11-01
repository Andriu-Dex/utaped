# Revisión de interfaz y cierre de cambios

Fecha: 2026-10-29. Autorización del propietario para realizar los commits pendientes, incluidos cambios visuales staged y de trabajo. Sin push.

## Cambios revisados

Acceso y confirmación de recuperación; navegación administrativa agrupada; indicadores y reportería; directorios, grupos y pertenencias; unidades institucionales, cargos y asignaciones; creación de usuarios, importación CSV, avisos y auditoría. Tablas y filtros conservan resultados durante consultas; se mantienen formularios cerrados inicialmente, confirmaciones de descarte, permisos y errores recuperables. Tema oscuro y presentación PC/tablet comprobados.

Se incorporan también las mejoras funcionales documentadas en el reporte 43: pendientes personales, comparación de rondas y checklist T1/T2/T3. No se altera el motor documental ni los PDF firmados. Los cambios se agrupan en dos commits: interfaz y asistencia al flujo documental.

## Verificación

- Lint, TypeScript y build correctos; sigue el aviso anterior de tamaño de bundle.
- Once pruebas de aceptación correctas: formularios/cancelación, procedencia institucional, navegación administrativa en cuatro tamaños/temas, consultas con recuperación, indicadores/exportación, permisos del panel administrativo, importación CSV y los cuatro escenarios nuevos de asistencia.
- Capturas actuales de auditoría en escritorio y tablet oscura revisadas. Sin desbordamiento global ni excepciones JavaScript en el recorrido administrativo.
- Se conserva la evidencia backend previa de 225 pruebas generales, cuatro regresiones de flujo y dos comprobaciones ampliadas de plazos. No se repitió la batería Java porque desde esas verificaciones solo había nuevos cambios visuales.
- diff --check correcto. Logs de esta revisión en backend/target/2026-10-29-commit-review-acceptance.log; datos y credenciales fuera de Git.

## Proceso del puerto 5173

Se identificó node.exe PID 28180 ejecutando el Vite de frontend de este repositorio. Era el servidor de desarrollo que permanecía abierto de la sesión anterior. Se cerró únicamente ese proceso y se confirmó el puerto 5173 libre. No se cerró el navegador ni se detuvieron los servicios Docker; el frontend compilado permanece disponible en http://127.0.0.1:64431.
