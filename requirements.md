# Requisitos de UTAPED

## Reglas provisionales y completado documental — 2026-10-21

La autorización del propietario incorpora las decisiones técnicas del [plan de entrega](docs/planning/institutional-rules-and-execution-report.md), manteniendo pendiente su ratificación institucional. Supera únicamente las contradicciones históricas indicadas aquí:

- UT-T3-130: Informe de ejecución independiente por corte de Plan aprobado, numeración I/II…, actividades seleccionadas, resultados/FODA/mejoras, PDF A4 y circuito de firma/revisión. T3 es identificador interno; el código oficial de plantilla es `UTA-SGC-A-1-1-P4-T2`. T2 final permanece independiente.
- UT-TPL-130: estructuras publicadas/versionadas por formato, orden de secciones y apartados adicionales con obligatoriedad/texto inicial; snapshot por documento. Conservar secciones oficiales, su estilo, firmas/control final y artefactos existentes.
- UT-SEC-130: búsqueda de candidatos por nombre y ámbito; solo coordinación puede delegar integrantes ordinarios. Esa alta no habilita restablecer contraseñas de cuentas ajenas. Descartados manuales solo ADMIN; historia cerrada solo mediante cargo vigente habilitado expresamente y con alcance de carrera/facultad.
- UT-PER-130: días hábiles para máximos de prórroga; cinco días hábiles por defecto al crear una ventana sin fin explícito. Extensión por ámbito autorizada/auditada; no se extiende un período cerrado. Fechas anteriores no cambian retroactivamente.
- UT-CUS-130: cierre, descarga de ZIP completo para custodia, verificación de copia local mediante SHA-256 y purga explícita con motivo tras gracia configurable de 30 días. Retener inventario y auditoría; no hay purga automática.
- UT-QPX-130: constancia PDF de Quipux opcional por flujo congelado, registrada por coordinación tras aprobación; bloquea evidencias, informes derivados y cierre mientras esté pendiente. La descarga del aprobado permite tramitarlo. Sin integración con Quipux.
- UT-IND-130: cobertura documental y avance declarado son indicadores distintos. Avance = media simple de porcentajes aprobados por actividad (último corte; T2 final prevalece). Mostrar actividades incluidas y ausencia de declaración; no presentar una ponderación institucional ficticia.

La fecha de elaboración es la preparación del artefacto firmable, incluida en los bytes antes de la primera firma. Cada firma conserva su fecha/hora propia; no se sustituye la fecha después de firmar. El QR repite metadatos de la firma y no acredita un validador institucional externo. [Contrato y operación](docs/api/institutional-rules-and-execution.md).

## Matriz y selección de catálogos — 2026-10-20

El propietario confirma conservar el flujo actual, aclarar que la fuente editable puede ser la comisión o un documento y automatizar la denominación colectiva al seleccionar todos los integrantes. Priorizar opciones de catálogo donde las reuniones solicitan listas; mantener Otro cuando corresponde. Se autorizan opciones de ejemplo precargadas para catálogos aún no entregados, sin convertirlas en datos o reglas institucionales oficiales. [Reporte](docs/reports/2026-10-20-01-matrix-catalogs-and-collective-responsibility.md).

## Autoría y revisión — 2026-10-17

El propietario autoriza corregir observaciones/órganos colegiados y completar varios elaboradores con un único editor, firmas sucesivas y cargos. Confirma que Administración designa por período a la persona que registra una decisión colegiada sin figurar como firmante personal. Una observación de la ronda exige devolución antes de aprobación. La primera firma congela el contenido y la última firma de elaboración inicia revisión. Se corrigen los encabezados T1/T2 usando Prototipos como referencia visual y conservando plantillas institucionales A4. [Contrato](docs/api/document-authorship-and-review.md).

## Ejemplos con presentación realista — 2026-10-17

