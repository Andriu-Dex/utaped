# Edición con previsualización simultánea

Actualización posterior 2026-10-27: «Revisar y firmar» comparte la vista dividida y los modos PC/tablet de los otros pasos. Acciones a la izquierda y un visor a la derecha; al firmar muestra el archivo conservado y al aprobar la ronda muestra su PDF final. Finalizar vuelve a Documentos sin aprobar. No se ofrecen descargas preliminares ni un enlace redundante al mismo visor. Sustituye la presentación final a ancho completo de la entrega anterior.

Actualización 2026-10-27: el Plan T1 tiene cinco pasos. Se fusionan preparación, revisión final y firma en el último; las opciones de elaboradores/circuito se despliegan sin añadir otro paso. La previsualización de edición sigue siendo transitoria y el último paso prepara automáticamente un artefacto estable. Las referencias anteriores a seis pasos describen el diseño previo.

Decisión del propietario: 2026-10-17. Primera entrega: **Contenido del Plan T1**. Se mantienen los pasos de edición y la preparación/firma existentes. Matriz como editor simultáneo y T2 quedan para una extensión posterior.

En PC, formulario y documento A4 se muestran juntos; un control de ancho permite repartir el espacio. En tablet se alterna Editar contenido / Previsualizar documento. La página puede ampliarse en un diálogo accesible con Escape y retorno de foco. Se respetan los temas de la aplicación; el documento conserva sus colores institucionales. No se añaden estilos para celulares.

Tras 1.4 segundos sin escribir se solicitan los textos actuales (incluidos título/unidad/carrera aún no guardados) junto con la versión del Plan. La vista muestra Borrador y distingue cambios sin guardar y contenido pendiente de visualizar. Foco en Justificación/Objetivo lleva a la página real correspondiente. La navegación usa la cantidad de páginas generadas, nunca un número fijo. Actualizar vista permite reintentar sin perder textos.

La vista usa el mismo T1Template/T1PdfEngine que la preparación oficial. Puede renderizar texto y matriz incompletos: una matriz sin actividades conserva una fila vacía de presentación bajo los encabezados combinados, sin crear una actividad. No omite las validaciones del paso de preparación/firma.

## Contrato técnico

POST /api/work-plans/{targetDocId}/artifacts/draft-preview, con sesión y CSRF. Entrada: rowVersion, justification/objective (hasta 50000 caracteres cada uno), title/institutionalUnit/career opcionales (hasta 200), pageIndex (-1 para sección actual o 0–499), section JUSTIFICATION/OBJECTIVE. Respuesta sin caché HTTP: sourceRowVersion, pages reales, sections (índices desde cero), pageIndex e imagen PNG base64.

Se permite únicamente al editor autorizado de un T1 DRAFT vigente; lector ajeno recibe 404 y documento no editable 403. Una versión anterior recibe 409 sin escribir cambios. No crea stored_file, document_artifact, firmas, historial ni guardado del borrador. El contenido se usa solo en memoria; los PDF firmados se consultan por sus rutas conservadas.

Las solicitudes del editor se serializan y las respuestas obsoletas se descartan tras cambios de texto, documento o desmontaje. El servidor comparte el límite de una generación concurrente con la preparación oficial, mantiene como máximo dos PDFs de hasta 12 MiB en caché de proceso con reutilización de dos minutos y revalida acceso/estado/versión antes de usarla. Las entradas caducadas se retiran al siguiente acceso; no existe persistencia en disco de esta caché. La página PNG usa el renderizador acotado existente. Errores, límites o servicio ocupado conservan el formulario y permiten reintentar.

No es autosalvado, artefacto firmable ni sustituto del documento aprobado. La preparación definitiva sigue creando el artefacto completo con contenido guardado y validado.
