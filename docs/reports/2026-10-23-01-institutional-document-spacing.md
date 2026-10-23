# Espaciado institucional de T1 y T2

Fecha: 2026-10-23. Rama: `fix/institutional-document-spacing`.

## Diagnóstico y corrección

La composición del encabezado eliminaba las alturas de las filas, reducía el escudo de aproximadamente 63 a 40 puntos y repartía las columnas de una manera distinta de la plantilla. Esto comprimía visualmente el encabezado y acercaba la primera sección al bloque superior.

Se mantienen las proporciones de columnas de la referencia al ancho A4, el tamaño original del escudo, alturas mínimas de filas que permiten crecer cuando el texto requiere más líneas y una separación explícita antes del contenido. Se eliminan párrafos vacíos del logo y márgenes verticales añadidos que alteraban las medidas. Se conservan los bordes completos y el ajuste del texto institucional.

El cuerpo conserva las propiedades de la plantilla oficial: texto Helvetica de 10 puntos, justificación y espaciado sencillo; títulos de 14 puntos y sus separaciones. No se cambia la redacción del usuario ni se agregan párrafos artificiales: la referencia contiene varios párrafos y el ejemplo mostrado contiene uno solo, por lo que su extensión seguirá siendo diferente.

La corrección se comparte entre T1 y T2. El motor pasa a `engine8` para las nuevas composiciones. No se modifican las plantillas originales ni los bytes de artefactos existentes o firmados, ni se altera el formato independiente del Informe de ejecución.

## Verificación

- Primera tanda: 12 pruebas de `DocumentIntegrationTests` y una de T2 extenso, sin fallos ni errores.
- Tras el último ajuste: cuatro pruebas aprobadas (altura/separación/bordes del PNG real, composición A4 y conservación histórica, T1 extenso con índice y firmas dinámicos, y T2 extenso que conserva el Plan base).
- Inspección visual del PNG de previsualización T1 y de páginas de T1/T2 extensos renderizadas con Poppler: comprobación de escala, espacio antes del contenido y ausencia de superposición entre encabezado y narrativa. No se afirma identidad píxel a píxel con Word ni aceptación institucional final.
- `git diff --check` aprobado. No se modificó frontend y no se atribuyen pruebas de interfaz a esta entrega.
- Entorno aislado `utaped-tests` eliminado al terminar, sin afectar el entorno manual. Backend de `utaped-sample` reconstruido conservando datos y URL local de Vite; `/api/auth/csrf` responde HTTP 200.

Evidencias locales ignoradas en `backend/target`: `spacing-regression.log`, `spacing-final.log`, `spacing-runtime.log` y `document-qa/header-borders-preview.png`, `spacing-t1-long.png`, `spacing-t2-long.png`. El conversor conserva avisos de fuentes/FO ya presentes; no impidieron generar los PDF ni superar las verificaciones.

## Uso

No requiere variables de entorno ni migraciones. Tras actualizar el backend, pulsa **Actualizar vista** en el borrador. Para una previsualización preparada previamente, genera una nueva; los documentos ya firmados conservan su presentación histórica.

Los cuatro archivos de interfaz que ya estaban en staged se conservan sin modificaciones y quedan fuera del commit de esta corrección. Sin push ni pull request.
