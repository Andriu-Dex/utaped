# ADR-0002 — Anexos privados y preparación T1

Fecha: 2026-10-03. Estado: adoptado como configuración técnica inicial del proyecto; no acredita aprobación institucional.

## Decisiones

El propietario confirmó almacenamiento local privado con volumen persistente, 10 MB por anexo y hasta 20 anexos como límites iniciales ajustables. Confirmó A4 para la previsualización conservando estructura y estilo institucional. La referencia mezcla A4 vertical, A4 horizontal y una sección final Carta: la composición normaliza páginas T1 a A4 manteniendo orientaciones. Los PDFs anexos conservan sus páginas originales.

Defensas técnicas adicionales configurables: 50 MB acumulados por Plan, 100 páginas por anexo y 12 MB por solicitud multipart. La generación admite hasta 450 páginas anexadas y 500 totales; son protecciones iniciales del motor, no políticas institucionales. Nginx permite 12 MB y 180 segundos; al aumentar la carga se debe ajustar también frontend/nginx.conf.

## Solución

PostgreSQL conserva metadatos, orden, hashes, snapshots y artefactos. document-data almacena bytes fuera del servidor estático. Claves UUID generadas en servidor; permisos Linux 0700 en directorio y 0600 en archivos; autorización por Plan explícito, propietario activo y pertenencia activa. Sesión y CSRF existentes. PDFBox valida contenido real, cifrado, acciones activas y límites; no se confía en extensión/MIME. Esta validación estructural no es antivirus ni validación criptográfica de firmas existentes.

docx4j convierte una copia de la plantilla DOCX oficial a PDF; PDFBox compone anexos, deriva paginación y renderiza. Se conserva logo embebido, estructura y estilos; tablas adaptadas proporcionalmente al ancho A4. Los índices se calculan desde destinos reales del documento. Solo se representa el elaborador conocido; no se inventan cargos, autoridades ni etapas institucionales.

Referencia incluida sin modificaciones: backend/src/main/resources/document-templates/t1-reference.docx. Original: `UTA-SGC-A-2-1-P7-T1 Formato Plan de trabajo (1).docx`, Documentos_guia del mockup congelado. SHA-256: `53b62c431e2760d5e6155d2191c13c59c093d095118a1770ff87304431db179e`. El adaptador genera una copia en memoria. Versión técnica: hash de plantilla + formato + versión del motor.

Cada previsualización conserva bytes PDF, snapshot, páginas y metadata de elaboración. SHA-256 se verifica al leer. Editar el Plan no reconstruye artefactos anteriores. Generación repetida para el mismo snapshot/motor reutiliza su artefacto, sin alterar estado, versión formal ni contador de edición. No aplica una firma: el documento sigue en borrador.

## Consecuencias

Baja lógica de anexos para conservar historia. Retención y eliminación definitiva siguen pendientes. Rollback ordinario limpia la escritura nueva; interrupciones abruptas pueden dejar huérfanos que requieren reconciliación sin borrado automático. Respaldar DB y volumen juntos con escrituras detenidas: [operación](../../development/document-storage.md).

El disco debe ser persistente; no se prepara esta implementación para hosts efímeros ni réplicas con discos independientes. Un adaptador de objetos compartidos depende de infraestructura futura. Generación limitada a una y renderizado a dos operaciones simultáneas por proceso.

Q-003/Q-007 mantienen pendientes flujos/firmas reales y Q-013 la aceptación institucional final del formato. T2 no está entregado.

## Fuentes verificadas

- [PDFBox: API](https://pdfbox.apache.org/3.0/getting-started.html), [versiones](https://pdfbox.apache.org/download.cgi) y [migración](https://pdfbox.apache.org/3.0/migration.html): PDFBox 3.0.8, Loader y procesamiento documental.
- [docx4j 17.1.0](https://www.docx4java.org/forums/announces/docx4j-17-1-0-released-t3180.html): conversión FO; versión fijada y probada con Java 21.

Se mantiene el stack existente; no se añade un servicio externo de conversión.
