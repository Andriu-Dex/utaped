# UTAPED — Guía de reunión para confirmar requisitos y decisiones

Preparada: 2026-10-05. Estado: preguntas para validación; no constituye normativa, aprobación institucional ni respuestas del cliente. Basada en requisitos, catálogo, implementación y Q-001–Q-019 vigentes. Las opciones mencionadas son alternativas para preguntar, no reglas elegidas.

## 1. Objetivo y forma de usar esta guía

Obtener reglas suficientemente precisas para implementar el sistema productivo y comprobar su aceptación. Llevar los mockups como referencia visual y los formatos oficiales como referencia documental. Una pantalla del prototipo no demuestra que una regla haya sido aprobada.

Para cada pregunta registrar respuesta, grupos/tipos a los que aplica, excepciones, persona que valida y en qué calidad, fecha, evidencia y vigencia. Si el interlocutor no puede confirmar una regla, registrar a quién consultará y una fecha de respuesta; no marcarla como aprobada. “Depende del grupo” requiere una respuesta separada por grupo. “Igual que ahora” requiere describir el procedimiento actual y aportar un caso real anonimizado.

No recopilar contraseñas, .p12/.pfx personales, claves privadas, tokens de Trello/GitHub ni credenciales de infraestructura en este documento. Solicitar ejemplos anonimizados, documentos públicos y especificaciones; gestionar accesos posteriormente por un canal seguro.

### Datos de la reunión

| Campo | Completar |
|---|---|
| Fecha y modalidad | |
| Participantes y área/función | |
| Responsable del proceso que puede validar reglas | |
| Contacto técnico DTIC o equivalente | |
| Responsable de formatos/documentación | |
| Responsable de datos personales/archivo | |
| Quien aceptará la primera versión | |
| Grupos/unidades y tipos documentales cubiertos | |
| Enlace o referencia del acta/evidencias | |

### Orden sugerido y prioridades

- **P0 — Antes de finalizar/enviar/revisar:** secciones 3–7: titularidad, flujos, decisiones, corrección, períodos y aceptación de firma.
- **P1 — Antes de ejecución y T2:** secciones 8–11: formatos, actividades, evidencias, Informe y permisos completos.
- **P2 — Antes de publicar y operar:** secciones 12–15: acceso, datos, infraestructura, comunicaciones y aceptación. P2 indica momento de resolución, no menor importancia de seguridad.
- **P3 — Funciones de IA:** sección 16. La parte código/Trello se acuerda con el propietario/equipo de desarrollo; no necesariamente con usuarios institucionales.

Si no participan todas las áreas, completar lo disponible y asignar responsables para las respuestas restantes. No exigir al cliente funcional que elija librerías o algoritmos técnicos.

## 2. Decisiones del proyecto ya establecidas

No es necesario volver a elegir estas bases salvo que la institución comunique una restricción incompatible:

| Base actual | Qué falta ratificar o aportar |
|---|---|
| Aplicación productiva modular; mockup externo congelado como referencia | Alcance de primera versión y aceptación de flujos/formato. |
| React/TypeScript, Java 21/Spring Boot y PostgreSQL/Flyway | Entorno, restricciones y soporte institucional; no reiniciar la selección tecnológica. |
| Credenciales locales | Altas/bajas, políticas de sesión/contraseña, recuperación y correo real. No hay cambio a SSO acordado. |
| Firma con archivo .p12/.pfx, contraseña y botón Firmar; sin persistir material privado | Aceptación institucional, procedimiento de identidad y requisitos de verificación/interoperabilidad. |
| Firma del elaborador sobre PDF conservado, con huella pública vinculada por administración | La cadena emisora y revocación actualmente NO se comprueban. La institución debe pronunciarse sobre aceptación y requisitos adicionales. |
| Borrador separado del PDF firmado; PDF histórico inmutable | Cuándo se congela el documento enviado y reglas de corrección/finalización. Hoy el Plan sigue DRAFT y no se envía ni aprueba. |
| Almacenamiento local privado; inicialmente 10 MB por anexo y 20 anexos, ajustables | Límites institucionales, evidencia, PDF total, custodia y retención. |
| T1 A4 con estructura/estilo institucional y matriz horizontal; anexos en su formato original | Aceptación final por institución, originales vigentes y T2. |
| Agente para inspeccionar código y planificación de UTAPED en Trello | Fuentes, permisos, criterios, ejecución, destinatarios y presupuesto. No inspecciona proyectos institucionales por defecto. |

Implementado no equivale a aprobado institucionalmente. La firma simple entregada tampoco equivale a finalización, revisión o validez jurídica acreditada.

## 3. Alcance, titularidad y creación del Plan — Q-001/Q-002

