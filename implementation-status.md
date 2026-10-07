# Estado de implementación productiva

Inicio: 2026-10-02. Actualización: 2026-10-06. PR #1–8 fusionados; base develop 248eeba. Entrega actual en feature/modern-workspace-access, con commits por bloque y un PR hacia develop.

El Plan Maestro del 2026-10-06 y la decisión posterior de rediseñar la interfaz prevalecen sobre las decisiones históricas. Las secciones anteriores fechadas se conservan como historial de entregas, no como preguntas institucionales aún abiertas cuando el Plan Maestro ya las resolvió. No se declara finalizado el proyecto.

| Área | Estado | Evidencia |
|---|---|---|
| Documentación inicial | Disponible; pendientes institucionales registrados | docs/requirements y docs/validation. |
| Stack productivo | Seleccionado e implementado | ADR-0001, manifests y Dockerfiles. |
| Credenciales locales | Implementado en alcance actual | Usuario/correo, CAPTCHA real, Argon2id y migración de bcrypt al login correcto, CSRF, sesión JDBC, cambio obligatorio, recuperación SMTP y reset ADMIN. Coordinador espera ámbitos. |
| Interfaz productiva | Base moderna implementada | Diseño propio, tokens/iconos, acceso, menú lateral, panel con datos reales, formularios/tablas, accesos administrativos y adaptación escritorio/tableta/móvil. Validación estética final del cliente pendiente. |
| Usuarios y permisos | Primera entrega implementada | Bootstrap, alta, directorio paginado, edición de nombre/permiso y activación con concurrencia; revocación y protección último admin. |
| Grupos y pertenencias | Primera entrega implementada | Alta/listado, edición/activación con concurrencia, MEMBER/COORDINATOR, asignación/retiro y aislamiento por objeto. |
| Períodos | Alta/consulta, rangos y ventana de elaboración de borradores | Fechas civiles inclusivas en zona configurable; cierre definitivo/reapertura pendientes. |
| Auditoría | Visor e historial documental inicial implementados | Filtros, paginación, nombres capturados para eventos nuevos y autorización por Plan; exportación CSV filtrada implementada; política de retención institucional pendiente. |
| Pruebas | Backend PostgreSQL y E2E de identidad ejecutados | docs/reports/2026-10-03-01-project-foundation.md. |
| CI | Workflow frontend/backend/E2E disponible | Base publicada y fusionada; cambios actuales verificados localmente, ejecución remota pendiente de publicación. |
| Notificaciones | Canal interno inicial implementado | Bandeja privada, contador, lectura y navegación con autorización; avisos de pertenencia y previsualización, sin correo/push. |
| Seguimiento personal | Panel inicial implementado | Contadores, filtros, distribución y documentos recientes bajo propiedad/pertenencia; sin métricas ficticias de aprobación. |
| Borradores T1 | Parcial; modelo pendiente de reconciliación | Persistencia/aislamiento/concurrencia implementados. Titularidad individual y carrera libre contradicen el nuevo modelo de comisión/carrera/período; migración pendiente, no habilitar como modelo institucional definitivo. |
| Actividades y matriz T1 | Primera entrega implementada | Catálogos por grupo, obligatorias/opcionales/Otra, fechas, responsables múltiples/colectivos, recursos/medios, fuente, resumen y persistencia atómica. |
| Catálogos y feriados | Administración inicial implementada | Altas, edición/desactivación de actividades/recursos/medios; denominación colectiva; feriados y restricción configurable por período. |
| Configuración de flujos | Primera entrega administrativa implementada | Etapas ordenadas T1/T2 por grupo; personas/roles/órganos, requerimiento de firma, borrador, revisiones inmutables y deshabilitación. No ejecuta revisión documental. |
| Anexos T1 | Implementados en alcance inicial | PDFs privados, sí/no, metadata, orden, etiquetas automáticas, baja lógica y concurrencia. |
| Preparación/previsualización T1 | Implementada en alcance inicial | Plantilla DOCX oficial, páginas T1 A4, matriz horizontal, índices reales, snapshot/PDF inmutables y visor paginado. |
| Preparación de firma | Implementada con bloqueo institucional | Snapshot de flujo/participantes, integridad y bloqueos por artefacto. Sin carga de secretos ni transiciones. |
| Núcleo criptográfico .p12/.pfx | Implementado; firma visible del elaborador conectada | CMS separado real RSA/EC, preservación incremental, comprobación de huella pública vinculada, vigencia e integridad. Modo PKIX interno disponible; la ruta de firma usa confianza directa por huella y declara cadena/revocación no comprobadas. |
| Firma visible del elaborador T1 | Primera entrega implementada | Administración vincula huella pública con evidencia de identidad; usuario selecciona archivo/contraseña. PDF independiente e inmutable, visor/descarga privados, ubicación real, reintentos idempotentes y auditoría. Sin almacenamiento de secretos. |
| T1 completo/T2, revisión, evidencias | Pendientes en producto | La firma del artefacto no finaliza ni envía el Plan; DRAFT permanece editable separado del firmado. Firmas de revisores, T2 y evidencias aún no entregados. |
| SMTP institucional/HTTPS/operación | Pendiente | Mailpit captura correo local; Compose local no es publicación institucional. |
| JPA/API v1/OpenAPI/Testcontainers | Pendiente de transición | Los módulos existentes siguen con JDBC y pruebas PostgreSQL aisladas por Compose. No se atribuye cumplimiento a tecnologías aún no incorporadas. |

