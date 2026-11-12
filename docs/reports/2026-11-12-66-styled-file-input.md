# Selector de archivos institucional

Fecha: 2026-11-12. Rama: `develop`.

## Objetivo

Los campos de archivo mostraban el control nativo del navegador («Choose File / No file chosen»), sin estilo y en el idioma del navegador. Se detectó durante la grabación del video comercial, en la verificación de la copia de custodia.

## Cambios

- Nuevo componente compartido `FileInput` (`frontend/src/shared/FileInput.tsx`) con el botón «Seleccionar archivo» y el nombre del archivo elegido o «Ningún archivo seleccionado», con soporte de modo oscuro.
- El campo nativo sigue en el formulario: visualmente oculto, pero enfocable con teclado y compatible con `name`, `required`, `accept`, etiquetas y pruebas.
- Los textos visibles se generan por CSS. Así no forman parte del texto de la etiqueta, que conserva su nombre accesible exacto.
- El nombre mostrado se limpia al elegir otro archivo, al restablecer el formulario y cuando el código vacía el campo (anexos).
- Se reemplazaron los 9 campos de archivo: estructuras documentales, custodia del período, cargos (CSV), importación de usuarios, anexos, firma del elaborador, evidencias, constancia de Quipux y firma de revisión.

## Validaciones ejecutadas

- Frontend: `npm run lint`, `tsc -b` y `npm run build` sin errores.
- `scripts/test-acceptance.ps1` con el cambio: 59 aprobadas y 6 fallidas. La misma ejecución sin el cambio (línea base) da exactamente las mismas 6 fallas, que ya existían en `develop`: `confirmations`, `document-creation`, `draft-preview`, `management-reporting` (pendientes de Inicio), `product-circuit` (guardado del Informe final) y `work-assistance` (comparación de rondas). Todas las pruebas que suben archivos aprueban.
- Revisión visual del componente en tema claro y oscuro, en una página aislada con el CSS compilado.

## Pruebas de aceptación que ya fallaban

Se investigaron las 6 pruebas que fallaban en `develop` antes de este cambio. Ninguna era un fallo del producto; todas quedaron desactualizadas por decisiones del 31/10 y el 05/11, o dependían de datos que la ejecución por defecto no carga.

| Prueba | Causa | Tratamiento |
|---|---|---|
| `document-creation` | Desde el 31/10, «Informe final» se deshabilita si el responsable no tiene un Plan aprobado vigente | Verifica la opción según `canCreateDerived` y mantiene que un borrador nunca es base de un Informe |
| `confirmations` | Usaba «Informe final» solo para provocar la confirmación anidada | La provoca con «Cancelar» sobre un borrador modificado |
| `draft-preview` | Desde el 31/10 el Plan se guarda solo; ya no hay contenido «sin guardar» | Comprueba la sincronización y que no se crea artefacto; conserva navegación, tableta y reintento |
| `management-reporting` | Desde el 05/11, sin pendientes la bandeja se contrae en «Al día» | Abre «Ver filtros» antes de buscar «Todos» |
| `product-circuit` | El autoguardado del Informe deja «Guardar Informe» sin aviso que mostrar | Espera el estado «Contenido guardado» |
| `work-assistance` (2) | Necesitan documentos preparados | Se omiten con motivo explícito si faltan; con `-PreparedDocuments` aprueban las 4 |

Además, `institutional-reference` falló una vez porque Escape cerraba la lista de comisiones en lugar del diálogo. La prueba ahora espera a que la lista se cierre.

Validación final: `scripts/test-acceptance.ps1` con 63 aprobadas y 2 omitidas por falta de documentos preparados; `scripts/test-acceptance.ps1 -PreparedDocuments -TestPattern work-assistance` con 4 de 4 aprobadas.
