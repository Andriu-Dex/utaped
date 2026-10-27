# Encabezado institucional: celdas de Fecha de elaboración

## Corrección

Se retiró la combinación que extendía Fecha de elaboración por toda la fila. El motor conserva la celda de fecha y la celda gris vacía a la derecha, con su división vertical alineada con la fila de unidad y carrera. Se mantienen el logo, bordes, proporciones A4 y espacios institucionales.

La corrección se comparte entre T1 y T2. La versión de composición pasa de engine8 a engine9 para que los borradores obtengan el encabezado nuevo. Los artefactos previamente firmados no se modifican ni reconstruyen.

## Cambios acumulados

Se revisan para commit los ajustes compactos de matriz y badge único, previsualización automática T1, catálogo de unidades y herencia de procedencia, corrección del encabezado y sus pruebas/documentación. Se excluyen respaldos, archivos de entorno, builds y resultados de pruebas.

## Validación y publicación

- Tres pruebas documentales focalizadas aprobadas: bordes/divisor de la fila de fecha, composición e historial, y encabezado administrativo con conservación de artefactos.
- Revisión visual del PNG generado: fecha y celda gris independiente alineadas con la división superior, sin cambiar proporciones ni separación del contenido.
- Suite final de aceptación: 19 pruebas aprobadas, incluido flujo real T1/T2/T3, firmas secuenciales, devolución, permisos y catálogo de unidades.
- Lint limpio; compilación frontend aprobada durante la construcción del entorno de aceptación. Persiste la advertencia anterior de tamaño del bundle.
- Backend manual actualizado mediante scripts/start-development.ps1, conservando datos y credenciales. Recargar la vista para obtener la composición nueva en documentos editables.
- Entornos desechables limpiados. Respaldos, variables de entorno y artefactos de pruebas permanecen ignorados.
- Commit de implementación y pruebas: d48bfad, feat: completar edición y previsualización documental institucional. La documentación se registra en un commit separado.
- Sin push ni pull request en esta entrega.
