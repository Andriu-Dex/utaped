# Comprobación documental de la auditoría

Fuentes: DOCX oficiales T1/T2/T3 y PDF TI-JD2026-UTIT-PLAN-V01-signed/Informe_ejecucion. Se inspeccionó OOXML de tamaño/estructura sin editar originales y se renderizaron PDFs nuevos generados por las pruebas del motor vigente.

| Elemento | Comprobación ejecutada | Resultado / límite |
|---|---|---|
| Tamaño | Inventario real de páginas PDF y secciones DOCX | A4 vertical/apaisado donde corresponde; Carta original normalizada por autorización |
| Encabezado | PDF oficial y muestras T1/T2; posición de texto y bordes | Etiqueta/valor, fecha derecha, facultad y carrera propias; encabezado conservado |
| Portada | T1 corto/largo y T2/T3 | Alineaciones y separación propias de cada formato; no copiar datos particulares del ejemplo |
| Índices | Páginas reales, destinos y comparación visual | Índices ajustados a contenido, firmas y anexos; no número fijo |
| Matriz | Página 4 oficial y T1 generado; PDF y raster de dos motores | Rejilla completa en PDF/MuPDF; Poppler a baja resolución puede suavizar algunos trazos. No se reescribió un PDF correcto por antialiasing |
| Anexos vacíos | T1 página 5, T2 página 3 y T3 página 4 de fixtures vacías | Apartado numerado y No aplica; referencias existentes no se omiten |
| Firmas/control | Páginas finales T1/T2/T3 y asserts geométricos | Slots dinámicos, historia y espacios; formatos particulares conservados |
| Extensión | T1 13 páginas, T2 18, T3 6 y estructura adicional de 6 | Contenido final comprobado por pruebas; muestras visuales de principio/final y páginas de tabla |
| Integridad | Hash, firmas incrementales y rondas T1/T2/T3 | Bytes históricos conservados; hash nuevo legítimo tras otra firma |

Evidencia local reproducible en backend/target/integral-audit: pdf-inventory.json, contactos de nueve PDFs, official-matrix/current-matrix, current-matrix-mupdf, current-no-annex y current-t3-body. Los PDF fuente están en backend/target/document-qa y proceden de la ejecución actual. No usar PNGs antiguos de esa carpeta como evidencia de esta versión.

No se modificó el motor ni ninguna plantilla oficial en esta auditoría. La verificación técnica/visual de muestras no equivale a aceptación institucional de todos los datos, ni a comparación píxel por píxel de documentos con contenido distinto. El guion de aceptación incluye la revisión final con el propietario.
