# Entrega — Anexos y preparación documental T1

Fecha: 2026-10-03. Repositorio productivo: `C:\Documentos\D-Proyectos\Git\utaped`. Rama: `feature/t1-document-preparation`, creada desde develop tras verificar PR #3 fusionado, merge `b24b53109a30828dde1ad117f2ea6a7463ab674e`. El mockup externo permanece congelado y sin modificaciones. Sin commit, push ni PR en esta entrega.

## Objetivo y resultado

Entrega ampliada para cubrir la preparación real del Plan T1: guardar contenido/matriz, gestionar anexos privados, comprobar requisitos, generar PDF desde referencia institucional, navegar páginas y conservar previsualizaciones anteriores. El Plan permanece en borrador; no se simula una firma ni se considera aprobado/finalizado.

## Decisiones del propietario

- Almacenamiento local privado con volumen persistente; 10 MB por anexo y hasta 20 anexos como configuración inicial ajustable.
- Páginas T1 A4 conservando estructura/estilo institucional y matriz horizontal. PDFs anexos conservan sus páginas originales.

Límites técnicos adicionales: 50 MB acumulados, 100 páginas por anexo, 12 MB por solicitud; motor limitado a 450 páginas anexadas y 500 totales. No constituyen políticas institucionales. Retención/operación y aceptación institucional de plantilla siguen pendientes.

## Cambios

- Flyway V6: metadata privada de archivos, anexos con orden/requestKey/baja lógica y artefactos con snapshot, hashes, páginas y espacios de elaboración.
- PDFs validados por contenido, sin cifrado ni acciones activas admitidas; límites de tamaño/páginas. Escritura UUID fuera del frontend, SHA-256 verificado al leer y limpieza en rollback ordinario.
- Autorización por ID explícito: propietario activo y pertenencia vigente. CSRF para mutaciones; descarga privada no-store; sin acceso público al volumen.
- Anexos: Sí/No, alta real, título/descripción, descarga original, orden, renumeración A/B/... y baja lógica. Comparten concurrencia con información general y matriz. Retry idempotente con requestKey; conflictos conservan datos locales.
- Opciones: nota de protección de datos seleccionada explícitamente, sin inferencia por nombre del grupo.
- Preparación: unidad, justificación, objetivo, matriz guardada y completa, obligaciones, responsables/colectivos y fechas configuradas. Anexos=Sí requiere PDF. Errores identificados por sección.
- Motor T1: copia confiable de DOCX institucional, conversión docx4j y composición/validación PDFBox. Conserva logo embebido, estructura, encabezado, portada, matriz, pie e historial inicial; adapta anchos a A4 y evita títulos aislados de su contenido.
- Índices basados en destinos reales, paginación estable tras insertar anexos, metadata de cada página y ubicación efectiva del elaborador. Sin números de página o pageCount fijos.
- PDF/snapshot inmutables: editar/quitar anexos conserva bytes anteriores. Repetir generación del mismo contenido/motor reutiliza artefacto. No cambia estado, versión formal ni rowVersion.
- Visor de páginas reales con imágenes privadas, navegación, descarga e identificación de contenido anterior. Tamaño de imagen reservado mientras carga; UI comprobada en móvil/escritorio.
- Docker: volumen documental persistente, fuentes necesarias y conversor sin Word/LibreOffice. Nginx admite cargas hasta 12 MB; timeout de generación 180 s.
- Documentación: ADR-0002, API, almacenamiento/respaldo, decisiones parciales, catálogo trazable, modelo de datos y estado de implementación.

## Validaciones ejecutadas

| Verificación | Resultado |
|---|---|
| Frontend npm run build | Correcto: TypeScript y Vite. |
| Frontend npm run lint | Correcto: oxlint. |
| PostgreSQL aislado + Maven test | 22 pruebas, 0 fallos/errores. DB utaped_test, seis migraciones válidas. |
| DocumentIntegrationTests | 4 pruebas: validación/cifrado/acciones, orden/renumeración, aislamiento, concurrencia, idempotencia, revocación, solo lectura, rollback/integridad, snapshot histórico y composición corta/larga. |
| Playwright con Nginx/Docker | Flujo completo correcto, incluida carga PDF mayor a 1 MB, edición, generación, navegación y conservación del PDF tras quitar anexo. |
| Playwright con Vite | Flujo completo correcto, con páginas renderizadas y dimensiones móviles sin desbordamiento. |
| Revisión PDF renderizado | 17 páginas revisadas: escenario corto de 8 con anexos y largo de 9 con 35 actividades. Logo, encabezados, orientación, continuación de tabla, índices, pie e historial comprobados visualmente. |
| git diff --check | Sin errores de espacios. |

Los archivos PDF/PNG de QA están en backend/target/document-qa; las capturas E2E en frontend/test-results. Son salidas ignoradas, no documentos de usuarios versionados. No se afirma haber ejecutado restauración operacional completa ni CI remoto de esta rama.

## Fuentes y alcance pendiente

Referencia original incluida sin alterar: `UTA-SGC-A-2-1-P7-T1 Formato Plan de trabajo (1).docx` del directorio Documentos_guia del mockup. SHA-256: `53b62c431e2760d5e6155d2191c13c59c093d095118a1770ff87304431db179e`. [Decisión y fuentes oficiales de PDFBox/docx4j](../architecture/adr/0002-t1-document-preparation.md).

FR-ANX-001–005 cubiertos inicialmente; UT-SEC-020/022/023/025 cubiertos para previsualización inicial; UT-SEC-017/019/021/024 parciales por condición institucional, aceptación final, campos manuales y flujos/firmas pendientes.

No entregados: firma criptográfica, cargos/autoridades configuradas, revisión/observaciones, aprobación/finalización, T2, evidencias de ejecución, agente código/Trello, retención definitiva y automatización de respaldos. Validación estructural PDF no equivale a antivirus o validación de firmas existentes. Interrupciones abruptas pueden requerir reconciliar archivos huérfanos; no hay borrado automático.

Próximo paso recomendado: revisar esta entrega y autorizar su commit/publicación/PR hacia develop; después ampliar configuración de flujos por grupo y bandeja de revisión, precisando Q-003 y el contrato de firma Q-007.
