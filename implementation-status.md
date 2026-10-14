# Estado de implementación productiva

## Estado vigente al 2026-10-13

La tanda amplia incorpora un circuito documental de desarrollo integrado. No equivale a completar todos los apartados del Plan Maestro ni a aprobación institucional para producción. Los apartados anteriores conservados debajo son registros históricos de entregas parciales; la tabla y límites de esta sección expresan el estado actual.

| Capacidad | Alcance entregado |
|---|---|
| Organización | Edición/desactivación de carreras con concurrencia, historial visible de cargos/principales, recuperación delegada explícitamente a coordinadores; catálogo de carreras, ámbitos grupo/carrera/período, integrantes/cargos y responsable principal. Titularidad y unicidad de nuevos T1 por ámbito; acceso explícito, transferencia y concurrencia. |
| T1 | Editor con matriz y anexos, artefactos A4 de plantilla institucional, firma real del elaborador y envío a revisión atómico en documentos por ámbito. |
| Revisión | Bandeja propia paginada con búsqueda/filtros/indicadores personales, tareas secuenciales, observaciones generales y resaltados, devolución y rondas independientes de versión formal, aprobación y descarga final autorizada. |
| Firmas sucesivas | Certificados personales temporales, vinculación pública por usuario, append incremental y comprobación de CMS/bytes previos. Sin material privado persistente ni afirmación de cadena/revocación/TSA. |
| Ejecución | PDF privado o enlace HTTPS versionado según el medio congelado en matriz; referencias textuales en T2 y manifiesto histórico, permisos del responsable asignado, fechas personales, cobertura e historial de evidencias. |
| Prórrogas | Coordinador del ámbito, destinatario individual, fecha original conservada, concurrencia e historial. Máximo técnico ajustable de tres días calendario dentro del período, pendiente de ratificación institucional. |
| T2 | Informe independiente derivado de T1 aprobado, narrativas, porcentaje manual, observaciones, delegaciones opcionales, anexos/evidencias y plantilla oficial A4; firma y revisión compartidas. |
| Versiones y archivo | Archivo lógico con motivo y familia formal T1/T2. Una actualización de aprobado crea nuevo ID/DRAFT/ronda 1 y conserva el anterior firmado. Una familia de Informe por Plan, una versión activa. |
| Flujos temporales | Plantillas generales, revisión asignada a cada ámbito y T1/T2, guardado atómico y activación explícita del período. Participantes congelados, sin sustitución silenciosa por cambios de plantilla. Nuevas firmas requieren activación. |
| Cierre e historial | ACTIVE→CLOSED con motivo, concurrencia, ZIP privado verificado y archivo sin purga. Revisión pendiente conserva estados/bytes en solo lectura. Descarga histórica filtrada por permisos y por aprobación de PDFs. |
| Avisos | Programador de vencimiento/evidencia pendiente con fechas personales, períodos vigentes y deduplicación persistente; avisos privados de restablecimiento de acceso. |
| Seguimiento | Filtros y CSV de totales/estados/cobertura con permisos. ADMIN ve agregados sin acceso automático a contenido privado. |
| Desarrollo | Entorno y semillas aislados en utaped_dev, siete usuarios ficticios, configuración y pruebas sin modificar la DB principal. |
| Importación | CSV administrativo UTF-8 prevalidado por fila, lotes atómicos, cuentas USER con contraseña aleatoria individual de descarga única y pertenencia opcional sin principal/permisos de recuperación; caducidad y limpieza de filas temporales. |
| CAPTCHA accesible | Mismo reto visual y audio español privado, teclado, reproducción/pausa/repetición, ID/sesión/caducidad/consumo únicos, sin proveedor externo. Aceptación de pronunciación con usuarios pendiente. |
| Carga documental | Escenarios aislados de tres autores/ámbitos, T1 extensos y T2 con evidencia paginada: generación, rasterizado, descarga, conflictos de evidencia y firmas concurrentes. Métricas por fase; no acredita capacidad institucional sostenida. |
| Acceso temporal | Cambio obligatorio de contraseña mantiene el acceso con una sesión nueva; revoca sesiones previas y enlaces. El cambio voluntario normal sigue cerrando las sesiones. |
| Interfaz | Se conserva la modernización del propietario y se amplía con tarjetas, métricas, editores por pasos, feedback toast, modo oscuro y adaptación a PC/tableta. |
| Redacción | Propuestas T1/T2 con consentimiento, comparación, aplicar/descartar y protección ante cambios locales/versiones. Groq/OpenAI detrás de interfaz backend; deshabilitado por defecto. El propietario configuró proveedor/modelo/clave en sus entornos; aceptación de privacidad/calidad institucional pendiente. |
| Documentos extensos | Navegación directa por página, firmas/anexos/observaciones y estado de carga. Aceptación T1 extenso y T2 de 18 páginas con evidencia/índice/slots/firmas e integridad del Plan base. |
| Operación | Override HTTPS 64430 con servicios internos sin publicar, SMTP externo configurable y preflight. Respaldo conjunto DB/volumen/imágenes, validación y restauración en proyectos nuevos; ensayo completo con datos ficticios. No desplegado institucionalmente. |