**A quién consultar:** responsable del proceso y elaboradores de grupos/comisiones. **Salida:** lista de documentos/grupos y reglas de creación sin ambigüedad.

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| A01 | ¿Qué grupos, comisiones y unidades usarán UTAPED en la primera versión? ¿Solo FISEI? | Lista oficial, tipo, denominación y alcance de cada uno; qué queda fuera. |
| A02 | ¿El Plan pertenece a un docente o al grupo/comisión? ¿Quién figura como elaborador y quién es responsable institucional? | Titular, autor visible, responsables y permisos; distinguir elaboración individual de representación colectiva. |
| A03 | ¿Puede existir más de un Plan del mismo docente, grupo y período? ¿En qué casos? | Número permitido, criterio de duplicado, excepción y quién la autoriza; aclarar si borradores cuentan. |
| A04 | ¿Varias personas editan un mismo Plan o cada una crea el suyo? ¿Hay delegación o sustitución del elaborador? | Actores, secciones editables, límites y procedimiento de cambio; no confundir colaboración con compartir credenciales. |
| A05 | ¿Qué ocurre si se creó un Plan por error: eliminar borrador, anular, cancelar o archivar? ¿Después de firma/envío puede cancelarse? | Actor, estados permitidos, motivo obligatorio, efecto en duplicados y conservación del historial. |
| A06 | ¿Se aprueba el asistente de seis pasos del mockup o se necesita matriz como séptimo paso separado? | Orden exacto y agrupación de Información, Contenido, Actividades/Matriz, Anexos, Previsualización y Firma/Finalización. |
| A07 | ¿Qué campos son obligatorios al guardar borrador y cuáles solamente al enviar? | Lista por sección, longitudes/reglas especiales y ejemplo válido/inválido; título, unidad, carrera, justificación, objetivo y fuente. |
| A08 | ¿Unidad académica, carrera, cargo y nombres se obtienen de catálogos o se escriben manualmente? | Catálogos oficiales, responsable de mantenimiento y relación de cada usuario/grupo con esos valores. |
| A09 | ¿Se necesita copiar un Plan previo o importar documentos existentes? | Documentos elegibles, datos que se copian, qué debe actualizarse y qué no debe heredarse (firmas, decisiones, evidencias). |

Solicitar al menos un Plan típico y un caso excepcional anonimizados. Acordar si estas reglas son comunes o requieren configuración por grupo.

## 4. Flujo de aprobación por grupo y tipo — Q-003/Q-018

**A quién consultar:** quienes elaboran, revisan y validan el proceso. **Salida imprescindible:** una ficha distinta por grupo y por T1/T2; no aceptar una lista genérica de cargos como flujo completo.

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| F01 | ¿Quién recibe el T1 después del elaborador y quién recibe el T2? ¿Cuál es el orden? | Etapas ordenadas, nombre de cada acción y resultado esperado. |
| F02 | ¿Cada etapa la realiza una persona, todas las personas de un rol, una selección concreta o un órgano colegiado? | Forma de asignación, usuarios/roles aplicables y resolución de representantes. |
| F03 | ¿Hay etapas o revisores en paralelo? ¿Qué debe terminar antes de activar la siguiente etapa? | Secuencia y dependencias; distinguir paralelo dentro de etapa de etapas paralelas. |
| F04 | ¿Deben aprobar TODOS los revisores obligatorios, basta UNO o se necesita una cantidad/quórum? | Regla exacta por etapa; qué ocurre con quienes no intervinieron y cómo se muestra esa situación sin atribuirles aprobación. |
| F05 | Si un revisor devuelve y otro aprueba, ¿qué decisión prevalece? ¿Puede seguir decidiendo el resto? | Resolución de decisiones contradictorias, cancelación/espera de tareas y resultado de la etapa. |
| F06 | ¿Una persona puede elaborar y revisar/aprobar el mismo documento? ¿Puede ocupar varias etapas? | Compatibilidades, conflictos de interés y quién los controla. |
| F07 | ¿Qué etapa requiere firma electrónica y cuál solo una decisión autenticada? ¿Hay firma del órgano o de su representante? | Requisito por etapa, acción/cargo visible y firmante real autorizado. |
| F08 | ¿Qué pasa si una etapa no tiene participantes activos, falta un representante o cambia un cargo? | Bloqueo, sustitución o reasignación autorizada; quién decide y evidencia requerida. |
| F09 | ¿El flujo queda fijado al crear, firmar o enviar? Si cambia después, ¿afecta documentos en curso? | Momento de captura, regla de migración/excepción y trato de expedientes históricos. |
| F10 | ¿Qué evento hace que el Plan esté aprobado y listo para ejecutar? ¿Es distinto de finalizado o validado? | Evento verificable, autoridad, estado visible y acciones habilitadas. No basta “cuando todos terminan”. |
| F11 | ¿Hay plazos de revisión, recordatorios, suplencias o escalamiento? | Cantidad de días, hábiles/calendario, responsable, evento de inicio y efecto de vencimiento; nunca aprobación automática implícita. |
| F12 | ¿Se requieren fases externas o aprobación documental fuera de UTAPED? | Etapa de salida/retorno, responsable y evidencia; completar sección 6. |

### Ficha para completar por cada flujo

Grupo/unidad: ____ · Tipo: T1 / T2 · Vigencia: ____ · Validador/evidencia: ____.

| Orden | Etapa/acción | Persona/rol/órgano y ámbito | Obligatorios y opcionales | Secuencial/paralelo | Regla exacta de avance | ¿Firma? ¿Quién? | Si devuelve | Plazo |
|---|---|---|---|---|---|---|---|---|
| 1 | | | | | | | | |
| 2 | | | | | | | | |
| 3 | | | | | | | | |

Añadir filas necesarias. Registrar también quién inicia el flujo y cuál es la condición final. Usar la [ficha de confirmación existente](workflow-signature-confirmation.md) como apoyo técnico.

**Casos que deben explicar durante la reunión:** todas las aprobaciones; una devolución; una aprobación y una devolución simultáneas; revisor ausente; baja de miembro con tarea activa; cambio de flujo mientras se revisa. Pedir resultado esperado de cada caso.