Decisión del propietario: utilizar personas ficticias con nombres completos y cargos legibles en el entorno de desarrollo, en lugar de etiquetas técnicas como «Desarrollo principal». Conservar credenciales, permisos e identidades técnicas; no atribuir cargos ni firmas a personas reales. Las carreras, grupos, período y catálogos siguen las fuentes oficiales de docs/templates. La actualización respeta nombres/cargos personalizados y documentos previamente creados o firmados. [Cuentas del escenario](docs/development/sample-environment.md).

## Justificación inicial administrable — 2026-10-17

La revisión del staged del propietario incorpora una configuración ADMIN para el texto inicial de Justificación del T1, con control de versión y auditoría. Se copia al crear un Plan nuevo; editar o restaurar la configuración no reescribe documentos existentes, campos vaciados intencionalmente ni artefactos firmados. El autor puede modificar su texto o restaurarlo explícitamente desde el editor. Esta configuración no sustituye las plantillas oficiales DOCX ni modifica su formato A4. El texto inicial aportado en el staged es editable y no constituye ratificación institucional de las referencias normativas que menciona. [Contrato](docs/api/document-templates.md).


## Previsualización simultánea — 2026-10-17

UT-UI-120, CONFIRMADO-PROYECTO: comenzar por Contenido del T1, edición izquierda y documento A4 derecha en PC, ancho ajustable y página ampliable. En tablet alternar edición/vista. Usar el motor institucional real, permitir ver cambios locales sin presentarlos como guardados, conservar pasos/validaciones y mostrar exclusivamente bytes conservados tras firmar. No alterar contenido de otro documento ni crear artefactos firmables automáticamente. [Diseño y límites](docs/design/simultaneous-preview.md).


## Fuentes oficiales y datos de desarrollo — 2026-10-17

El propietario confirma que los archivos de docs/templates contienen información institucional oficial. Deben guiar carreras, unidades/comisiones, actividades y estructura documental; la excepción expresa es el tamaño Carta de una referencia: la salida T1/T2 utiliza A4. Las cuentas y asignaciones de desarrollo siguen siendo ficticias. Los registros actuales son ejemplos reemplazables y se autoriza limpiar/reconstruir el entorno de desarrollo, con respaldo previo para operaciones masivas. Esta aclaración actualiza la conservación de los antiguos escenarios de pruebas; no autoriza borrar una futura base institucional.


## Entorno diario y aislamiento de pruebas — 2026-10-16

UT-DEV-110, CONFIRMADO-PROYECTO: un entorno manual persistente (utaped-sample existente), PostgreSQL/backend/Mailpit con Vite o frontend Docker opcional sobre los mismos datos. Conservar volúmenes y credenciales; no fusionar bases ni borrar históricos de revisión. Aceptación automatizada exclusivamente en utaped-acceptance desechable, sin fallback manual; E2E/integración bajo demanda con limpieza y opción KeepOnFailure. Vite sigue el puerto diario y permite API_TARGET explícito. Puerto institucional 64430 sin cambios. [Guía](docs/development/sample-environment.md).


Estado: línea base inicial para revisión; no constituye aprobación institucional nueva.

## Fuente vigente desde el 2026-10-06

El [Plan Maestro del propietario](docs/decisions/PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md) prevalece sobre las contradicciones de la documentación anterior. Los apartados fechados anteriores describen decisiones históricas y entregas parciales; no mantienen abiertas decisiones ya resueltas por ese plan. Su incorporación no significa que sus 92 apartados estén implementados.

El propietario confirmó además un rediseño completo de la interfaz productiva: azul predominante con colores complementarios, jerarquía moderna y navegación accesible. Prototipos conserva valor funcional; su estética no se replica. Los formatos institucionales T1/T2 se conservan independientemente de este rediseño.

Requisitos entregados en esta etapa: UT-AUTH-020 usuario separado del correo; UT-AUTH-021 nombres/apellidos separados para altas nuevas; UT-AUTH-022 Argon2id con migración de bcrypt al autenticar correctamente; UT-AUTH-023 CAPTCHA alfanumérico de servidor, vinculado a sesión, 3 minutos, renovación y un solo intento; UT-AUTH-024 restablecimiento ADMIN con contraseña temporal, auditoría y revocación; UT-UI-020 sistema visual, navegación, adaptación a escritorio/tableta/móvil y estados reales. Son confirmados por el propietario, prioridad P0/P1, derivados de §§6–7 y del cambio visual del 2026-10-06. Aceptación y pruebas: [contrato de acceso](docs/api/local-access.md) y [ADR-0003](docs/architecture/adr/0003-modern-workspace-access.md). La variante de restablecimiento por coordinador depende del modelo de ámbitos de carrera/período todavía pendiente; no se concede ese permiso global.

