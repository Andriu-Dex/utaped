# Catálogo y procedencia institucional

## Cambios

- Catálogo administrable de unidades académicas y administrativas en Carreras y ámbitos: alta, edición, desactivación, auditoría y control de concurrencia. Formularios cerrados hasta una acción explícita, confirmación de cancelación y foco de teclado.
- Carrera vinculada a unidad académica. Ámbito con selectores de tipo, unidad y carrera filtrada; las unidades administrativas no admiten carrera. Validación equivalente en backend.
- Información general del T1 muestra procedencia con selectores bloqueados cuando deriva del ámbito. En documentos anteriores sin ámbito se selecciona del catálogo. Informes T2/T3 heredan y muestran la procedencia bloqueada del Plan; las versiones formales conservan el tipo.
- El encabezado T1/T2 usa el tipo correcto y elimina la carrera en unidades administrativas. Se preservan A4, bordes y proporciones.
- Migración V34: catálogo y relaciones para carreras/ámbitos, unicidad incluyendo unidad y tipo congelado por documento. Precarga solo FISEI como continuidad del dato existente; no crea unidades administrativas inventadas.
- Compatibilidad de hashes académicos anteriores sin reescritura de artefactos firmados. Renombrar una unidad no modifica los documentos ya creados.

## Revisión y pruebas

- Lint y build frontend aprobados. Se mantiene la advertencia anterior de tamaño del bundle.
- 19 pruebas Playwright aprobadas en entorno desechable, incluyendo alta de unidad administrativa desde UI, configuración de ámbito, herencia bloqueada sin carrera y circuito completo T1/T2/T3 con firmas.
- 30 casos backend aprobados entre actividades (3), composición documental (13), organización (7), unidades (5, incluidos los heredados del contrato base) y plantillas (2). La última comprobación focalizada de plantillas aprobó tras restaurar sus expectativas originales sobre la justificación predeterminada.
- El caso documental administrativo comprueba texto del encabezado, ausencia de carrera, conservación de bytes y vigencia del artefacto después de renombrar el catálogo.
- Inspección visual de la captura de procedencia administrativa. Los fixtures antiguos se ajustaron para utilizar la unidad y carrera del catálogo; se conservaron las comprobaciones de permisos, concurrencia, firma y paginación.
- Aplicado al entorno manual mediante scripts/start-development.ps1, sin ResetData: V34 confirmada y backend disponible para Vite. Sin cambios de variables de entorno, credenciales ni reinicialización de registros. Respaldo PostgreSQL previo legible en backend/target/before-institutional-units.dump, ignorado por Git; no se modificaron archivos documentales.
- Entornos desechables de aceptación y backend limpiados al finalizar. Basta recargar la página local. Sin commit ni push.

Contrato: docs/api/institutional-units.md. Se conservaron los cambios previos staged. Sin commit ni push.
