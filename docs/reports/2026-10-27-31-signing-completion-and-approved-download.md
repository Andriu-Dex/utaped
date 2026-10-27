# Revisión dividida, finalización y descarga aprobada

Decisión del propietario: revisión y firma en el paso 5 con el mismo espacio dividido de los otros pasos; eliminar «Ver documento firmado», descargar únicamente el PDF aprobado y permitir volver a Documentos después de registrar la firma. Se autoriza cerrar todos los cambios pendientes mediante commits locales.

## Flujo

- T1 mantiene cinco pasos. En el último, las acciones quedan a la izquierda y el visor único a la derecha; conserva ajuste de ancho, ampliación y modos de presentación para tablet.
- La confirmación previa y los controles de certificado mantienen las protecciones de formulario pendiente y operación en curso.
- Tras firmar, «Finalizar y volver a Documentos» cierra el recorrido sin modificar el estado. Si faltan elaboradores, se mantiene pendiente de otras firmas; la última firma inicia la revisión existente.
- Se elimina el enlace redundante al visor y la descarga preliminar. La ruta antigua devuelve 403 aun para documentos anteriores sin ámbito; la vista de páginas continúa autorizada.
- «Descargar PDF aprobado» solicita el archivo de la ronda aprobada, con las firmas de revisión. Los errores quedan en Toast. El visor aprobado usa ese mismo archivo únicamente cuando la preparación seleccionada corresponde a dicha ronda; no mezcla páginas de artefactos históricos.

## Verificación

Se revisan también los cambios previamente staged: certificados aportados y metadata pública, migración V36, cinco pasos, modal de confirmación y visor único. No se alteran los documentos oficiales ni los bytes firmados.

Verificado: frontend lint/TypeScript/build; 214 pruebas backend sin fallos, errores ni omisiones; 24 pruebas de aceptación correctas sobre la versión final. Se comprobó descarga denegada antes de aprobación, comparación SHA-256 del archivo aprobado descargado, retorno a Documentos sin cambiar estado, devolución/ronda nueva, fuentes de página y diseño PC/tablet. Se revisaron las capturas del paso dividido.

Una ejecución concurrente ajena al recorrido manual reinició el entorno temporal durante una repetición. Se incorporó exclusión mutua al lanzador de aceptación antes de tocar variables/credenciales/contenedores: la siguiente ejecución espera, conservando la limpieza y liberación en finally. Comprobados bloqueo concurrente y ejecución posterior en cola. La ejecución final terminó correctamente y limpió sus recursos. Las pruebas no escribieron en la base manual; el entorno manual se actualizó con sus datos conservados.

Evidencias: `backend/target/2026-10-27-signing-completion-backend.log`, `backend/target/2026-10-27-signing-completion-release.log` y `frontend/test-results/final-review-desktop-dark.png`. Cierre mediante commits locales autorizados, sin push.