## Decisiones productivas confirmadas por el propietario

2026-10-07: la interfaz se destina a PC y tablet; no se añaden estilos específicos para celular. Modo claro/oscuro con botón iconográfico flotante inferior derecho, luna/sol y tooltip de la acción al pasar el puntero o enfocar con teclado. Preferencia local persistente, sistema como valor inicial y documentos institucionales con sus colores originales. Esta decisión actualiza el alcance visual anterior que incluía móvil.

2026-10-02: credenciales locales; selección tecnológica delegada e implementación de la base técnica autorizada. [ADR-0001](docs/architecture/adr/0001-production-foundation.md) establece stack, sesiones y migraciones. Confirmación del proyecto, no normativa institucional nueva. [Estado de entrega](implementation-status.md) distingue funcionalidades parciales de módulos todavía pendientes.

## Entrega del circuito documental del 2026-10-12

La autorización de la tanda amplia implementa las reglas del Plan Maestro con datos de desarrollo aislados; no confirma nuevas políticas institucionales. Se conserva la precedencia del Plan Maestro sobre la especificación candidata del 2026-10-11.

| ID | Requisito y fuente | Estado / prioridad | Aceptación y evidencia |
|---|---|---|---|
| UT-ORG-030 | Ámbitos de grupo, carrera y período; principal único; Plan por ámbito. Plan Maestro §§3 y organización. | CONFIRMADO-PROYECTO, P0 | Duplicado rechazado; miembro solo lectura; transferencia explícita; pruebas OrganizationIntegrationTests. |
| UT-REV-030 | Revisión secuencial de todos los ejecutores, PDF firmado conservado, devolución y nueva ronda. Plan Maestro §3.4 y revisión. | CONFIRMADO-PROYECTO, P0 | No actúa una etapa futura; devolución conserva bytes y versión formal; firmas acumulativas; ReviewExecutionIntegrationTests y circuito navegador. |
| UT-EXE-030 | PDF por medio y versiones independientes, permisos de responsables. Plan Maestro §3.7 y ejecución. | CONFIRMADO-PROYECTO, P1 | Carga/reemplazo autorizado y conflicto de versión; cobertura completa sin inferir avance físico. |
| UT-T2-030 | Informe final derivado y único como familia del Plan aprobado; identidad y firmas separadas. Plan Maestro §3.6 e Informe. | CONFIRMADO-PROYECTO, P0/P1 | Recorrido T1→T2 aprobado sin cambiar bytes/estado del Plan; versiones formales dentro de una familia. |
| UT-VER-030 | Archivo sin purga y nueva versión formal distinta de ronda/evidencia. Plan Maestro sobre versionado y archivo. | CONFIRMADO-PROYECTO, P0 | Nuevos IDs, DRAFT/ronda 1, aprobación y PDF anterior conservados; autorización y concurrencia. |
| UT-UI-030 | Extender el nivel visual de los cambios staged del propietario a las nuevas vistas. Decisión del 2026-10-11. | CONFIRMADO-PROYECTO, P1 | Navegación, editor por pasos y capturas PC/tableta con modo oscuro; sin estilos nuevos para teléfonos. |

Las prórrogas son individuales y auditadas. Se implementa como configuración técnica inicial un máximo de tres días calendario dentro del período; cómputo institucional definitivo pendiente. T2 usa inicialmente la ventana del período académico completo y exige explicar la falta de evidencia. La descarga final depende de aprobación. Los agregados se limitan a estados/cobertura; no se inventa una fórmula institucional de avance físico.

