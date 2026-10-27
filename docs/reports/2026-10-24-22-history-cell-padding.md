# Espacio interior del historial de cambios

Fecha: 2026-10-24. Ajuste autorizado por el propietario, sin commit.

Las celdas de datos de T1/T2 incorporan 3 pt (60 twips) de margen interno superior e inferior mediante tcMar. Conservan centrado vertical/horizontal y altura mínima adaptable; no se reduce la fuente ni se fija una altura que recorte descripciones largas. No cambia la cabecera gris. T1 engine15 y T2 engine14 permiten regenerar los borradores con el ajuste sin tocar los archivos firmados.

Verificación específica aislada de InstitutionalHistoryTests y de composición/paginación histórica de DocumentIntegrationTests. Dos comprobaciones aprobadas, sin fallos/errores. Log: backend/target/2026-10-24-history-padding.log. Revisado el PDF renderizado en backend/target/document-qa/history-padding.png. Se limpiaron los recursos de pruebas al finalizar. No hay cambios de frontend.