## 5. Finalización, observaciones, correcciones y versiones — Q-003/Q-006/Q-017/Q-018

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| R01 | ¿Firmar y enviar son acciones separadas? ¿Puede conservar una firma sin enviar? ¿Cuándo se bloquea la edición? | Secuencia exacta, confirmación requerida y relación entre borrador, artefacto firmado y documento enviado. |
| R02 | ¿Qué nombre y significado tendrá cada estado de T1/T2? ¿Qué acciones puede realizar cada actor en él? | Tabla de transiciones; distinguir borrador, firmado, enviado, devuelto, en corrección, validado, en ejecución y finalizado si aplican. |
| R03 | ¿Quién puede registrar observaciones generales, por sección/página o por zona resaltada? ¿Cuándo puede editarlas/eliminarlas? | Autor autorizado, momento de congelación y si quedan borradores privados antes de decidir. |
| R04 | ¿Quién ve observaciones de otros revisores y las respuestas del elaborador? | Visibilidad antes/después de decisión y acceso en rondas/histórico. |
| R05 | ¿Devolver exige observaciones o motivo obligatorio? ¿Debe responderse cada observación? | Validaciones de devolución y de reenvío; mecanismo de respuesta/resolución. |
| R06 | Tras corregir, ¿se reinicia todo el flujo o se vuelve a la etapa que devolvió? ¿Qué aprobaciones deben repetirse? | Regla por etapa/grupo, nueva ronda y asignaciones; firmas previas permanecen históricas y no validan bytes nuevos. |
| R07 | ¿Qué se puede editar durante una corrección? ¿Puede cambiar matriz, responsables, anexos o solo las secciones observadas? | Alcance, revalidación necesaria y plazo de corrección. |
| R08 | ¿Quién autoriza una nueva versión formal 2.0/3.0 y por qué? ¿Qué ocurre con la versión vigente? | Procedimiento y evidencia, relación de versiones y efecto en ejecución/T2. Una devolución por sí sola no crea versión formal. |
| R09 | ¿Puede retirarse un envío, revocarse una decisión o anularse un aprobado? | Actor, precondiciones, motivo y procedimiento; preservar decisiones originales como historial. |
| R10 | ¿Cómo se comunica un conflicto de edición o una operación ya realizada? | Comportamiento esperado: recargar/comparar/reintentar, conservación del contenido local y prohibición de sobrescribir silenciosamente. |

### Tabla de transiciones a ratificar

| Tipo | Estado origen | Acción/evento | Actor y ámbito | Precondiciones | Estado destino | ¿Contenido editable? | Artefacto/ronda/firmas afectados | Avisos |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | | |

No basta confirmar etiquetas; completar las acciones y precondiciones que cambian el estado.

## 6. Procedimiento externo / QIPOC — Q-004

**Consulta condicional:** primero confirmar si existe y aplica. Si no aplica, registrar expresamente NO APLICA y quién lo confirmó.

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| X01 | ¿Qué es QIPOC en este proceso y qué grupos/documentos lo necesitan? | Nombre oficial, función, procedimiento y versión vigente; no inferir su significado. |
| X02 | ¿Quién descarga el documento, realiza la gestión externa y carga el resultado? | Actores, estado de salida y quién tiene permiso sobre el expediente. |
| X03 | ¿Qué archivo se descarga y qué retorna: mismo PDF con nuevas firmas, documento distinto o acta? | Formato, límites, relación con artefacto de salida y validación de integridad si aplica. |
| X04 | ¿Quién verifica el retorno y qué evidencia convierte la gestión en aprobada? | Decisión, firma requerida, estado final y tratamiento de rechazo/archivo equivocado. |
| X05 | ¿Es carga manual o existe integración institucional autorizada? ¿Qué pasa mientras no responde? | Contrato/contacto técnico si existe, espera, reintentos y actuación en fallos; no pedir contraseñas. |

## 7. Períodos, fechas, cierre y excepciones — Q-006/Q-011

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| P01 | ¿Qué ventanas separadas hay para elaborar, enviar, corregir, ejecutar, cargar evidencia e informar? | Fecha/hora inicial y final por período y grupo, dependencia entre ventanas y quién las administra. |
| P02 | ¿La zona oficial es America/Guayaquil? ¿El último día admite hasta las 23:59:59 o se corta a otra hora? | Zona, precisión e inclusión del instante límite; definir qué ocurre exactamente al inicio del día siguiente. |
| P03 | Si una carga empieza antes del límite y termina después, ¿se acepta? ¿Cuenta recepción, finalización de carga o guardado? | Evento exacto del servidor usado para decidir, para envío, firma y evidencia. |
| P04 | ¿Se permiten actividades en fines de semana/feriados? ¿Se bloquea inicio/fin o todos los días? | Regla por período/grupo, feriados nacionales/locales/institucionales y responsable del calendario. |
| P05 | ¿Qué ocurre con un documento o revisión pendiente al cerrar período? ¿Quién lo cierra? | Transición, solo lectura y destino de tareas; distinguir cierre automático y cierre autorizado. |
| P06 | ¿Existen prórrogas o reaperturas? ¿A quién se aplican y quién las autoriza? | Motivo/evidencia, fecha/hora, ámbito y acciones permitidas; no suponer excepción universal. |
| P07 | ¿Se puede corregir un documento devuelto fuera de la ventana original? | Plazo propio o autorización, actor y efecto sobre el cierre. |
| P08 | ¿Cuándo queda fijada la fecha de elaboración del T1/T2? ¿Qué muestra una corrección o versión formal nueva? | Regla para portada, firmas e historial; fechas distintas que no deben confundirse. |

