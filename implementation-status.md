# Estado de implementación productiva

Revisión de entrega: documentos institucionales, acciones de bandeja y mejoras de notificaciones verificados para cierre en commits locales. [Reporte](docs/reports/2026-10-23-17-staged-review-and-commits.md).

Acciones documentales visibles: descarte de borradores y archivo de aprobados desde la tabla, modal con motivo, control de concurrencia y permisos derivados del servidor. [Reporte](docs/reports/2026-10-23-16-document-list-lifecycle-actions.md).

Fidelidad documental: revisión de staged/worktree, encabezado institucional, índices con páginas y enlaces reales, matriz T1 completa y previsualización nítida. T1/T2 engine12 y T3 engine8 para composiciones nuevas. [Reporte](docs/reports/2026-10-23-15-institutional-document-fidelity-review.md).

Encabezado T1/T2: etiqueta de fecha a la izquierda y valor a la derecha; portada con un único tipo de unidad según procedencia, engine10. [Reporte](docs/reports/2026-10-23-13-header-date-value-and-unit-label.md).

Encabezado T1/T2: Fecha de elaboración conserva dos celdas y divisor vertical según el original; engine9 para composición nueva, sin reescribir firmados. [Reporte](docs/reports/2026-10-23-12-header-date-cells-and-commits.md).

Procedencia institucional: catálogo académico/administrativo, carreras y ámbitos asociados, selección de catálogo e información heredada bloqueada en Planes/Informes; V34 preserva registros y artefactos. [Reporte](docs/reports/2026-10-23-11-institutional-provenance-catalog.md).

Paso 5 T1: previsualización automática con reutilización del artefacto vigente, pendientes y reintento; conserva documentos no editables y bloquea avance incompleto a firma. [Reporte](docs/reports/2026-10-23-10-automatic-plan-preview.md).

Responsables del resumen: nombres agrupados en un único badge por celda; representación colectiva conservada. [Reporte](docs/reports/2026-10-23-09-single-responsibles-badge.md).

Ajuste visual de matriz: Plazo en una línea y responsables con etiquetas compactas; detalle colectivo en tooltip. [Reporte](docs/reports/2026-10-23-08-matrix-column-presentation.md).

Responsables colectivos: etiquetas de ejemplo según tipo de grupo, reparación limitada de semillas antiguas y trazabilidad individual visible en resumen. Revisión de mejoras visuales y navegación secuencial. [Reporte](docs/reports/2026-10-23-07-collective-responsibility-and-ui-review.md).


Navegación secuencial T1: Anexos → Preparación → Firma con regreso al paso anterior y protección de cambios. Compilación/lint y recorrido interactivo mediante Playwright aprobados. [Reporte](docs/reports/2026-10-23-06-plan-step-navigation.md).


## Revisión de interfaz — 2026-10-23

Mejoras del propietario en documentos, matriz, formularios y notificaciones revisadas e integradas con correcciones de teclado/foco/solo lectura, filtros, tokens de color y protección de la selección de elaboradores. [Reporte](docs/reports/2026-10-23-05-ui-review-and-hardening.md).

## Validación de avance desde matriz — 2026-10-23

Siguiente: Anexos exige al menos una actividad completa incluso sin cambios pendientes; toast y foco contextual, sin perder el borrador. Guardar una matriz vacía continúa disponible. [Reporte](docs/reports/2026-10-23-04-matrix-step-validation.md).

## Escenarios documentales preparados — 2026-10-23

Carga opcional y repetible en desarrollo: Plan editable, Plan en revisión, Plan aprobado con evidencia e Informe en borrador. Artefactos reales, firmas autofirmadas de ejemplo sin persistir claves, servicios normales y conservación de documentos/credenciales existentes. Ya cargados en utaped-sample. [Reporte](docs/reports/2026-10-23-03-prepared-example-documents.md).

## Usabilidad del paso 3 — 2026-10-23

Resumen de matriz antes del detalle, campos pendientes, acceso al editor por actividad y navegación directa a Anexos con guardado de cambios y conservación ante errores. PC/tablet, temas y teclado. [Reporte](docs/reports/2026-10-23-02-matrix-overview-and-navigation.md).

## Corrección documental — 2026-10-23

