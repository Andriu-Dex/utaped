# Revisión de la nueva especificación maestra

Fecha: 2026-10-11. Alcance: revisión documental comparativa y comprobaciones estáticas dirigidas del código. Sin cambios de código, base de datos, configuración, commit ni push.

## Dictamen

**El documento es útil y debe conservarse. No es simplemente una copia, pero tampoco está listo para reemplazar sin ajustes la fuente vigente.** Consolida de forma más operativa muchas decisiones anteriores y añade criterios de aceptación, contratos propuestos, controles de concurrencia y un plan de entrega. Repite gran parte del Plan Maestro porque resume el mismo producto; esa repetición es útil como consolidación, pero mantener dos documentos igualmente normativos produciría divergencias.

No hay una contradicción general en el núcleo del negocio: titularidad organizacional, unicidad por grupo/carrera/período, editor principal, tres estados formales, revisión secuencial, rondas independientes de versiones, firma privada temporal, evidencias sin aprobación independiente, T2 vinculado y archivo sin purga coinciden con el Plan Maestro.

**No confirma que ya estén todas las funciones requeridas ni que todo esté implementado.** Omite requisitos expresos posteriores y contiene afirmaciones sobre fuentes que esta revisión no puede ratificar.

## Fuentes revisadas

- [Nueva especificación](../decisions/UTAPED_ESPECIFICACION_MAESTRA_COMPLETA_PARA_AGENTE.md): §§0–30, tablas de requisitos/aceptación, decisiones y pendientes.
- [Plan Maestro del 2026-10-06](../decisions/PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md).
- `AGENTS.md`, `requirements.md`, `implementation-status.md`, `docs/decisions/open-questions.md`.
- `docs/requirements/functional.md`, catálogo trazable, requisitos del agente de inspección, registro de fuentes y contrato de firma.
- Código de acceso/tema, configuración Vite, servicio de Planes, preparación documental y firma del elaborador; manifiesto backend.
- Inventario de `docs/templates`: incluye el PDF de unidades/comisiones julio–diciembre 2026.

Se contabilizan **52 requisitos RF identificados en tablas, 18 RNF y 40 escenarios AT** en el documento nuevo. Varias funciones adicionales solo aparecen en prosa: el número de RF no equivale a una cobertura completa ni a requisitos atómicos plenamente trazados.

## Qué ya existe y se vuelve a consolidar

| Tema | Evaluación |
|---|---|
| Plan por comisión/unidad, carrera y período | Coincide con Plan Maestro §§3 y 8; sigue pendiente de migración productiva. |
| Editor único y participantes adicionales sin coedición automática | Coincide con §§8 y 16; no autoriza compartir credenciales. |
| DRAFT / IN_REVIEW / APPROVED | Coincide con §3.5; ejecución y archivo separados del estado documental. |
| Revisión secuencial, todos los actores obligatorios | Coincide con §§3.4 y 23–33; falta ejecución productiva. |
| Devolución como nueva ronda, no nueva versión formal | Coincide con §§32–35. |
| Un Informe Final vinculado al Plan | Coincide con §§3.6 y 41–44; distinguir familia y versiones. |
| PDF canónico e inmutable y firma .p12/.pfx temporal | Coincide con §§19–22 y decisiones de firma; no confundir certificado público con credencial privada. |
| Medios PDF y referencias digitales, reemplazo y plazos | Coincide con §§38–40; referencias digitales no son una regla completamente nueva. |
| Privacidad, roles, auditoría y permisos históricos | Coincide con el Plan Maestro; no concede a ADMIN acceso documental general. |
| IA opcional, red local y puerto 64430 | Compatible con el Plan Maestro y arquitectura prevista; la disponibilidad del proveedor/infraestructura aún requiere verificación operativa. |

## Qué aporta de valor

1. Una matriz funcional de permisos y separación de funciones más fácil de revisar con el cliente.
2. Criterios concretos de aceptación AT-001–040 para orientar pruebas de regresión y entregas.
3. Tratamiento explícito de unicidad cuando no hay carrera, concurrencia, idempotencia y diferencias entre transacción DB y filesystem.
4. Contratos y modelo de datos conceptuales más detallados, con advertencia de reconciliarlos con las tablas/APIs existentes.
5. Distinción entre Informe Final de Plan y otros documentos que también puedan usar el formato T2.
6. Mejor delimitación de pendientes reales: retención, firma, plazos, porcentajes e infraestructura, sin bloquear trabajo independiente.
7. Plan por dependencias y criterios de aceptación del producto completo; útil para organizar entregas amplias.

Son mejoras de especificación y precisión técnica, no necesariamente decisiones nuevas confirmadas institucionalmente.

## Discrepancias y ajustes necesarios

