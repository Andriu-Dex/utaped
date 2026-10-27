# Previsualización persistente del Plan T1

## Decisión del propietario

Cerrar los ajustes previos de portada en commit local (60889c7) y conservar sin commit la nueva implementación para revisión. El visor del paso 5 es la referencia visual; se conserva su navegador, referencias y escritorio documental. No se publica la rama.

## Comportamiento

- Un único panel permanece montado al cambiar entre los seis pasos del T1. El formulario cambia a la izquierda y el documento aparece a la derecha en PC.
- Información general dirige a portada; contenido a justificación/objetivo; matriz a su página horizontal; anexos a su sección. Sin anexos se muestra y explica la sección de firmas que seguirá.
- Título/textos se previsualizan sin guardar, con espera de 1,4 segundos y solicitudes serializadas. La matriz, los autores y los anexos reflejan cambios guardados. Los cambios pendientes se identifican y la última vista permanece durante la actualización.
- El paso 5 se denomina Revisión final y conserva bloqueos, correcciones, estructuras adicionales y preparación automática. El paso 6 abre el artefacto preparado en su ubicación real de firmas. No se firma la vista transitoria ni se regeneran artefactos firmados.
- Se puede explorar otras páginas, usar flechas en el visor seleccionado, cambiar ancho, ampliar el documento o alternar formulario/documento. En tablet se alternan las vistas para conservar legibilidad; revisión/firma muestran también el documento con la distribución inicial.
- La ampliación tiene controles de página, contención y restauración de foco, y se cierra con Escape. Sus controles se montan al abrirla para evitar etiquetas duplicadas en diálogos cerrados.
- Navegación deshabilitada durante carga de imagen, errores recuperables y contraste explícito en el visor oscuro. Una generación transitoria que está terminando permite reintento acotado al preparar el artefacto final.

## Alcance técnico

Se reemplaza el panel antiguo exclusivo del paso 2. Las páginas de sección proceden de los destinos del PDF o de las páginas de anexo realmente compuestas, con autorización y control de versión existentes. No se añaden tablas, almacenamiento ni servicios externos. El borrador no crea artefactos de firma. T2/T3 conservan sus propios flujos y se incluyen en la regresión de aceptación.

## Verificación

- 33 pruebas backend de documentos y estructuras aprobadas; repetidas las 16 de documentos tras el ajuste final de anexos. Incluyen secciones de borradores incompletos, matriz horizontal, foco de anexos, aislamiento, falta de escritura de artefactos y conservación de bytes.
- 24 pruebas de aceptación aprobadas en el entorno descartable utaped-acceptance: panel estable entre pasos, cambios sin guardar, teclado, ampliación, tablet, recuperación de errores, artefactos de solo lectura y circuito T1/devolución/firmas/evidencias/T2/versiones.
- lint, TypeScript y build aprobados. Continúa el aviso conocido de bundle superior a 500 kB.
- Revisión de capturas de escritorio/tablet y verificación de contraste del visor oscuro tras la transición de tema. Colores computados de controles: fondo rgb(23,55,82), texto rgb(217,239,255).
- utaped-sample actualizado, conservando datos. Frontend y backend locales disponibles. Los entornos temporales de pruebas se limpiaron.

Evidencia de ejecución ignorada en Git: backend/target/2026-10-26-persistent-preview-backend.log, backend/target/2026-10-26-persistent-preview-backend-final.log y backend/target/2026-10-26-persistent-preview-acceptance-verified.log.