Encabezado T1/T2 con proporciones institucionales, escudo sin reducción, altura de filas y separación explícita antes del contenido. Se conserva A4, paginación dinámica y bytes históricos; las nuevas composiciones usan engine8. [Reporte](docs/reports/2026-10-23-01-institutional-document-spacing.md).

## Estado vigente al 2026-10-21

La tanda amplia incorpora un circuito documental de desarrollo integrado. No equivale a completar todos los apartados del Plan Maestro ni a aprobación institucional para producción. Los apartados anteriores conservados debajo son registros históricos de entregas parciales; la tabla y límites de esta sección expresan el estado actual.

| Capacidad | Alcance entregado |
|---|---|
| Organización | Edición/desactivación de carreras con concurrencia, historial visible de cargos/principales, recuperación delegada explícitamente a coordinadores; catálogo de carreras, ámbitos grupo/carrera/período, integrantes/cargos y responsable principal. Titularidad y unicidad de nuevos T1 por ámbito; acceso explícito, transferencia y concurrencia. |
| Creación documental | Modal de creación Plan de Trabajo / Informe de ejecución / Informe final, título propuesto por grupo/unidad y backend automático, búsqueda/paginación de Planes aprobados autorizados; crear o abrir Informe existente. Foco contenido, Escape/cancelación protegidos, selección conservada al filtrar. Históricos intactos; código individual pendiente de definición. |
| T1 | Editor con matriz y anexos, artefactos A4 de plantilla institucional, firmas reales sucesivas de los elaboradores y envío a revisión atómico tras la última firma en documentos por ámbito. |
| Estructura documental | ADMIN publica versiones de secciones/orden y apartados adicionales con texto inicial/obligatoriedad para T1/T2/T3. Cada documento conserva su estructura y contenido; los nuevos cambios no reescriben históricos. Se protegen encabezados, tablas oficiales y firmas/control final. |
| Plantillas de texto T1 | ADMIN configura/restaura la justificación inicial con versión, auditoría y formulario por acción. Se copia solo en Planes nuevos; históricos y blancos intencionales permanecen intactos. V23; [contrato](docs/api/document-templates.md). |
| Vista simultánea T1 | Paso Contenido con formulario y página real A4 en PC, ancho ajustable, alternancia en tablet, ampliación y reintento. Textos locales sin autosalvado ni artefacto firmable; mismo motor, contenido incompleto permitido y matriz vacía sin actividad ficticia. Endpoint privado con control de versión/estado/permisos y caché acotada en memoria. [Reporte](docs/reports/2026-10-17-03-simultaneous-t1-preview.md). Matriz como editor simultáneo y T2 pendientes de extensión. |
| Revisión | Bandeja propia paginada con búsqueda/filtros/indicadores personales, tareas secuenciales, observaciones generales y resaltados, devolución y rondas independientes de versión formal, aprobación y descarga final autorizada. |
| Firmas sucesivas | Certificados personales temporales, vinculación pública por usuario, append incremental y comprobación de CMS/bytes previos. Sin material privado persistente ni afirmación de cadena/revocación/TSA. |
| Ejecución | PDF privado o enlace HTTPS versionado según el medio congelado en matriz; referencias textuales en T2 y manifiesto histórico, permisos del responsable asignado, fechas personales, cobertura e historial de evidencias. |
| Prórrogas | Coordinador del ámbito, destinatario individual, fecha original conservada, concurrencia e historial. Máximo técnico ajustable de tres días hábiles (fines de semana/feriados registrados excluidos) dentro del período. Reapertura de elaboración por ámbito y coordinador, con motivo y versión. Cinco días hábiles iniciales si no se indica fecha final; fechas explícitas preservadas. Ratificación institucional pendiente. |
| Informe de ejecución | Tipo interno T3; plantilla oficial UTA-SGC-A-1-1-P4-T2. Cortes I/II… de un T1 aprobado: actividades seleccionadas, porcentajes, análisis FODA, conclusiones/recomendaciones, mejoras, evidencias, anexos, autores, PDF A4, firma y revisión propias. No reemplaza al T2 final ni modifica al Plan. |
| Quipux | Exigencia opcional del flujo congelado; tras aprobar, coordinación registra PDF privado de constancia antes de evidencias/informes derivados/cierre del período. No hay integración remota ni modificación del PDF firmado. |
| T2 | Informe independiente derivado de T1 aprobado, narrativas, porcentaje manual, observaciones, delegaciones opcionales, anexos/evidencias y plantilla oficial A4; firma y revisión compartidas. |
| Versiones y archivo | Archivo lógico con motivo y familia formal T1/T2. Una actualización de aprobado crea nuevo ID/DRAFT/ronda 1 y conserva el anterior firmado. Una familia de Informe final por Plan, una versión activa. Borradores descartados manualmente: acceso exclusivo ADMIN; archivo automático por cierre es histórico, no descarte. |
| Flujos temporales | Plantillas generales, revisión asignada a cada ámbito y T1/T2/T3, guardado atómico y activación explícita del período. Participantes congelados, sin sustitución silenciosa por cambios de plantilla. Nuevas firmas requieren activación. Habilitación administrativa explícita de un T3 nuevo en períodos activados, una sola vez y sin alterar snapshots T1/T2. |
| Cierre e historial | ACTIVE→CLOSED con motivo, concurrencia, ZIP privado verificado; custodia completa ADMIN, verificación SHA-256 de la copia local y purga explícita tras gracia configurable (30 días). Inventario/auditoría retenidos; no hay borrado automático. Revisión pendiente conserva estados/bytes en solo lectura. Historia de períodos cerrados mediante cargo vigente con permiso administrativo explícito (ejemplos Decanato/Subdecanato), limitado por carrera cuando aplica; PDF histórico filtrado por aprobación. El ADMIN técnico no hereda permiso de lectura. |
| Avisos | Programador de vencimiento/evidencia pendiente con fechas personales, períodos vigentes y deduplicación persistente; avisos privados de restablecimiento de acceso. |
| Seguimiento | Filtros/CSV de totales/estados/cobertura y avance declarado separados: media simple de últimos resultados aprobados por actividad, prevaleciendo T2 final sobre T3; se informa la cantidad de actividades incluidas. ADMIN ve agregados sin acceso automático a contenido privado. |
| Desarrollo | Entorno y semillas aislados en utaped_dev, siete usuarios ficticios; Julio–Diciembre 2026 fijo, 3 carreras, 23 grupos, 30 ámbitos, 55 actividades y catálogos procedentes de templates. Configuración y pruebas sin modificar la DB principal ni importar firmas/autoridades reales. [Procedencia](docs/development/institutional-reference.md). |
| Aislamiento de entornos | Un entorno manual persistente utaped-sample; tres contenedores con Vite o cuarto frontend Docker opcional, mismos datos. Aceptación en utaped-acceptance desechable, guardas sin fallback manual, limpieza E2E/backend/aceptación y KeepOnFailure. Copias históricas detenidas y volúmenes conservados. Verificadas 177 pruebas backend, 16 de aceptación y regresiones adicionales de la tanda; lint/build aprobados. [Reporte](docs/reports/2026-10-16-02-single-development-and-isolated-tests.md). |
| Limpieza de ejemplos | Autorizada el 2026-10-17: respaldo validado, retirados los 122 documentos y usuarios extra de prueba; conservadas siete cuentas/contraseñas. Referencias oficiales reconstruidas: 3 carreras, 23 grupos, 30 ámbitos de Julio–Diciembre 2026, 55 actividades y 26 elementos de catálogo. A4 confirmado; archivos antiguos conservados fuera de la UI. [Reporte](docs/reports/2026-10-17-01-clean-development-examples.md). |
| Importación | CSV administrativo UTF-8 prevalidado por fila, lotes atómicos, cuentas USER con contraseña aleatoria individual de descarga única y pertenencia opcional sin principal/permisos de recuperación; caducidad y limpieza de filas temporales. |
| CAPTCHA accesible | Mismo reto visual y audio español privado, teclado, reproducción/pausa/repetición, ID/sesión/caducidad/consumo únicos, sin proveedor externo. Aceptación de pronunciación con usuarios pendiente. |
| Carga documental | Escenarios aislados de tres autores/ámbitos, T1 extensos y T2 con evidencia paginada: generación, rasterizado, descarga, conflictos de evidencia y firmas concurrentes. Métricas por fase; no acredita capacidad institucional sostenida. |
| Acceso temporal | Cambio obligatorio de contraseña mantiene el acceso con una sesión nueva; revoca sesiones previas y enlaces. El cambio voluntario normal sigue cerrando las sesiones. |
| Interfaz | Se conserva la modernización del propietario y se amplía con tarjetas, métricas, editores por pasos, feedback toast, modo oscuro y adaptación a PC/tableta. |
| Navegación documental | Diseño staged revisado; URL conserva documento y sección T1/T2 al recargar, protege cambios ante navegación del historial y sigue nuevos IDs de Informes/versiones. Solo recupera contenido guardado, sin autosalvado del borrador local. 8 E2E generales y 11 escenarios sample verificados, con repetición focalizada de recarga/circuito. [Reporte](docs/reports/2026-10-16-01-staged-ui-review-and-docker-plan.md). |
| Formularios por acción | Creación, edición y acciones cerradas inicialmente, con apertura explícita por botón; acceso/filtros visibles. Cancelación con confirmación de cambios y retorno de foco; editores documentales y flujos ya abiertos por acción conservados. Lint/build, 7 E2E generales y 8 de sample aprobados. [Reporte](docs/reports/2026-10-14-02-explicit-form-actions.md). |
| Confirmaciones y avisos | 41 llamadas confirm nativas sustituidas por modal asíncrono compartido; sin alert/confirm/prompt en src. Toasts y cambio de tema operables sobre el modal activo, teclado/foco de controles portales corregido. Staged del propietario validado y commit 59ceeb2; 7 E2E generales, 9 sample de regresión y 2 específicos aprobados. [Reporte](docs/reports/2026-10-14-04-custom-confirmations-and-staged-review.md). |
| Redacción | Propuestas T1/T2 con consentimiento, comparación, aplicar/descartar y protección ante cambios locales/versiones. Groq/OpenAI detrás de interfaz backend; deshabilitado por defecto. El propietario configuró proveedor/modelo/clave en sus entornos; aceptación de privacidad/calidad institucional pendiente. |
| Documentos extensos | Navegación directa por página, firmas/anexos/observaciones y estado de carga. Aceptación T1 extenso y T2 de 18 páginas con evidencia/índice/slots/firmas e integridad del Plan base. |
| Operación | Override HTTPS 64430 con servicios internos sin publicar, SMTP externo configurable y preflight. Respaldo conjunto DB/volumen/imágenes, validación y restauración en proyectos nuevos; ensayo completo con datos ficticios. No desplegado institucionalmente. |