## Espacio de trabajo y acceso — entrega 2026-10-06

UT-AUTH-020–024 y UT-UI-020 implementados en el alcance de [ADR-0003](docs/architecture/adr/0003-modern-workspace-access.md). Fuentes: Plan Maestro §§6–7 y decisión visual del propietario. Se incorporan sus plantillas/documentos como referencias, sin importar personas o permisos incompletos ni modificar Prototipos.

- Alta con nombres/apellidos y username normalizado, directorio con búsqueda por identificador y perfiles compatibles con registros históricos. V12 deriva usernames existentes y conserva nombres sin inferir su desglose.
- Hashes nuevos Argon2id; bcrypt previo sigue validándose y se actualiza después de credenciales correctas, con protección frente a cambio concurrente.
- CAPTCHA PNG real: seis caracteres, tres minutos, sesión, consumo atómico e invalidación al regenerar; sin bypass ni respuestas en claro. URL única para evitar reutilización de imágenes. Mantiene rate limiting y CSRF.
- Reset ADMIN con concurrencia, confirmación UI, contraseña temporal, revocación de sesiones/enlaces y auditoría sin secretos. El permiso de coordinador sigue pendiente del nuevo ámbito autorizado.
- Nueva base visual común para módulos existentes, menú/contexto/identidad, panel real y estados vacíos. Los períodos se expanden sin perder historial. Se conserva advertencia al abandonar cambios y se ignoran respuestas de identidad anteriores al logout.
- Login tras logout/expiración espera el nuevo CSRF antes de montar el reto, evitando sesiones anónimas paralelas. Una cuenta revocada no deja bloqueado el navegador para acceder con otra identidad.

Verificaciones: frontend lint/build, 59 pruebas backend con PostgreSQL aislado y tres escenarios E2E completos sobre Docker/Nginx. Se corrigieron fixtures de fecha que mezclaban UTC y America/Guayaquil sin debilitar controles. Capturas revisadas en escritorio/tableta/móvil y regresión de planificación, documentos, firma, administración y notificaciones. [Reporte nuevo](docs/reports/2026-10-06-01-modern-workspace-access.md).

Próxima implementación: carreras y pertenencias por período/cargo, responsable principal, titularidad y unicidad por comisión/carrera/período, archivo de borradores y conservación histórica. Después: workflow secuencial, tareas/observaciones/rondas, múltiples firmas y firma→envío atómico; luego ejecución/evidencias y T2. El rediseño no concede permisos ni resuelve esas transiciones por apariencia visual.

## Trazabilidad

FR-AUTH-001/003/004/005 cubiertos en esta entrega; FR-AUTH-002 parcialmente: contraseñas temporales para altas administrativas, sin importación masiva. FR-GRP-001/002/003 cubiertos; FR-GRP-006 parcial: MEMBER/COORDINATOR, no roles personalizados. FR-PER-001/002 cubiertos para alta/consulta; FR-PER-003 parcial hasta edición/configuración posterior. FR-ARCH-DB-001 implementado.

Directorio paginado y edición de perfiles están implementados. Administración completa, roles personalizados, importación y políticas institucionales de períodos/retención aún no se entregan. Las reglas institucionales pendientes no se consideran resueltas por la existencia de tablas o pantallas.