La firma/envío antes bloqueada ahora se habilita en documentos por ámbito con ejecutores individuales explícitos. Una etapa colegiada sin representante sigue bloqueada. Los PDFs históricos no se regeneran ni se convierten automáticamente al nuevo flujo. Cierre de períodos, ZIP, agentes, redacción asistida, evidencias externas y capacidades restantes siguen registrados en [estado actual](implementation-status.md).

## Creación documental — decisión del propietario del 2026-10-14

UT-UI-100 / UT-DOC-100, CONFIRMADO-PROYECTO, P1: recuperar el selector Plan de Trabajo / Informe y proponer nombre institucional automático según grupo/unidad. El Informe exige selección de Plan aprobado del responsable principal y conserva la familia existente. Permitir ajustar el título del Plan; no renombrar históricos ni inventar códigos individuales. Fuente: corrección autorizada por el propietario tras revisar Plan Maestro §42 y prototipos. Aceptación: nombres sin entrada manual, búsqueda/paginación autorizada, descarte confirmado, conservación al filtrar, rechazo de Plan no aprobado y reapertura sin duplicado. [Contrato](docs/api/document-circuit.md), [reporte](docs/reports/2026-10-14-01-document-creation-options.md).

## Fuentes y alcance

### Accesibilidad y carga documental — 2026-10-13

| ID | Requisito y fuente | Estado / prioridad / aceptación |
|---|---|---|
| UT-AUTH-090 | Alternativa de audio al CAPTCHA visual, mismo código/sesión/caducidad/consumo, sin servicio externo. Autorización del propietario del 2026-10-13. | CONFIRMADO-PROYECTO, P1. Teclado, reproducción y renovación; otra sesión/consumo/expiración rechazados; V22, HTTP, PCM y Playwright. |
| UT-DOC-090 | Carga acotada de generación/renderizado/descarga, evidencias y firmas concurrentes con integridad/aislamiento y mediciones. Misma autorización. | CONFIRMADO-PROYECTO, P0/P1. Tres autores/ámbitos, ocho lectores, conflictos de versiones y firma idempotente; PostgreSQL aislado y JSON por fase. |

[Contrato de acceso](docs/api/local-access.md), [pruebas y límites](docs/testing/captcha-and-document-load.md). Aceptación de audio con usuarios y dimensionamiento/SLA institucional pendientes; no se declara conformidad WCAG completa ni se amplían permisos.

### Aceptación y operación — 2026-10-12

| ID | Requisito técnico entregado | Evidencia |
|---|---|---|
| UT-REV-080 | Navegación directa por página real, accesos derivados a firmas/anexos/observaciones y estado de carga, conservando el PDF firmado y resaltados. | Navegador compartido; circuito Playwright y revisión visual PC/tableta. |
| UT-ACC-080 | Comprobar separación de ADMIN, principal, coordinador, integrante, revisor, aprobador y ajeno; baja con sesión abierta. | Matriz HTTP y nuevo escenario de siete identidades, sin ampliar permisos institucionales. |
| UT-DOC-080 | T2 extenso con contenido completo, índices y slots reales, evidencia paginada y firma/descarga que conserva bytes y Plan base. | Caso de 18 páginas y render Poppler, adicional a T1 extenso existente. |
| UT-OPS-080 | HTTPS en 64430, configuración privada de SMTP y respaldo consistente DB/archivos/imágenes con restauración aislada verificada. | Override productivo, preflight, herramientas operativas y ensayo de datos ficticios. Insumos/aprobación institucional pendientes. |

Estos IDs registran configuración/aceptación técnica dentro de la entrega autorizada; no crean normativa, roles nuevos ni políticas de retención. [Aceptación](docs/testing/role-and-document-acceptance.md), [operación y límites](docs/operations/production-readiness.md).

### Cierre histórico y flujos temporales — 2026-10-12