Persistencia: Flyway V13–V33 y almacenamiento privado real. Los borradores individuales anteriores conservan sus IDs; se prohíbe crear nuevos por esa ruta fuera de fixtures de prueba. API: [circuito documental](docs/api/document-circuit.md) y [períodos/archivo](docs/api/period-control.md). Escenario: [entorno de ejemplo](docs/development/sample-environment.md). Evidencia: [circuito](docs/reports/2026-10-12-01-large-document-circuit.md) y [cierre y flujos](docs/reports/2026-10-12-02-period-closure-and-workflows.md). Administración/alertas: [contrato](docs/api/contextual-administration.md) y [reporte](docs/reports/2026-10-12-03-contextual-administration-and-notices.md).

Importaciones/enlaces: [contrato](docs/api/user-imports-and-external-evidence.md) y [reporte](docs/reports/2026-10-12-04-user-imports-and-external-evidence.md). Validación de esta tanda: 116 ejecuciones de pruebas de backend sin fallos, ocho escenarios E2E, lint/build y revisión visual de interfaces/T2.

Redacción/bandeja: [contrato](docs/api/writing-and-review-inbox.md), [configuración](docs/development/writing-configuration.md) y [reporte](docs/reports/2026-10-12-05-writing-assistance-and-review-inbox.md). Validación vigente: 129 ejecuciones backend en 19 suites sin fallos, cinco E2E generales y cuatro escenarios del entorno de ejemplo, lint/build y revisión visual PC/tableta. Transporte IA con fixtures locales; no se consumió una cuenta externa. El agente de código/Trello se desarrolla aparte por el propietario.

