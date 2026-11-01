# Seguimiento documental separado de elaboración

Fecha: 27 de octubre de 2026.

El Plan T1 agrupa las consultas en «Seguimiento del documento», en su cabecera y fuera del formulario y los cinco pasos. Contiene rondas y observaciones, historial del documento y ejecución/evidencias exclusivamente para Planes aprobados. Crear o abrir Informe T2 permanece como acción independiente para quien tenga permiso.

La navegación conserva el paso desde el que se abrió el seguimiento. Los accesos se bloquean durante operaciones o mientras existan cambios sin guardar, con explicación visible. Se conservan las protecciones de salida de las pantallas de ejecución y revisión y las autorizaciones del servidor. No se modifican el motor documental, artefactos firmados ni contratos de firma.

El desplegable permite Tab, apertura con flecha abajo, cierre con Escape y recuperación del foco, además de cierre al pulsar o enfocar fuera. Su presentación se comprobó en escritorio y tablet con tema oscuro.

## Verificación ejecutada

- `npm --prefix frontend run lint`, `typecheck` y `build`: correctos. Vite mantiene su aviso de tamaño del bundle.
- `scripts/test-acceptance.ps1 -TestPattern 'plan-follow-up|product-circuit'`: dos pruebas correctas en la ejecución final. Comprueban teclado, cierre, separación del editor, ausencia de ejecución en borradores, protección de cambios, regreso al paso de contenido y circuito completo de aprobación, firmas secuenciales, evidencias, T2 y versión formal.
- La primera ejecución detectó que la descripción ampliaba el nombre accesible de los botones; corregido con etiquetas explícitas y verificado en la repetición final.
- Capturas locales revisadas: `frontend/test-results/plan-follow-up-desktop.png` y `plan-follow-up-tablet-dark.png`.
- `git diff --check`: correcto. El entorno de pruebas fue desechable; no se utilizó la base manual.

Los nueve archivos visuales que ya estaban staged al comenzar se conservan separados de esta entrega. No se publicó la rama ni se cambió la configuración institucional pendiente.
