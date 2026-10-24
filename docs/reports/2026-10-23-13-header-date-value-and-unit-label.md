# Valor de fecha y tipo de unidad en documentos

Se corrige la interpretación de la fila de fecha: la celda izquierda contiene únicamente Fecha de elaboración y la derecha contiene la fecha. La portada T1/T2 y el texto institucional complementario muestran solo Unidad académica o Unidad administrativa según la procedencia congelada del documento, sin conservar la etiqueta genérica con ambas alternativas.

La composición pasa a engine10 para renovar previsualizaciones de borradores. Los originales de templates y los artefactos firmados se conservan.

Validación: tres pruebas documentales T1 aprobadas (bordes, composición e historial, unidad administrativa) y un circuito T2 con firma y aprobación aprobado. Se comprueba la posición horizontal real de la fecha frente a su etiqueta y el texto específico de portada. Inspeccionadas las imágenes de T1 y portada T2. No hubo cambios frontend; no se repitió la suite E2E.

Backend local actualizado con scripts/start-development.ps1, conservando datos y credenciales. Las previsualizaciones de borradores se actualizan al volver a entrar al paso 5; los firmados mantienen sus bytes originales. Entorno de pruebas desechable eliminado. Sin commit ni push en esta corrección.
