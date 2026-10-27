# Firma sin generación duplicada y orden de documentos

Fecha: 2026-10-24. Sin commit por instrucción vigente.

## Implementación

El paso 6 del T1 consulta la preparación existente y no muestra la tarjeta Revisión del contenido ni Generar previsualización. No realiza POST de generación. Un borrador editable sin preparación vigente muestra Volver a previsualización y no monta acciones de firma. Los errores de consulta permiten reintentar. Se conserva la consulta de artefactos históricos y el circuito de coautores/documentos firmados; los formularios de firma se bloquean ante edición pendiente de secciones o constancia, sin descartar sus campos por esa sola edición. T2/T3 conservan su espacio combinado de preparación/firma.

Ordenar por ofrece actualización reciente/antigua y título A–Z/Z–A. Se aplica a la consulta completa antes de LIMIT/OFFSET, mediante cuatro expresiones SQL constantes y desempate por UUID. No se interpola entrada del usuario en ORDER BY; un valor desconocido devuelve 400. La ruta existente conserva su orden por defecto. Cambiar orden vuelve a página 1, conserva búsqueda/grupo/período y no altera los permisos de lectura. El formulario se adapta en dos columnas a tablet, con el botón Buscar al final.

## Verificación

Lint, TypeScript y build aprobados, con el aviso conocido de bundle. Se añade integración HTTP para cuatro órdenes, filtros, paginación y aislamiento; aceptación para el selector en escritorio/tablet oscuro y para reutilización del mismo artefacto en paso 6, ausencia de generación y bloqueo ante composición desactualizada. Resultados finales: 3 pruebas backend y 22 pruebas de aceptación aprobadas. Revisadas las capturas document-ordering-desktop.png y document-ordering-tablet-dark.png. Logs: backend/target/2026-10-24-document-sort-backend.log y backend/target/2026-10-24-signing-and-sort-verified.log. Los entornos de prueba se limpiaron al finalizar. Se actualizó utaped-sample sin restablecer datos. No se realizaron commits ni publicaciones.