Aceptación/operación: [escenarios](docs/testing/role-and-document-acceptance.md), [herramientas](docs/operations/production-readiness.md) y [reporte](docs/reports/2026-10-12-06-role-acceptance-and-operational-readiness.md). Validación actual: 131 ejecuciones backend en 19 suites, diez escenarios E2E y siete pruebas de herramientas, sin fallos finales; lint/build y revisión visual. Recuperación de 57 documentos/156 archivos e imágenes originales; HTTPS con certificado temporal, cookie Secure, login y PDF firmado con hash intacto. Cuarenta consultas de salud concurrentes no acreditan dimensionamiento institucional.

Accesibilidad/carga (2026-10-13): [contrato](docs/api/local-access.md), [escenarios reproducibles](docs/testing/captcha-and-document-load.md) y [reporte](docs/reports/2026-10-13-01-accessible-captcha-and-document-load.md). Validación: 135 ejecuciones backend en 20 suites, siete E2E generales y cinco de ejemplo; dos escenarios de audio repetidos sobre Nginx, sin fallos finales. Lint/build y revisión visual PC/tableta. No se modificaron datos principales, proveedores ni variables privadas. La concurrencia de firma se prueba en servicios reales con fixture de transporte seguro; no es carga de certificados por HTTPS.

Corrección del 2026-10-14: selector y nombres automáticos autorizados; [reporte](docs/reports/2026-10-14-01-document-creation-options.md). 137 ejecuciones backend en 20 suites, siete E2E generales y seis de ejemplo aprobados; circuito reejecutado para reapertura sin duplicación. Lint/build y capturas PC/tableta. Ajustes visuales staged del propietario conservados en commit separado. Sin push/PR de esta rama ni modificaciones de DB principal.