| ID | Requisito / fuente | Estado / aceptación |
|---|---|---|
| UT-PER-040 | ACTIVE→CLOSED, archivo sin purga, PDFs/historial conservados. Plan Maestro §53. | CONFIRMADO-PROYECTO, P0. Cierre con motivo/concurrencia; rollback ante integridad fallida; bloqueo de documentos, revisiones y organización. |
| UT-WFL-040 | Revisión T1/T2 por ámbito y período, fija al activar. Plan Maestro §§3.9, 9 y 29. | CONFIRMADO-PROYECTO, P0. Activación explícita, ejecutores congelados; cambios de plantilla no alteran asignaciones. |
| UT-HIS-040 | Consulta histórica de documentos autorizados sin nuevos permisos por cambio de comisión. Plan Maestro §52. | CONFIRMADO-PROYECTO, P0. ZIP filtrado por permisos actuales y aprobación; ADMIN sin pertenencia no puede descargar contenido privado. |

La indicación de período actual usa fechas y estado; no automatiza un cierre ni una designación institucional. Se exige activación de flujos antes de nuevas firmas. Límite técnico inicial del ZIP: 128 MiB sin comprimir, ajustable; generación en memoria. Retención, reapertura, permisos de Decano/Subdecano y sustitución de ejecutores permanecen pendientes. [Contrato y operación](docs/api/period-control.md).

2026-10-05: el propietario confirma el flujo sencillo de seleccionar .p12/.pfx, introducir contraseña y firmar, con comprobaciones básicas de identidad, vigencia e integridad y sin persistir material privado. Se implementa firma visible del artefacto del elaborador mediante huella pública vinculada administrativamente. La cadena emisora y revocación se declaran NO COMPROBADAS; no se atribuye aprobación institucional, PAdES o sellado de tiempo. El borrador editable permanece separado del PDF firmado inmutable. Finalización, envío y aprobación siguen bloqueados. [Contrato y límites](docs/decisions/signature-review-contract.md).

2026-10-04: el propietario confirma firma mediante archivos .p12/.pfx. Los flujos institucionales aún no están confirmados; se autoriza preparación técnica conservando finalización/envío/aprobación deshabilitados. Identidad del certificado, confianza, revocación y perfil siguen pendientes en [contrato de firma](docs/decisions/signature-review-contract.md).

2026-10-03: el propietario confirma almacenamiento local privado persistente, límites iniciales ajustables de 10 MB/anexo y 20 anexos, y páginas T1 A4 conservando estructura/estilo. [ADR-0002](docs/architecture/adr/0002-t1-document-preparation.md). Retención, límites institucionales definitivos, aceptación final del formato y firma real siguen pendientes.

La fuente heredada completa se conserva en [baseline-requirements.md](docs/reference/baseline-requirements.md), incluyendo todos los IDs FR-* y las secciones sin identificador. Las reglas de simulación de esa fuente describen solamente el mockup.

La aplicación requiere autenticación, autorización, persistencia, almacenamiento e integridad reales. Ninguna funcionalidad DEMO se considera entregada en producción. La procedencia y huellas se registran en [source-register.md](docs/reference/source-register.md).

## Especificación organizada

Nuevo alcance confirmado el 2026-10-03: [agente para inspeccionar el proyecto, Trello y proponer mejoras](docs/requirements/project-advisor-agent.md). El propietario lo construye aparte según decisión del 2026-10-12; queda fuera de las entregas del producto institucional. Estos tres requisitos nuevos complementan los 122 registros de la fuente heredada y no bloquean la entrega de UTAPED.

Entrega 2026-10-12: asistencia de redacción para JUSTIFICATION/OBJECTIVE de T1 y BACKGROUND/CONCLUSIONS/IMPROVEMENTS de T2. Solo texto existente, autorización de borrador antes/después de la llamada, consentimiento por envío, comparación y aplicar/descartar sin guardado automático. Groq/OpenAI configurables y deshabilitados por defecto; requiere cuenta/modelo/clave y aceptación operativa. No se conserva historial textual. Bandeja propia paginada, búsqueda literal por título/grupo/período y filtros de estado/tipo, sin límite visual de 200. [Contrato y límites](docs/api/writing-and-review-inbox.md).

