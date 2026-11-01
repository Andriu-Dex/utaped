# Matriz de auditoría integral

Inicio 2026-10-27, base 06c3b53, rama fix/integral-product-audit. Esta matriz agrupa reglas relacionadas sin sustituir sus fuentes ni declarar aceptación institucional.
Resultados técnicos registrados al 2026-10-27. «Conforme en escenarios ejecutados» identifica evidencia concreta, no una garantía de todas las combinaciones posibles ni aceptación institucional. A32 distingue pruebas locales de configuración institucional. El reporte final fija recuentos y límites.

## Escenarios e invariantes

| ID | Requisitos | Fuente vigente | Actor / estado | Entrada / precondición | Esperado | Evidencia a ejecutar | Resultado / estado |
|---|---|---|---|---|---|---|---|
| A01 | FR-AUTH-*, UT-AUTH-020..024 | Maestro §§6–7; local-access | Anónimo/usuario/temporal, sesión vigente/expirada | Acceso, CAPTCHA/audio, recuperación, contraseña | Identidad estable, CSRF y cambio temporal obligatorio | FoundationIntegrationTests; e2e | Acceso temporal, CAPTCHA imagen/audio, recuperación por correo local y logout comprobados; E2E 8/8. Conforme en escenarios ejecutados. |
| A02 | FR-GRP-*, UT-SEC-130 | Maestro §§8,59–60; contextual-administration | ADMIN/coordinador/integrante/ajeno; ámbito vigente | Organización, pertenencias y rutas API | Lectura y delegación según objeto; revocación en sesión | OrganizationIntegrationTests; ContextualAdministrationIntegrationTests; role-acceptance | Accesos cruzados y revocación verificados; recuperación de integrantes corregida (IA-04). Conforme en escenarios ejecutados. |
| A03 | FR-PER-*, UT-PER-130 | Maestro §§9,12,38–39; decisiones 2026-10-21 | ADMIN/coordinador; abierto/cerrado | Períodos, ventanas, feriados y prórrogas | Rangos válidos, días hábiles y rechazo en cerrado | PeriodControlIntegrationTests; ActivityIntegrationTests | Rangos, feriados, días hábiles y cierre verificados; fallo de consulta de plazo recuperable (IA-04). Conforme en escenarios ejecutados. |
| A04 | FR-T1-001..005 | work-plan-drafts; decisión 2026-10-14 | Principal; DRAFT | Modal Crear documento, URL y bandeja | Ámbito/unidad heredados, cancelar conserva contexto | WorkPlanIntegrationTests; document-creation; institutional-units | Creación optativa, modal y unidad/carrera heredadas comprobados. Conforme en escenarios ejecutados. |
| A05 | FR-T1-CONT-*, UT-UI-120 | decisiones 2026-10-17/27; simultaneous-preview | Principal; DRAFT | Contenido y visor persistente | Cambios locales no se presentan guardados; visor único | draft-preview; automatic-preview; plan-wizard | Vista local sin guardado implícito, reintentos y protección del borrador comprobados. Conforme en escenarios ejecutados. |
| A06 | FR-ACT-*, FR-DATE-* | Maestro §§11–12; work-plan-matrix | Principal; actividades obligatorias/opcionales | Editor y resumen de matriz | Fechas/rangos correctos y Otra explícita | ActivityIntegrationTests; plan-wizard | Catálogo/obligatorias/Otra y rangos comprobados por integración y wizard. Conforme en escenarios ejecutados. |
| A07 | FR-RESP-* | Maestro §11; decisión 2026-10-20 | Principal; 1/varios/todos | Responsables, tabla y PDF | Identidades trazables y denominación colectiva configurada | ActivityIntegrationTests; institutional-reference; product-circuit | Selección individual y colectiva contrastada entre datos, resumen y salida; sin nueva corrección. Conforme en escenarios ejecutados. |
| A08 | FR-REC-*, FR-MED-* | Maestro §13; work-plan-matrix | Principal; opciones y Otro | Catálogo, validación inline y persistencia | Otro no vacío, foco y borrador conservados | ActivityIntegrationTests; plan-wizard | Otro vacío rechazado con foco y conservación; valores no vacíos contados. Conforme en escenarios ejecutados. |
| A09 | FR-MAT-*, AUD-MATRIX-GATE | decisión 2026-10-23 | Principal; matriz vacía/incompleta/completa | Paso 3→4 y guardado separado | Guardar vacío permitido; avanzar exige actividad completa | plan-wizard; automatic-preview | Matriz vacía guardable, avance incompleto bloqueado; regresión vigente. Conforme en escenarios ejecutados. |
| A10 | FR-ANX-* | Maestro §14; decisión 2026-10-27 | Principal; anexos sí/no, error, cancelar | Anexos T1/T2/T3 | PDF privado, límites, orden y No aplica | DocumentIntegrationTests; plan-wizard; product-circuit | Cancelación, carga y salida conservadas; anexos vacíos T1/T2/T3 revisados. Conforme en escenarios ejecutados. |
| A11 | FR-SIGN-*, BR-004/005 | author-signing; decisión 2026-10-27 | Principal/coautor; turno y artefacto vigente | Preparar, inspeccionar, confirmar, firmar | Firma real, secretos temporales, autorización y hash | Pkcs12PdfSignerTests; ReviewExecutionIntegrationTests; product-circuit | Firma PKCS12, permisos/turno, contraseña y bytes protegidos; circuito real de aceptación aprobado. Conforme en escenarios ejecutados. |
| A12 | AUD-AUTHORS | document-authorship-and-review; decisión 2026-10-17/27 | Principal/coautor; DRAFT antes/después de firma | Configurar autores y consulta sin wizard | Editor único, cargos y firmas ordenadas | ReviewExecutionIntegrationTests; document-authors | Coautor sin wizard, turno bloqueado/habilitado, cancelación y firma secuencial comprobados. Conforme en escenarios ejecutados. |
| A13 | FR-REV-*, FR-FLOW-* | Maestro §§23–32; workflow-configuration | Revisor/aprobador/colegiado en turno | Inbox, Documentos, historial y PDF | Solo asignado actúa; observación exige devolución | WorkflowIntegrationTests; ReviewExecutionIntegrationTests; product-circuit | Secuencia, observaciones, resaltado, devolución y registrador colegiado comprobados; UI común de revisión. Conforme en escenarios ejecutados. |
| A14 | AUD-REVIEW-RECOVERY | NFR-UX-001; plan fase H | Revisor; fallo inicial 503 | Detalle y reintento | No carga infinita, no decisión sobre estado fallido | integral-audit: review detail failures | Defecto reproducido y corregido IA-01; prueba roja antes y verde después. Conforme en escenarios ejecutados. |
| A15 | BR-003/004; UT-SEC-030..032 | Maestro §§31–33,50 | Principal/revisor; RETURNED→DRAFT→IN_REVIEW | Corregir y reenviar; versión formal | Nueva ronda/artefacto; historial previo inmutable | ReviewExecutionIntegrationTests; product-circuit | T1 por UI e integración; T2/T3 amplían devolución y nueva firma en pruebas reales, conservando Plan y bytes. Conforme en escenarios ejecutados. |
| A16 | UT-SEC-034; AUD-DOWNLOAD | Maestro §34; decisión 2026-10-27 | Participante/ajeno; previo/aprobado | Enlaces y endpoints de descarga | Preliminar bloqueado; aprobado exacto con firmas | ReviewExecutionIntegrationTests; product-circuit | Preliminares 403, descarga aprobada y SHA-256 comprobados. Conforme en escenarios ejecutados. |
| A17 | UT-SEC-019..026; FR-DOC-A4-001; FR-TPL-* | docs/templates; decisiones 2026-10-23..27 | T1/T2/T3; corto/largo/vacío | Portada, encabezado, índices, matriz, anexos, firmas, historial | A4, bordes/espaciado y páginas/slots reales | DocumentIntegrationTests; DocumentStructureIntegrationTests; InstitutionalHistoryTests; revisión PDF | Composición y muestras visuales T1/T2/T3 revisadas; ver checklist PDF. No se modificaron templates ni motor. Conforme en escenarios ejecutados. |
| A18 | FR-EVI-*, BR-010/018 | Maestro §§35–41; external-evidence | Responsable/coordinador; APPROVED y plazos | PDF/URL, observar, reemplazar y validar | Una evidencia vigente, versión e independencia | ReviewExecutionIntegrationTests; product-circuit | PDF/URL, permisos, sustitución, validación y concurrencia comprobados. Conforme en escenarios ejecutados. |
| A19 | UT-IND-130 | institutional-rules-and-execution | Principal/coordinador; sin declaración/0%/valor | Reporte de cobertura y avance | Cobertura no equivale a avance; cero distinto de vacío | TrackingIntegrationTests; ReviewExecutionIntegrationTests | Cobertura y declaración independientes, ausencia frente a cero y media del último corte comprobados. Conforme en escenarios ejecutados. |
| A20 | UT-SEC-035..040 | Maestro §§42–44; document-circuit | Principal; Plan aprobado | T2 resultados, delegación, anexos, firma/revisión | Derivado independiente, final único, datos propios | ReviewExecutionIntegrationTests; product-circuit | Derivación, firma, devolución/corrección y aprobación de T2; Plan base conservado. Conforme en escenarios ejecutados. |
| A21 | UT-T3-130 | decisión 2026-10-21; institutional-rules-and-execution | Principal; corte de Plan aprobado | T3 selección, FODA/mejoras y firma/revisión | Corte propio y formato UTA-SGC-A-1-1-P4-T2 | ReviewExecutionIntegrationTests; product-circuit | Corte, FODA, acciones, firma, devolución y nueva revisión T3 comprobados; siguiente corte independiente. Conforme en escenarios ejecutados. |
| A22 | UT-QPX-130 | decisión 2026-10-21; formal-submission | Coordinación; flujo requerido/opcional | Constancia Quipux | Bloqueos selectivos; no falso pendiente tras error | ReviewExecutionIntegrationTests; escenario recuperación de constancia | Bloqueo Quipux por configuración verificado en backend; error de consulta corregido IA-02. Conforme en escenarios ejecutados. |
| A23 | UT-SEC-049..050; BR-017 | Maestro §46; administrative-directory; user-imports | ADMIN/coordinador delegado | Altas, perfiles, importación y recuperación | Revalidación/errores, entrega temporal única, roles acotados | AdministrationIntegrationTests; UserImportIntegrationTests; user-import | Altas/importación/credencial temporal/recuperación y límites de coordinación comprobados. Conforme en escenarios ejecutados. |
| A24 | UT-TPL-130; FR-TPL-* | document-templates; decisión 2026-10-21 | ADMIN; borrador/publicado | Plantillas y estructuras | Cambios afectan documentos nuevos; snapshots previos fijos | DocumentTemplateIntegrationTests; DocumentStructureIntegrationTests; template-administration | Estructura publicada, datos por documento y textos iniciales congelados comprobados. Conforme en escenarios ejecutados. |
| A25 | UT-SEC-051; UT-NOT-050 | Maestro §§40,56; notifications | Destinatario autorizado | Campana, filtros, avisos y enlaces | Objeto correcto, sin duplicación y error recuperable | NotificationIntegrationTests; notification-usability | Campana, filtros, foco, destinatarios y enlaces inaccesibles comprobados. Conforme en escenarios ejecutados. |
| A26 | UT-SEC-052..055; BR-012; UT-CUS-130 | Maestro §§51–54; period-control; audit-history | ADMIN/cargo autorizado; período cerrado | Auditoría, archivo, ZIP/custodia/purga | Mínimo privilegio, integridad y confirmación de copia | AuditIntegrationTests; AuditExportIntegrationTests; PeriodControlIntegrationTests; institutional-governance | Archivo, historial acotado, custodia y rechazo de purga sin verificación comprobados en entorno aislado. Conforme en escenarios ejecutados. |
| A27 | UT-SEC-054..056; NFR-PERF-001 | tracking; document-circuit | Participantes; filtros/páginas/orden | Inicio, Documentos, Reportes | Totales/orden global y errores distintos de vacío | TrackingIntegrationTests; WorkPlanIntegrationTests; document-ordering; ui-recovery | Orden global, filtros/paginación, totales y recuperación sin falsos vacíos comprobados. Conforme en escenarios ejecutados. |
| A28 | FR-ARCH-AI-001 | Maestro §57; writing-and-review-inbox | Autor; proveedor disponible/error | Mejorar redacción y aplicar/descartar | Consentimiento, propuesta explícita, sin guardado implícito | WritingIntegrationTests; HttpAiProviderTests; e2e | Proveedor mediante dobles y propuesta explícita comprobados; configuración externa real no habilitada ni facturada. Conforme en escenarios ejecutados. |
| A29 | NFR-UX-001; UT-UI-020 | Maestro §58; decisiones de interfaz | Todos; PC/tablet claro/oscuro | Navegación y formularios compartidos | Foco, jerarquía, no overflow, contraste, acciones claras | integral-audit; action-forms; confirmations; capturas | 13 módulos en cuatro tamaños/temas, foco/confirmaciones y capturas revisados; contraste puntual >=4.5. Errores auxiliares corregidos IA-06. Conforme en escenarios ejecutados. |
| A30 | BR-001/002; NFR-SEC-001 | Maestro §§59–67 | 7 identidades + anónimo; acceso cruzado | UI, API, archivos y revocación | Aislamiento por ID, CSRF y no fuga de secretos | FoundationIntegrationTests; role-acceptance; ReviewExecutionIntegrationTests | Siete identidades y anónimo, sesiones/CSRF y revocación activa; ningún permiso ampliado por las correcciones. Conforme en escenarios ejecutados. |
| A31 | NFR-INT-001; NFR-DOC-001 | Maestro §§63–64,78 | Autores/revisores concurrentes; docs largos | Firma, generación, upload y descarga | Conflictos seguros, idempotencia y bytes íntegros | ReviewExecutionIntegrationTests: bounded/concurrent load | Tres autores, documentos largos, ocho lecturas y escrituras concurrentes conservan integridad; conflictos esperados. Conforme en escenarios ejecutados. |
| A32 | NFR-OPS-001; NFR-DATA-001 | Maestro §§68–71,79; production-readiness | Operación aislada | Migraciones, respaldo/restauración, TLS/SMTP | Ensayo técnico separado de contrato institucional | test_operations.py; test-operations-transport.py; migración Flyway en suites | Migraciones, 11 guardas y respaldo/restauración actual de 4 documentos/10 archivos aprobados. HTTPS, cookie Secure y hash del aprobado comprobados; infraestructura institucional pendiente. Conforme local; validación institucional/operativa real pendiente. |