Persistencia: Flyway V13–V22 y almacenamiento privado real. Los borradores individuales anteriores conservan sus IDs; se prohíbe crear nuevos por esa ruta fuera de fixtures de prueba. API: [circuito documental](docs/api/document-circuit.md) y [períodos/archivo](docs/api/period-control.md). Escenario: [entorno de ejemplo](docs/development/sample-environment.md). Evidencia: [circuito](docs/reports/2026-10-12-01-large-document-circuit.md) y [cierre y flujos](docs/reports/2026-10-12-02-period-closure-and-workflows.md). Administración/alertas: [contrato](docs/api/contextual-administration.md) y [reporte](docs/reports/2026-10-12-03-contextual-administration-and-notices.md).

Importaciones/enlaces: [contrato](docs/api/user-imports-and-external-evidence.md) y [reporte](docs/reports/2026-10-12-04-user-imports-and-external-evidence.md). Validación de esta tanda: 116 ejecuciones de pruebas de backend sin fallos, ocho escenarios E2E, lint/build y revisión visual de interfaces/T2.

Redacción/bandeja: [contrato](docs/api/writing-and-review-inbox.md), [configuración](docs/development/writing-configuration.md) y [reporte](docs/reports/2026-10-12-05-writing-assistance-and-review-inbox.md). Validación vigente: 129 ejecuciones backend en 19 suites sin fallos, cinco E2E generales y cuatro escenarios del entorno de ejemplo, lint/build y revisión visual PC/tableta. Transporte IA con fixtures locales; no se consumió una cuenta externa. El agente de código/Trello se desarrolla aparte por el propietario.

Aceptación/operación: [escenarios](docs/testing/role-and-document-acceptance.md), [herramientas](docs/operations/production-readiness.md) y [reporte](docs/reports/2026-10-12-06-role-acceptance-and-operational-readiness.md). Validación actual: 131 ejecuciones backend en 19 suites, diez escenarios E2E y siete pruebas de herramientas, sin fallos finales; lint/build y revisión visual. Recuperación de 57 documentos/156 archivos e imágenes originales; HTTPS con certificado temporal, cookie Secure, login y PDF firmado con hash intacto. Cuarenta consultas de salud concurrentes no acreditan dimensionamiento institucional.

Accesibilidad/carga (2026-10-13): [contrato](docs/api/local-access.md), [escenarios reproducibles](docs/testing/captcha-and-document-load.md) y [reporte](docs/reports/2026-10-13-01-accessible-captcha-and-document-load.md). Validación: 135 ejecuciones backend en 20 suites, siete E2E generales y cinco de ejemplo; dos escenarios de audio repetidos sobre Nginx, sin fallos finales. Lint/build y revisión visual PC/tableta. No se modificaron datos principales, proveedores ni variables privadas. La concurrencia de firma se prueba en servicios reales con fixture de transporte seguro; no es carga de certificados por HTTPS.

### Trabajo que aún falta para completar el producto