### Trabajo que aún falta para completar el producto

- Ratificación institucional de retención/reapertura/cargos; los permisos históricos y la purga provisional controlada ya están implementados. Exportación histórica en streaming para volúmenes mayores al límite técnico inicial de 128 MiB. El ZIP no sustituye un respaldo restaurable.
- Política frente a bajas/cambio de ejecutores de flujos ya activados, sin invalidar asignaciones y firmas históricas.
- Administración avanzada de ámbitos, tabla oficial completa de cargos/fechas de vigencia, pertenencias globales por carrera y alta/importación delegada. Catálogo de cargos y asignación por período/carrera, CSV de asignaciones, integrantes ordinarios delegados, edición de carreras, historial visible de cargos/prórrogas y recuperación contextual explícita ya entregados. Agregar un miembro delegado no permite restablecer su contraseña global.
- Archivos adjuntos no PDF y formatos documentales oficiales adicionales: pendientes de especificación/plantillas. Referencias HTTPS ya entregadas, sin descarga ni comprobación remota de disponibilidad/integridad.
- Importación delegada, actualización de cuentas por archivo y distribución institucional de credenciales aún no entregadas. CSV administrativo ya implementado. Canales/frecuencia institucional y correo de recordatorios pendientes.
- Aceptación de tratamiento de datos, presupuesto y calidad real de redacción. El propietario configuró Groq/modelo/clave en el entorno principal y agregó variables al ejemplo; se verificó una llamada real con texto ficticio en la revisión anterior. Las pruebas automatizadas controlan sus respuestas y no consumen la cuenta. El agente de código/Trello lo construye el propietario por separado; queda fuera de este producto.
- Aceptación del audio con usuarios, necesidades que no cubre imagen/audio, prueba de carga documental sostenida y del transporte de firma HTTPS en infraestructura institucional. Carga acotada e integridad ya comprobadas; no se fija SLA. La API anterior de tareas conserva límite 200 por compatibilidad.
- Ratificación de ventanas T2, días de prórroga, múltiples elaboradores y secuencias/personas oficiales; los ejemplos no reemplazan esas decisiones.
- Insumos reales HTTPS/DNS/CA/firewall y SMTP institucional, custodio/cifrado/frecuencia/retención/RPO/RTO de respaldos y despliegue operativo. Las herramientas y restauración técnica de ejemplo ya están comprobadas; todavía se requiere ensayo/aceptación institucional. Puerto web principal 64430; el override productivo requiere selección explícita.

Validación de la tanda: [reporte del 2026-10-21](docs/reports/2026-10-21-01-institutional-completion-and-stabilization.md).

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

## Revisión del staged — 2026-10-17