## Decisiones vigentes que sustituyen documentación histórica

| Tema | Regla actual | Sustituye / límite |
|---|---|---|
| T1 | Cinco pasos, visor persistente y revisión/firma unificadas | No seis pasos ni preparación duplicada |
| Consulta | Autor edita; coautor firma; lectores/revisores no ven creación | No derivar permiso desde el nombre del rol |
| Certificado | Aportado/inspeccionado/confirmado, sin pin administrativo obligatorio | Cuenta ejecutora y titular separados; confianza institucional pendiente |
| Descarga | PDF final aprobado para participantes autorizados | Sin enlace preliminar ni descarga tras la primera firma |
| Fecha | Preparación antes de firmar, congelada; firma con hora propia | No reemplazar la fecha documental por la última firma |
| Evidencias | PDF o URL según medio congelado | URL no equivale a PDF ni el servidor acredita el recurso remoto |
| Versiones | Formal, ronda y evidencia independientes | Devolución no incrementa versión formal |
| T3 | Corte configurable y formato propio | No copiar wizard/formato T1 por comodidad |
| Interfaz | PC/tablet, ambos temas, formularios optativos, Toast/confirmación | No móvil como requisito nuevo ni alerts nativos |
| Custodia | ZIP verificado, gracia y purga explícita configurada | Sin borrado automático al cerrar; política institucional pendiente |