## Entrega de borradores T1

Commit de esta entrega autorizado el 2026-10-03. El nuevo agente de inspección de proyecto/Trello está registrado como requisito, sin implementación ni cambios al esquema documental: [alcance y propuestas](docs/requirements/project-advisor-agent.md).

FR-T1-001 parcial: identidad autenticada, grupo y período; unidad y carrera son texto manual mientras no exista catálogo institucional. FR-T1-004 registra fecha civil al crear; FR-T1-005 depende de finalización futura. FR-T1-CONT-001/002 disponen de un campo persistido por documento, permitiendo valores incompletos en borrador. No se afirma que el asistente T1 completo esté implementado.

Q-001 y Q-002 siguen pendientes: no se redefine el número de pasos; hay dos secciones de edición inicial. Unicidad por docente/grupo/período configurable y deshabilitada provisionalmente. La idempotencia de una solicitud no equivale a unicidad institucional.

Validación anterior: frontend build/lint, 15 pruebas backend con PostgreSQL aislado y E2E ampliado con Vite y Nginx/Docker. Reporte: [borradores T1](docs/reports/2026-10-03-03-work-plan-drafts.md).

## Actividades y matriz T1

FR-ACT-001–006, FR-DATE-001–004, FR-RESP-001–005, FR-REC-001–005, FR-MED-001–003, FR-MAT-001/002 y UT-SEC-011 cubiertos en alcance inicial de borradores. No constituye aceptación institucional de catálogos/feriados; los datos deben configurarse por administración. Se conservan snapshots de selección y se valida pertenencia activa al volver a guardar. No hay PDF ni evidencia en este módulo.

La matriz comparte el contador técnico del Plan y no permite sobrescrituras entre secciones o sesiones. Obligaciones nuevas se incorporan solo al borrador editable, sin reescribir selecciones ya guardadas; solo lectura muestra exclusivamente la matriz guardada.

Q-001/Q-002 siguen pendientes; no se ha fijado el asistente completo ni cambiado la unicidad provisional. El agente interno código/Trello sigue documentado y no implementado.

Evidencia anterior: [reporte de actividades y matriz](docs/reports/2026-10-03-05-work-plan-activities.md); entrega fusionada por PR #3. Anexos y preparación documental se incorporan en la entrega siguiente descrita abajo.

## Anexos y preparación documental T1

FR-ANX-001–005 implementados en alcance inicial. UT-SEC-017 parcial: condición explícita de nota de datos personales. UT-SEC-019 parcial hasta aceptación institucional; UT-SEC-020/022/023/025 cubiertos en previsualización inicial. UT-SEC-021 parcial con campos institucionales manuales; UT-SEC-024 parcial: solo elaborador conocido, sin firma ni workflow institucional supuesto.

El propietario confirmó almacenamiento local privado persistente con 10 MB/anexo y 20 anexos ajustables, y A4 manteniendo estructura/estilo. Q-010/Q-013 parcialmente resueltas; retención, aceptación final y operación institucional siguen pendientes. [ADR-0002](docs/architecture/adr/0002-t1-document-preparation.md), [API](docs/api/t1-document-preparation.md) y [respaldo](docs/development/document-storage.md).

Flyway V6 incorpora archivos privados, anexos y artefactos. PDF real validado, hash comprobado en lecturas, snapshot independiente y PDFs anteriores conservados tras editar/quitar anexos. pageCount y espacios de elaboración derivados de composición real. Generar no altera versión formal, estado ni contador de edición.

Validación: 22 pruebas backend con PostgreSQL aislado; frontend build/lint; E2E ampliado con Vite y Nginx/Docker. Revisión visual de las 8 páginas del escenario con anexos y 9 del escenario de contenido largo. [Reporte de entrega](docs/reports/2026-10-03-07-t1-document-preparation.md).

Siguen pendientes firma real, ejecución de flujos por grupo, revisión/observaciones, T2, evidencias y agente interno código/Trello. [Publicación autorizada](docs/reports/2026-10-03-08-t1-publication.md); revisión y fusión a cargo del propietario. Próxima entrega sugerida: configurar flujos por grupo y bandeja de revisión, definiendo primero Q-003/Q-007 y sin simular firmas productivas.



## Flujos, administración y trazabilidad

