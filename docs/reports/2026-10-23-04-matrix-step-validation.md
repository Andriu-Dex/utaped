# Validación para continuar desde la matriz

Fecha: 2026-10-23. Rama: `fix/institutional-document-spacing`.

## Corrección

«Siguiente: Anexos» comprobaba la matriz solo cuando había cambios pendientes. Una matriz vacía guardada podía avanzar sin validación. Ahora exige al menos una actividad en cada intento, incluso si el borrador está sincronizado.

Con cero actividades se conserva el paso, se muestra el toast «Agregue al menos una actividad para continuar» y se enfoca el botón de agregar. Antes de avanzar se comprueban título, fechas ordenadas dentro del período, restricción configurada de feriados, responsables disponibles, recursos y medios, incluyendo «Otro» con descripción. Una actividad incompleta recibe el foco en su detalle y conserva sus datos. Los errores de controles se comunican mediante toast, sin diálogos nativos.

No se cambia el guardado de borradores: sigue permitiéndose guardar una matriz vacía; las validaciones existentes para guardar actividades se mantienen. Volver/salir conserva la confirmación de descarte. La navegación directa guarda cambios válidos antes de abrir Anexos y conserva los bloqueos por conflicto/error existentes. No cambian servicios backend ni la preparación/firma documental.

## Verificación

- Lint y compilación TypeScript/Vite aprobados. Permanece el aviso no bloqueante de bundle superior a 500 kB.
- Se amplía la aceptación para reproducir el caso inicial vacío, comprobar el foco y verificar que una matriz vacía guardada tampoco puede avanzar; continúa siendo posible guardarla como borrador.
- Las primeras ejecuciones detectaron un selector de foco dependiente de una clase visual y selectores ambiguos entre el botón de apertura y el del formulario modal. Se corrigió el foco mediante el atributo semántico y se acotaron los botones de prueba a la región del formulario, compatible con formulario integrado o modal.
- Repetición final: 16 pruebas de aceptación aprobadas, incluido el caso vacío inicial/guardado, actividad incompleta, guardado válido, recuperación tras error y circuito completo T1/T2. El entorno desechable fue eliminado al finalizar. `git diff --check` aprobado.
- Sin cambios backend ni datos del entorno manual. Pruebas sobre `utaped-acceptance` desechable. Logs ignorados: `backend/target/matrix-step-validation*.log`.

## Entrega

Con Vite basta recargar la página. Sin variables de entorno ni migraciones. Se conservan las modificaciones de interfaz del propietario en staged y en el árbol de trabajo; el commit incluye únicamente el cambio de validación y sus pruebas/documentación. Sin push ni pull request.
