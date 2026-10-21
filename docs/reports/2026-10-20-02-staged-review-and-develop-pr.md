# Revisión del staged y preparación del PR hacia develop

Fecha: 2026-10-20. Rama: `feature/document-authorship-and-review`.

## Archivos revisados y commit

Commit `d5d79af`: se retira el botón duplicado de expansión de la cabecera y únicamente sus estilos asociados. Se conserva el botón del propio menú lateral con etiqueta accesible, tooltip, aria-expanded y persistencia de su estado. No se retiró la capacidad de volver a expandir el menú.

Se incorpora la transcripción completa aportada por el propietario: 1053 líneas, sin modificar su contenido. SHA-256 `50182038cda38f0fdb8cbe877d3c904e8aae4a751ce014efc8cce9f60c3b4a34`, coincidente con la auditoría anterior. Constituye evidencia de reuniones, no aceptación del producto actual ni sustitución automática de decisiones posteriores.

## Comprobaciones de esta revisión

- git diff --cached --check aprobado.
- Lint y compilación TypeScript/Vite aprobados. Persiste el aviso no bloqueante de bundle de 515,60 kB.
- Una prueba de aceptación existente aprobada: acceso administrativo, contraer menú, expansión desde el botón conservado, edición/cancelación de plantilla y permisos. Entorno utaped-acceptance desechable limpiado al terminar.
- No hubo cambios backend en el staged, por lo que no se repitió su suite por esta revisión. Las validaciones de coautores, firmas, encabezados, catálogos y circuito Plan/Informe están detalladas en los reportes respectivos de esta rama.

## Publicación autorizada

El propietario autoriza publicar temporalmente esta rama y abrir el pull request con base develop. El historial incluye autoría múltiple y firmas sucesivas, revisión colegiada y observaciones, encabezados institucionales completos, matriz con catálogos y colectivo automático, nombres ficticios realistas y la revisión del staged.

Se comprobó el remoto actualizado mediante fetch, sin commits adicionales de develop pendientes de incorporar en esta rama. No se modifica main ni se fusiona el pull request. El propietario realizará revisión, fusión y eliminación de la rama remota. La URL del PR se entrega al finalizar la operación.