Solicitar un calendario real aprobado y ejemplos de fin de período, día feriado y devolución fuera de plazo.

## 8. Firma electrónica y aceptación institucional — Q-007

**A quién consultar:** responsable del proceso; dudas de interoperabilidad/confianza a DTIC o al área competente identificada por el cliente. No solicitar secretos. El uso .p12/.pfx ya está decidido; no es necesario proponer otro mecanismo sin restricción nueva.

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| S01 | ¿Acepta la institución el flujo simple archivo + contraseña + Firmar y la firma visible actual? | Aceptación por tipo/etapa y validación del PDF de ejemplo; quién puede aprobarla. |
| S02 | ¿Quién comprueba que el certificado público pertenece al usuario y cómo lo documenta? | Responsable, comprobación confiable, dato vinculante y referencia de evidencia; hoy se vincula huella SHA-256 por administración. |
| S03 | ¿Cómo se gestiona alta, renovación, pérdida o desvinculación del certificado? | Actor, procedimiento, vigencia de la vinculación y acceso al historial. Desvincular en UTAPED no consulta revocación del emisor. |
| S04 | Actualmente comprobamos huella, vigencia, uso e integridad; no comprobamos cadena emisora ni revocación. ¿Se acepta ese alcance o hay requisitos institucionales adicionales? | Pronunciamiento explícito y responsable. Si requiere confianza/revocación, pedir especificación pública de emisores/cadenas y OCSP/CRL, y qué hacer ante resultado desconocido o falta de conexión. |
| S05 | ¿Se requiere un perfil PAdES, sello de tiempo externo o conservación verificable a largo plazo? | Requisito concreto y verificador/servicio autorizado si corresponde; la hora del servidor no equivale a sello de tiempo. Si el cliente no sabe, asignar consulta a DTIC. |
| S06 | ¿Con qué herramienta se acepta/verifica el PDF? ¿Qué mensaje o evidencia debe producir? | Herramienta/versión y criterio; un visor puede advertir confianza aunque la firma matemática sea correcta. |
| S07 | ¿Qué debe mostrarse en la celda de firma: nombre, cargo, fecha/hora y algún otro dato? | Ejemplo oficial y posición/legibilidad; no añadir QR/códigos institucionales sin formato aprobado. |
| S08 | ¿Se firma una vez todo el documento o hay varios firmantes sucesivos/en paralelo? ¿Se incluyen anexos en los bytes firmados? | Orden, alcance, celda por firmante y tratamiento de firma fallida; múltiples firmas aún no están entregadas. |
| S09 | ¿Puede firmar un suplente o representante? ¿Cómo se acredita su autorización? | Identidad real, asignación, período y cargo visible; nunca usar el certificado o cuenta de otra persona. |
| S10 | ¿Qué sucede si el certificado vence o se desvincula después de firmar? | Política de consulta/aceptación histórica y nuevas firmas; conservar siempre bytes originales. |
| S11 | ¿Se requiere firma externa y reingreso del PDF además de firma dentro de UTAPED? | Casos autorizados, verificación del retorno y permisos; relacionar con X03. |

Solicitar PDFs firmados de muestra anonimizados o autorizados y certificados **públicos** de prueba cuando sea necesario. La institución define sus requisitos; el equipo traduce estos requisitos al contrato técnico sin inventar aceptación legal.

## 9. Formatos oficiales, plantillas y datos del documento — Q-001/Q-012/Q-013

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| D01 | ¿Cuáles son las versiones oficiales vigentes de UTA-SGC-A-2-1-P7-T1 y T2? | DOCX original, PDF de ejemplo aceptado, fecha/revisión, custodio y evidencia de vigencia. |
| D02 | ¿Se ratifica A4, con matriz horizontal y anexos en tamaño original? ¿T2 también usa A4? | Formato por sección/tipo y cualquier excepción; aclarar discrepancia con originales Carta. |
| D03 | ¿Qué estructura, encabezados, logos, pie, numeración, índices, tablas y orden son obligatorios? | Ejemplo anotado de cada documento; campos fijos frente a configurables. |
| D04 | ¿Qué instrucciones del DOCX se deben omitir y qué notas sí deben aparecer? | Lista exacta y condición de aparición; en especial contactos y nota de protección de datos. |
| D05 | ¿Se permiten secciones/columnas opcionales o cambios de orden por grupo? ¿Quién publica una nueva plantilla? | Configuración permitida, aprobación, vigencia y tratamiento de borradores frente a artefactos ya conservados. |
| D06 | ¿Qué denominación institucional muestra “Seleccionar todos” los responsables? | Denominación por grupo/caso y aprobación; selección individual se conserva para trazabilidad. |
| D07 | ¿Qué datos lleva el historial formal de cambios y quién registra el motivo? | Versiones, fecha, descripción y autoridad; no mezclar ronda de revisión ni versión de evidencia. |
| D08 | ¿Se entrega solo PDF o también DOCX/otro formato? ¿Se necesita PDF/A, copia para impresión o metadatos específicos? | Formatos realmente requeridos y criterio de aceptación técnica; derivar consulta al área competente. |

Solicitar aprobación visual de ejemplos cortos y largos, con tablas extensas, nombres largos, varios firmantes y anexos. Validar páginas reales y legibilidad; no fijar un número de páginas arbitrario.

