# Navegación persistente del Plan y acciones de anexos — 23/10/2026

## Cambios

Se aplica la revisión de `instruction.md` autorizada por el propietario, manteniendo la entrega sin commit para su revisión.

- Los seis pasos comparten la cabecera del Plan y el navegador, con `aria-current="step"`, número del paso y referencia al documento. El navegador permanece visible durante el desplazamiento; su posición se ajusta a la altura real de la cabecera global.
- La cabecera completa no ocupa de forma fija la pantalla de la tablet: el navegador mantiene una referencia compacta al documento. Se retiran los regresos «Volver al Plan» y los indicadores duplicados dentro del asistente. Historial, revisión y ejecución conservan sus propios regresos.
- Un componente común presenta Anterior secundario y Siguiente azul institucional, también en modo oscuro, con controles de al menos 44 px. La matriz incorpora Anterior: Contenido. Guardar conserva su acción independiente.
- Los pasos Información general y Contenido comparten su borrador: alternar entre ambos conserva lo escrito. Salir hacia otro editor requiere confirmación cuando hay cambios locales. La configuración de autores/archivo se protege también antes de desmontar sus formularios.
- La matriz reutiliza sus comprobaciones al avanzar desde el pie o el navegador. No se pasa a Anexos con actividades vacías/incompletas. Las comprobaciones de preparación y firma permanecen en servidor; la previsualización puede mostrar pendientes sin generar un artefacto inválido.
- La cabecera se sincroniza con el estado recibido al preparar o firmar, evitando mostrar Borrador cuando el documento ya pasó a revisión. Un error de carga permite salir hacia otro paso.
- La navegación se bloquea durante operaciones activas. La preparación de firma informa solo indicadores booleanos de formulario pendiente/operación; certificados y contraseñas continúan exclusivamente en el formulario y en el envío temporal original, sin persistencia o logs.
- Descargar, editar, subir, bajar y quitar anexos usan SVG del sistema, títulos/nombres accesibles y tamaño uniforme. Quitar utiliza rojo discreto y confirmación. Se conservan descarga original, orden, edición, estados de solo lectura y versiones anteriores.

No se modifican backend, motor documental, plantillas oficiales ni artefactos firmados.

## Validación

Se añade un recorrido completo específico: seis pasos persistentes y paso activo, protección/cancelación de cambios, validación de matriz vacía, recarga en el paso actual, anexos PDF reales, orden, edición, descarga verificada por SHA-256, eliminación cancelada/confirmada, borrador de anexo pendiente y navegación hasta preparación/firma. Se comprueban la posición del navegador respecto a la cabecera y el color principal de Siguiente en temas claro y oscuro.

Los recorridos existentes se adaptan a la navegación visible. El circuito de firma espera la pantalla de destino antes de comprobar su previsualización, evitando asumir que la consulta del documento es instantánea.

Lint, TypeScript sin emisión y build aprobados. Permanece el aviso conocido de tamaño del bundle. La aceptación final pasó 21/21 pruebas (1,8 minutos), incluyendo navegación, recuperación tras errores de carga, firma secuencial, devolución, evidencias y T2. Se usó únicamente el entorno desechable, con limpieza al finalizar. Log: `backend/target/persistent-wizard-final-result.log`. Se revisaron las capturas de escritorio y tablet en modo oscuro, incluido el navegador fijo durante la previsualización.

Capturas y logs ignorados en `frontend/test-results/plan-wizard-*.png` y `backend/target/persistent-wizard-*.log`. Sin commits ni publicaciones.
