# Control de historial conforme al ejemplo oficial

Fecha: 2026-10-24. Sin commit por instrucción vigente del propietario.

## Causa y cambio

La fila de datos del T1 se clonaba de la cabecera: heredaba sombreado gris y altura mínima de una línea. Además, agregaba v a la versión y omitía la denominación del grupo en su descripción.

Se conserva la cuadrícula de tres columnas del original y se separa su estilo de los datos: cabecera gris; fila blanca, altura mínima 704 twips del template (crece si el contenido lo requiere), centrado horizontal/vertical y fuente Helvetica 10 pt del ejemplo firmado. La versión formal se presenta sin prefijo, en negrita; descripción y fecha en peso normal. La descripción usa la denominación propia del grupo, por ejemplo «Elaboración del plan de trabajo de la Unidad de Titulación». La fecha sigue siendo la fecha real de preparación, no la fecha particular del ejemplo.

T2 tenía el mismo problema de herencia de cabecera; reutiliza el formateador. T1 engine14 y T2 engine13 invalidan solo la reutilización de composiciones antiguas para nuevas preparaciones. No se alteran firmas ni archivos conservados.

## Verificación

Prueba sobre las tablas originales T1/T2: filas/columnas, fondo blanco sin tema heredado, altura adaptable, alineación, peso de versión, descripción y fecha. Integración con PDF comprueba descripción y ausencia de v1.0. 194 pruebas backend aprobadas, sin fallos ni errores. Log: backend/target/2026-10-24-history-review.log. Revisados los recortes renderizados history-corrected.png y history-t2-corrected.png en backend/target/document-qa: fondo blanco, bordes, centrado, altura y texto correctos. El entorno de pruebas se limpió al finalizar. Se actualizó el backend local con scripts/start-development.ps1, sin reiniciar datos; la nueva composición se obtiene al abrir el paso 5 de un borrador. No se hicieron commits ni publicaciones.