## 10. Actividades, matriz y evidencias — Q-010/Q-011/Q-017

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| E01 | ¿Cuál es el catálogo inicial de actividades obligatorias/opcionales, recursos y medios por grupo? | Datos oficiales, vigencia, quién los administra y texto/códigos si existen. |
| E02 | ¿Se permite actividad “Otra”, recurso “Otro” y medio “Otro”? ¿Requieren aprobación? | Campos obligatorios, quién aprueba y si se convierten después en catálogo. |
| E03 | ¿Qué cambios de actividad, fechas o responsables se permiten después de aprobar el Plan? | Actor, autorización, necesidad de nueva versión formal y efecto sobre ejecución/T2. |
| E04 | ¿Cuándo empieza la ejecución y cuándo se considera completada una actividad? | Precondiciones y regla; distinguir archivo cargado, evidencia validada y actividad cumplida. |
| E05 | ¿Se ratifica exactamente un PDF vigente por medio de verificación? ¿Puede un PDF servir para varios medios/actividades? | Cardinalidad y regla de vinculación; no confundir anexos del Plan con evidencia de ejecución. |
| E06 | Con varios responsables, ¿cualquiera puede cargar/reemplazar? ¿Solo coordinador? ¿Hay delegación? | Permisos por acción y casos de retiro/sustitución; identificar al actor real de cada carga. |
| E07 | ¿Quién valida cada evidencia y cómo se asigna? ¿Puede validar quien la cargó? | Revisor, ámbito, conflictos de interés y si exige decisión o firma. |
| E08 | ¿Cuál es el plazo de evidencia: fin de actividad u otra fecha? ¿Se puede reemplazar después de observar o validar? | Fechas/horas, estados permitidos, excepción y responsable; completar P02/P03/P06. |
| E09 | ¿Cada reemplazo debe reiniciar validación y conservar la versión anterior? ¿Puede retirarse una carga equivocada? | Regla por estado, motivo, acceso a versiones y comportamiento de tareas pendientes sobre versión antigua. |
| E10 | ¿Qué significa evidencia observada y qué hace el responsable para subsanarla? | Motivo obligatorio, aviso, plazo y retorno a validación; no confundir con devolución del Plan. |
| E11 | ¿Hay actividades canceladas, no realizadas o parcialmente cumplidas? ¿Cómo se justifican y revisan? | Estados/campos, autorización y representación en T2 e indicadores. |
| E12 | ¿Qué límites reales se necesitan para anexos, evidencias y PDF compuesto? ¿Se aceptan PDF con contraseña, firmas previas o contenido activo? | MB, cantidad por documento/medio, páginas, formatos permitidos y tratamiento de rechazo/análisis de archivos. Los 10 MB/20 anexos son valores técnicos iniciales. |

Solicitar un caso completo: actividad → dos medios → carga → observación → reemplazo → validación → representación en el Informe, con fechas concretas y actores distintos.

## 11. Informe T2 — Q-005/Q-013

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| T01 | ¿Desde qué estado del Plan se puede crear un Informe? ¿Debe existir aprobación o ejecución concluida? | Estados elegibles, ventana y actor autorizado. |
| T02 | ¿Se permite Informe independiente sin Plan? ¿Para qué grupos y con qué autorización? | Casos habilitados, datos obligatorios y procedencia de actividades; si no aplica, prohibición expresa. |
| T03 | ¿Hay un Informe por Plan o varios (parcial, final, por período, por responsable)? | Cardinalidad, tipos, cobertura y criterio de duplicado. |
| T04 | ¿Lo crea el elaborador del Plan, cualquier miembro o una persona asignada? | Titular/autor, sustitución y permisos de edición/firma. |
| T05 | ¿Qué datos del Plan se copian y quedan fijados? Si el Plan cambia después, ¿el Informe conserva su referencia original? | Versión formal/artefacto base, importación de datos y tratamiento de cambios sin modificar silenciosamente el Informe. |
| T06 | ¿Qué secciones requiere T2 y cuándo aparece tabla de contactos? | Orden, obligatoriedad y condición; antecedentes, desarrollo, conclusiones, mejoras, contactos, anexos, firmas e historial. |
| T07 | ¿Qué se informa por actividad: ejecución, fechas reales, resultados, porcentaje, causas de incumplimiento, observaciones y evidencias? | Campos exactos, cálculo de porcentajes si aplica y ejemplos; evitar indicadores inventados. |
| T08 | ¿Puede enviarse el T2 con evidencias faltantes, observadas o pendientes de validar? | Requisitos de envío y excepciones autorizadas. |
| T09 | ¿El T2 incorpora evidencias como páginas anexas, referencias o enlaces privados? | Artefacto exacto que se firma, acceso de revisores y disponibilidad futura. |
| T10 | ¿El flujo T2 es igual al T1? ¿Qué evento cierra Informe y Plan? | Ficha de flujo propia, firma requerida, devolución/corrección y condición de cierre. |

Solicitar un T2 aprobado y un T2 con actividad no cumplida o cumplimiento parcial, anonimizados.

