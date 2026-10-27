# Portada T1 conforme al ejemplo oficial

Fuente: `docs/templates/TI-JD2026-UTIT-PLAN-V01-signed.pdf`, primera página. Comparación visual y de posiciones del PDF oficial con el artefacto generado.

## Corrección

- Unidad y carrera con justificación de párrafo (última línea a la izquierda), en mayúsculas y sin párrafo vacío entre ellas.
- Se conserva el párrafo separador después de carrera, antes del título centrado; período centrado y separado conforme a la composición de origen.
- Se elimina un párrafo vacío inicial que desplazaba el nombre de la universidad unos 21 pt hacia abajo.
- Los bloques se distribuyen mediante el flujo de párrafos: no se fijan posiciones absolutas ni se copian nombres, fechas o carreras del ejemplo. A4 y datos propios del documento conservados.
- Versión de composición T1 incrementada a engine17 para detectar previsualizaciones antiguas de borradores. Los artefactos existentes y firmados conservan sus bytes.

## Verificación

- 33 pruebas de integración documental y estructura pasaron (16 + 17); repetida además la prueba de artefacto inmutable con nuevas comprobaciones sobre el PDF: bloques en portada, margen compartido, justificación y separación del título. También pasó.
- TypeScript y build web correctos; continúa el aviso conocido de tamaño del bundle.
- PDF generado revisado visualmente frente al original; unidad/carrera agrupadas, título centrado separado, período centrado y portada en una sola página.
- Entorno manual utaped-sample actualizado sin restablecer sus datos. Cambios sin commit para revisión.

La corrección corresponde a la portada T1; no altera el diseño del encabezado tabular compartido ni la portada T2.
