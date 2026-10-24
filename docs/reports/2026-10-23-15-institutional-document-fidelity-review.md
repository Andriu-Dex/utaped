# Fidelidad documental y revisión de cambios — 23/10/2026

## Alcance y fuentes

Se revisaron separadamente los cambios que estaban en el índice de Git y las modificaciones del propietario fuera del índice. Se conservaron las mejoras de interfaz y se corrigieron las regresiones encontradas. No se modificó la selección staged ni se hicieron commits o publicaciones.

Fuentes contrastadas, sin modificar sus archivos:

- `docs/templates/UTA-SGC-A-2-1-P7-T1 Formato Plan de trabajo (1).docx` y su referencia incorporada.
- `docs/templates/UTA-SGC-A-2-1-P7-T2 Formato Informe (1).docx` y su referencia incorporada.
- `docs/templates/TI-JD2026-UTIT-PLAN-V01-signed.pdf`, especialmente portada, índices, encabezado y matriz horizontal.
- Referencia incorporada del Informe de ejecución, para comprobar que el cambio compartido de índices no altera su formato.

La información oficial se utiliza como referencia institucional. Cada documento conserva su título, procedencia, carrera, fecha y responsables: no se copia indiscriminadamente Unidad de Titulación, sus carreras o su fecha a otros ámbitos. Se conserva la decisión explícita de utilizar A4 y una etiqueta específica académica o administrativa en T1/T2.

## Correcciones

- Encabezado: geometría del ejemplo, escudo y leyenda de dos líneas; título azul, gris institucional, etiquetas a la izquierda y valores de unidad/carrera y fecha a la derecha. Fecha española con mes escrito. El trazo de 0,75 pt y la previsualización a dos píxeles por punto mantienen visibles las divisiones; se conserva el límite de 1800 píxeles y la concurrencia de renderizado.
- Portada T1: carrera antes del nombre del Plan; eliminación de párrafos vacíos que desplazaban el índice a una posición arbitraria de la segunda página. Los prefijos documentales canónicos no se duplican en portada/encabezado.
- Índices: tabulación con líderes de puntos, texto de 10 pt y página calculada desde la composición. Sin números estimados/fijos. Se mantienen firmas e historial, índice de tablas, anexos y estructuras publicadas. Los enlaces se reasignan a las páginas ensambladas, incluidos los desplazamientos por anexos.
- Matriz T1: tabla horizontal con seis columnas efectivas y Cronograma dividido en Desde/Hasta; anchos contrastados con el ejemplo, fechas legibles, texto Helvetica compatible, cabeceras repetidas y todos los bordes explícitos. Se respetan las combinaciones verticales/horizontales. Se recupera el espacio del título y la leyenda de tabla.
- Tablas T2, firmas e historial: bordes explícitos para evitar dependencia de estilos heredados del conversor. Los anchos de sus secciones se conservan.
- Firma/preparación: se conserva el rediseño del propietario; se recupera la semántica de lista ordenada, se evita tratar la firma obligatoria como error rojo y se permite ajustar mensajes largos sin desbordar.
- Se corrigió el espacio XML del texto del encabezado. Los artefactos firmados siguen conservando sus bytes; las versiones nuevas del motor solo afectan composiciones nuevas.

## Validación

- Frontend: lint, comprobación TypeScript sin emisión y compilación aprobados. Permanece el aviso conocido de tamaño del bundle, sin error de compilación.
- Siete pruebas documentales aisladas aprobadas: encabezado rasterizado; procedencia administrativa e inmutabilidad tras renombrado; T1 con anexos y enlaces; T1 largo con 35 actividades e índice dinámico; estructura publicada/reordenada; T2 con evidencias, firma y aprobación; Informe de ejecución independiente.
- Dos comprobaciones posteriores de T1/T2 aprobadas tras eliminar prefijos duplicados en títulos.
- Aceptación aislada: 19/19 recorridos aprobados (2,9 minutos), incluidos devolución, firmas secuenciales, evidencias, T2, versión formal, permisos y navegación.
- Entorno local `utaped-sample` actualizado sin restablecer datos; fuentes oficiales y selección staged conservadas.
- Los enlaces del PDF se comprueban contra páginas reales y no contra la portada. La comprobación del índice admite la extracción de líderes de tabulación, conservando la verificación del número de página real.
- QA visual de todas las páginas de T1 corto (8), T1 largo (12), T2 firmado (5), Informe de ejecución (6) y estructura personalizada (7). El número varía según contenido, anexos y flujo; no constituye una regla institucional fija.
- La revisión distingue estilos de plantilla de datos del escenario: los PDFs de prueba utilizan fixtures y no sustituyen registros institucionales.

Las imágenes/PDF de comprobación y logs permanecen en `backend/target/fidelity-review`, `backend/target/document-qa` y `backend/target/fidelity-*.log`, excluidos de Git. La sustitución métrica de fuentes en Linux se conserva; no se declara equivalencia binaria con el PDF firmado del ejemplo.