| Prioridad / clasificación | Hallazgo y referencia | Evidencia vigente / recomendación |
|---|---|---|
| Alta — conflicto de precedencia | §1.1 dice que reemplaza el Plan Maestro cuando difiera; §30 se declara fuente de verdad. `AGENTS.md` y `requirements.md` todavía designan al Plan del 06-10 como fuente vigente. | La incorporación de un archivo para revisión no equivale a aprobar todas sus diferencias. Adoptar una sola fuente canónica después de reconciliar; registrar las decisiones posteriores explícitas del propietario. |
| Alta — contradicción con decisión posterior de UI | §17.3 y §26 exigen login con tarjeta central, prohíben composición 50/50 y lista lateral, bajo la etiqueta «aprobado». | El propietario pidió rediseñar el frontend productivo; el acceso actual usa `access-story` y `access-form-pane` y recibió ajustes posteriores. Esa regla reintroduce una decisión del mockup como obligación productiva. Mantener el diseño actual salvo solicitud nueva específica del propietario. |
| Alta — contradicción de avisos operativos | RF-AUT-005/006 y §17.3 piden errores inline también para login/CAPTCHA. | El propietario ordenó reemplazar los mensajes de advertencia por toast el 07-10. Mantener toast para avisos operativos/error y validación por campo donde corresponda; no eliminar errores inline de campos como «Otro». Aclarar la distinción, en lugar de imponer un solo mecanismo a todo. |
| Alta — requisito omitido | §§16 y 23 describen IA de redacción y Trello como metodología; no especifican el agente de inspección de código y planificación. | [Requisitos del agente](../requirements/project-advisor-agent.md): FR-AGENT-001–003 confirmados por el propietario. Incorporarlos como herramienta interna separada del producto institucional, con lectura autorizada de repositorio/Trello y recomendaciones con evidencia. |
| Media — requisito omitido | No se define modo claro/oscuro ni su botón/preferencia. | Decisión del 07-10 en `requirements.md`, reporte de temas y `ThemeToggle.tsx`. Añadirlo al alcance, con PC/tableta y documento institucional sin inversión de colores. |
| Alta — ambigüedad interna de porcentaje T2 | §10.3 exige calcular porcentaje desde evidencias/actividades y evitar cifras incompatibles; §12.3 admite escala institucional/manual documentada y la declara pendiente; REP-001 también está abierto. | Separar «cobertura de evidencias», «actividades completas» y «porcentaje reportado de ejecución». Definir cuál ocupa el T2 y cómo se valida. No atribuir a carga de PDF un resultado físico ni introducir fórmula definitiva sin confirmación. |
| Alta — interpretación todavía pendiente | §9.2 propone que la última firma de varios elaboradores inicie revisión y prescribe orden configurado; SIG-002 requiere confirmación de la secuencia. | Es una propuesta técnica razonable, no evidencia nueva del cliente. Preparar política configurable y validarla antes de activar un flujo institucional diferente. No convertir el ejemplo en regla irreversible. |
| Media — fuentes incompletas / no verificables | §1 cita transcripciones por nombres genéricos y un `client-decision-guide.pdf`; no se ubicaron esos nombres en `docs`. | Enlazar los originales disponibles o registrar que la cita es reconstruida/no verificada. No restaurar documentos eliminados. La atribución [CLIENTE] exige fuente/fecha verificable, no solo etiqueta. Tampoco se pudo corroborar en otros documentos de `docs` la afirmación de §23 «metodología elegida explícitamente por el usuario: Scrum + Kanban»; puede proceder de material externo, pero necesita referencia. |
| Alta — insumo institucional omitido | El inventario §1.2 y listado §6.2 no incorporan `UNIDADES Y COMISIONES JULIO_DICIEMBRE_2026_(SISTEMAS-SOFTWARE-TI).pdf`. | Existe en `docs/templates` y fue aportado posteriormente por el propietario. Incorporarlo con vigencia y procedencia; conciliarlo con `comisiones.xlsx`, sin asumir correos o etapas de revisión que el PDF no especifica. No se reauditaron aquí los nombres/personas del PDF. |
| Media — falta de especificación visual de observaciones | §§8.3/10.5 admiten observaciones por sección/página y AT-014 pide «observación resaltada», pero no define claramente selección de una zona del PDF y geometría del resaltado. | La línea base exige resaltado sencillo sobre una zona y observación asociada. Conservar esa capacidad expresamente; distinguir resaltado visual de texto en un panel y vincular la anotación a artefacto/ronda. |
| Media — transición técnica, no incumplimiento nuevo | §18 propone JPA, API v1, Testcontainers y HTML→PDF; el modelo §19 utiliza nombres distintos de las tablas actuales. | Parte ya figuraba en el Plan Maestro. El código actual usa JDBC, `/api`, pruebas PostgreSQL por Compose y DOCX oficial para T1. Las rutas/tablas del documento son orientativas; no renombrar todo ni sustituir render correcto por cumplir ejemplos. Planificar transición compatible solo donde aporte y esté requerida. |
| Media — confusión entre objetivo y estado | RF-AUT-006/§17.3 siguen hablando de CAPTCHA DEMO que debe reemplazarse; §27.1 afirma «ya existe motor de asignación». | CAPTCHA productivo de servidor ya está implementado. Existe configuración de flujos, no motor completo de tareas/actores por carrera/período. Corregir esas frases y separar requisitos objetivo de `implementation-status.md`. |
| Media — descarga pendiente de delimitación | §§8.4/26 prohíben PDF final antes de APPROVED, como ya hacía el Plan Maestro. El producto parcial permite descargar preview y artefacto firmado del elaborador en DRAFT. | Es una brecha ya documentada, no una contradicción nueva entre maestros. Delimitar «PDF final», previsualizaciones, artefactos históricos y anexos originales; no cambiar endpoints de descarga sin contrato y pruebas de la política aprobada. |
| Media — límites de la política de firma | SIG-001/§10.6 elevan la política DTIC a bloqueo de uso productivo. | La entrega vigente valida identidad mediante huella pública vinculada, vigencia e integridad y declara cadena/revocación NOT_CHECKED. No descartar ese trabajo ni asumir que OCSP/TSA son decisiones ya aprobadas. Distinguir alcance técnico entregado, aceptación institucional pendiente y cualquier ampliación posterior. |
| Media — trazabilidad de IDs | Nueva numeración RF/RNF/AT frente a FR-* y UT-* existentes. Firmas, observaciones, IA y reportes contienen requisitos en prosa sin RF individual. | Crear correspondencia de IDs existentes→nuevos, conservar historial y atomizar reglas faltantes. No hacer que renumerar aparente eliminar requisitos ni acreditar pruebas no ejecutadas. |
| Baja — formato de reportes | §25 y §29 piden `docs/REPORTE_AUDITORIA_IMPLEMENTACION_VS_ESPECIFICACION.md`. | El propietario estableció `docs/reports` y Markdown nuevo por entrega. Ajustar la ruta/nomenclatura del documento, conservando la matriz de auditoría propuesta. |