## Dependencias institucionales (no son una conformidad técnica)

Catálogos/asignaciones oficiales definitivos; correspondencia cuenta–titular, CA/revocación/tiempo confiable; corte definitivo T3 y formatos adicionales; sustitución de participantes ya congelados; dominio/TLS/SMTP/red; custodio, retención y RPO/RTO/SLA; accesibilidad con usuarios y aceptación formal del cliente. Fuente: open-questions y Plan Maestro §85, actualizados por decisiones fechadas.

## Evidencia de reuniones y precedencia

En `docs/decisions/Todas las reuniones.txt`, L2 solicita opciones por comisión y selección de responsables; L7 incluye anexos «No aplica», fidelidad del formato, carreras por combo y selección temporal del certificado; L8 trata revisión por páginas y observaciones. L451–472 recoge el trámite QIPOC/Quipux y su falta de reglamento. No se transforma la presentación del prototipo en aceptación del producto: las decisiones posteriores 2026-10-17/21/27 fijan la autoría, los bloqueos configurables y la firma vigente.

## Índice de todos los requisitos heredados

Los 122 IDs del catálogo de 2026-10-02 se conservan aquí para evitar omisiones. La columna remite al escenario de auditoría y a su fuente vigente; no ratifica frases históricas sustituidas (SSO, seis pasos, pin administrativo, firma simulada, móvil o purga automática). Los 92 apartados del Plan Maestro figuran en el inventario.