- [Requisitos funcionales y aceptación](docs/requirements/functional.md).
- [Requisitos no funcionales](docs/requirements/non-functional.md).
- [Reglas de negocio](docs/requirements/business-rules.md).
- [Glosario](docs/domain/glossary.md), [flujos](docs/domain/workflows.md) y [datos conceptuales](docs/domain/data-model.md).
- [Decisiones pendientes](docs/decisions/open-questions.md).
- [Catálogo trazable de 122 registros fuente](docs/requirements/catalog.md), con [datos estructurados](docs/requirements/catalog.json).
- [Reconciliación con el mockup](docs/validation/reconciliation.md), [permisos](docs/validation/permission-matrix.md) y [plan de validación](docs/validation/validation-plan.md).

## Formularios mediante acciones explícitas — 2026-10-14

| ID | Descripción y fuente | Estado / prioridad / aceptación |
|---|---|---|
| UT-UI-110 | El propietario exige que creación, edición y acciones comiencen cerradas y se abran con un botón. Aclaración expresa: acceso y filtros visibles. | CONFIRMADO-PROYECTO, P1. Entrar al módulo no abre formularios mutadores; Crear/Editar/Adjuntar/Firmar/Observar los abre expresamente. Cancelar o Escape confirma cambios pendientes, conserva datos si se rechaza y devuelve el foco al cerrar. Los errores conservan los datos. Los editores abiertos por Editar o pasos del asistente ya cumplen. PC/tabletas, ambos temas. |
| UT-UI-111 | El propietario confirma modal para Crear documento; editores extensos en pantalla. | CONFIRMADO-PROYECTO, P1. Modal nombrado, fondo sin interacción, foco contenido, Tab/Shift+Tab, Escape/cancelación protegidos, foco restaurado y desplazamiento interno en PC/tabletas; conserva selector T1/T2 y permisos. |
| UT-DEV-111 | El propietario fija Julio–Diciembre 2026 y solicita datos de templates para los ejemplos. | CONFIRMADO-PROYECTO, P1. Catálogo trazable de carreras/grupos/actividades/recursos/medios, semillas idempotentes en utaped_dev, cuentas ficticias y sin importar firmas/aprobaciones. Fechas del semestre fijas; ventanas de prueba diferenciadas de calendario oficial. |
| UT-UI-112 | El propietario exige toasts o modales de confirmación, sin alertas simples. | CONFIRMADO-PROYECTO, P1. Sin llamadas alert/confirm/prompt en el frontend productivo; decisiones asíncronas antes de modificar/descartar, cancelación conserva datos, teclado/foco y superposición en ambos temas. Toasts accesibles sobre el modal activo. La protección de recarga/cierre pertenece al navegador, independiente de las acciones internas. |
| UT-UI-113 | Persistencia de pantalla/documento/paso incorporada en el staged del propietario, revisada el 2026-10-16. | CONFIRMADO-PROYECTO, P1. Recarga restaura último contenido guardado y paso autorizado T1/T2; URL sin credenciales, permisos de servidor conservados y limpieza al cerrar sesión. Atrás/Adelante confirma cambios antes de salir; nueva versión/Informe actualiza identidad en URL. No implica autosalvado de cambios sin guardar. |

Dependencias: permisos, validaciones y protección de navegación existentes; no cambia el contrato backend. Verificación: Playwright general y sample, incluidas apertura, ausencia de campos al entrar, cancelación rechazada/aceptada, teclado, filtros y circuito T1/T2. [Reporte](docs/reports/2026-10-14-02-explicit-form-actions.md).

## Estados

- HEREDADO: aparece en las fuentes; requiere revisión productiva.
- CONFIRMADO-PROYECTO: decisión explícita del propietario, sin atribuir aprobación institucional.
- PENDIENTE DE VALIDACIÓN INSTITUCIONAL: falta evidencia institucional.
- PROPUESTO: recomendación aún no adoptada.
- DEMO: comportamiento de referencia que no acredita cumplimiento productivo.

Registrar para cada requisito nuevo: ID, descripción, fuente, estado, prioridad, aceptación, dependencias y pruebas. Para confirmar registrar quién validó, cuándo y evidencia.