## 12. Usuarios, roles, visibilidad y cambios de miembros — Q-008/Q-012/Q-017

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| U01 | ¿Quién da de alta usuarios, verifica sus datos y asigna grupos/roles? ¿Solo correo institucional? | Proceso de alta y catálogo de roles; dominios autorizados y casos externos. |
| U02 | ¿Qué puede ver/hacer cada rol en documentos propios, del grupo, asignados e históricos? | Matriz por acción, ámbito y estado; no basta una lista de nombres de roles. |
| U03 | ¿Administración necesita leer contenido/descargar documentos ajenos o solo administrarlos? | Permiso explícito, finalidad, ámbito y auditoría. Actualmente no hay acceso documental adicional por ser ADMIN. |
| U04 | ¿Un nuevo miembro puede leer documentos anteriores? ¿Un miembro retirado conserva acceso a los propios/históricos? | Regla temporal por documento/evidencia; acceso actual no implica derecho histórico automático. |
| U05 | ¿Qué ocurre con documentos, responsabilidades y revisiones pendientes si se desactiva usuario/grupo o cambia coordinador? | Reasignación, autoridad, notificaciones, plazos y conservación de atribución original. |
| U06 | ¿Qué pasa al desactivar o cambiar una actividad/recurso/medio de catálogo? | Efecto en nuevos borradores, borradores guardados, aprobados, evidencias e histórico. |
| U07 | ¿Se necesita importación CSV/Excel? ¿Qué identificador evita duplicados? | Plantilla, campos, actualización frente a alta, errores por fila y responsable; no importar contraseñas reales. |
| U08 | ¿Qué política de contraseña, sesión e intentos debe aplicarse? ¿Se requiere MFA o CAPTCHA productivo? | Reglas cuantificadas, tiempo de inactividad/duración máxima, sesiones simultáneas, bloqueo/desbloqueo y alternativa accesible al CAPTCHA. Credenciales locales siguen siendo la base. |
| U09 | ¿Cómo recupera acceso alguien sin correo disponible? ¿Quién autoriza el restablecimiento? | Canal y procedimiento verificado; envío de acceso temporal, vencimiento y cambio obligatorio. |

### Matriz de permisos a completar

Repetir para crear, leer, editar, descargar, firmar, enviar, observar, devolver, aprobar, cargar/reemplazar/validar evidencia, exportar, reasignar, cerrar y administrar.

| Acción | Rol/actor | Ámbito: propio/grupo/asignado/otro | Tipos y estados | Condición de pertenencia | Excepción/delegación | Responsable que valida |
|---|---|---|---|---|---|---|
| | | | | | | |

## 13. Datos personales, archivo y auditoría — Q-010/Q-012

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| V01 | ¿Qué datos personales son indispensables y para qué finalidad? ¿Se requiere cédula, teléfono o contactos? | Campo, finalidad, obligatoriedad, fuente y personas que pueden verlo; no exigir cédula por suposición. |
| V02 | ¿Cuándo debe incluirse la nota de datos personales y cuál es su texto oficial? | Condición objetiva, texto/versionado y área que lo aprueba; no dejar condición a interpretación del programador. |
| V03 | ¿Qué documentos/evidencias contienen información restringida y quién puede descargarlos/exportarlos? | Clasificación y acceso por categoría; si hay publicación, versión anonimizada y autorización explícita. |
| V04 | ¿Cuánto tiempo se conserva cada clase: borrador, anexo retirado, firmado, evidencias/versiones, observaciones, auditoría y backups? | Años/días por clase, inicio del cómputo, custodia y fundamento/procedimiento aplicable. |
| V05 | ¿Cuándo se archiva o elimina y quién lo autoriza? ¿Hay suspensión de eliminación por trámite pendiente? | Procedimiento, motivo/evidencia y alcance; distinguir baja lógica de eliminación física y de copia de respaldo. |
| V06 | ¿Quién puede consultar/exportar auditoría y qué datos debe incluir? | Eventos, filtros, identidad/fecha necesarias, retención y ámbito; acceso protegido contra modificación ordinaria. |
| V07 | ¿Qué procedimiento aplica a incidentes o solicitudes sobre datos personales? | Responsable/contacto, actuación requerida y plazos institucionales; solicitar política oficial, no inventar asesoría jurídica. |

## 14. Infraestructura, respaldo y soporte — Q-009/Q-010/Q-016

**A quién consultar:** DTIC o quien realmente alojará y operará la aplicación. Las credenciales se entregarán aparte por canal seguro.

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| O01 | ¿Dónde se alojará: servidor institucional o proveedor autorizado? ¿Quién administra cada componente? | Entorno aprobado, recursos disponibles, acceso de mantenimiento y compatibilidad con stack/Docker/volumen privado. |
| O02 | ¿Será accesible por Internet, red institucional o VPN? ¿Qué dominio y TLS se utilizarán? | Red, DNS, certificados HTTPS, proxy confiable y responsable de renovación. HTTP local no es configuración productiva. |
| O03 | ¿Qué servicio SMTP enviará recuperación/avisos y qué remitente se autoriza? | Host/puerto/TLS, restricciones y contacto; sin registrar contraseña en acta/repositorio. |
| O04 | ¿Cuántos usuarios, grupos, documentos/año y usuarios simultáneos se esperan? | Valores iniciales y crecimiento, tamaño/páginas típicos y máximos; sirven para pruebas de carga y capacidad. |
| O05 | ¿Qué disponibilidad y tiempos de respuesta son aceptables? ¿Hay horario crítico o mantenimiento? | Valores medibles por función (consulta, PDF, firma/carga), horario y criterio de medición. |
| O06 | ¿Cuánto dato puede perderse y cuánto tiempo puede tardar una recuperación? | RPO (pérdida máxima, minutos/horas) y RTO (tiempo máximo para restablecer); responsable de backups y pruebas. |
| O07 | ¿Cómo se respaldan juntos PostgreSQL y archivos, dónde y cuánto se conservan? | Frecuencia, ubicación autorizada, protección y ensayo de restauración completo; DB sola no recupera PDFs. |
| O08 | ¿Quién atiende incidencias, autoriza actualizaciones y mantiene dependencias? | Contactos/roles, ventanas, procedimiento de actualización/rollback y canales de soporte. |
| O09 | ¿Qué logs/monitorización se permiten y qué política existe para memoria/volcados? | Retención, acceso y exclusión de secretos/cuerpos de firma; nunca capturar .p12/.pfx o contraseña. |
| O10 | ¿Existe información histórica que deba migrarse antes de entrar en producción? | Origen, cantidad, calidad, archivos, permisos y validación de migración; no importar localStorage DEMO como datos oficiales. |