| ID heredado | Apartado de origen | Escenario vigente / evidencia |
|---|---|---|
| UT-SEC-001 | 1 · OBJETIVO GENERAL | A01–A32 (objetivo) |
| UT-SEC-002 | 2 · ACTORES | A02/A11/A12/A13/A30 |
| FR-AUTH-001 | 3 · AUTENTICACIÓN | A01 |
| FR-AUTH-002 | 3 · AUTENTICACIÓN | A01 |
| FR-AUTH-003 | 3 · AUTENTICACIÓN | A01 |
| FR-AUTH-004 | 3 · AUTENTICACIÓN | A01 |
| FR-AUTH-005 | 3 · AUTENTICACIÓN | A01 |
| FR-GRP-001 | 4 · GRUPOS INSTITUCIONALES | A02 |
| FR-GRP-002 | 4 · GRUPOS INSTITUCIONALES | A02 |
| FR-GRP-003 | 4 · GRUPOS INSTITUCIONALES | A02 |
| FR-GRP-004 | 4 · GRUPOS INSTITUCIONALES | A02 |
| FR-GRP-005 | 4 · GRUPOS INSTITUCIONALES | A02 |
| FR-GRP-006 | 4 · GRUPOS INSTITUCIONALES | A02 |
| FR-PER-001 | 5 · PERÍODOS | A03 |
| FR-PER-002 | 5 · PERÍODOS | A03 |
| FR-PER-003 | 5 · PERÍODOS | A03 |
| UT-SEC-006 | 6 · PLAN DE TRABAJO T1 | A04/A05 |
| UT-SEC-007 | 7 · SECUENCIA DEL WIZARD T1 | A05/A09/A11 |
| FR-T1-001 | 8 · INFORMACIÓN GENERAL DEL PLAN | A04 |
| FR-T1-002 | 8 · INFORMACIÓN GENERAL DEL PLAN | A04 |
| FR-T1-003 | 8 · INFORMACIÓN GENERAL DEL PLAN | A04 |
| FR-T1-004 | 8 · INFORMACIÓN GENERAL DEL PLAN | A04 |
| FR-T1-005 | 8 · INFORMACIÓN GENERAL DEL PLAN | A04 |
| FR-T1-CONT-001 | 9 · CONTENIDO DEL PLAN | A05/A28 |
| FR-T1-CONT-002 | 9 · CONTENIDO DEL PLAN | A05/A28 |
| FR-T1-CONT-003 | 9 · CONTENIDO DEL PLAN | A05/A28 |
| FR-T1-CONT-004 | 9 · CONTENIDO DEL PLAN | A05/A28 |
| FR-T1-CONT-005 | 9 · CONTENIDO DEL PLAN | A05/A28 |
| FR-ACT-001 | 10 · ACTIVIDADES | A06 |
| FR-ACT-002 | 10 · ACTIVIDADES | A06 |
| FR-ACT-003 | 10 · ACTIVIDADES | A06 |
| FR-ACT-004 | 10 · ACTIVIDADES | A06 |
| FR-ACT-005 | 10 · ACTIVIDADES | A06 |
| FR-ACT-006 | 10 · ACTIVIDADES | A06 |
| UT-SEC-011 | 11 · MATRIZ DE ACTIVIDADES | A06/A09 |
| FR-DATE-001 | 12 · FECHAS | A03/A06 |
| FR-DATE-002 | 12 · FECHAS | A03/A06 |
| FR-DATE-003 | 12 · FECHAS | A03/A06 |
| FR-DATE-004 | 12 · FECHAS | A03/A06 |
| FR-RESP-001 | 13 · RESPONSABLES | A07 |
| FR-RESP-002 | 13 · RESPONSABLES | A07 |
| FR-RESP-003 | 13 · RESPONSABLES | A07 |
| FR-RESP-004 | 13 · RESPONSABLES | A07 |
| FR-RESP-005 | 13 · RESPONSABLES | A07 |
| FR-REC-001 | 14 · RECURSOS | A08 |
| FR-REC-002 | 14 · RECURSOS | A08 |
| FR-REC-003 | 14 · RECURSOS | A08 |
| FR-REC-004 | 14 · RECURSOS | A08 |
| FR-REC-005 | 14 · RECURSOS | A08 |
| FR-MED-001 | 15 · MEDIOS DE VERIFICACIÓN | A08/A18 |
| FR-MED-002 | 15 · MEDIOS DE VERIFICACIÓN | A08/A18 |
| FR-MED-003 | 15 · MEDIOS DE VERIFICACIÓN | A08/A18 |
| FR-MAT-001 | 16 · FUENTE Y ELABORACIÓN DE MATRIZ | A07/A09 |
| FR-MAT-002 | 16 · FUENTE Y ELABORACIÓN DE MATRIZ | A07/A09 |
| UT-SEC-017 | 17 · NOTA DE DATOS PERSONALES | A10/A17 |
| FR-ANX-001 | 18 · ANEXOS | A10 |
| FR-ANX-002 | 18 · ANEXOS | A10 |
| FR-ANX-003 | 18 · ANEXOS | A10 |
| FR-ANX-004 | 18 · ANEXOS | A10 |
| FR-ANX-005 | 18 · ANEXOS | A10 |
| UT-SEC-019 | 19 · PREVISUALIZACIÓN T1 | A05/A17 |
| UT-SEC-020 | 20 · ENCABEZADO T1 | A17 |
| UT-SEC-021 | 21 · PORTADA T1 | A17 |
| UT-SEC-022 | 22 · PIE DE PÁGINA T1 | A17 |
| UT-SEC-023 | 23 · ÍNDICES | A17 |
| UT-SEC-024 | 24 · FIRMAS T1 | A11/A17 |
| UT-SEC-025 | 25 · HISTORIAL | A15/A17/A26 |
| UT-SEC-026 | 26 · FIRMA Y FINALIZACIÓN | A11/A16 |
| FR-SIGN-001 | 27 · FIRMA ELECTRÓNICA | A11/A30 |
| FR-SIGN-002 | 27 · FIRMA ELECTRÓNICA | A11/A30 |
| FR-SIGN-003 | 27 · FIRMA ELECTRÓNICA | A11/A30 |
| FR-SIGN-004 | 27 · FIRMA ELECTRÓNICA | A11/A30 |
| FR-SIGN-005 | 27 · FIRMA ELECTRÓNICA | A11/A30 |
| FR-REV-001 | 28 · REVISIÓN DOCUMENTAL | A13 |
| FR-REV-002 | 28 · REVISIÓN DOCUMENTAL | A13 |
| FR-REV-003 | 28 · REVISIÓN DOCUMENTAL | A13 |
| UT-SEC-029 | 29 · OBSERVACIONES | A13 |
| UT-SEC-030 | 30 · RESALTADO | A13 |
| UT-SEC-031 | 31 · DEVOLUCIÓN | A15 |
| UT-SEC-032 | 32 · VERSIÓN FORMAL | A15 |
| FR-FLOW-001 | 33 · FLUJOS INSTITUCIONALES | A13 |
| FR-FLOW-002 | 33 · FLUJOS INSTITUCIONALES | A13 |
| FR-FLOW-003 | 33 · FLUJOS INSTITUCIONALES | A13 |
| FR-FLOW-004 | 33 · FLUJOS INSTITUCIONALES | A13 |
| FR-FLOW-005 | 33 · FLUJOS INSTITUCIONALES | A13 |
| UT-SEC-034 | 34 · PROCEDIMIENTO EXTERNO / QIPOC | A22 |
| UT-SEC-035 | 35 · INFORME T2 | A20 |
| UT-SEC-036 | 36 · CREACIÓN DEL INFORME | A20 |
| UT-SEC-037 | 37 · INFORME DERIVADO | A20 |
| UT-SEC-038 | 38 · SECCIONES T2 | A20/A17 |
| UT-SEC-039 | 39 · DESARROLLO DE ACTIVIDADES DEL INFORME | A18/A19/A20 |
| UT-SEC-040 | 40 · NOTAS INSTRUCTIVAS T2 | A17/A20 |
| UT-SEC-041 | 41 · PAGECOUNT | A17 |
| UT-SEC-042 | 42 · EJECUCIÓN DE ACTIVIDADES | A18 |
| UT-SEC-043 | 43 · AUTORIZACIÓN DE ACTIVIDADES | A18/A30 |
| UT-SEC-044-INTRO | 44 · EVIDENCIAS | A18 |
| FR-EVI-001 | 44 · EVIDENCIAS | A18 |
| FR-EVI-002 | 44 · EVIDENCIAS | A18 |
| FR-EVI-003 | 44 · EVIDENCIAS | A18 |
| FR-EVI-004 | 44 · EVIDENCIAS | A18 |
| FR-EVI-005 | 44 · EVIDENCIAS | A18 |
| UT-SEC-045 | 45 · FECHA LÍMITE DE EVIDENCIA | A03/A18 |
| UT-SEC-046 | 46 · ESTADOS DE EVIDENCIA | A18 |
| UT-SEC-047 | 47 · VALIDACIÓN DE EVIDENCIA | A18 |
| UT-SEC-048 | 48 · REEMPLAZO DE EVIDENCIA | A18 |
| UT-SEC-049 | 49 · ADMINISTRACIÓN | A23/A24 |
| UT-SEC-050 | 50 · USUARIOS | A01/A23 |
| UT-SEC-051 | 51 · NOTIFICACIONES | A25 |
| UT-SEC-052 | 52 · AUDITORÍA | A26 |
| UT-SEC-053 | 53 · REPORTES | A27 |
| UT-SEC-054 | 54 · HISTÓRICO | A26 |
| UT-SEC-055 | 55 · CIERRE DE PERÍODO | A26/A32 |
| UT-SEC-056 | 56 · ESTADOS VACÍOS Y ERRORES | A14/A22/A29 |
| UT-SEC-057 | 57 · ACCIONES EN TABLAS | A29 |
| UT-SEC-058 | 58 · IDENTIDAD VISUAL | A29 |
| UT-SEC-059 | 59 · RESPONSIVIDAD | A29 |
| UT-SEC-060 | 60 · REGLA FINAL | A01–A32 |
| FR-DOC-A4-001 | 61 · DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL | A28/A32 |
| FR-TPL-001 | 61 · DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL | A28/A32 |
| FR-TPL-002 | 61 · DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL | A28/A32 |
| FR-ARCH-AI-001 | 61 · DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL | A28/A32 |
| FR-ARCH-DB-001 | 61 · DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL | A28/A32 |
