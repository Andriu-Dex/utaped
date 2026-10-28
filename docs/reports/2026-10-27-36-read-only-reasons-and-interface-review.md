# Motivos de solo lectura y revisión de interfaz

Fecha: 27 de octubre de 2026.

## Hallazgo y corrección

La matriz asociaba cualquier `editable=false` con «ventana de elaboración cerrada». La comprobación del Plan señalado confirmó estado `IN_REVIEW`, período activo y fin de elaboración el 31 de diciembre de 2026: el bloqueo correspondía a la revisión, no al plazo.

Documento, matriz y anexos ahora incluyen `readOnlyReason`, derivado de la misma condición del servidor que determina la edición. Distingue archivo, cierre de período, responsable principal, revisión, aprobación, firma y fechas efectivas con extensiones. Se mantiene la protección de escritura; no se reabre ningún documento ni se altera su estado o PDF firmado. [Contrato](../api/document-editability.md).

Información general, contenido, matriz, anexos e informes usan un aviso informativo persistente junto al contenido, sin una advertencia flotante que atribuya una fecha incorrecta. Los motivos desconocidos usan un texto general. Una devolución no promete edición fuera del plazo autorizado.

También se corrigió la etiqueta del visor: al consultar un PDF conservado, no se anuncia la sección del formulario como si necesariamente fuera la página visible. No se reconstruye el artefacto firmado.

## Cambios visuales revisados

Se integran las mejoras previas del propietario: botones de regreso con icono, título y nombre accesible en consulta, revisión, historial, matriz, anexos, preparación y ejecución; presentación del menú de seguimiento y estilos compartidos. Se conservaron los manejadores de navegación, confirmaciones y protecciones de cambios pendientes.

Se ajustó el orden de capas de cabecera, menú, pasos y barra superior. La cabecera ya no cubre la navegación general al desplazarse. Una regresión de interacción comprueba que la barra superior recibe la interacción en su área visible.

## Comprobaciones

- Frontend: lint, TypeScript y build correctos; permanece el aviso de Vite sobre tamaño del bundle.
- Aceptación general: 29 de 30 casos correctos. El caso de importación agotó cinco segundos esperando la imagen de captcha; su repetición aislada pasó sin modificar autenticación ni importación.
- Regresión final del menú y del circuito T1/T2: dos casos correctos, incluidos teclado, regreso al paso de origen, capas al desplazar, firma, devolución, corrección, aprobación, evidencias y conservación de bytes. Se verificó el aviso de revisión en general, matriz y anexos y la etiqueta del PDF conservado.
- Se revisaron capturas de escritorio y tablet oscura del menú y la captura del aviso en revisión. Los escenarios se ejecutaron en entornos desechables, separados del entorno manual.
- Se añadió una regresión backend que distingue revisión/aprobación de fechas, compara documento/matriz/anexos, verifica el rechazo de escrituras y comprueba ventanas futuras, vencidas y archivo.
- `scripts/test-backend.ps1`: proceso terminado con código 0. Los reportes de esta ejecución suman 221 pruebas, sin fallos, errores ni casos omitidos.

El alcance no constituye una nueva aceptación institucional de reglas pendientes. Los registros de las pruebas quedan localmente en `backend/target/2026-10-27-read-only-*.log`; no forman parte del repositorio.
