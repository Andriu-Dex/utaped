# Un solo visor antes y después de firmar

El bloque de firma del elaborador duplicaba el documento: conservaba el visor de preparación y añadía otro visor con el PDF firmado. Se elimina el segundo visor; el existente cambia a las páginas del artefacto firmado real, sin reconstruir el PDF ni modificar firmas.

En T1 el visor persistente muestra «Documento firmado», abre la página de firma a partir de la metadata y conserva navegación, referencias, ampliación y reintento. El bloque inferior conserva el resultado, titulares, fechas, usuario ejecutor y estado; ofrece «Ver documento firmado» (vuelve al mismo visor) y descarga privada. T2/T3 reutilizan su visor existente con la misma fuente firmada.

El estado de la vista firmada se asocia al identificador explícito del artefacto seleccionado. Cambiar de preparación no debe mezclar firmas; al recargar se consulta de nuevo el estado del servidor. Durante la recarga se conserva la última vista disponible. Una vez iniciada la revisión, se oculta la confirmación previa a firmar y no se muestran bloqueos de nueva firma a quien ya firmó.

Verificación manual sobre el documento ya firmado del entorno local: un único visor, página de firma y enlace de regreso operativos, sin aplicar nuevas firmas. Se preservan los cambios previamente staged del propietario. Sin commit.

Validación automática: TypeScript, lint y build correctos; 24 pruebas de aceptación correctas, incluido el circuito real T1/T2/T3 y revisión. La regresión verifica fuente firmada, ausencia de imágenes duplicadas, ampliación y persistencia tras recargar. Evidencia: `backend/target/2026-10-27-single-signed-preview-final.log`; captura manual: `backend/target/2026-10-27-single-signed-preview.png`. Los recursos de aceptación fueron limpiados por el lanzador.