## 15. Comunicaciones, reportes, accesibilidad y aceptación — Q-015/Q-016

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| N01 | ¿Qué eventos notifican a quién: envío, asignación, devolución, corrección, aprobación, evidencia y vencimiento? | Tabla evento/destinatarios/canal; bandeja interna ya disponible, correo/push no se presuponen. |
| N02 | ¿Los avisos son inmediatos o resúmenes? ¿Qué recordatorios se necesitan? | Frecuencia, horario, umbrales, preferencias y tratamiento de correo fallido; lectura no equivale a aprobación. |
| N03 | ¿Qué reportes necesita cada actor y qué filtros/exportaciones? | Nombre, finalidad, campos, ámbito, PDF/CSV/Excel y ejemplo. No se plantean rankings ni sanciones. |
| N04 | ¿Qué dispositivos, navegadores y necesidades de accesibilidad deben soportarse? | Versiones, móvil/escritorio, teclado/lector de pantalla y estándar institucional si existe. |
| N05 | ¿Qué parte del mockup se aprueba y qué cambios solicita el cliente antes del producto? | Pantalla/flujo concreto, problema, cambio y ejemplo; distinguir aprobación visual de regla funcional. |
| N06 | ¿Cuál es el alcance obligatorio de la primera versión y qué puede quedar para después? | Módulos prioritarios, dependencias, piloto, grupos participantes y fecha objetivo a estimar. |
| N07 | ¿Quién ejecutará y firmará la aceptación del piloto? ¿Qué casos y documentos probarán? | Personas, criterios verificables, evidencias y procedimiento de corrección de hallazgos. |
| N08 | ¿Se necesita capacitación, manuales y apoyo de arranque? | Audiencia, formato, responsables y condiciones para habilitar usuarios reales. |

## 16. IA de redacción y agente de código/Trello — Q-014/Q-019

Son dos funciones distintas. No compartir permisos, fuentes o datos entre ellas automáticamente.

### 16.1 Ayuda de redacción T1/T2 — cliente y área competente

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| I01 | ¿Se habilita ayuda de IA para esta versión? ¿En qué campos y para qué acciones? | Justificación, objetivo u otros; corrección/sugerencia, límites y aceptación manual. |
| I02 | ¿Qué datos pueden enviarse a un proveedor externo y cuáles deben excluirse? | Categorías permitidas, anonimización, restricciones de ubicación/retención y responsable que autoriza. |
| I03 | ¿Qué proveedores están autorizados y quién cubre el costo? | Restricciones, presupuesto mensual/por uso, responsable y límites; selección técnica después de confirmar estas condiciones. |
| I04 | ¿Se guardan solicitudes/respuestas y durante cuánto tiempo? ¿Quién las ve? | Trazabilidad mínima, retención y acceso; nunca aplicar cambios sin revisión humana. |
| I05 | ¿Qué significa una sugerencia aceptable y qué debe pasar si la IA falla? | Ejemplos y métricas acordadas; la indisponibilidad no debe impedir redactar/guardar manualmente. |

### 16.2 Agente de inspección del desarrollo — propietario/equipo técnico

| ID | Pregunta concreta | Información que debemos obtener |
|---|---|---|
| G01 | ¿Qué repositorio, ramas/revisiones, carpetas y documentación puede inspeccionar? | Lista autorizada, exclusiones de secretos/datos privados y acceso de solo lectura. |
| G02 | ¿Qué tableros/listas/tarjetas de Trello puede leer? ¿Incluye comentarios y anexos? | IDs/enlaces públicos o referencias, alcance y titular de conexión; credenciales fuera del documento. |
| G03 | ¿Cómo se relacionan tarjetas con requisitos, commits y PRs? | Convención de IDs/etiquetas y ejemplos; cuándo una tarea se considera implementada o pendiente. |
| G04 | ¿Qué mejoras debe sugerir: requisitos faltantes, bugs, seguridad, pruebas, dependencias o planificación? | Prioridades, severidad y evidencia mínima por hallazgo; hechos distintos de hipótesis. |
| G05 | ¿Quién solicita/recibe el análisis y con qué frecuencia? | Manual/programado, disparadores, destinatarios autorizados y formato de reporte. |
| G06 | ¿La primera versión será solo lectura y recomendaciones con revisión humana? ¿Se plantea escritura futura? | Confirmación del límite; cambios de código/Trello requieren alcance y autorización separados. |
| G07 | ¿Qué proveedor/presupuesto, datos permitidos y retención tendrán los análisis? | Límites, ubicación de resultados, acceso y eliminación; distinto presupuesto/política de la ayuda de redacción. |
| G08 | ¿Qué casos demostrarán que el agente funciona correctamente? | Hallazgo trazable a archivo/revisión/tarjeta, error de acceso informado, exclusión de secretos y ausencia de acciones no autorizadas. |

## 17. Documentos y evidencias que necesitamos recibir

