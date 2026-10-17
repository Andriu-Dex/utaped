# Previsualización simultánea del contenido T1

Fecha: 2026-10-17. Rama: feature/simultaneous-document-preview, creada desde origin/develop tras el merge del PR #12 (31ee2e4). El archivo frontend/pnpm-lock.yaml encontrado sin seguimiento queda fuera de esta entrega.

## Entrega

En el paso 2 Contenido del T1 editable se presenta un formulario junto a la página A4 real del documento. El ancho del formulario es ajustable en PC. En tablet se alterna Editar contenido / Previsualizar documento y se puede ampliar la página en un diálogo con Escape. La interfaz conserva el estilo existente, modo oscuro e independencia de los colores del documento institucional.

La vista se actualiza después de 1.4 segundos sin escribir, sigue la sección Justificación/Objetivo y ofrece navegación por páginas y reintento. Se distingue borrador, cambios sin guardar y contenido pendiente de previsualizar. Guardar borrador sigue siendo una acción explícita; no se agrega autosalvado ni se debilitan las validaciones de preparación/firma.

La imagen proviene del mismo T1Template/T1PdfEngine que genera el PDF institucional, con paginación real A4. Se aplican en memoria los textos actuales, incluidos los campos generales locales. La plantilla admite la matriz aún vacía mediante una fila en blanco bajo sus encabezados combinados: no se crea una actividad ficticia. La composición de matrices completas se conserva.

POST /api/work-plans/{targetDocId}/artifacts/draft-preview exige sesión, CSRF, permiso de edición, T1 DRAFT y rowVersion vigente. No guarda textos ni crea document_artifact/stored_file/firmas. La preparación formal continúa separada. No se generan vistas de edición de documentos firmados, aprobados o ajenos. Las rutas existentes de consulta de artefactos firmados conservan sus bytes.

Para evitar regenerar el PDF al cambiar de página, se conservan como máximo dos PDFs en memoria (12 MiB por PDF y dos minutos de reutilización). Acceso/estado/versión se revalidan antes de usarlos. La caché no se guarda en DB ni almacenamiento privado. Se comparte el límite de una generación concurrente con la preparación oficial; las solicitudes del editor se serializan y las respuestas obsoletas no reemplazan el contenido actual. Errores o servicio ocupado permiten reintentar sin perder el formulario.

## Validación

- Backend completo sobre PostgreSQL aislado: 145 ejecuciones en 21 suites, sin fallos ni errores. Incluye dos casos nuevos: textos largos/vacíos con matriz incompleta, paginación/sección/PNG/A4, ausencia de persistencia, conflicto de versión, documento ajeno y rechazo después del envío a revisión.
- Aceptación aislada sobre frontend Docker: 12 escenarios aprobados. El nuevo recorrido comprueba dos paneles, ancho por teclado, imagen de textos sin guardar, ausencia de artefacto, ampliación/Escape, alternancia en tablet, modo oscuro, error 503/reintento y guardado explícito posterior.
- E2E generales: ocho escenarios aprobados, incluyendo recarga/paso, accesibilidad del CAPTCHA, identidad, flujos, recuperación y tema. Su entorno se eliminó al finalizar.
- Lint/build frontend aprobados, sin advertencias finales de lint. Diff --check sin errores.
- Revisadas las capturas locales live-preview-desktop.png y live-preview-tablet-dark.png en frontend/test-results. La página institucional conserva fondo blanco en ambos temas; las capturas de página completa incluyen los elementos fijos en la posición del desplazamiento al capturar.
- Entorno local actualizado con start-development.ps1, sin variables nuevas ni reinicio de datos. Al comprobar la DB manual había 7 usuarios, 1 Plan y 30 ámbitos. No se ejecutaron pruebas sobre esa base ni se eliminaron sus registros. Los entornos desechables de aceptación/backend se limpiaron al terminar.

El primer ensayo detectó el fallo de la matriz vacía en los encabezados combinados de la plantilla; se corrigió y las dos pruebas específicas backend pasaron. La prueba visual detectó además una asociación inestable de etiquetas implícitas después de actualizar el texto; Justificación/Objetivo ahora usan etiquetas explícitas con identificadores por documento. El selector del panel de edición se simplificó. Los resultados finales de regresión se registran arriba.

## Uso y alcance

Entorno diario: scripts/start-development.ps1 desde raíz; npm run dev en frontend, URL http://127.0.0.1:5173. Abrir un Plan editable y pasar a Contenido. No requiere nuevas variables, migraciones ni datos semilla. Los datos manuales permanecen separados de las pruebas.

Esta primera entrega cubre Contenido del T1. El siguiente paso es validar su comodidad con el propietario y extender el patrón al editor de matriz y al T2. No se afirma que esas extensiones estén implementadas. Se mantienen las reglas de apertura explícita de editores, aislamiento documental y artefactos firmados inmutables.

[Diseño y límites](../design/simultaneous-preview.md). Commit local al finalizar; sin push/PR en esta implementación.