## Diferencias con la implementación que son pendientes reales

La especificación objetivo requiere capacidades todavía pendientes y compatibles con el plan de desarrollo: organización por carrera/período/cargo, propiedad organizacional de T1, asistente completo, archivo lógico, workflow de tareas secuenciales, observaciones y rondas, múltiples firmas, ejecución/evidencias/prórrogas, T2, plantillas administrables, reportería y operación institucional.

Comprobación estática concreta: `WorkPlanService.java` todavía consulta/autoriza por `teacher_id`; la carrera continúa como texto. La nueva especificación no introduce esa brecha: ya está registrada en el estado y Plan Maestro. Tampoco la configuración administrativa de flujos equivale a revisión documental ejecutable.

Las diferencias entre objetivos de una especificación y módulos parciales **no prueban por sí solas que el código actual esté mal**; identifican entregas necesarias. La fecha del documento tampoco acredita las atribuciones institucionales o elimina decisiones explícitas posteriores del propietario.

## Recomendación para incorporar el archivo

1. Conservarlo como consolidación candidata y corregir los puntos anteriores, sin tocar aún las funcionalidades actuales.
2. Registrar referencias verificables de cada decisión nueva o reinterpretada. Las decisiones de seguridad/firma, T2 y múltiples elaboradores deben conservar sus pendientes explícitos.
3. Incorporar temas, toast, separación administrativa, agente código/Trello e insumo institucional posterior.
4. Crear equivalencias de requisitos y una matriz de brechas con evidencia por archivo/prueba. Esta revisión es comparativa y dirigida: **no sustituye una auditoría exhaustiva RF→código→prueba de todo el repositorio**.
5. Cuando el propietario adopte la consolidación corregida, actualizar `AGENTS.md`, `requirements.md`, índice de decisiones y registro de pendientes para reconocer una sola fuente. Mantener el Plan Maestro anterior como antecedente histórico, sin borrar la trazabilidad.
6. Mantener la próxima entrega propuesta: carreras/pertenencias/cargos/responsable principal y titularidad/unicidad por ámbito. No reiniciar la fundación ya entregada ni retroceder al diseño del mockup.

## Verificación y límites

Lectura/comparación documental, conteo de identificadores, inventario de archivos y búsquedas estáticas dirigidas. No se ejecutaron tests, build ni análisis legal/criptográfico nuevo porque no hay implementación. No se accedió a Trello ni se corroboraron reuniones originales fuera de las fuentes disponibles. No se inspeccionó la DB ni se alteraron usuarios. El archivo nuevo del propietario permanece intacto y staged; este reporte y su índice quedan sin staging ni commit.
