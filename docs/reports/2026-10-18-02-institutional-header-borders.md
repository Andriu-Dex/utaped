# Bordes completos del encabezado T1/T2

Fecha: 2026-10-18. Rama: `feature/document-authorship-and-review`.

## Corrección

Se normalizan explícitamente los bordes exteriores e interiores del encabezado compartido T1/T2. El formato anterior dependía de bordes parciales heredados del DOCX, con grosor de medio punto y sin definir todas las divisiones de las celdas. En el rasterizado de la vista previa faltaban líneas o se apreciaban discontinuidades.

La composición utiliza líneas continuas de un punto y color gris oscuro `475569`, con contorno superior/inferior/lateral, separación del logo y divisiones entre universidad, título, unidad/carrera y fecha. Se conserva la celda vertical combinada del logo, sin atravesarla con líneas horizontales. Se tomó como referencia el encabezado de `DocumentPdfPageViewer.tsx` de Prototipos, sin modificar ese proyecto.

No cambian el contenido, los márgenes A4, el logo ni las firmas. El motor pasa a engine6 para que las nuevas preparaciones no reutilicen el formato anterior. Los artefactos ya firmados conservan sus bytes.

## Verificación

- Tres pruebas de integración aprobadas, sin errores ni fallos: bordes visibles en el PNG real de la vista previa, previsualización extensa sin persistencia y T2 extenso con evidencias, firmas y conservación del Plan aprobado.
- La prueba de regresión comprueba cinco niveles de líneas horizontales visibles en el encabezado rasterizado, en lugar de verificar únicamente las propiedades XML.
- Inspección visual del PNG T1 generado por PDFBox (el renderizador de la aplicación) y de la página T2 renderizada con Poppler: contorno superior/lateral/inferior y divisiones visibles, sin líneas atravesando el logo.
- `git diff --check` aprobado. Pruebas en utaped-tests desechable, con limpieza posterior; sin resetear datos manuales.
- Backend local reconstruido y disponible mediante `scripts/start-development.ps1`, conservando datos y credenciales. No se modificó código frontend ni se atribuyen sus comprobaciones a esta corrección.

Evidencias locales ignoradas: `backend/target/header-borders-tests.log`, `backend/target/document-qa/header-borders-preview.png` y `backend/target/document-qa/header-borders-t2-03.png`. Los avisos conocidos de fuentes del conversor no impidieron generar ni verificar los documentos.

## Entrega y uso

No se requieren variables de entorno ni migraciones nuevas. Tras actualizar el backend, actualizar la vista simultánea o generar una previsualización nueva en un borrador. Una previsualización guardada anteriormente puede conservar el diseño previo; no se reescriben documentos firmados.

Los cambios de interfaz y la transcripción que ya estaban en staging pertenecen al propietario y se conservan sin incluirlos en este commit.