Modernización del propietario conservada: menú plegable, editor/matriz T1, campos T2, vista simultánea y estudio modal de redacción con propuesta editable. Corregidas sustituciones silenciosas de justificaciones históricas, foco y confirmaciones anidadas, protección de propuestas editadas, nombres accesibles de matriz/logout y salto directo de página. La configuración ADMIN T1 no cambia documentos existentes ni plantillas A4. Backend completo (146 casos) y dos pruebas focalizadas aprobados; 13 escenarios de aceptación, ocho E2E generales, lint/build y revisión visual PC/tablet aprobados. [Revisión y validaciones](docs/reports/2026-10-17-04-staged-templates-and-ui-review.md).

## Personas ficticias realistas — 2026-10-17

Semillas con siete nombres completos y cargos legibles; actualización acotada de etiquetas originales, sin sobrescribir perfiles personalizados. Credenciales, permisos, documentos y artefactos conservados. Aplicado en utaped-sample con respaldo previo y comprobación de integridad; nuevas instalaciones usan la misma presentación. [Reporte](docs/reports/2026-10-17-05-realistic-development-identities.md).

## Auditoría de reuniones — 2026-10-17

La transcripción completa de reuniones hasta el 06-10-2026 confirma faltantes del producto: estructura versionada de plantillas, múltiples elaboradores/cargos y administración contextual, entre otros. Se localizaron diferencias concretas en aprobación con observaciones registradas, órganos colegiados sin personas y acceso a borradores descartados. Son hallazgos de inspección estática, no correcciones entregadas ni pruebas nuevas. Quipux, retención y cómputos de plazos requieren reconciliación; la transcripción no sustituye automáticamente el Plan Maestro ni demuestra aceptación del producto. [Matriz, fuentes y prioridades](docs/reports/2026-10-17-06-meeting-transcripts-gap-audit.md).

## Tanda de autoría y revisión — 2026-10-18

Entregado: selección independiente de elaboradores T1/T2, principal único editor, cargos congelados, firmas sucesivas previas a revisión, aviso al siguiente coautor y congelación tras la primera firma. Decisión colegiada con registrador designado sin firma personal, bloqueo de completar/firmar con observaciones de la ronda y encabezado institucional compartido corregido. V24; [contrato](docs/api/document-authorship-and-review.md). Verificados 150 backend durante la tanda y 4 escenarios finales focales, 15 de aceptación más 2 finales sobre código actual, 8 E2E, lint/build y revisión visual. [Reporte de entrega](docs/reports/2026-10-18-01-document-authorship-review-and-headers.md). No incluye todavía catálogo completo de cargos, Quipux, retención ni delegaciones administrativas avanzadas.

## Bordes del encabezado institucional — 2026-10-18

Bordes exteriores e interiores explícitos y continuos para T1/T2, con logo combinado y separación de unidad/carrera. engine6 evita reutilizar nuevas preparaciones del motor anterior; artefactos firmados conservados. [Reporte](docs/reports/2026-10-18-02-institutional-header-borders.md).

## Matriz y catálogos — 2026-10-20

Fuente editable aclarada, «Elaborado por» automático y representación colectiva al seleccionar todos, sin casilla adicional. Combo de actividades con texto solo en Otra; recursos/medios conservan catálogos y Otro. Semillas adicionales opcionales en grupos sin catálogo: 20 ejemplos separados de las 55 actividades de fuentes oficiales, exclusivamente en desarrollo. [Reporte](docs/reports/2026-10-20-01-matrix-catalogs-and-collective-responsibility.md).

## Reglas institucionales provisionales — 2026-10-20

F1: prórrogas de evidencias en días hábiles (sin fines de semana ni feriados registrados) y reapertura individual de elaboración por ámbito concedida por su coordinador, con historial y auditoría (V25). F2: fecha de elaboración fijada al preparar la firma y sello visible con QR, firmante y fecha en el espacio asignado. F3: copia de custodia del período y eliminación de su documentación tras período de gracia, conservando inventario, huella y auditoría (V26). F4: cargos institucionales por período y carrera, etapas de flujo por cargo, importación CSV y alta/retiro de integrantes por el coordinador del ámbito (V27). [Reporte](docs/reports/2026-10-20-04-institutional-rules-and-execution-report.md).
