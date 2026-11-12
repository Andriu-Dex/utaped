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

## Pendiente

- Investigar y corregir las 6 pruebas de aceptación que ya fallaban en `develop` antes de publicar en `main`.
