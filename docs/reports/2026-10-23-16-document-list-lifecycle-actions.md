# Descarte y archivo visibles en documentos — 23/10/2026

## Problema y comportamiento

La acción de archivo existía dentro de «Versiones formales y archivo», seguida del botón «Actualizar o archivar documento» y el motivo. Esa ubicación no era visible desde la bandeja y llamaba archivo también al descarte de un borrador.

La tabla ahora muestra «Descartar borrador» o «Archivar», junto a Editar/Ver, según el estado. El formulario de confirmación identifica el documento y explica qué se conserva. Exige motivo, permite cancelar y protege el texto escrito con la confirmación compartida. El botón inicial de foco es Cancelar; se contiene el foco y se restaura al cerrar. Tras descartar, se recarga la lista y se enfoca su encabezado, ajustando la página si se vació la última.

## Integración

- Se reutiliza el endpoint de archivo y su autorización existente; no se crea borrado físico.
- La respuesta de lista incorpora `canManage` y `rowVersion`; el permiso se obtiene de la misma función que el detalle. No se cambian las reglas de responsables, períodos, firmas o revisión.
- Las acciones se ocultan sin permiso, en revisión o con archivo previo. Las restricciones por Informes asociados siguen comprobándose en servidor.
- Se bloquea el doble envío y se conserva el motivo ante errores/conflictos. Los mensajes usan Toast; las decisiones, modal. Los borradores descartados siguen accesibles únicamente según la política administrativa existente.
- En el editor se utiliza también «Descartar borrador» para DRAFT. Los aprobados conservan «Archivar documento» y sus opciones de versión formal.
- Se mantienen las modificaciones documentales previas y no se altera la selección staged mediante comandos Git.

## Validación

La prueba añadida cubre permisos de lectura, rechazo 403 en servidor, motivo vacío/espacios, cancelación simple y anidada, conservación ante 409, foco, descarte confirmado, trazabilidad administrativa y aislamiento de otro documento. El circuito completo comprueba además que Archivar abre el modal para un aprobado sin alterar su contenido al cancelar. Se revisa la presentación en escritorio y tablet oscura.

Resultados finales: lint, TypeScript sin emisión y build aprobados; 20/20 recorridos de aceptación aprobados en entorno desechable (1,8 minutos). Se inspeccionaron las capturas del descarte en tablet oscura y del archivo de aprobado en escritorio. El entorno local se actualizó sin reinicializar sus datos. Permanece el aviso conocido de tamaño del bundle, sin errores de compilación.

Evidencia local ignorada: `backend/target/lifecycle-table-acceptance-final.log`, `frontend/test-results/discard-document-tablet-dark.png` y `frontend/test-results/archive-document-desktop.png`. No se realizan commits ni publicaciones en esta entrega.