- Política institucional de retención/reapertura y permisos contextuales de Decano/Subdecano; exportación histórica en streaming para volúmenes mayores al límite técnico inicial de 128 MiB. El ZIP no sustituye un respaldo restaurable.
- Política frente a bajas/cambio de ejecutores de flujos ya activados, sin invalidar asignaciones y firmas históricas.
- Administración avanzada de ámbitos, catálogo completo de cargos/fechas de vigencia, pertenencias globales por carrera y alta/importación delegada. Edición de carreras, historial visible de cargos/prórrogas y recuperación contextual ya están entregados.
- Archivos adjuntos no PDF y formatos documentales oficiales adicionales: pendientes de especificación/plantillas. Referencias HTTPS ya entregadas, sin descarga ni comprobación remota de disponibilidad/integridad.
- Importación delegada, actualización de cuentas por archivo y distribución institucional de credenciales aún no entregadas. CSV administrativo ya implementado. Canales/frecuencia institucional y correo de recordatorios pendientes.
- Aceptación de tratamiento de datos, presupuesto y calidad real de redacción. El propietario configuró Groq/modelo/clave en el entorno principal y agregó variables al ejemplo; se verificó una llamada real con texto ficticio en la revisión anterior. Las pruebas automatizadas controlan sus respuestas y no consumen la cuenta. El agente de código/Trello lo construye el propietario por separado; queda fuera de este producto.
- Aceptación del audio con usuarios, necesidades que no cubre imagen/audio, prueba de carga documental sostenida y del transporte de firma HTTPS en infraestructura institucional. Carga acotada e integridad ya comprobadas; no se fija SLA. La API anterior de tareas conserva límite 200 por compatibilidad.
- Ratificación de ventanas T2, días de prórroga, múltiples elaboradores y secuencias/personas oficiales; los ejemplos no reemplazan esas decisiones.
- Insumos reales HTTPS/DNS/CA/firewall y SMTP institucional, custodio/cifrado/frecuencia/retención/RPO/RTO de respaldos y despliegue operativo. Las herramientas y restauración técnica de ejemplo ya están comprobadas; todavía se requiere ensayo/aceptación institucional. Puerto web principal 64430; el override productivo requiere selección explícita.

Próxima etapa propuesta: recorrido de aceptación con el cliente, evaluación del audio con usuarios y ajuste de incidencias; cerrar reglas institucionales pendientes y validar carga/insumos reales de operación con DTIC. El agente externo no bloquea estas tareas. No se declara terminado el producto institucional.

## Registro histórico de entregas

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

## Actualización 2026-10-07: contraseña y toast

Diseño de cambio de contraseña del propietario revisado y registrado por separado. Avisos operativos/error/advertencia convertidos a toast común en los módulos actuales; conserva validaciones de campos y confirmaciones de acciones sensibles. Tres escenarios E2E, lint/build y capturas de escritorio/móvil verificados. [Reporte](docs/reports/2026-10-07-01-password-layout-and-toasts.md). Sigue en el PR #10 abierto hacia develop.

## Actualización 2026-10-07: temas y destinos de interfaz

Modo claro/oscuro con selector iconográfico flotante, tooltip, preferencia del sistema, persistencia local y sincronización entre pestañas. Documentos institucionales/CAPTCHA sin inversión de colores. Destinos vigentes: PC y tablet, sin nuevos estilos específicos de celular. Lint/build y cinco escenarios E2E verificados. [Reporte](docs/reports/2026-10-07-02-color-themes.md). Por instrucción actual del propietario solo se realiza commit local; no se actualiza el PR ni se hace push hasta nueva indicación.

## 2026-10-07 — Espacio administrativo independiente

- ADMIN inicia en Panel general: usuarios registrados/activos, grupos activos, períodos registrados y eventos administrativos recientes, consultados en APIs existentes.
- Navegación y renderizado independiente de usuarios, grupos/pertenencias, períodos, actividades, recursos/medios, feriados, flujos, auditoría y certificados. Directorio de usuarios y perfiles de grupos separados.
- Inicio personal y documentos separados del espacio administrativo. Una cuenta ADMIN con pertenencia activa puede cambiar de espacio sin cambiar de identidad ni ampliar los permisos del servidor.
- Protección de formularios sin guardar al navegar, cerrar sesión, recargar y cambiar de espacio; guardado de un formulario conserva la advertencia de otros formularios pendientes.
- Sin cambios de backend, migraciones, variables de entorno ni estilos nuevos para teléfonos. Carreras/cargos, plantillas administrables, reportes globales y ejecución de revisión/aprobación continúan pendientes conforme al alcance anterior.
- Cambios locales sin commit, push ni actualización del PR, por instrucción expresa. Validación detallada en el reporte de esta entrega.

## 2026-10-07 — Proxy persistente de desarrollo

Vite obtiene automáticamente el puerto del backend desde `BACKEND_PORT` del `.env` principal; `API_TARGET` de la terminal o `frontend/.env.local` permite un destino explícito. Sin configuración conserva 8080, el predeterminado de Compose. Se agrega ejemplo seguro y documentación de reinicio/arranque de servicios. No se modifican puertos Docker ni variables privadas del propietario. Sin commit.