Configuración T1/T2 por grupo, borrador, etapas ordenadas, destinatarios personales/roles/órganos colegiados, revisiones técnicas inmutables y deshabilitación. FR-FLOW-001–005 cubiertos para configuración; asignación a documentos y ejecución institucional pendientes de Q-003/Q-007/Q-018. Configurar un flujo no equivale a aprobar un documento ni conceder acceso.

Directorio con búsqueda literal/paginación, filtros, perfiles de usuario/grupo y contadores de concurrencia. Las bajas son lógicas, conservan documentos y revocan el ámbito aplicable. Visor de eventos administrativo y registro del Plan con autorización exacta, sin acceso privilegiado a archivos ajenos. La exportación CSV administrativa filtrada está implementada; no existe política de retención institucional.

Evidencia: [reporte de entrega ampliada](docs/reports/2026-10-03-09-workflow-administration-audit.md), [flujos](docs/api/workflow-configuration.md), [directorio](docs/api/administrative-directory.md), [auditoría](docs/api/audit-history.md).

## Seguimiento, notificaciones y exportación

UT-SEC-051 parcial: bandeja interna privada, contador, lectura y navegación con autorización; primeros avisos de pertenencia y previsualización. Asignación/revisión/evidencias/vencimientos y canales externos dependen de módulos/políticas aún pendientes. Q-015/Q-017 no se consideran resueltas. Seguimiento personal usa datos reales de T1 y ventanas vigentes, sin ranking ni sanciones.

Exportación ADMIN de auditoría con filtros aplicados, todas las filas hasta un límite técnico ajustable, snapshot consistente, control de acceso, representación segura de valores de texto y registro de descarga. No exporta PDF ni cambia retención; no constituye un reporte institucional definitivo.

[Reporte de entrega](docs/reports/2026-10-03-10-tracking-notifications-audit-export.md). Siguiente paso recomendado: acordar contrato de firma real y reglas de asignación/transición por grupo para implementar finalización T1 y revisión sobre artefactos exactos. Q-003/Q-007/Q-018 bloquean ese comportamiento; T2/evidencias se construyen después sobre decisiones y artefactos preservados.

## Preparación de firma y snapshots de flujo — entrega 2026-10-04

Mecanismo .p12/.pfx confirmado por el propietario el 2026-10-04; flujos aún no ratificados. Nuevos artefactos fijan revisión vigente T1, etapas y participantes/nombres/actividad resueltos, conservando órganos sin representantes personales cuando así están configurados. Cambios de configuración o participantes afectan vigencia de preparación y nunca reescriben PDFs anteriores. Consulta/UI por documento y artefacto explícitos, con integridad verificada y acceso del elaborador vigente, sin bypass ADMIN ni acceso de futuros revisores.

El núcleo interno genera y verifica una firma CMS real sin recomponer el PDF, exige huella pública y raíces confiables explícitas y borra los arrays de contenedor/contraseña en éxito/fallo. No se conserva ninguna referencia en servicio persistente. La destrucción completa de copias internas JCA/JVM no puede garantizarse. No está conectado a HTTP, almacenamiento, asignaciones, finalización ni notificaciones.

FR-SIGN-* permanece parcial/no habilitado en producto: vinculación real de cuenta/certificado, revocación, TSA/perfil, representación visible y múltiples firmas no están entregados. Q-003/Q-018 no se cierran; no se agrega ningún estado distinto de DRAFT. [Contrato](docs/decisions/signature-review-contract.md), [ficha de validación](docs/validation/workflow-signature-confirmation.md) y [reporte de entrega](docs/reports/2026-10-04-01-signature-core-workflow-snapshots.md).

## Firma visible del elaborador — entrega 2026-10-05

La entrega actual conecta el núcleo a firma/consulta/descarga privadas, con huella pública administrativa, comprobación básica y ubicación visible derivada. El PDF firmado queda separado del borrador editable y conserva bytes/hashes históricos. FR-SIGN-001–005 son PARCIAL: se entrega elaborador, no múltiples firmantes o aceptación institucional. Cadena/revocación se declaran no comprobadas. HTTPS por defecto, sin persistir secretos; entorno HTTP solo explícito local. Continúan DRAFT y los bloqueos de finalización/envío/aprobación. [API](docs/api/author-signing.md) y [reporte](docs/reports/2026-10-05-01-visible-author-signing.md).