- Lista oficial de grupos/unidades, actores y catálogos iniciales; datos minimizados y autorizados.
- Fichas de flujo completas por grupo/tipo; tabla de transiciones y matriz de permisos.
- Originales vigentes T1/T2 y ejemplos aprobados anonimizados, incluida firma visible, historial y anexos.
- Calendario real, reglas de plazo/cierre y procedimientos autorizados de excepción/corrección.
- Procedimiento QIPOC/externo si aplica; ejemplo de entrada/retorno sin secretos.
- Especificación de aceptación de firma y contacto técnico para pendientes; material público de prueba si corresponde.
- Políticas de acceso, datos personales, archivo, respaldo y operación, o responsable/fecha para proporcionarlas.
- Plantillas de importación y ejemplos de reportes/notificaciones requeridos.
- Restricciones de infraestructura/correo y plan de piloto/aceptación.
- Para el agente: fuentes autorizadas de GitHub/Trello y criterios, sin tokens ni credenciales.

No solicitar todos los datos personales reales para definir reglas; ejemplos anonimizados suelen bastar. Si una norma no existe, registrar decisión pendiente, responsable y alcance de una propuesta a validar.

## 18. Registro de respuestas y cierre de la reunión

### Plantilla por decisión

Copiar para cada ID respondido; una sola fila “sí” no sustituye el detalle solicitado en las tablas.

```text
ID(s) de pregunta:
Q relacionado:
Respuesta exacta y regla aplicable:
Grupos / T1-T2 / estados cubiertos:
Excepciones y quién las autoriza:
Ejemplo permitido:
Ejemplo que debe rechazarse y resultado esperado:
Persona / área que valida y en qué calidad:
Fecha de validación y vigencia:
Documento / enlace / referencia de evidencia:
Estado: propuesta / confirmada por proyecto / validada institucionalmente / pendiente / no aplica
Si pendiente: responsable de responder y fecha comprometida:
Impacto o prioridad de entrega:
```

No cambiar a VALIDADA INSTITUCIONALMENTE sin evidencia y atribución a la persona/área competente. Una decisión técnica del propietario puede ser CONFIRMADA POR PROYECTO sin fingir ratificación institucional.

### Lista de salida mínima para la siguiente implementación

- [ ] Titularidad, duplicados y quién puede crear/editar definidos.
- [ ] Ficha T1 del grupo piloto: etapas, actores, paralelismo, regla de avance y firmas.
- [ ] Resolución de devolución, decisiones contradictorias y reenvío/corrección.
- [ ] Relación entre firmar, enviar, congelar contenido y aprobar.
- [ ] Ventanas y tratamiento de corrección fuera de plazo/cambios de miembros.
- [ ] Aceptación o requisitos adicionales para la firma entregada.
- [ ] Validador/evidencia y criterios de aceptación del grupo piloto.

### Seguimiento de pendientes

| ID(s) | Falta responder | Área/persona responsable | Fecha comprometida | Módulo bloqueado | Evidencia pendiente |
|---|---|---|---|---|---|
| | | | | | |

Al recibir respuestas, actualizar open-questions.md, requisitos atómicos, contratos/permisos y casos de aceptación. Esta guía por sí sola no cierra ninguna Q ni autoriza flujos productivos.

## 19. Trazabilidad de las preguntas pendientes

| Cuestión | Secciones/IDs principales |
|---|---|
| Q-001 Asistente T1 | 3: A06–A08; 9: D03–D05. |
| Q-002 Titularidad/duplicados | 3: A02–A05. |
| Q-003 Flujos | 4: F01–F12; 5: R01–R09; ficha de etapas. |
| Q-004 Externo/QIPOC | 6: X01–X05. |
| Q-005 Informe | 11: T01–T10. |
| Q-006 Cierre/excepciones | 5: R06–R09; 7: P01–P08. |
| Q-007 Firma | 8: S01–S11; 5: R01; 14: O02/O09. |
| Q-008 Acceso | 12: U01/U07–U09; 14: O03. |
| Q-009 Infraestructura | 14: O01–O03/O08; stack ya decidido, sección 2. |
| Q-010 Archivos/retención | 10: E05/E08–E12; 13: V03–V05; 14: O06/O07. |
| Q-011 Fechas/feriados | 7: P01–P04/P07/P08; 10: E08. |
| Q-012 Datos/acceso | 9: D04; 12: U02–U04; 13: V01–V07. |
| Q-013 Formatos | 9: D01–D08; 11: T06/T07/T09. |
| Q-014 Ayuda de IA | 16.1: I01–I05. |
| Q-015 Avisos/importación | 12: U07; 15: N01–N03. |
| Q-016 Operación/aceptación | 14: O04–O09; 15: N04–N08. |
| Q-017 Bajas/cambios | 4: F08/F09; 5: R06/R07; 10: E03/E06; 12: U04–U06. |
| Q-018 Decisiones obligatorias | 4: F03–F05; 5: R06; ficha de etapas. |
| Q-019 Agente | 16.2: G01–G08; decisiones del propietario/equipo. |

Fuentes internas: [requisitos](../../requirements.md), [estado de implementación](../../implementation-status.md), [cuestiones abiertas](../decisions/open-questions.md), [catálogo completo](../requirements/catalog.md), [matriz de permisos](permission-matrix.md), [reconciliación con mockup](reconciliation.md), [contrato de firma](../decisions/signature-review-contract.md), [agente](../requirements/project-advisor-agent.md). No se consultó al cliente ni se recibieron respuestas al preparar esta guía.
