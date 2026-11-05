# 2026-11-05 — Rediseño y Optimización Visual del Modal «Crear documento»

## Objetivo
Optimizar la ergonomía, jerarquía y presentación visual del diálogo modal **«Crear documento»** (`DocumentCreation.tsx`, `document-creation.css`), eliminando la duplicidad visual de controles apilados (buscador + menú desplegable nativo) mediante un **Combobox interactivo unificado**, balanceando las columnas del formulario, erradicando el scrollbar vertical interno en pantallas de escritorio y ratificando que el usuario no elige carrera ni período, sino que estos provienen configurados por la administración institucional.

## Diagnóstico previo
1. **Duplicidad y confusión de controles de entrada:**
   - La presencia de un campo de texto de búsqueda posicionado inmediatamente sobre un `<select>` generaba una apariencia de "doble formulario", confundiendo al usuario sobre qué campo debía llenar.
2. **Asimetría de altura entre columnas:**
   - La columna izquierda acumulaba cuatro bloques verticales (etiqueta, campo de búsqueda, selector nativo y tarjeta de metadatos), mientras la columna derecha solo contenía el campo de título, generando un gran vacío blanco improductivo.
3. **Desbordamiento vertical y scrollbar interno:**
   - La suma de paddings de la tarjeta anidada (`.card`) dentro del contenedor modal (`.action-form-dialog`), junto con los márgenes de las tarjetas de tipo documental, superaba la altura disponible en resoluciones de 900px de alto, provocando un scrollbar vertical y recortando el encabezado del diálogo.
4. **Desalineación del badge de conteo:**
   - La etiqueta «23 disponibles» rompía la línea y se centraba entre la etiqueta y el input.

## Cambios realizados
- `frontend/src/shared/Icon.tsx`:
  - Incorporado el icono SVG institucional `chevronDown` para selectores y menús desplegables.
- `frontend/src/planning/DocumentCreation.tsx`:
  - Reemplazado el conjunto buscador + select nativo por un **Combobox interactivo unificado** accesible:
    - Campo de entrada único con icono de búsqueda y control de apertura/cierre o limpieza (`✕`).
    - Menú flotante absoluto (`.scope-combobox-dropdown`) con lista desplazable, resaltado por teclado (`ArrowDown`, `ArrowUp`, `Enter`, `Escape`) y cierre al hacer clic fuera (`useRef` + listener global).
    - Cada elemento del listado resalta el nombre de la comisión en negrita y la carrera/período en tipografía secundaria legible.
  - Reorganizada la cabecera de las columnas mediante `.scope-col-label`, alineando horizontalmente el título «Comisión o unidad asignada» con el badge «23 disponibles» a la derecha, y nivelando la altura con la columna del título del Plan.
  - Formato horizontal compacto para los metadatos fijos (`.scope-metadata-badges-row`), mostrando los badges de carrera asignada y período vigente lado a lado con icono semántico y nota aclaratoria.
- `frontend/src/shared/action-form.css`:
  - Asignada una altura mínima confortable al diálogo modal (`min-height: min(660px, calc(100dvh - 64px))`), otorgando amplitud vertical a la ventana para que las opciones, el combobox y el menú desplegable respiren con soltura sin provocar cortes ni barras de scroll en pantallas de escritorio.
- `frontend/src/planning/document-creation.css`:
  - Anulados los bordes, paddings y sombras redundantes de `.card` cuando se encuentra contenido dentro de `.action-form-dialog`.
  - Ampliadas las dimensiones verticales de las tarjetas de tipo documental (T1/T2/T3) con padding de 14px 16px e iconos de 40px, mejorando su legibilidad y presencia institucional.
  - Ajustados los campos del formulario (`height: 46px`) y el menú desplegable flotante (`max-height: 250px`), permitiendo visualizar de 4 a 5 comisiones completas con holgura.
  - Estilos dedicados para el Combobox (borde activo azul, elevación en dropdown, hover sutil) y compatibilidad total con tema oscuro (`:root[data-theme='dark']`).

## Validaciones ejecutadas
- **Linter Frontend (`oxlint`):** 0 errores, 0 advertencias en 133 archivos analizados con 116 reglas.
- **Compilación TypeScript / Vite:** `tsc -b && vite build` completado exitosamente sin discrepancias de tipos.
- **Sincronización en Docker:** Paquetes actualizados en caliente en el contenedor en ejecución `utaped-frontend-1`.
- **Verificación automatizada con Playwright:**
  - Apertura del modal en tema claro (`modal_create_combobox_initial_light.png`): sin scrollbar vertical, distribución balanceada y etiquetas niveladas.
  - Despliegue interactivo del Combobox (`modal_create_combobox_dropdown_open.png`): listado flotante con opciones estilizadas.
  - Filtrado predictivo al escribir (`modal_create_combobox_filtered.png`): filtrado en tiempo real sin recarga.
  - Selección de comisión (`modal_create_combobox_selected_light.png`): autocompletado del título, despliegue de badges de carrera/período y habilitación del botón «Crear borrador».
  - Verificación en modo oscuro (`modal_create_combobox_selected_dark.png`): contraste adecuado en superficies, textos y bordes.

