# 2026-11-05 — Carga Inmediata y Transición Suave en la Previsualización Documental

## Objetivo
Optimizar los tiempos de carga y la experiencia de usuario en el visor de previsualización documental simultánea (`PlanPreview.tsx`, `plan-preview.css`), eliminando el retraso artificial de espera (*debounce*) en cargas iniciales y cambios de paso limpios (reduciéndolo a 0 ms), implementando la retención de vista anterior (*Stale-While-Revalidate*) con transición suave de opacidad (evitando parpadeos o pantallas vacías) e incorporando una caché en memoria para reaperturas instantáneas de documentos.

## Diagnóstico previo
1. **Retraso artificial innecesario de 1.4 segundos:**
   - La lógica de temporizado en `PlanPreview.tsx` aplicaba incondicionalmente un retraso de `1400 ms` siempre que la página solicitada era menor a cero (`requestedPage < 0`).
   - Si bien este retraso es necesario como *debounce* cuando el usuario escribe texto en los campos de justificación u objetivo para no saturar al servidor, se ejecutaba erróneamente en la **primera carga al abrir el documento** o al **cambiar de paso**, obligando al usuario a esperar 1.4 segundos ociosos antes de iniciar la solicitud al backend.
2. **Parpadeo con pantalla vacía (*empty state* abrupto):**
   - Durante esos 1.4 segundos de espera más el procesamiento de generación del PDF, el estado `result` se encontraba vacío (`null`), haciendo que el visor desmontara la página del documento y presentara una caja gris estática con el texto *"Preparando su documento..."*.
   - Al navegar entre los pasos 1, 2, 3 o 4, este vacío visual generaba una sensación de lentitud o recarga desde cero, a pesar de que el backend completaba el renderizado en ~200 ms.

## Cambios realizados
- `frontend/src/planning/PlanPreview.tsx`:
  - **Temporizador adaptativo inteligente (0 ms vs 900 ms):**
    - Se diferencian las condiciones de edición activa frente a navegación y carga inicial:
      - Primera carga del documento (`!loadedPlanId.current || isFirstLoadForPlan`): **0 ms** (inmediata).
      - Navegación entre pasos o pestañas sin edición de texto (`!textDirty`): **0 ms** (inmediata).
      - Cambio explícito de página con el navegador (`requestedPage >= 0`): **0 ms** (inmediata).
      - Escritura activa en campos de texto (`textDirty`): **900 ms** de *debounce* reactivo para no sobrecargar el compilador de PDF.
  - **Transición suave (*Stale-While-Revalidate*):**
    - Se amplió la condición de obsolescencia visual (`stale`) para incluir estados donde la petición está en curso (`busy`), esperando la página de una nueva sección (`awaitingSection`) o decodificando la imagen PNG (`imagePending`).
    - Durante estos estados transitorios, el visor **conserva visible la última página generada** aplicando una atenuación elegante (`opacity: 0.65`) y el badge superior *"Actualizando vista…"*.
    - Al recibirse la nueva página, la transición visual ocurre sin parpadeos ni desplazamiento de layout, retornando suavemente a opacidad plena.
  - **Caché en memoria de vistas recientes (`draftViewCache`):**
    - Se incorporó un almacenamiento LRU en memoria de sesión que conserva la última vista previa de los borradores editados recientemente.
    - Al volver a ingresar a un documento previamente visitado, la página se renderiza **en el fotograma inicial (0 ms)** mientras se valida en segundo plano si hubo cambios, erradicando por completo el estado vacío.
  - **Indicador de carga refinado en estado inicial:**
    - Se rediseñó el contenedor de documento vacío (`.plan-preview-empty.is-loading`), añadiendo un micro-indicador giratorio (`.preview-spinner`) sobre el icono del documento para comunicar actividad fluida durante la compilación en frío inicial.
- `frontend/src/planning/plan-preview.css`:
  - Incorporada la transición fluida `transition: opacity .2s ease-in-out;` en `.document-reading-desk`.
  - Estilizado el estado `.plan-preview-empty.is-loading` con fondo tonal suave institucional y borde sutil.
  - Creada la animación `@keyframes preview-spin` para el indicador de carga inicial.
  - Implementado soporte visual completo para modo oscuro (`:root[data-theme='dark']`) en bordes, fondos y contrastes del spinner.

## Validaciones ejecutadas
- **Linter Frontend (`oxlint`):** 0 errores, 0 advertencias en 133 archivos analizados con 116 reglas. Se cuidó el acceso seguro a referencias (`useRef`) fuera del cuerpo de renderizado.
- **Compilación TypeScript / Vite:** `tsc -b && vite build` completado exitosamente sin discrepancias de tipos.
- **Sincronización en Docker:** Paquetes actualizados en caliente en el contenedor en ejecución `utaped-frontend-1`.
- **Verificación automatizada con Playwright:**
  - Apertura inicial de documento: carga de espacio de trabajo en 86 ms.
  - Cambio entre secciones (Paso 1 a Paso 2): comprobado que el visor de lectura permanece visible sin pantalla en blanco (`isDeskStillThere: true`) y actualiza la página de Justificación en **199 ms**.
  - Reapertura de documento con caché en memoria: documento y previsualización listos y visibles inmediatamente en **61 ms**.
  - Verificación en modo oscuro (`preview_step2_dark.png`): renderizado de página y controles con contraste de diseño UTAPED.
