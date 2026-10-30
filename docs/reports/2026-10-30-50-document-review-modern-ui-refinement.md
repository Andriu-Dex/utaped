# Rediseño visual y modernización de la Revisión Documental

Fecha: 2026-10-30

## Contexto y objetivos

La pantalla de **Revisión documental** (`ReviewWorkspace.tsx`) es el núcleo de trabajo para los revisores y autoridades institucionales en UTAPED. Siguiendo el estándar del **UTAPED Modern Design System** aplicado previamente al Panel General Administrativo, se requirió elevar la jerarquía visual, reducir la sobrecarga vertical de elementos secundarios y transformar el visor del documento y el panel de decisiones en una experiencia de usuario moderna, legible y consistente tanto en escritorio como en tablet y modo oscuro.

## Mejoras implementadas

1. **Bandeja de revisión con tarjetas KPI institucionales (`.review-inbox-totals`)**:
   - Se rediseñaron las métricas resumen (`Etapas pendientes`, `Intervenciones completadas`, `Documentos devueltos`) con tarjetas KPI modernas, iconos temáticos con fondo diferenciado (`tone-success`, `tone-amber`), tipografía de alto impacto y animación sutil de elevación en hover.
   - Se ajustó el espaciado superior (`.review-inbox-header`) para evitar apiñamiento contra la barra de navegación institucional.

2. **Barra de contexto documental unificada (`.review-summary`)**:
   - Se reemplazó la fila flotante desarticulada por una barra de herramientas institucional compacta con elevación y borde sutil.
   - Incorpora chips de metadatos enriquecidos: estado semántico con punto de pulso (`En revisión · Ronda X`), contador de páginas con icono (`6 páginas`), sello de inmutabilidad (`PDF conservado`) y botón contextual de descarga de versión aprobada.

3. **Visor de lectura y mesa técnica de drafting (`.document-reading-desk`)**:
   - Se creó un lienzo técnico de lectura con fondo sutil y textura de mesa de trabajo que enmarca la hoja A4 del PDF firmado con sombra multi-capa realista y bordes redondeados.
   - La indicación de interacción (*"Arrastra sobre la página para asociar un resaltado..."*) se transformó en un callout estilizado con icono de búsqueda y un botón de acción rápida para recargar la imagen sin tener que desplazarse al pie del documento.
   - Se preservó estrictamente la posición `sticky` con elevación del navegador de páginas (`DocumentPageNavigator`), permitiendo navegar y saltar entre observaciones sin perder el contexto.

4. **Stepper de la ruta de validación (`.review-stages-card`)**:
   - Se sustituyeron los círculos concéntricos por baldosas de etapas redondeadas (*squircles*) numeradas (`.stage-step-tile`):
     - Checkmark en verde institucional para etapas completadas (`stage-completed`).
     - Baldosa azul institucional UTA (`#0b4f8a`) con número visible en negrita para la etapa activa (`TU TURNO`).
     - Baldosa neutra con número en gris suave para etapas pendientes (`PENDIENTE`).
   - Cada paso incluye el rol, el nombre completo del participante, su badge de estado semántico y la indicación de requerimiento de firma electrónica (`Firma electrónica requerida`).

5. **Reestructuración de la barra lateral en tres tarjetas independientes**:
   - En lugar de anidar formularios y herramientas dentro de un único bloque, el panel derecho se organiza en tres tarjetas limpias:
     - **Tarjeta 1 (`Etapas del flujo`)**: Stepper visual con las baldosas numeradas y el estado de cada participante.
     - **Tarjeta 2 (`Observaciones de la ronda`)**: Botón principal `+ Agregar observación`, listado de observaciones técnicas y formulario de edición integrado directamente dentro de la tarjeta, eliminando los bordes y rellenos redundantes (*box-in-a-box*).
     - **Tarjeta 3 (`Tu decisión`)**: Mensaje preventivo sobre observaciones de ronda, formulario de firma digital y botones directos `Completar etapa` y `Devolver para corrección`, eliminando completamente la barra gris intrusiva (`HERRAMIENTAS DEL DOCUMENTO:`).

6. **Tarjetas de observaciones de la ronda (`.review-observations`)**:
   - Tarjetas tipo comentario de revisión técnica con avatar de iniciales del revisor, indicador de alcance (`📍 Zona · Pág. X`, `📄 Pág. X` o `🌐 General`), badge de edición y acciones de editar y eliminar accesibles con confirmación protegida.
   - Enlace directo con icono de ojo para saltar inmediatamente a la página del documento vinculada a la observación.

7. **Trazabilidad entre rondas (`RoundComparison.tsx` y `work-assistance.css`)**:
   - Estilizado de la tarjeta de trazabilidad para que permanezca compacta y esbelta cuando está cerrada, evitando empujar el visor innecesariamente hacia abajo.
   - Columnas comparativas en dos paneles limpios (`Antes · Ronda X` vs `Ahora · Ronda Y`) con colores suaves de contraste tanto en modo claro como en modo oscuro (eliminando los fondos blancos en tema oscuro).

8. **Soporte completo de modo oscuro y tablet**:
   - Paleta completa de modo oscuro para todos los nuevos componentes (`:root[data-theme='dark']`): contrastes cuidados, mesa de lectura oscura con hoja PDF iluminada, chips legibles y acentos en azul y verde institucional.
   - En tabletas (768px a 1150px), el visor de lectura ocupa el ancho completo y el panel lateral se organiza fluidamente en dos columnas adaptativas.

## Verificación realizada

1. **Compilación y tipado estricto**:
   - `npm run build`: 123 módulos transformados sin errores TypeScript.
2. **Linter**:
   - `npm run lint`: 0 errores y 0 advertencias en oxlint (124 archivos).
3. **Pruebas de aceptación automatizadas en contenedor aislado (`scripts/test-acceptance.ps1`)**:
   - `tests/sample/review-signing-draft.spec.ts`: 3 pruebas aprobadas (100%), cubriendo:
     - Preparación protegida de certificados y navegación sticky.
     - CRUD de observaciones, etiquetas normalizadas de etapa y estados editados.
     - Preservación de página asociada, conversión a general y cancelación con advertencia.
   - `tests/sample/contextual-administration.spec.ts`: Aprobada (paginación y filtros de la bandeja en escritorio y tablet oscura).
   - `tests/sample/integral-audit.spec.ts`: Aprobada (tolerancia a fallos de detalle y reintento).
   - `tests/sample/work-assistance.spec.ts`: Aprobada (comparación explícita de contenido conservado entre rondas).
   - `tests/sample/product-circuit.spec.ts`: Circuito multi-rol integral aprobado (59.6s).
4. **Inspección visual mediante capturas de pantalla**:
   - Se validaron las capturas generadas en escritorio (`screen-review-workspace-desktop.png`), tablet en modo oscuro (`screen-review-workspace-tablet-dark.png`), bandeja de entrada (`screen-review-inbox-desktop.png`) y comparación entre rondas (`screen-round-comparison-desktop.png`).