Prioridades propuestas: P0 integridad y permisos; P1 flujo documental y ejecución; P2 facilidades complementarias. Ante contradicción, registrar pregunta y no implementar una norma supuesta. Cada entrega actualiza requisitos afectados, decisiones, estado y pruebas.

La línea base conserva la cobertura conocida; la exhaustividad final requiere revisar formatos oficiales, políticas operativas y preguntas pendientes con la institución.

## Separación de espacios por responsabilidad — 2026-10-07

Decisión del propietario: el administrador inicia en un panel administrativo, con navegación independiente para usuarios, grupos/pertenencias, períodos, actividades, recursos/medios, feriados, flujos, auditoría y certificados. Sus métricas proceden de registros reales. El inicio personal y los documentos no forman parte de ese espacio.

Una cuenta ADMIN con pertenencia activa puede elegir explícitamente su espacio documental, manteniendo la misma identidad y las restricciones de propiedad/pertenencia del servidor. ADMIN no concede aprobación, revisión ni acceso general a documentos. Las notificaciones y la seguridad de la cuenta se conservan. Navegar o cambiar de espacio debe advertir sobre cambios sin guardar. No se presentan módulos todavía inexistentes como funcionalidades entregadas.

## Administración contextual y avisos — 2026-10-12

| ID | Requisito y fuente | Estado / aceptación |
|---|---|---|
| UT-ORG-050 | Edición y desactivación de carreras, historial de cargos/principales. Plan Maestro §8. | CONFIRMADO-PROYECTO, P1. Concurrencia, baja sin purga, registro inicial explícito y cambios conservados; ámbitos cerrados de solo lectura. |
| UT-AUTH-050 | Recuperación por coordinador autorizado, limitada al ámbito. Plan Maestro §6.6. | CONFIRMADO-PROYECTO, P0. ADMIN delega expresamente; no propia cuenta/ADMIN/otro ámbito; revoca sesiones/enlaces, fuerza cambio, auditoría y aviso privado. |
| UT-NOT-050 | Avisos internos de actividad próxima a vencer y evidencia pendiente. Plan Maestro §§40 y 56. | CONFIRMADO-PROYECTO, P1. Plazo personal, aprobación, pertenencia/acceso actuales, cierre/archivo respetados, deduplicación concurrente persistente. |
| UT-EXE-050 | Historial visible de prórrogas individuales. Plan Maestro §39. | CONFIRMADO-PROYECTO, P1. Plazos anterior/nuevo, motivo, autorizador y fecha por actividad/documento; datos originales conservados. |

Frecuencia técnica inicial ajustable: comprobación horaria, aviso previo de tres días, reiteración diaria de pendientes vencidos. No equivale a frecuencia institucional confirmada. [Contrato y límites](docs/api/contextual-administration.md), [reporte y pruebas](docs/reports/2026-10-12-03-contextual-administration-and-notices.md). Las menciones anteriores a recuperación por coordinador pendiente quedan superadas en este alcance; alta/importación delegada y otros permisos institucionales siguen pendientes.


## Importación y evidencias externas — 2026-10-12

| ID | Requisito y fuente | Estado / aceptación |
|---|---|---|
| UT-AUTH-060 | Importación administrativa con contraseña temporal. Plan Maestro §§6–7. | CONFIRMADO-PROYECTO, P1. CSV prevalidado, errores por fila, lote atómico, revalidación concurrente, USER y pertenencia opcional; credencial individual descargada una vez, cambio obligatorio y auditoría. No sobrescribe cuentas. |
| UT-EXE-060 | Referencias/repositorios como evidencia y texto al final de T2. Plan Maestro §§40 y 44. | CONFIRMADO-PROYECTO, P1. Medio PDF/URL definido y conservado en la matriz; HTTPS, responsable/plazo/período, versiones, cobertura, historial y T2 congelado. Sin convertir URL en PDF ni afirmar integridad remota. |

Límites técnicos y contrato: [importaciones y enlaces](docs/api/user-imports-and-external-evidence.md). No quedan ratificados cargos/fechas institucionales, alta delegada ni política de retención por entregar estas capacidades.
