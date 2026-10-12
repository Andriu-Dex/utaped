# UTAPED — ESPECIFICACIÓN MAESTRA INTEGRAL PARA DESARROLLO

**Producto:** UTAPED — *UTA + Planificación y Evidencias Docentes*  
**Nombre visible del sistema:** Gestión Documental Académica  
**Institución:** Universidad Técnica de Ambato (UTA), Facultad de Ingeniería en Sistemas, Electrónica e Industrial (FISEI)  
**Versión de esta consolidación:** 2026-10-11  
**Destinatario:** agente de desarrollo de frontend, backend, base de datos, seguridad, firma electrónica, PDF, pruebas e implantación.  
**Finalidad:** única especificación operativa coherente que reúne reglas de negocio, pantallas, flujos, configuración, decisiones técnicas, pruebas, documentación, implantación y **conflictos que no deben resolverse inventando hechos**.

> **Advertencia sobre fuentes:** este documento reúne lo recuperado del historial, transcripciones y archivos de proyecto disponibles. No equivale a una firma de aceptación institucional ni garantiza incluir material privado que nunca fue compartido. Todo punto marcado **[PENDIENTE INSTITUCIONAL]** necesita respuesta o evidencia antes de producción. El agente puede construir arquitectura y pruebas con datos DEMO sin asumir que éstos son información oficial.

---

## 0. INSTRUCCIONES NO NEGOCIABLES PARA EL AGENTE

1. **Antes de programar**, inspeccionar repositorio, README, `AGENTS.md`, `requirements.md`, `implementation-status.md`, migraciones Flyway, tests, mockup y documentos originales. Generar un reporte de brechas por requisito: `IMPLEMENTADO / PARCIAL / AUSENTE / CONTRADICTORIO / NO_VERIFICABLE`.
2. **No reescribir código correcto** ni alterar pantallas ya aprobadas solo por preferencia estética. Integrar backend en el frontend existente React/TypeScript/Vite.
3. Ante discrepancias prevalece: (a) decisión explícita posterior del cliente con fuente; (b) feedback posterior confirmado del mockup; (c) reunión anterior; (d) acuerdo técnico del equipo; (e) valor DEMO. **La fecha manda solo cuando realmente existe contradicción**, no para eliminar funciones compatibles.
4. Distinguir **[CLIENTE]** (expresamente acordado), **[EQUIPO]** (decisión técnica tomada), **[DEMO]** (dato/caso provisional), **[PENDIENTE INSTITUCIONAL]**, **[PENDIENTE DTIC]** y **[CONFLICTO]**. No presentar decisiones técnicas como aceptación del cliente.
5. Utilizar IDs/relaciones para datos canónicos, snapshots de denominaciones para históricos y controles de autorización backend por cada operación. Nada de lógica permanente ligada a nombres DEMO como Andrea, Carlos o Patricia.
6. Ante decisiones pendientes, crear abstracciones o configuraciones preparadas, **pero no activar un comportamiento de negocio sensible no validado**. Las reglas técnicas de seguridad no serán opcionales desde Administración.
7. Mantener `formal_version` separado de `review_round`, `document_state` separado de `execution_status` y `currentUser` separado de `activeContext`.
8. Toda operación crítica debe conservar trazabilidad e historial; documentos firmados, PDFs ya revisados, firmas y evidencias versionadas **no se sobrescriben**.
9. Aplicar cambios mediante migraciones no destructivas, pruebas y revisión de regresión. Migraciones aplicadas no se editan; no perder datos históricos al reconciliar cambios del dominio.
10. Prohibido exponer credenciales, firmas privadas, contraseñas, claves de IA o archivos privados en navegador, logs, repositorio o directorio web.
11. No afirmar que el mockup equivale a seguridad o firma legal productiva. El archivo define **cómo debe ser el software terminado**, no que todo esté implementado hoy.
12. El agente debe dejar rastro de cada tarea en documento de cambios y matriz RF→código→prueba, **pero no ensuciar documentos académicos finales con historia de borradores si el entregable exige versión 1.0**.

---

## 1. INVENTARIO DE FUENTES Y PROVENIENCIA

### 1.1 Fuentes de cliente y análisis

| Fuente (nombre recuperable) | Información que aporta | Condición |
|---|---|---|
| `Pegado text(2).txt` | Primera síntesis extensa de levantamiento: acceso, catálogos, T1, evidencias, plazos, jerarquía, firma, auditoría, IA | Contexto histórico; decisiones posteriores prevalecen |
| `Audios 1-Apuntes_104128.txt` | Transcripción de primera reunión, con fragmentos de reconocimiento imperfecto | Contrastar frases ambiguas, no copiar errores de voz |
| `Se ha pegado el markdown(20260910-000448).md` | Revisión real del prototipo con cliente | Feedback y matices sobre rol, responsable, firma, revisiones |
| `Pegado text(6).txt` | Transcripción adicional, con partes repetidas y errores de audio | Fuente complementaria no normativa |
| `Texto pegado(1).txt` | **Reunión posterior de aclaración del backend**: titularidad, unicidad, multicarrera, firma, estados, revisión secuencial, Informe Final, prórrogas, permisos, IA, puerto y servidor | Fuente funcional prioritaria de octubre de 2026 |
| Feedback de mockup de septiembre | T1 seis pasos, período y unidad automáticos, actividades unificadas, anexos Sí/No, confirmación, nuevo Login | Conservación de UX ya validada |
| `PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md` | Consolidación anterior de reglas del producto y arquitectura | Referencia; este archivo la reemplaza cuando difiera |
| `client-decision-guide.pdf` | Inventario de preguntas y dependencias | **No** usar como prueba de que algo no fue decidido: se redactó sin reconciliar las reuniones |
| `Se ha pegado el markdown(9).md`, `(10).md` y archivos fechados 20260909–20260915 | Inventarios/reportes de módulos React DEMO, auditoría, catálogos, reportes, document engine, T1/T2 y pruebas | Evidencia de prototipo, **no** backend real |

### 1.2 Fuentes institucionales/documentales

- `UTA-SGC-A-2-1-P7-T1 Formato Plan de trabajo (1).docx`: T1 de referencia; incluye **Justificación, Objetivo, Matriz de Actividades, Anexos, Firmas, Control de Historial de Cambios**.
- `UTA-SGC-A-2-1-P7-T2 Formato Informe (1).docx`: T2 de referencia; incluye **Antecedentes, Desarrollo de Actividades, Conclusiones, Oportunidades de Mejora, Registro de Contactos/Delegaciones, Anexos, Firmas e Historial**. El original contempla informes no derivados de plan; **no confundir esa posibilidad general con el Informe Final único del Plan**, que el cliente sí exige enlazar.
- `TI-JD2026-UTIT-PLAN-V01-signed.pdf`: referencia del aspecto final del Plan firmado; contiene seis páginas A4 (según reporte de revisión del mockup). Es ejemplo, no semilla documental productiva.
- `comisiones.xlsx`: lista referencial de 23 grupos/unidades/clubes FISEI. El nombre de la columna es «Nombre Específico Oficial», pero confirmar vigencia y relaciones antes de producción.
- `PROPUESTAS DE MEJORA CARRERA DE TI.xlsx`: fuente de ejemplos de oportunidades/acciones de mejora; no convertir todo su contenido en actividades obligatorias para todas las unidades.
- Formatos/catálogos futuros prometidos por el cliente: deben incorporarse mediante Administración con procedencia y fecha de vigencia.

### 1.3 Principio de fuentes

Registrar en `docs/decisions/` la fuente y fecha de cada regla que afecte titularidad, firma, actores, permisos o retención. Los comentarios del cliente no son licencia para inventar identificadores institucionales o leyes. Ante un nuevo archivo oficial posterior, registrar versión y reconciliar antes de alterar plantillas históricas.

---

## 2. VISIÓN, LÍMITES Y OBJETIVOS

UTAPED es un sistema web interno FISEI para gestionar **planificación → aprobación documental → ejecución de actividades → evidencias → informe final → archivo y reportería**. Reemplaza seguimiento disperso por documentos aislados/correos; concentra trazabilidad institucional, sin evaluar el rendimiento personal de docentes mediante rankings o sanciones automáticas.

**Alcance funcional del producto completo:** administración institucional, cuentas, perfiles y membresías por período/carrera, documentos T1/T2 y otros tipos configurados, PDF institucional, firma digital real, workflow jerárquico, observaciones, versiones, evidencias, control de plazos, reportes agregados, notificaciones, historial, auditoría y asistencia opcional de redacción.

**Límites:** no red social, no LMS, no gestor general de expedientes externo sin decisión, no evaluación disciplinaria, no servicio público de registro, no sustitución automática de autoridades, no decisión autónoma mediante IA. La biblioteca de formatos es extensible, pero un tipo nuevo no adquiere funcionalidades legales/validaciones por mera aparición de un selector.

**Canal objetivo [CLIENTE]:** escritorio como principal, tabletas como soporte; no obligación de optimizar edición documental para teléfono. Compatibilidad web, sin aplicación nativa.

**Escala orientativa [EQUIPO]:** primer grupo de alrededor de 200 usuarios y crecimiento potencial de 2.000 docentes; dimensionar con métricas reales, no escoger microservicios por una cifra estimada.

---

## 3. GLOSARIO DE DOMINIO Y REGLAS DE IDENTIDAD

| Término | Significado correcto |
|---|---|
| Persona/usuario | Identidad humana autenticada, independiente del cargo |
| Rol global | Privilegio transversal: ADMIN, eventualmente autoridad institucional configurada |
| Ámbito/contexto activo | Combinación autorizada de período, carrera, grupo y función; **no** cambia la identidad |
| Grupo institucional | Comisión, unidad, club u otro grupo administrable; no confundir con departamento de TI |
| Miembro / Coordinador / Otro | Tipo de relación orgánica del usuario con el grupo; la figura de responsable editor es una asignación documental explícita |
| Responsable editor | Persona principal habilitada para editar el documento; puede ser reemplazada con trazabilidad |
| Elaborador | Persona registrada para la matriz de firmas y participación; no implica coedición automática |
| Plan T1 | Documento de planificación institucional basado en formato T1 |
| Actividad | Unidad de planificación con fechas, responsables, recursos y medios requeridos |
| Matriz T1 | Representación **derivada** de actividades; no segunda tabla editable |
| Anexo | Archivo complementario del documento; admite formatos documentales permitidos |
| Medio de verificación | Tipo de soporte requerido para acreditar una actividad |
| Evidencia | Archivo PDF o referencia de recurso/repositorio para un medio durante ejecución |
| Informe Final T2 | Documento **único y vinculado** a un Plan; resume ejecución y evidencia |
| Revisión/ronda | Ciclo de aprobaciones del contenido firmado en esa ronda; puede repetirse sin crear versión formal |
| Versión formal | Revisión sustancial de documento previamente APPROVED; v1.0→v2.0, etc. |
| Artefacto | Secuencia exacta de bytes PDF (y firmas incrementales) archivada de manera inmutable |
| Fuente oficial | Plantilla, listado o regla publicada/entregada por autoridad; distinta de seed DEMO |

El nombre **UTAPED** es identificador/acrónimo de proyecto; **Gestión Documental Académica** es el nombre visible aprobado. Mostrar el mismo producto bajo dos nombres sin explicación solo en documentos históricos; en UI prevalece el nombre visible.

---

## 4. ROLES, ACTORES Y AUTORIZACIÓN

### 4.1 Actores funcionales

1. **Usuario docente/integrante:** inicia sesión, consulta ámbitos asignados, visualiza tareas/documentos permitidos, participa en actividades y carga evidencias autorizadas.
2. **Responsable editor del Plan (jefe/coordinador asignado al documento):** único editor activo del Plan; registra actividades, anexos, colaboradores/elaboradores; firma, recibe devoluciones y corrige; realiza Informe Final (salvo reasignación institucional trazable).
3. **Elaborador adicional:** aparece en la tabla formal, puede firmar cuando le corresponda, pero no obtiene edición conjunta por ser elaborador.
4. **Revisor designado:** solo accede a un documento cuando su tarea está activa; lee artefacto, agrega observaciones, firma/aprueba o devuelve. No edita contenido.
5. **Coordinador de ámbito:** organiza miembros/asignaciones autorizadas, puede reasignar cargos y gestionar prórrogas en su ámbito, conforme a matriz explícita. No gana derecho universal a ver expedientes ajenos.
6. **Validador/aprobador final:** etapa institucional que cierra workflow; según configuración puede ser Decano, Subdecano u otro cargo autorizado. No hardcodear una autoridad para todos los grupos.
7. **Decano/Subdecano:** acceso de autoridad a reportes e históricos dentro de su ámbito, separado del derecho de firmar documentos no asignados.
8. **Administrador:** configura usuarios, grupos, períodos, catálogos, plantillas, flujos, feriados y auditoría autorizada. Ser ADMIN no autoriza aprobar/firmar documentos ajenos ni leer todo el contenido sin permiso específico.
9. **Servicio/sistema:** ejecuta transiciones automáticas, alertas, auditoría y generación; nunca firma como persona.

### 4.2 Matriz de permisos mínima

| Operación | Responsable editor | Miembro | Revisor activo | Coordinador | Aprobador activo | Admin |
|---|---|---|---|---|---|---|
| Crear Plan T1 para su ámbito | Sí | No, salvo asignación | No | Sí si es editor designado | No | Solo gestión técnica autorizada |
| Editar T1 `DRAFT` | Sí | No | No | Solo si editor reasignado | No | No por defecto |
| Firmar elaboración | Si slot propio | Si slot propio | No antes de etapa | Si slot propio | No antes de etapa | No |
| Leer artefacto en etapa activa | Si propio | Según ámbito | Sí | Según asignación | Sí al turno | Solo si permiso documental |
| Registrar observaciones/devolver | No | No | Sí | Si etapa asignada | Sí | No |
| Aprobar/firma de revisión | No si es elaborador | No | Sí al turno | Si actor único asignado y sin incompatibilidad | Sí al turno | No |
| Cargar evidencia de actividad | Si responsable permitido | Si responsable permitido | No por rol | Si asignado/responsable | No | No por defecto |
| Aprobar prórroga | No | No | No | Sí dentro de ámbito | Solo si configuración lo permite | No por defecto |
| Gestionar catálogos/plantillas | No | No | No | Acotado según facultad | No | Sí |
| Consultar histórico institucional amplio | Solo propio dentro de autorización | No automático | Solo período/alcance | Según política | Decano/Subdecano según cargo | Solo permiso expreso |
| Ver auditoría global | No | No | No | Auditoría de su ámbito si autorizada | Según autoridad | Sí bajo privilegios específicos |

**Regla de autorización:** cada endpoint verifica `userId + periodId + groupId + careerId + documentId + actor/assignment + current state/step`, independientemente de los botones de React. Denegar acceso por IDOR incluso cuando el usuario conoce un UUID válido. `activeContext` es filtro de trabajo, **no identidad distinta**.

### 4.3 Separación de funciones [CLIENTE]

- En una misma planificación una persona no puede desempeñar dos etapas incompatibles; el cliente rechazó que el mismo revisor vuelva como validador. Implementar verificación de asignación duplicada **por identidad**, no solo texto de cargo.
- El elaborador no se autoaprueba. Actor de etapa futura no puede ni recibir tarea activa ni ver artefacto de revisión antes de su turno, salvo permisos explícitos de consulta institucional ajenos al workflow (éstos no otorgan potestad de decidir).
- Las firmas siempre corresponden al usuario autenticado titular de la etapa/slot. Prohibida firma suplantando a otro.

---

## 5. REQUERIMIENTOS FUNCIONALES — AUTENTICACIÓN Y CUENTAS

| ID | Requisito verificable | Origen |
|---|---|---|
| RF-AUT-001 | Login único institucional; sin registro público ni opción «Crear cuenta». | CLIENTE |
| RF-AUT-002 | Autenticación real en backend mediante username/identificador institucional y contraseña. Guardar por separado `username` y `institutional_email`; puede mostrarse correo en UI. | CLIENTE + EQUIPO |
| RF-AUT-003 | Admin (y Coordinador en alcance autorizado) crean/importan personas y asignan credenciales, carreras, grupos y cargos. | CLIENTE |
| RF-AUT-004 | Primera autenticación con contraseña temporal obliga cambio de contraseña antes de cualquier función. | CLIENTE |
| RF-AUT-005 | Mostrar/ocultar contraseña, errores inline, estados de carga y validación accesible. | UX |
| RF-AUT-006 | CAPTCHA alfanumérico visible en Login rediseñado, con regeneración que conserva credenciales, comparación clara y error inline; DEMO actual debe reemplazarse/validarse desde backend en producción, sin confundirlo con seguridad real. | MOCKUP + EQUIPO |
| RF-AUT-007 | Recuperación con token de un uso por correo cuando SMTP esté disponible; caducidad, invalidación y no revelar existencia de cuenta. | CLIENTE + EQUIPO |
| RF-AUT-008 | Recuperación administrativa alternativa: actor autorizado emite contraseña temporal y obliga nuevo cambio; auditar acción. | CLIENTE |
| RF-AUT-009 | Cierre de sesión invalida sesión, limpieza de estado sensible y nuevo desafío al regresar al login. | EQUIPO |
| RF-AUT-010 | Permitir cambio de contexto entre roles/grupos/carreras autorizados sin cambiar `currentUser`. | CLIENTE + EQUIPO |
| RF-AUT-011 | Desactivar usuario e invalidar sesiones/credenciales activas; conservar acciones y firmas históricas. | EQUIPO |
| RF-AUT-012 | Protección contra fuerza bruta, enumeración de cuentas, fijación de sesión y sesiones caducadas; no depender solo de CAPTCHA. | SEGURIDAD |
| RF-AUT-013 | «Recordar sesión» no almacena contraseña; si se conserva en producción, aplicar duración limitada autorizada y cookie segura. | EQUIPO |

**Datos de usuario confirmados:** nombres, apellidos, identificador/username institucional, correo, carrera(s), cargo(s)/membresía(s). **No** pedir cédula ni teléfono como datos obligatorios. Un correo de recuperación genérico de proyecto no se convierte en cuenta individual del usuario.

---

## 6. ADMINISTRACIÓN INSTITUCIONAL Y CONFIGURACIÓN

| ID | Funcionalidad | Condiciones |
|---|---|---|
| RF-ADM-001 | Dashboard administrativo con conteos reales del repositorio y filtros por período/grupo/carrera/tipo. | No datos hardcodeados |
| RF-ADM-002 | CRUD lógico de usuarios, importación CSV/Excel con previsualización, validación, detección de duplicados, resumen de errores por fila y activación/desactivación. | No sobrescribir identidad por nombre; auditar |
| RF-ADM-003 | Gestión de Facultades, Carreras, Unidades académicas/administrativas, Grupos y tipos `COMISION/UNIDAD/CLUB/OTRO`. | Datos vigentes y relaciones |
| RF-ADM-004 | Asignación de múltiples carreras a una persona; membresías por grupo, carrera y período; jefe/principal y cargos operativos. | No duplicar asignación activa |
| RF-ADM-005 | Gestión de períodos: altas, intervalos, activación, cierre, restricciones de solapamiento y ventanas de elaboración/revisión/ejecución/informe. | Política temporal configurable |
| RF-ADM-006 | Gestión de feriados/días restringidos: fecha, nombre, ámbito nacional/local/institucional, activar/desactivar, uso en validación. | Fines de semana no equivalen automáticamente a feriados |
| RF-ADM-007 | Catálogo de tipos de actividades, actividades sugeridas por grupo, obligación/optativo, recursos, medios de verificación y categorías institucionales. | Versionado o snapshot; «Otra/Otro» configurable |
| RF-ADM-008 | Asignación por cargo/función de revisores y validadores por tipo documental, grupo, carrera y período. | Un workflow activo congelado por período |
| RF-ADM-009 | Editor de plantilla por tipo: ordenar secciones, activar opcionales, editar elementos configurables, conservar obligatorias y marco técnico fijo; confirmación antes de publicar. | Versionar y afectar solo nuevos documentos |
| RF-ADM-010 | Configurar parámetros operativos (límites de anexos, ventanas, plazos, notificaciones) con auditoría y límites de seguridad. | No habilitar saltos de validación legal |
| RF-ADM-011 | Consultar auditoría de cambios administrativos y acciones sobre documentos con filtros, orden, paginación y detalle. | Solo privilegio autorizado |
| RF-ADM-012 | Carga de configuración DEMO o oficial, identificando estado `DEMO/PROJECT_DEFAULT/INSTITUTIONAL_CONFIRMED`, origen, fecha y responsable de carga. | No mezclar seeds de prueba con prod |
| RF-ADM-013 | Cierre/exportación de período y consulta histórica; sin purga automática hasta resolver la retención. | CONFLICTO RET-001 |
| RF-ADM-014 | Administrar catálogo de tipos documentales extensibles; habilitar tipo solo al existir formato, reglas, workflow y validaciones adecuadas. | No inventar contenido de formatos no entregados |

### 6.1 Período y asignaciones

- Período actual automático; el docente **no** digita ni selecciona arbitrariamente otro período al crear T1.
- Al crear documento, backend resuelve Facultad, Unidad, Carrera y período según las membresías. Con una carrera, se autoselecciona; con varias, presenta un selector restringido a carreras autorizadas.
- **[CLIENTE]** Workflow estructural de un período no se cambia durante el semestre. Configurar antes de apertura. Correcciones operativas por ausencia requieren sustitución de asignación autorizada y trazabilidad, no reescritura silenciosa de una firma anterior.
- Los catálogos/plantillas pueden prepararse para el **siguiente** período. El expediente de período anterior conserva sus versiones/snapshots.
- El valor `Julio – Diciembre 2026` es ejemplo del prototipo, **no** el único período posible del sistema.

### 6.2 Grupos conocidos de `comisiones.xlsx` (referencia, no carga productiva automática)

Los 23 registros de referencia son:

1. Club Académico de Telecomunicaciones;
2. Club Académico de Ingeniería Industrial;
3. Club Académico de Tecnologías de la Información;
4. Club Académico de Software;
5. Club de Robótica;
6. Comisión de Gestión de Plataformas Virtuales / Aula Virtual;
7. Comisión de Eventos Académicos / Congresos;
8. Comisión del Consejo Académico Universitario (CAU);
9. Comisión Custodio de Sustancias Catalogadas;
10. Comisión Sistema de Gestión de la Calidad;
11. Comisión de CTT y Talleres Tecnológicos;
12. Comisión de Diseño, Actualización / Ajustes de Proyectos;
13. Comisión de Redes y Laboratorios;
14. Comisión de Editores de Revistas Científicas Indexadas;
15. Comisión Plan de Contingencia;
16. Prácticas Preprofesionales / Prácticas Laborales;
17. Seguimiento a Graduados e Inserción Laboral;
18. Tutor de Seguimiento al Desempeño Estudiantil;
19. Unidad de Planificación y Evaluación (UPE);
20. Unidad de Titulación;
21. Unidad de Vinculación con la Sociedad;
22. Unidad Operativa de Investigación FISER / Investigación;
23. Unidad de Organización Curricular (UOC).

La fila «PLANIFICACIÓN DE UN EVENTO EN PARTICULAR» es una **nota asociada a eventos**, no un vigésimo cuarto grupo. Verificar nombres exactos, carreras y vigencia con los datos posteriores que entregue el cliente. **Nunca adjudicar revisores ni docentes inventados a esta lista**.

---
## 7. PLAN T1 — TITULARIDAD, UNICIDAD Y EDITOR

### 7.1 Regla más reciente [CLIENTE]

El **Plan pertenece institucionalmente al grupo/comisión/unidad**, y no de forma vitalicia al docente que lo creó. Una persona puede integrar varios grupos; el mismo grupo puede tener responsables de dos carreras. Cada ámbito debe mantener su propio expediente y flujo.

**Clave funcional orientativa**: `plan_type + period_id + group_id + career_scope_id`. El `teacherId`/`createdBy` es atributo de autoría, **no** componente de unicidad de Plan. En grupos administrativos sin carrera: no usar `NULL` en una restricción UNIQUE PostgreSQL corriente pensando que se comportará como valor único; emplear ámbito organizacional normalizado, `NULLS NOT DISTINCT` si la versión de PostgreSQL lo permite, o índices parciales equivalentes. Excluir de la ocupación funcional borradores descartados/archivados, preservándolos en auditoría.

**Caso multicarrera [CLIENTE]:** persona principal pertenece a Software y Tecnologías de la Información; crea/gestiona un Plan para cada carrera, con documento y circuito de aprobación independientes. Si necesita copiar contenido común del primero al segundo, la copia es explícita o parametrizada **sin compartir estado/firmas**. No reinterpretar «mismo archivo» como permiso para hacer una sola instancia firmada con dos flujos concurrentes.

**Editor exclusivo [CLIENTE]:** una persona principal designada puede crear/modificar el Plan `DRAFT`; otros miembros no editan simultáneamente. El responsable puede registrar otros elaboradores para los slots de firma, y múltiples responsables de **actividades**, sin convertirlos en coeditores del Plan. La reasignación de editor por cambio de cargo es acción de autoridad autorizada con auditoría.

| ID | Regla |
|---|---|
| RF-T1-001 | Crear exactamente un Plan vigente por `tipo + período + grupo + ámbito carrera`. |
| RF-T1-002 | Vincular cada Plan a su comisión/unidad como titular organizacional y a uno o varios elaboradores documentales, con un editor principal único. |
| RF-T1-003 | Validar membresía y carrera del solicitante en servidor. |
| RF-T1-004 | Detectar Plan ya existente en backend y DB bajo concurrencia; indicar acceso al existente en lugar de duplicar. |
| RF-T1-005 | Archivar borrador accidental sin DELETE físico; registrar motivo, autor y fecha; solo personal auditor/admin con permiso lo consulta. |
| RF-T1-006 | Guardar borradores incompletos y autosave; no exigir justificación completa al simple guardado temporal. |
| RF-T1-007 | Impedir edición cuando el documento está en revisión o aprobado, salvo nueva ronda por devolución o nueva versión formal. |
| RF-T1-008 | Mantener histórico de transferencias de responsable/editor y de los firmantes de cada artefacto. |

### 7.2 Wizard T1: seis pasos [CLIENTE, feedback posterior de mockup]

| Paso | Pantalla | Campos y operación | Validación |
|---:|---|---|---|
| 1 | **Información general** | Período actual, Unidad académica/administrativa, Facultad, Carrera, Grupo, responsable editor, versión; autocompletado del perfil/relaciones. | Grupo/carrera deben pertenecer al ámbito autorizado; combinación única. |
| 2 | **Contenido** | Justificación y Objetivo T1; ayuda de redacción IA por campo. | **Justificación obligatoria** para finalizar. Campos obligatorios de plantilla se validan. |
| 3 | **Actividades** | Lista y alta/edición individual con tipo POA/Plan de Mejoras/Acción de Mejora/Otra, texto, Desde, Hasta, responsables, recursos, medios. | Fechas, al menos un responsable, catálogos y medios requeridos; matriz se deriva sin entrada duplicada. |
| 4 | **Anexos** | «¿Este Plan de Trabajo contiene anexos? Sí/No». Si Sí: subir, sustituir, eliminar antes de firma, ordenar; no exigir descripción. | Si Sí, aplicar reglas de adjuntos; si No, no mostrar uploader. |
| 5 | **Previsualización** | Render institucional paginado, A4, matriz, firmas pendientes/historial, zoom/pantalla completa. | Preview usa los mismos datos validados del artefacto futuro. |
| 6 | **Firma y Finalización** | Confirmación («Cancelar»/«Firmar y finalizar»), firma correspondiente, avance automático al workflow. | No iniciar transición si validaciones o firmantes requeridos no cumplen. |

**Corrección de devolución:** al regresar a `DRAFT`, **reabrir contenido y actividades existentes**, sin obligar a elegir otra vez tipo/grupo ni crear Plan nuevo; abrir en el paso de contenido/corrección y destacar campos observados. Se puede modificar cualquier sección, no solo la señalada, conservando datos válidos.

### 7.3 Actividades y matriz T1

**Regla [CLIENTE]:** toda la configuración de actividad está en una sola pantalla/formulario. La matriz PDF **se genera** desde esos datos; no crear una entidad capturable distinta.

- `activity_type`: `POA | PLAN_MEJORAS | ACCION_MEJORA | OTRA`; **clasificación**.
- `requirement_type`: `MANDATORY | OPTIONAL`; **obligatoriedad**, independiente de `activity_type`; los mandatos pueden depender del grupo/carrera.
- `description`: texto de la actividad (si «Otra», texto específico).
- `start_date`, `end_date`: fechas civiles dentro del período (`LocalDate`); mostrar `DD/MM/YYYY` en tabla/PDF.
- `sort_order`: orden institucional modificable antes de firma, preservado en matriz.
- `activity_responsibles`: uno o varios usuarios elegibles de la comisión/carrera; **mínimo uno**. Si son todos, conservar sus IDs/identidades y usar representación colectiva configurada solo en PDF.
- `activity_resources`: cero o varios según reglas del catálogo; recurso puede ser humano/tecnológico/económico/material u «Otro» especificado.
- `activity_verification_media`: uno o varios según catálogo; incluye requeridos, tipo de evidencia `PDF/REFERENCE` cuando aplique y su obligatoriedad. No confundir recurso con medio.
- `execution_status` o derivado: pendiente/en curso/completa/impedida según evidencias y fechas. **No forma parte del enum documental de tres estados.**

| ID | Regla de actividad |
|---|---|
| RF-ACT-001 | Alta, edición y retiro de actividades del borrador; evitar borrar evidencia o actividad histórica aprobada. |
| RF-ACT-002 | Combo de tipo POA, Plan de Mejoras, Acción de Mejora, Otra (no usar tipos como simples filtros). |
| RF-ACT-003 | Una actividad no puede empezar después de terminar ni tener extremos fuera del período. |
| RF-ACT-004 | Puede abarcar feriados, pero **fecha de inicio y fecha de fin no pueden ser feriado**; el catálogo lo gestiona Admin. |
| RF-ACT-005 | Responsables elegibles según grupo/carrera; mínimo uno. |
| RF-ACT-006 | Selección de recursos y medios desde catálogos activos; «Otros» con especificación cuando se permitan. |
| RF-ACT-007 | La matriz T1 se recalcula automáticamente, sin segunda edición ni divergencias. |
| RF-ACT-008 | Se debe poder ordenar, consultar, filtrar y corregir actividades del borrador. |
| RF-ACT-009 | Una vez aprobado, plan y actividades planificadas quedan congelados para ese artefacto; cambios materiales generan versión formal nueva. |

### 7.4 Temporalidad y feriados

- Al crear/modificar actividad, comprobar `period.start_date ≤ start_date ≤ end_date ≤ period.end_date`.
- Un feriado dentro del rango no invalida la actividad; en los extremos sí (conferido por cliente).
- `America/Guayaquil` para fechas de negocio. Para límites diarios, comparar contra fin de fecha local con intervalo semiabierto `[inicio, día siguiente 00:00)`; mostrar al usuario «hasta las 23:59». No depender de reloj del navegador.
- El cliente señaló que **la ventana habitual de elaboración de documentos es de unos cinco días**. No hardcodear cinco días por cada Plan: configurar `submission_window` por tipo/período/ámbito y registrar excepción autorizada. La cifra es referencia verbal, no calendario oficial entregado.
- Si la acción requiere prórroga, la extensión debe quedar vinculada al sujeto adecuado (persona/actividad/documento), guardar fecha original y nuevo deadline, no alterar la fecha base sin historial.

### 7.5 Anexos T1 [CLIENTE, última UX]

- Pregunta Sí/No, no formulario complejo.
- Si «Sí», permitir adjuntar **documentos habituales** PDF, DOCX, XLSX, PPTX, imágenes, TXT y otros formatos seguros permitidos por política; **«cualquier documento» no significa ejecutables**.
- Subida con modal sencillo: seleccionar archivo, cancelar/agregar; nombre original visible una sola vez; descripción opcional. Numeración «Anexo A/B/C» generada, orden ajustable. Reemplazar/remover solo antes de firma o en nueva ronda editable.
- Si «No», no imprimir anexos inexistentes ni espacios vacíos artificiales.
- Valores técnicos de desarrollo: 10 MB/anexo, 20 anexos máximos como **configuración DEMO**, no norma oficial. Analizar tamaño total/timeout de infraestructura.
- Diferenciar anexos del Plan de evidencias de ejecución: los primeros no son obligatoriamente PDF; las segundas sí, salvo medio de tipo referencia autorizado.

---

## 8. MODELO DOCUMENTAL, ESTADOS, RONDAS, VERSIONES E INMUTABILIDAD

### 8.1 Exactamente tres estados **formales** [CLIENTE]

```text
DRAFT  → IN_REVIEW  → APPROVED
   ↑         │
   └─────────┘  (devolución obligatoriamente observada)
```

| Estado | Significado | Edición |
|---|---|---|
| `DRAFT` | Nuevo borrador, borrador corregible por devolución o nueva versión formal en construcción. | Únicamente editor autorizado. |
| `IN_REVIEW` | Contenido firmado/congelado en un artefacto y tareas del workflow activas secuencialmente. | Nadie edita contenido. |
| `APPROVED` | Todas las etapas y firmas obligatorias completadas. Artefacto histórico inmutable. | Nadie edita; solo generar nueva versión formal. |

No crear estados documentales independientes `EN_CORRECCION`, `VALIDADO`, `EN_EJECUCION`, `FINALIZADO`, `DEVUELTO` en lugar de estos tres. Para UX sí hay **subestados derivados**, como `returned_reason`, `return_count`, `active_step`, `execution_progress`, `period_status` y estados de evidencia, **sin contaminar enum formal**.

**Reglas de transición:**

- `DRAFT → IN_REVIEW` al completar validación y firma(s) de elaboración obligatorias; no existe otro botón «Enviar» luego de la última firma.
- `IN_REVIEW → DRAFT` al devolver con **una o más observaciones obligatorias**, auditando actor, etapa, firma/artifact histórico y fecha. La siguiente etapa NO se habilita.
- `IN_REVIEW → APPROVED` únicamente al culminar el último revisor/aprobador firmante; guardar artefacto final.
- `APPROVED` no puede retroceder a `DRAFT` ni revocarse por operación normal. Cambios materiales: nueva **versión formal** vinculada a la aprobada.
- Borrador descartado: marca `archived/discarded`, sin eliminación física y sin que ocupe unicidad vigente.

### 8.2 Ronda de revisión ≠ versión formal

Ejemplo obligatorio:

```text
Plan v1.0 — Ronda 1 → firma elaborador → revisor 1 devuelve.
Plan v1.0 — Ronda 2 → firma elaborador → revisor 1 firma → revisor 2 firma → autoridad aprueba.
Plan v1.0 → APPROVED.

Cambio sustancial posterior:
Plan v2.0 → DRAFT (copia controlada de v1.0) → workflow completo → APPROVED.
```

- Una devolución no crea v2.0, aunque deba crearse un nuevo PDF/ronda firmada.
- La versión formal nueva se crea **solo partiendo de una aprobada** que exige actualización material; copia datos/documento origen, mantiene v1.0 inmutable y repite el workflow.
- El cliente distinguió pequeños errores ortográficos de cambios relevantes; **la política de qué cambio es «material» es de negocio y puede requerir autorización**. No prometer detección automática semántica; admitir motivo y solicitud explícita.
- Historial formal del T1: `Versión | Descripción del Cambio | Fecha de Actualización`, con fecha `DD/MM/YYYY` y texto narrativo trazable.

### 8.3 Revisores, devolución y editor

- Los revisores solo **observan, aprueban/firman o devuelven**. No cambian contenidos.
- La devolución debe **explicar el motivo** y puede contener varias observaciones por sección/página/fragmento.
- Corregir desde contenido ya existente; no obligar a recrear tipo documental ni identidad institucional; todo el borrador vuelve a estar editable por el único editor principal, con marcas de observaciones.
- Al reenviar se inicia **ronda nueva desde la primera etapa de revisión**, no solamente desde quien devolvió, porque se firman bytes nuevos y aprobaciones previas no se trasladan automáticamente.
- Registrar decisiones previas históricas y tareas canceladas/obsoletas de la ronda anterior. El revisor actual no debe poder operar sobre una tarea obsoleta.

### 8.4 PDF final y descarga [CLIENTE]

- El revisor accede a visor seguro del **artefacto exacto** congelado de su turno, no a una regeneración a partir de datos actuales.
- **Antes de `APPROVED`: no habilitar descarga del PDF final institucional**; sí visor autenticado para editar/revisar según rol. Nota técnica: un navegador que visualiza bytes puede permitir su extracción; esta limitación no equivale a DRM. Aplicar controles de acceso, encabezados, sin botón de descarga, y auditar accesos.
- Tras `APPROVED`: descarga autenticada para personas/roles autorizados, guardando auditoría; conservar histórico.
- No crear códigos QR/hash/resolución falsos como adornos. Una representación visible de firma no sustituye criptografía verificable.

---

## 9. WORKFLOW INSTITUCIONAL DE REVISIÓN Y FIRMA

### 9.1 Orden estrictamente secuencial [CLIENTE, reunión más reciente]

**No hay etapas paralelas ni dos revisores simultáneos del mismo documento**. Aunque el prototipo anterior contempló revisores obligatorios paralelos, **queda sustituido** por el acuerdo posterior. Varios revisores siguen siendo obligatorios, uno tras otro. Ninguna etapa futura se activa antes de la actual.

Ejemplo genérico configurable:

```text
Elaboración y firmas requeridas
       ↓
Responsable / unidad de revisión o planificación
       ↓
Revisor designado adicional, si aplica
       ↓
Coordinador asignado
       ↓
Autoridad final: DECANO o SUBDECANO u otra configurada
       ↓
APPROVED
```

**No hardcodear ese ejemplo**: algunos grupos difieren, y T2/u otros tipos documentales pueden tener flujos propios. Cliente prometió tabla por grupo/carrera/tipo con revisores, aprobadores y personas concretas. Modelar cargos estables y designaciones por período.

### 9.2 Acciones por turno

| Actor en turno | Acciones | Efecto |
|---|---|---|
| Elaborador(es) requerido(s) | Ver PDF, declarar conformidad, firmar | Completa slot de elaboración; al completar todos inicia revisión automáticamente |
| Revisor activo | Ver **artefacto congelado**, observar, `Aprobar y firmar`, `Devolver` | Firma y pasa al siguiente o devuelve a DRAFT |
| Validador final | Ver artefacto de turno, firmar/aprobar o devolver | Última firma → APPROVED |
| Actor futuro | Ninguna acción sobre ese documento | No llega a su bandeja hasta que le toque |

**[CONFLICTO operativo de firmas múltiples]:** la reunión indicó «apenas firma se envía a revisión» y a la vez «todos los elaboradores de la tabla deben firmar». Se implementa una interpretación técnica segura: si hay **un** elaborador requerido, su firma inicia revisión; si hay varios, cada uno firma en orden configurado y **la última firma requerida inicia revisión automáticamente**, sin botón adicional. Si el cliente exige otra secuencia, se adapta mediante política de firma, manteniendo autenticidad individual. No copiar la clave privada de un elaborador a otro.

### 9.3 Definición/versión y tareas

- `workflow_definition`: estructura abstracta por tipo/grupo/carrera.
- `workflow_version`: versión publicable, con estatus y período de vigencia.
- `workflow_step`: orden entero, cargo/función, obligatoriedad, firma requerida, acción.
- `period_workflow_assignment`: cargo a usuario concreto para el período.
- `workflow_instance`: snapshot de la definición y personas resueltas al inicio de una ronda.
- `workflow_task`: actor, paso, estado `LOCKED/ACTIVE/APPROVED/RETURNED/OBSOLETE`, timestamps y enlace al artefacto/ronda.
- `review_observation`: autor, referencia sección/página, texto, timestamps, ronda y marca de resolución.

Solo una tarea de revisión `ACTIVE` por documento/ronda; índice/restricción o control transaccional en DB para evitar carreras. No autorizar una aprobación cuando el artefacto, ronda, usuario o estado no coincidan.

### 9.4 Cambios y ausencias

- Al inicio del período se cargan miembros, encargados, cadenas de revisión y autoridades según comisión/carrera.
- Una vez el período está operando, **no modificar la estructura** del workflow del período. Crear nueva versión para período siguiente.
- Si queda actor inactivo/cargo vacante, el Coordinador autorizado gestiona **sustitución de asignación**, sin cambiar la estructura ni borrar acciones realizadas; bloquear el paso si no existe reemplazo autorizado válido.
- Evitar doble rol de una misma persona en etapas distintas del mismo expediente (decisión explícita de cliente).
- No permitir que Admin salte etapas para «desbloquear» un expediente; usar procedimiento excepcional autorizado y auditado si la institución lo define.

---

## 10. PDF, PLANTILLAS, VISOR Y FIRMA ELECTRÓNICA

### 10.1 Composición PDF en backend [EQUIPO]

El frontend puede mostrar previsualización A4 con la misma semántica, pero **el backend genera el PDF canónico** a partir del snapshot validado. El visor de revisión consume los bytes del artefacto guardado y firmado. No firmar un PDF arbitrario suministrado por el browser.

Usar combinación viable de HTML/CSS→PDF (p. ej. OpenHTMLtoPDF) y manipulación incremental (Apache PDFBox), con pruebas de páginas, fuentes, imágenes, tabla, orientación, fechas, pie y firma. La selección concreta de bibliotecas es técnica y debe considerar mantenimiento y licencia.

### 10.2 Formato T1 `UTA-SGC-A-2-1-P7-T1`

**Fuente:** DOCX oficial de referencia y PDF firmado de ejemplo. Mantener:

- Identidad/seal institucional, `SISTEMA DE GESTIÓN DE LA CALIDAD`, `UNIVERSIDAD TÉCNICA DE AMBATO`, código de formato, título dinámico del Plan/grupo.
- Encabezado con `Unidad académica:` o `Unidad administrativa:` según metadata; si académico mostrar Facultad y Carrera, si administrativo **no** inventar carrera ni mostrar etiqueta combinada.
- Fecha de elaboración en texto largo («20 de septiembre de 2026»), mientras la **matriz** usa `DD/MM/YYYY`.
- Índice de contenido/índice de tablas cuando correspondan al formato; secciones obligatorias Justificación, Objetivo, Matriz, Anexos si corresponde, Firmas, Historial.
- Matriz con columnas **Actividades | Desde | Hasta | Responsable | Recursos | Medios de verificación**. A4 horizontal solo para matriz si hace falta, resto A4; verificar saltos/paginación.
- Tabla de firmas **ACCIONES | NOMBRE | CARGO | FIRMA**, roles dinámicos a partir de slots y workflow; no duplicar firma o filas no aplicables.
- Título exacto `CONTROL DE HISTORIAL DE CAMBIOS`; columnas `Versión | Descripción del Cambio | Fecha de Actualización`.
- Pie en cada página: texto de uso interno a izquierda, `Formato Nº: UTA-SGC-A-2-1-P7-T1` al centro, número de página dinámico a derecha. No añadir bordes/lineas que el original no tenga.
- Evitar imprimir textos de ayuda/instrucciones del DOCX («complete», «en caso de»), campos vacíos o repeticiones de nombre de anexos.
- Responsable colectivo visual según catálogo/configuración («Responsable de la unidad» u otra denominación vigente), sin perder nombres individuales internamente.

### 10.3 Formato T2 `UTA-SGC-A-2-1-P7-T2`

- Mismo marco institucional pertinente al T2, con `Unidad académica`/`Unidad administrativa` excluyentes.
- Antecedentes, Desarrollo de Actividades, Conclusiones, Oportunidades de Mejora; Registro de contactos/gestiones **solo para delegaciones/visitas/comisiones a las que aplica**; Anexos; Firmas; Historial.
- Para Informe Final **derivado de Plan**, conservar tabla `Actividades | Medios de verificación | Porcentaje de ejecución | Observaciones` o su reproducción exacta actualizada por plantilla oficial; calcular su porcentaje desde evidencia/actividades, no permitir cifras incompatibles como fuente de verdad.
- El DOCX T2 menciona informes sin Plan; éstos solo se habilitan como **otro tipo documental institucional específico** tras entrega de formato/proceso. Nunca crear un «Informe Final de Plan» huérfano.
- Los anexos/evidencias documentales PDF se incorporan al final como páginas secuenciales, tal como pidió el cliente, con índice/cantidad controlados; para URLs/repositorios, referencia textual o anexo descriptor, no tratar enlaces como PDF binario.

### 10.4 Plantillas

- Encabezado/pie y elementos institucionales invariables son marco técnico controlado.
- Secciones obligatorias pueden reordenarse **dentro del contenido** según configurador aprobado, pero no eliminarse; opcionales pueden activarse/desactivarse, siempre dentro de límites institucionales y sin cambiar históricos.
- Una publicación crea `template_version`; documento captura snapshot de la versión vigente; cambios afectan **documentos nuevos**, no borradores ya asociados ni PDFs firmados.
- «Restaurar predeterminado» actúa sobre una plantilla seleccionada; nunca borra documentos, catálogos ni otras plantillas.
- Si el custodio documental entrega versión oficial nueva, migrar el template para expedientes futuros y preservar antiguos.

### 10.5 Visor [MOCKUP APROBADO]

- Páginas A4; navegación anterior/siguiente, contador, zoom, scroll vertical/horizontal y carga progresiva; panel lateral de observaciones/acciones.
- Al abrir preview normal puede colapsar sidebar de la app para dar espacio, sin ocultar herramientas necesarias.
- Modo expandido **real** ocupa ventana (100vw×100vh), conserva barra/panel y zoom/página; tecla Escape sale sin perder navegación.
- Revisor ve el artefacto de **su ronda**, no generación re-renderizada desde otro estado.
- Observación puede señalar sección/página; si la UI usa anclas, debe guardar identificadores estables que sobrevivan paginación si cambia contenido.
- Deshabilitar botones críticos si no es turno; explicar el motivo accesiblemente.

### 10.6 Firma criptográfica real [CLIENTE + EQUIPO]

- Interfaz acepta **PKCS#12 `.p12/.pfx` y contraseña** para firmante actual, con opción/advertencia DEMO solo en mockup.
- En producción firmar PDF criptográficamente con clave privada temporal en memoria; guardar PDF firmado, certificado público/cadena admisible, SHA-256 de artefacto, sujeto/emisor/serie, algoritmo y auditoría. **Nunca guardar `.p12/.pfx`, clave privada ni contraseña** en DB, storage, archivo temporal persistente o logs.
- Verificar contraseña, correspondencia titular–identidad institucional y condiciones de validez/confianza conforme a política institucional. Cadena, revocación (OCSP/CRL), sellado de tiempo, perfil PAdES/verificación de firma deben resolverse con DTIC antes de declarar una firma jurídicamente verificable.
- **Firma incremental:** cada firmante añade firma al PDF acumulado sin invalidar bytes de firmas anteriores. La firma de un reviewer no debe regenerar entero un PDF y destruir firmas precedentes. Guardar artefacto firmado por cada etapa con hash distinto y referencias parentales.
- Tabla visible: rol/acción, nombre, cargo, firma; el cliente indicó QR/representación visible **con texto asociado**. QR es representación o enlace de verificación, **no** sustituto de firma criptográfica. No generar QR «válido» ni verificador público si aún no existe servicio autorizado.
- Mostrar diálogo confirmatorio antes de acción de firmar/finalizar: cancelar es no-op, y documento permanece editable si sigue DRAFT.
- Firmar y pasar al siguiente actor inmediatamente tras la última firma requerida; no pedir otro clic «Enviar».
- La verificación técnica de firmado y la persistencia del artefacto deben anteceder a cualquier transición `IN_REVIEW/APPROVED`.

### 10.7 Atomicidad de PDF+firma+DB+storage

Una transacción de DB no revierte automáticamente escrituras de filesystem ni operaciones remotas. Implementar staging privado, hash y verificación, persistencia atómica/metadatos, `outbox` e idempotencia, con compensación/limpieza de archivos huérfanos ante fallos. Nunca dejar un documento `IN_REVIEW` sin PDF válido, ni tarea activa sin referencia a artefacto de revisión.

---
## 11. EJECUCIÓN DE ACTIVIDADES, EVIDENCIAS Y PRÓRROGAS

### 11.1 Separación del estado documental

La **aprobación documental** (`APPROVED`) habilita la ejecución; **no** significa automáticamente que las actividades están terminadas. Registrar progreso operativo independiente, derivado de evidencias y fechas. No introducir estado formal «EN EJECUCIÓN» en `documents` si contradice los tres estados acordados.

| ID | Requerimiento de ejecución |
|---|---|
| RF-EJE-001 | Vista de actividades aprobadas por plan/ámbito/período con fechas, responsables, medios exigidos y progreso. |
| RF-EJE-002 | Permitir al responsable autorizado abrir detalle de actividad, consultar medios y cargar evidencias. |
| RF-EJE-003 | Calcular medios exigidos, medios con evidencia vigente, pendientes y actividades completadas. |
| RF-EJE-004 | Mostrar días/horas restantes según reloj del servidor, zona `America/Guayaquil`, estado de cierre y prórrogas. |
| RF-EJE-005 | Restringir acceso/carga a responsables asignados o delegados según permisos; las evidencias no son públicas. |
| RF-EJE-006 | Registrar cada carga, reemplazo, lectura autorizada y extensión en auditoría. |
| RF-EJE-007 | No modificar la matriz T1 aprobada al cargar evidencia. |
| RF-EJE-008 | El seguimiento y porcentajes se calculan de la fuente canónica, no de valores DEMO ni edición arbitraria del usuario. |

### 11.2 Evidencia documental vs recurso digital [CLIENTE]

- **Regla general:** los medios de verificación documentales se acreditan mediante **PDF**; no DOCX editable como evidencia final.
- Puede existir un medio de verificación definido como **enlace/repositorio** (ejemplo mencionado por cliente: repositorio con videos y archivos). En ese caso guardar URL/identificador, título, contexto y comprobación razonable; no exigir PDF cuando el catálogo de ese medio oficialmente indique `REFERENCE`. No confundirlo con el T1, cuyos anexos admiten otros documentos.
- Al diseñar cada medio: `evidence_kind = PDF | REFERENCE` y `required = true/false` configurado por catálogo/documento, sin aceptar libremente código ejecutable ni URLs peligrosas.
- **Completitud [CLIENTE]:** una actividad se marca completa cuando tiene evidencia vigente en **todos los medios requeridos**. No implementar un workflow extra de aprobaciones de evidencia como requisito general: la última reunión aclaró que los responsables cargan y verifican su soporte.
- No reutilizar estado DEMO antiguo `PENDIENTE_VALIDACION/VALIDADA/OBSERVADA` como requisito obligatorio de producción para cada evidencia. Si se desea mantenerlo por compatibilidad del mockup, encapsularlo como metadato legado/opcional por tipo, **sin bloquear la actividad completa por falta de firma externa de evidencia**.
- Si una evidencia se reporta incorrecta, permitir sustituirla dentro del plazo; tras vencimiento solo mediante extensión autorizada. La nueva versión no borra la anterior.

### 11.3 Cardinalidad y reutilización

- Por cada medio documental requerido mantener **una evidencia PDF vigente**, con historial de `evidence_version`; una nueva carga puede reemplazar a la vigente y hacer anterior obsoleta, preservando ambas.
- Distintos medios o actividades pueden señalar el mismo blob físico bajo almacenamiento deduplicado **si la política lo autoriza**, pero conservar asociaciones, responsables, hashes y metadatos independientes.
- Según la última reunión: dentro de una comisión, actividades diferentes normalmente producen evidencias distintas; **entre comisiones** un mismo PDF podría servir como evidencia en casos legítimos. No imponer `UNIQUE(sha256)` global con rechazo; usar SHA-256 para integridad y eventual deduplicación transparente, nunca como prueba automática de incumplimiento.
- Evidencias del T1 de un período no deben accederse por adivinar `storageKey`; vincular derechos por Plan, actividad, medio y ámbito.

### 11.4 Fechas límite

- Regla normal: `deadline = end_date de actividad, 23:59 hora local`, a menos que exista extensión institucional autorizada.
- Vencido el plazo, backend deniega carga/reemplazo normal. Un botón oculto en React no garantiza bloqueo.
- En un límite concurrente, usar timestamp de recepción completada/commit fiable en servidor y validación en transacción; **[PENDIENTE INSTITUCIONAL]** el cliente no aclaró de forma inequívoca qué ocurre con una carga empezada antes de medianoche y terminada después. Default técnico seguro: archivo íntegro recibido y autorizado antes del deadline para considerarlo puntual.
- No confundir fecha Desde/Hasta del Plan (planificada) con deadline excepcional de carga (autorización posterior individual); siempre conservar fecha original para documento y reporte.

### 11.5 Extensión de plazo [CLIENTE]

- En la última reunión se estableció una **extensión individual**, gestionada por Coordinador/autoridad autorizada en su ámbito, con tope verbal de **aproximadamente tres días** para evidencia de actividad; otra frase del audio menciona «10 laborables» sin claridad. **[CONFLICTO DE UNIDAD DE PLAZO]**: parametrizar tipo de días (calendario/hábiles), máximo y ámbito, y confirmar antes de producción. Para desarrollo usar ejemplo DEMO «3 días calendario» etiquetado, no imponerlo como ley.
- La prórroga se asocia a sujeto correcto: `activity_id`, `beneficiary_user_id` o `responsible_assignment_id`, `original_deadline`, `extended_deadline`, `reason`, `authorized_by`, `created_at`, `status`; la base de actividad no cambia.
- Solo el beneficiario asignado recibe habilitación excepcional; no se extiende globalmente a todo el grupo por accidente.
- Crear una extensión no altera PDFs ni firma del Plan; sí aparece en trazabilidad, alerta y reportes temporales pertinentes.
- Rechazar extensión fuera de máximo o concedida por un actor sin autorización. No aceptar extensiones encadenadas para burlar límite sin política explícita.

### 11.6 Informar incumplimientos

- Actividad sin todas las evidencias tras fecha límite queda **pendiente/incompleta** con información de vencimiento. No marcar completada por transcurrir plazo.
- El cliente mencionó alertar al superior/Decano por eventos relevantes; usar notificación según destinatarios configurados y el workflow institucional, sin exponer información a autoridades que aún no tengan permiso.
- Si la actividad no se ejecutó, T2 debe reflejar su falta de ejecución mediante resultado/observaciones; no inventar sanciones ni falsificar evidencia.

---

## 12. INFORME FINAL T2 Y TIPOS DOCUMENTALES ADICIONALES

### 12.1 Informe Final T2 [CLIENTE posterior]

**Reglas definitivas del Informe Final de la planificación:**

1. No existe Informe Final sin Plan previo.
2. **Uno y solo uno** Informe Final vigente por Plan (modelar unicidad lógica con versionado formal del Informe; no crear dos Informes Finales paralelos). Puede tener rondas y versiones históricas, pero una sola línea de Informe Final por Plan.
3. No existen «informes finales parciales» de una planificación. Un informe de una actividad es **otro tipo** con su identidad propia.
4. El responsable/editor principal del Plan realiza el Informe Final, salvo reasignación trazable del cargo/responsabilidad; no entregar ni compartir contraseñas como solución a ausencias.
5. Se prellenan identidad, período, unidad, carrera, grupo, actividades, medios, resultados/porcentajes y referencias de evidencias desde Plan aprobado, conservando `source_plan_id` y `source_plan_version_id`. **T2 nunca modifica T1.**
6. El Informe Final sigue su propio flujo configurado por tipo/ámbito/período; estado documental también `DRAFT/IN_REVIEW/APPROVED`; firma de elaboradores/revisores/aprobador que corresponda.
7. Las evidencias PDF incorporadas al documento final se anexan como páginas al final, con numeración/índice. Un repositorio/enlace se registra como referencia documental con el texto del recurso.
8. Si una actividad no tiene evidencia y se considera incompleta, conservar el estado real y el comentario. No generar automáticamente datos de ejecución inexistentes.

### 12.2 Wizard T2 conservado del mockup

| Paso | Nombre | Fuente/validación |
|---:|---|---|
| 1 | Información General | Plan aprobado origen, grupo, unidad/carrera y período heredados; impedir fuente inválida. |
| 2 | Antecedentes | Redacción institucional, soporte/resolución, referencia Plan; editor asistido opcional. |
| 3 | Desarrollo de Actividades | Matriz derivada Plan, medios, progreso y observaciones; datos reportados sin tocar T1. |
| 4 | Conclusiones y Oportunidades | Textos y mejoras institucionales, ayuda IA de redacción por campo. |
| 5 | Registro de Contactos | Únicamente cuando aplica a delegación/visita/gestión; requerimientos de datos mínimos y privacidad. |
| 6 | Anexos | Evidencias/documentos del Informe, orden y asociación; PDFs concatenables. |
| 7 | Previsualización | A4, pie, firma, matriz, anexos y PDF institucional canónico. |
| 8 | Firma y Finalización | Confirmación, firmantes, envío automático a revisión según workflow T2. |

**Fuente T2 DOCX:** `Desarrollo de Actividades` contiene `Actividades | Medios de verificación | Porcentaje de ejecución | Observaciones`; no omitir porque en informes de seguimiento se prefieran conteos. **Contadores y porcentaje pueden coexistir**; ninguno debe ser ranking de rendimiento de personas.

### 12.3 Semántica de porcentaje

- Para una actividad con medios obligatorios: `avance = medios_requeridos_con_evidencia_vigente / total_medios_requeridos * 100` si se adopta criterio «cumplimiento por evidencia». El 100 % no sustituye una medida humana cualitativa ni certifica resultado material.
- Para una planificación: `actividades_completadas / actividades_planificadas_elegibles * 100` si el reporte solicitado mide ejecución de actividades. Definir denominador claro (todas u obligatorias) en cada reporte y mostrar conteos junto a porcentaje. Si `denominador = 0`, mostrar `N/A` en lugar de asignar 100 % sin actividad.
- La definición exacta de «porcentaje de ejecución» de T2 puede requerir una escala institucional distinta de completitud por evidencia; **[PENDIENTE FORMATO/CLIENTE]** no suponer que simplemente cargar documentos demuestra el porcentaje de ejecución física. Soportar métrica configurada/manual documentada y validada cuando el formato oficial lo exija, manteniendo evidencia histórica.

### 12.4 Otros tipos de documentos [CLIENTE]

Se mencionaron **planificación de evento**, informes por actividad, documentos de ejecución y otros informes además de Plan e Informe Final. Las plantillas oficiales y condiciones están por entregar.

Crear catálogo `document_type` con atributos:

- `code`, nombre oficial, versión de plantilla, elegibilidad (grupo/carrera/período);
- `source_required` y `source_type` (p. ej. Plan previo solo para Informe Final);
- secciones obligatorias y opcionales;
- campos tipados/esquema de validación;
- regla de cardinalidad;
- workflow/signer slots;
- anexos y evidencias;
- render canónico y ciclo de vida;
- flags de habilitación institucional.

**No** permitir que un Admin sin nueva plantilla oficial cree un documento genérico con firma legal inválida. Se puede dejar configurador de tipos preparado y habilitar cada tipo tras especificación completa.

**Aclaración importante del original T2:** contiene texto para informes no derivados de Plan. Se respeta como posibilidad del **formato genérico T2**, pero no se rompe la regla del **Informe Final de Plan**: éste **siempre** tiene fuente T1 y es único.

---

## 13. REPORTERÍA, INDICADORES Y DASHBOARDS

### 13.1 Destinatarios

- Responsable/docente: mis planes y actividades, documentos asignados, evidencias exigidas/cargadas, progreso y plazos.
- Revisor: tareas activas, estado de revisiones propias, observaciones emitidas, estadísticas documentales **de su ámbito**, no acceso a todo por ser revisor.
- Coordinador: planes y actividades de comisión/carrera/período, completitud y vencimientos, extensiones bajo su autoridad.
- Decano/Subdecano: consolidado del ámbito institucional y acceso histórico dentro de sus atribuciones, sin asignarse automáticamente facultad de firmar tareas de otros.
- Administrador: conteos de operación, auditoría y configuración; lectura detallada de documentos según política explícita.

### 13.2 Reportes pedidos [CLIENTE posterior]

1. **Por comisión:** número de planificaciones ejecutadas, total actividades planificadas, ejecutadas/completadas, pendientes, porcentaje de ejecución de la planificación.
2. **Consolidado de todas las comisiones:** número de actividades previstas y completadas, distribución y comparación agregada por comisión (conteos y porcentajes). El cliente pidió identificar comisiones con más actividades; **no convertirlo en ranking de docentes ni valoración individual del desempeño**.
3. **Por período y carrera:** filtros, resumen documental (`DRAFT/IN_REVIEW/APPROVED`), ejecución y evidencia.
4. **Reporte detallado Plan:** matriz de actividades, medios requeridos/cubiertos y observaciones documentales relevantes.
5. **Reporte de plazos:** actividades próximas a vencer/vencidas, extensiones autorizadas, evidencias faltantes.
6. **Histórico:** versiones aprobadas y PDF firmado, período, fecha, autoridad y trazabilidad; sujeto a regla de custodia y retención.
7. **Auditoría:** eventos filtrables por usuario, módulo, tipo, fecha, grupo, documento y request; exportación solo autorizada.

### 13.3 Principios de indicadores

- Consultas **derivadas de PostgreSQL**, no contar elementos DEMO estáticos.
- Filtrado de seguridad **antes** de agregación y detalle.
- Conteos + porcentajes cuando tengan denominador explicado; no inventar precisión estadística.
- Exportación PDF/CSV/Excel de reportes solo si el alcance de entrega lo exige; contrato de formato y permisos explícitos.
- El documento de planificación T1 y el Informe T2 deben conservar snapshots históricos. Cambiar configuración o nombres hoy no altera métricas publicadas de períodos anteriores sin reejecución formal documentada.

### 13.4 Dashboard de producción

Tarjetas sugeridas: Planes del período, borradores, en revisión, aprobados; actividades completadas/pendientes, evidencias faltantes, próximos vencimientos; visualizaciones por grupo/carrera/período; bandeja de tareas y alertas. Preferir jerarquía visual limpia, íconos informativos, drill-down y estados vacíos; no saturación de tarjetas inconexas.

---

## 14. NOTIFICACIONES Y COMUNICACIONES

### 14.1 Bandeja interna obligatoria

| Evento | Disparador | Destinatario permitido |
|---|---|---|
| `DOCUMENT_REVIEW_ASSIGNED` | Se activa turno de revisión | **Solo** revisor activo |
| `DOCUMENT_RETURNED` | Devolución con observación | Editor principal del documento |
| `DOCUMENT_APPROVED` | Concluyen firmas/etapas | Editor, ámbitos institucionales configurados |
| `DOCUMENT_SIGNATURE_REQUIRED` | Falta firma de elaborador/actor activo | Titular de slot |
| `ACTIVITY_DEADLINE_NEAR` | Ventana de alerta configurada | Responsables de actividad |
| `EVIDENCE_MISSING` | Medio obligatorio sin soporte cerca de vencimiento | Responsables según política |
| `EVIDENCE_DEADLINE_EXPIRED` | Vence sin completitud | Responsables/coordinador autorizados |
| `EXTENSION_GRANTED/REJECTED` | Autoridad decide | Beneficiario y quienes correspondan |
| `PERIOD_CLOSED` | Cierre controlado | Roles pertinentes |
| `PASSWORD_TEMPORARY_ISSUED` | Reset administrativo | Actor/usuario por canal seguro, sin exponer clave en notificación genérica |

Cada notificación guarda `recipient_user_id`, `entity_id`, evento, timestamp, read_at, payload minimizado y estado. Soportar no leídas, marcar una/todas, filtros, detalle/acción contextual. Los eventos deben ser idempotentes: una transacción/reintento no genera duplicados visibles.

### 14.2 Correo

SMTP es canal adicional para recuperación/notificaciones, pero **núcleo documental funciona sin servicio de correo**. No confiar únicamente en email para asignaciones de turno: bandeja interna es fuente canónica. `SMTP 587` es **puerto de salida** por confirmar en servidor, **no** segundo puerto público de aplicación. Si SMTP falla, recuperar acceso mediante reset administrativo y registrar alerta operativa.

### 14.3 No visibilidad de etapas futuras

La notificación y bandeja nunca exponen documento a quien aún no está en etapa activa, conforme al cliente. Usuarios con autorización histórica/autoridad institucional fuera de workflow se rigen por otra política explícita y esa consulta **no** abre tarea de revisión.

---

## 15. AUDITORÍA, TRAZABILIDAD Y HISTÓRICOS

### 15.1 Auditoría inmutable

Tabla append-only `audit_event` o mecanismo equivalente con:

```text
id UUID
occurred_at TIMESTAMPTZ UTC
actor_user_id UUID / system_actor
actor_context_id (si aplica)
action
entity_type
entity_id
period_id
scope_group_id
scope_career_id
request_id
ip_address / user_agent (según minimización de datos)
metadata_jsonb (blanqueado)
old_state / new_state cuando corresponda
```

Acciones a auditar:

- inicio/cierre de sesión, bloqueo, cambio de contraseña, reset e importación;
- altas, bajas lógicas, roles, membresías, responsables, catálogo, plantilla y workflow;
- creación/edición de borrador, archivo por error, firma, envío automático;
- observaciones, devolución, aprobación, transición de estado, nueva ronda, nueva versión;
- carga/reemplazo de anexos/evidencias y lectura/descarga autorizada de PDF final;
- concesión/rechazo de prórrogas, cambio de períodos, exportación histórica, cierre;
- errores de seguridad relevantes y ejecución de tareas críticas;
- invocaciones IA **sin guardar texto sensible del prompt ni respuesta**.

No auditar secretos en claro: contraseña, API key, token de recuperación, clave privada, binario `.p12/.pfx`. Append-only lógico más permisos DB; backup y control de integridad son necesarios para que la auditoría sea confiable.

### 15.2 Historial documental

Mostrar por Plan/Informe:

- versiones formales;
- rondas de revisión por versión;
- artefactos firmados de cada ronda/etapa;
- actores, decisiones, observaciones, fecha/hora;
- origen de actualización, diferencias relevantes;
- configuración snapshot del grupo/plantilla/workflow;
- firmas verificables y hashes.

### 15.3 Permisos históricos [CLIENTE posterior]

Un integrante nuevo no hereda acceso a todo el historial anterior de la comisión. Un exintegrante no conserva acceso automático a expediente ajeno por haber pertenecido antes. El cliente señaló que **Decano y Subdecano** pueden consultar el histórico institucional de su ámbito. Estas reglas se aplican por períodos, carreras y asignaciones efectivas, sin borrar autoría histórica.

### 15.4 Cierre del período y contradicción de retención [CONFLICTO RET-001]

En la reunión posterior el cliente solicitó simultáneamente: **(a)** que Decano/Subdecano puedan consultar históricos, y **(b)** que al final del semestre se descargue el historial y «se elimine todo». Una purga física total de DB/PDF contradice la consulta histórica y auditoría, y puede tener implicaciones de archivo/LOPDP.

**Solución segura implementable ahora [EQUIPO, no ratificación institucional]:**

1. `period.status ACTIVE → CLOSING → CLOSED/ARCHIVED`, sin cambiar estados documentales.
2. Cierre con validación de tareas pendientes, documentos a archivar y reporte de inconsistencias (no destruir pendientes silenciosamente).
3. Exportación verificable y autorizada de inventario, PDFs firmados, metadatos y trazabilidad; registrar quién ejecuta y hash del paquete.
4. Bloquear nuevas ediciones/cargas según política de período; conservar histórico y auditoría con permisos diferenciados.
5. Mantener respaldo y procedimiento de recuperación probado.
6. **No ejecutar purga física automática** hasta decisión explícita sobre retención, soportes oficiales, plazos, autorizaciones y repositorio institucional de custodia.

**[PENDIENTE INSTITUCIONAL]:** retención en servidor vs archivo externo, quién conserva el paquete, verificación de restauración, accesibilidad histórica, tiempos de retención y eliminación legal. No implementar «botón limpiar toda la base» en Admin.

---

## 16. ASISTENCIA DE REDACCIÓN CON IA

### 16.1 Alcance exacto [CLIENTE/MOCKUP]

La ayuda de IA solo asiste al usuario en campos narrativos autorizados: Justificación, Objetivo cuando aplique, Antecedentes, Conclusiones, Oportunidades, observaciones redactadas y otros campos configurados. Puede corregir ortografía, mejorar claridad y formalidad institucional. **No** decide aprobación, evalúa desempeño, inventa evidencias ni altera el contenido sin consentimiento.

Flujo obligatorio:

```text
El usuario escribe texto NO vacío → Solicitar mejora → Mostrar original y sugerencia
                         ↙                             ↘
                       Aplicar                         Descartar
                       solo ese campo                   sin modificar texto
```

- Un campo vacío devuelve «Ingrese un texto antes de solicitar una mejora.»; no autogenerar justificación ficticia.
- Aplicar/Descartar no afecta otros campos, sesiones o documentos.
- La solicitud no se guarda como nueva versión formal del documento por sí sola; modificar borrador sí queda registrado según política de autosave.
- Fallo de proveedor/no Internet/no cuota: **edición, guardado, firma, workflow y evidencias siguen funcionando** sin IA.
- El cliente indicó que **no necesita historial textual de prompts/respuestas**. Auditar solo evento mínimo anonimizado/técnico, no cuerpo confidencial.

### 16.2 Arquitectura

```text
Frontend React → /api/v1/ai/writing/suggest → AiWritingService
                                                 └─ AiProvider
                                                    ├─ MockAiProvider (DEV)
                                                    └─ GroqCloudProvider (habilitado/autoridad)
```

- **Decisión de proyecto previamente documentada:** despliegue on-premise FISEI, frontend/backend/PostgreSQL/storage locales; IA GroqCloud a través de HTTPS **saliente solo desde backend**, API key solo en servidor.
- Modelo/proveedor/capacidad Free Tier del contrato de proyecto **no es garantía de disponibilidad ni gratuidad permanente**: parametrizar provider/model, timeouts, cuota y fallo, y validar proveedor/condiciones antes de pasar datos reales.
- Se mencionó un correo genérico del sistema para administrar la credencial; no compartirlo con cuentas individuales ni exponer API key a usuarios. Los datos institucionales enviados a un tercero exigen autorización/política de protección de datos.
- Limitar tamaño de texto, tiempo, frecuencia y alcance; sanitizar entrada/salida, sin permitir que una instrucción dentro del contenido altere reglas del sistema.

---
## 17. FRONTEND COMPLETO, NAVEGACIÓN Y DISEÑO

### 17.1 Mantener el producto visual existente

Stack React + TypeScript + Vite. No migrar de framework sin aprobación. Conservar componentes T1, T2, PDF Viewer, catálogo Admin, reportes, notificaciones y auditoría que ya funcionaban en el mockup, modernizándolos solo por integración real, bugs o nuevos requisitos expresos. El mockup usa `localStorage` y datos DEMO: **producción debe usar API y estado de servidor**, sin borrar capacidades aprobadas.

### 17.2 Rutas/vistas mínimas (nombres sugeridos)

| Área | Pantallas y acciones |
|---|---|
| Público | `/login`, recuperación de contraseña, introducir código/enlace, nueva contraseña temporal, acceso denegado/404 |
| Inicio | `/dashboard` por contexto, accesos rápidos, tareas pendientes, próximos vencimientos, indicadores autorizados |
| Usuario | `/perfil`, cambio contraseña, contextos, preferencias permitidas, cerrar sesión |
| Documentos | `/documentos`, «Mis documentos», filtros por período/grupo/carrera/estado/tipo, documentos asignados, vista detalle |
| T1 | `/planes/nuevo`, `/planes/:id/editar`, wizard 6 pasos, preview, firma, enviado/revisión, historial/versiones |
| Revisión | `/revision`, `/revision/:taskId`, visor artefacto de turno, observaciones, devolución, confirmación de firma/aprobación, historial |
| Actividades | `/actividades`, `/planes/:id/ejecucion`, detalle, responsables, medios, carga/reemplazo evidencia, cuenta regresiva |
| Prórrogas | `/prorrogas`, solicitud/gestión por autoridad cuando aplique, historial de autorizaciones |
| T2 | `/informes/nuevo`, `/informes/:id/editar`, selección obligatoria de Plan, wizard 8 pasos, preview y firma |
| Reportes | `/reportes`, vista Docente/Coordinador/Autoridad/Admin, consolidado, detalle Plan, ejecución, históricos |
| Histórico | `/historico`, períodos cerrados, versiones firmadas autorizadas y exportaciones válidas |
| Notificaciones | panel campana y centro de notificaciones con filtro y marca de leído |
| Administración | `/admin`, usuarios/importación, unidades/carreras, grupos/membresías, períodos/feriados, catálogos, flujos/asignaciones, plantillas, tipos, auditoría |
| Auditoría | `/auditoria`, filtro, detalle evento, línea de tiempo del documento/actividad/evidencia |
| Operación | pantallas de fallo de servicio, permisos insuficientes, expiración de sesión, bloqueo por período |

La convención de rutas es técnica: si el repo usa otra estructura, integrar sin romper deep-links existentes. Guardar estado de filtros/paginación razonablemente, nunca credenciales ni archivos sensibles.

### 17.3 Login aprobado visualmente

- Fondo azul institucional en composición única con gradiente y geometría/puntos sutiles; **tarjeta central clara**, no división rígida azul/blanco de 50/50.
- Logo original, nombre `Gestión Documental Académica`, subtítulo `FISEI — Universidad Técnica de Ambato`.
- Correo/username, contraseña, iconos SVG (mail/lock/eye), mostrar/ocultar con aria-label.
- CAPTCHA visual alfanumérico de seis caracteres sin símbolos confusos, regeneración con icono, input independiente; error inline, control de accesibilidad. No confiar en CAPTCHA DEMO en producción.
- Recordar sesión si política lo permite y recuperación de contraseña.
- **No** botón de registro público, no lista publicitaria lateral de funcionalidades, no emojis.

### 17.4 UX institucional

- Azul institucional como color principal; blanco y grises, acentos de éxito/advertencia/error; evitar morado/neón/gamer o decoración excesiva.
- Diseño moderno sin saturar: jerarquía, iconos funcionales, breadcrumbs, tooltips, empty states, validación por campo, skeleton/loading, mensajes de error accionables, diálogos confirmatorios para firma/cierre/operaciones sensibles.
- Accesibilidad: etiquetas asociadas, foco visible, navegación teclado, botones con nombres accesibles, mensajes `aria-live` donde corresponda, alto contraste y zoom; especial cuidado con CAPTCHA visual.
- Soportar escritorio 1600×900 y 1366×768 y tabletas; sin overflow horizontal documental accidental. No invertir recursos prioritarios en editor para teléfonos.
- Al cambiar rol/contexto, conservar identidad; mostrar claramente institución/ámbito activo. Un selector DEMO que suplanta otra persona **no existe en producción**.
- Secciones previas T1/T2 aprobadas se congelan visualmente salvo necesidad demostrada.

### 17.5 Comportamiento de reset DEMO (solo entorno no productivo)

- `Restablecer documentos DEMO`: limpia documentos/actividades/evidencias/borradores de prueba y recupera seed de esos dominios; **no** borra usuarios, plantillas, catálogos, períodos ni configuración administrativa.
- `Restaurar predeterminado`: actúa sobre la **plantilla seleccionada**, no sobre otros tipos ni documentos.
- Recargar mockup preservaba DEMO; en producción persistencia está en PostgreSQL. El código de reseteo no debe estar disponible ni activable en modo productivo.

---

## 18. ARQUITECTURA, SEGURIDAD Y TECNOLOGÍAS [EQUIPO]

### 18.1 Topología de despliegue

Arquitectura aprobada de trabajo: **on-premise/institucional FISEI** (o infraestructura arrendada administrada por FISEI si DTIC lo define), acceso interno/por las redes autorizadas, con estos componentes bajo control institucional:

```text
Navegador institucional
         │  HTTPS (puerto externo institucional 64430, sujeto a verificación DTIC)
         ▼
Reverse proxy / terminación TLS
  ├── React estático / servidor web
  └── /api/v1 → Spring Boot (solo red interna)
                 ├── PostgreSQL (no expuesto públicamente)
                 ├── Storage de archivos privados
                 ├── Jobs / notificaciones / auditoría
                 └── HTTPS saliente → GroqCloud, solo si IA habilitada
```

El **cliente indicó puerto 64430 como único puerto externo disponible**. Es un dato de reunión, no promesa de red realmente configurada. Exponer solo reverse proxy, enrutar frontend+API bajo un origen, no publicar DB ni puertos Spring internos. SMTP 587 requiere **salida** permitida; no asumir que debe exponerse al exterior.

Funcionalidades núcleo (login local, Plan, PDF, firma, workflow, evidencias, reportes) deben operar sin Internet público **cuando la red local/servidor institucional sigan disponibles**. IA externa deja de funcionar sin salida a proveedor y nunca bloquea el resto.

### 18.2 Tecnologías adoptadas

| Capa | Elección | Justificación |
|---|---|---|
| Arquitectura | Monolito modular | Dominio transaccional y mantenimiento sencillo; no microservicios |
| Frontend | React + TypeScript + Vite | Base existente a conservar |
| Backend | Java 21, Spring Boot | Ecosistema estable, REST, seguridad, procesamiento documental |
| Persistencia | PostgreSQL | Integridad referencial/transacciones, `jsonb` solo donde justificado |
| Migración | Flyway | Historia versionada, sin `ddl-auto=update` en producción |
| ORM | Spring Data JPA/Hibernate | Relaciones/servicios, optimizar consultas críticas |
| API | REST `/api/v1`, OpenAPI | Integración y contratos testeables |
| Auth | Spring Security + sesión servidor y cookie `HttpOnly`, `Secure`, `SameSite`, CSRF | Mejor que tokens largos en `localStorage` para web interna |
| Contraseñas | Argon2id; BCrypt compatibilidad solo si requiere | Hash resistente, nunca cifrado reversible |
| PDF | Librería HTML→PDF institucional + PDFBox para manipulación | Soporte tablas, A4, merge y firma incremental |
| Firma | PDFBox + Bouncy Castle o solución equivalente verificada | Firmas PKCS#12 reales, no imagen/QR aislado |
| Storage | Privado local con interfaz `DocumentStorage`, futuro S3-compatible | No BLOB pesado en DB, backup conjunto |
| Pruebas | JUnit5, Mockito, Testcontainers PostgreSQL, Playwright E2E | Verificaciones funcionales y reales de base |
| Observabilidad | Logs estructurados, métricas sanitizadas, trace/request IDs | Diagnóstico sin exponer secretos |
| CI/QA | `mvnw`, lint/tsc, build, tests, SonarQube cuando aplique | Calidad y reproducibilidad |
| Orquestación DEV | Docker Compose | Fácil puesta en marcha y portabilidad |

No fijar librerías/productos sin revisar versión compatible y licencias. No declarar ISO certificada ni seguridad jurídica de firma sin validación institucional.

### 18.3 Estructura del repositorio deseada

```text
root/
  frontend/                       # React/TypeScript/Vite EXISTENTE (respetar estructura real)
  backend/
    src/main/java/.../
      auth/ users/ organization/ periods/ catalogs/
      documents/ plans/ activities/ templates/ rendering/
      workflow/ review/ files/ signatures/ evidence/
      reports/ notifications/ audit/ ai/ admin/ shared/
    src/main/resources/db/migration/
    src/test/
  database/                       # modelo/semillas separadas si procede
  infra/                          # compose, proxy, scripts backup/restore, variables ejemplo
  docs/
    requirements/ architecture/ decisions/ api/ database/
    security/ workflows/ testing/ deployment/ operations/
  manuals/
    user/ reviewer/ coordinator/ admin/
  .github/workflows/             # CI si aplica
  README.md
  CHANGELOG.md
```

Estructurar cada módulo por dominio/aplicación/infraestructura/web en medida suficiente; evitar tanto un `service` gigante como una arquitectura ornamental con decenas de capas sin valor.

### 18.4 Seguridad transversal

- TLS/HTTPS para credenciales y archivos, CSRF para sesión, cookies seguras, rate limits y manejo de sesiones.
- Usuarios de DB con privilegios mínimos, secretos por env/secret-store, rotación y archivo de ejemplo sin valores reales.
- Autorización por objeto en endpoints de búsqueda, detalle, descarga, firma y operaciones administrativas; negar por defecto.
- Inputs validados con DTO/Bean Validation; salida HTML/PDF sanitizada contra script/inyección/SSRF.
- Files: MIME real, extensión allowlist, tamaño, nombre seguro, UUID de storage, anti-path-traversal, protección contra ZIP bombs/malware según capacidad, no ejecutar contenido adjunto.
- Emisión y validación de token de recuperación: valor aleatorio de un uso, hash persistido, TTL corto, invalidación, respuesta que evita enumerar usuarios.
- Hash SHA-256 de artefactos/archivos para integridad; no usar SHA-256 solo como hashing de contraseñas.
- No registrar texto completo de IA, contraseñas, PKCS#12, claves, tokens, binarios en logs/telemetría.
- Saneamiento de metadatos y autorización en exportación/informes, incluyendo riesgo de inyección de fórmulas CSV.
- Backups protegidos y restauración periódicamente probada; no dejar paquetes históricos públicos.
- Principio de mínimo privilegio y separación de funciones para editar, firmar y administrar.

### 18.5 Fechas, versiones e identificadores

- Entidades principales UUID; no `Date.now()` ni nombres de persona para PK.
- `Instant`/`TIMESTAMPTZ` para eventos; `LocalDate`/`DATE` para fechas civiles; `America/Guayaquil` para ventanas y plazos.
- Optimistic locking `@Version` para entidades editables, conflictos HTTP 409; frontend conserva los cambios del usuario para permitir recuperación.
- Requests críticos con idempotency key/contexto/hash de payload y transacción; no doble firma por doble clic/reintento.
- Reemplazar políticas de front `localStorage` por servidor; migrations no destructivas de datos si el backend ya almacena expedientes.

### 18.6 IA y datos personales

- Proveedor IA configurado en backend (p. ej. GroqCloud); no se permite llamar a API de IA desde navegador ni comprometer credenciales con usuario.
- El uso de texto institucional con terceros necesita finalidad/permiso documentado; una cuenta genérica para API no constituye consentimiento automático para compartir datos personales.
- Integración IA opcional y degradación elegante si red/proveedor falla.

---

## 19. MODELO DE DATOS NORMALIZADO (REFERENCIA DE IMPLEMENTACIÓN)

El siguiente modelo es una **propuesta técnica [EQUIPO]** derivada de las reglas, no un DDL ejecutable. El agente debe reconciliarlo con las tablas/migraciones reales antes de crear/renombrar columnas. Las tablas finales deben usar `snake_case`, UUID, FKs, CHECKs, índices, timestamps y `@Version` donde aplique.

### 19.1 Personas, organización y configuración

| Tabla | Campos/relaciones críticos | Restricciones |
|---|---|---|
| `users` | id, username, institutional_email, given_names, surnames, active, created_at | username único, correo normalizado/único si política |
| `credentials` | user_id, password_hash, must_change_password, password_changed_at | Argon2id; secretos jamás en texto |
| `global_roles` y `user_global_roles` | roles ADMIN/autoridad; FK usuario | privilegios explícitos |
| `faculties` | nombre/código | catálogo activo |
| `careers` | faculty_id, nombre/código, active | no perder histórico |
| `units` | tipo académica/administrativa, faculty_id cuando corresponda | unidad académica no requiere dato administrativo ficticio |
| `organizational_groups` | type `COMISION/UNIDAD/CLUB/OTRO`, unit_id, name, active | identidad estable, nombres revisables |
| `user_careers` | user_id, career_id, vigencias | varias carreras permitidas |
| `group_memberships` | user, group, career_scope, period, membership_role, is_principal, fechas | no dos principales simultáneos en el mismo ámbito, salvo reemplazo registrado |
| `role_assignments` | user/role/group/career/period, from/to | autorización contextual |
| `periods` | nombre, start_date, end_date, status, ventanas | evitar solapamientos incompatibles |
| `holidays` | local_date, name, scope, active | deduplicación por fecha/ámbito |
| `system_settings` | setting key, scoped values, provenance status | seguridad no editable arbitrariamente |
| `catalog_activity_types`, `catalog_activities` | type, nombre, ámbito, active, mandatory_default | POA/MEJORAS/ACCIÓN/OTRA configurables |
| `catalog_resources` | nombre, tipo, ámbito, active | snapshot en doc |
| `catalog_verification_media` | nombre, `evidence_kind`, ámbito, active | PDF/referencia y obligación definidos |

### 19.2 Documentos y actividades

| Tabla | Campos/relaciones críticos | Restricciones |
|---|---|---|
| `document_types` | id, code, description, capabilities, active | tipos habilitados solo si definidos |
| `document_families`/`documents` | id, type_id, group_id, career_scope_id, period_id, responsible_editor_id, current_formal_version, document_state, archived | **unicidad de Plan vigente** por tipo+ámbito+período |
| `document_versions` | document_id, formal_version, parent_version_id, state, template_snapshot, created_by | historial versión formal independiente de rondas |
| `document_review_rounds` | version_id, round_no, state, created_at, returned_at, artifact_id | ronda aislada de versión |
| `document_sections` | version_id, section_key, content, order | campos tipados, no HTML arbitrario |
| `document_contributors` | version_id, user_id, role `EDITOR/ELABORATOR/...`, signature_required, order | firmantes vinculados a identidad real |
| `plans` | document_id, plan-specific attributes | 1:1 con documento tipo T1 |
| `activities` | plan_version_id, activity_type, requirement_type, description, start_date, end_date, sort_order | no duplicación matriz |
| `activity_responsibles` | activity_id, user_id | mínimo uno antes de finalizar |
| `activity_resources` | activity_id, catalog_resource_id, snapshot label, notes | FK/snapshot |
| `activity_verification_media` | activity_id, media_catalog_id, required, evidence_kind, label snapshot | fuentes requeridas de evidencias |
| `annexes` | document_version_id, file_id, sort_order, optional_description | documentos varios seguros |

### 19.3 Workflow, firma, evidencias e Informe

| Tabla | Campos/relaciones críticos | Restricciones |
|---|---|---|
| `workflow_definitions` | type/group/career scope | sin persona DEMO hardcodeada |
| `workflow_versions` | definition, version, period applicability, locked, provenance | no actualizar versión congelada |
| `workflow_steps` | version_id, order_no, required_role, signature_required | estrictamente secuenciales |
| `period_workflow_assignments` | workflow step/role, period, user | actor resuelto y autorizado |
| `workflow_instances` | document_round_id, workflow_version, immutable snapshot | por ronda |
| `workflow_tasks` | workflow instance, step, user, task_status, signed_artifact_id | una ACTIVE por vez |
| `review_observations` | task/round, section/page anchor, content, resolved_at | devolución exige ≥1 |
| `stored_files` | id, original_name, mime, length_bytes, sha256, storage_key, created_by | blob privado, no path del cliente |
| `document_artifacts` | version/round/step, storage file, sha256, parent_artifact_id, created_at | **append-only** |
| `artifact_signatures` | artifact_id, signer_id, cert_fingerprint, signing_time, slot, validation_metadata | sin clave privada |
| `evidences` | activity_verification_media_id, responsible assignment, active_version_id | medio vigente |
| `evidence_versions` | evidence_id, file_id/url_ref, version_no, timestamp, uploader, prior_version | no sobrescribir evidencias |
| `deadline_extensions` | activity_id, beneficiary user, original_deadline, new_deadline, reason, authority_id | límite parametrizado y auditoría |
| `final_reports` | document_id, source_plan_document_id, source_plan_version_id, responsible_user_id | **un Informe Final vigente por Plan**, con historial de versiones |
| `notifications` | recipient, event_type, entity_id, read_at | idempotencia evento |
| `audit_events` | actor/action/object/context/time/request/meta | append-only |
| `ai_configurations` | provider/model/limits/enabled, secret reference | API key fuera de tabla o cifrada en secret store |
| `idempotency_records` | key, actor, operation, payload_hash, result ref | reintentos sin efectos dobles |

### 19.4 Índices y constraints esenciales

- Un Plan vigente por `(type, period, group, career_scope)`, sin bloquear la creación después de archivado de borrador. Usar índice parcial o una identidad de ámbito normalizada.
- Un Informe Final vigente por Plan, sin prohibir sus versiones históricas.
- Una tarea `ACTIVE` por ronda/instancia documental; validación transaccional para no activar dos pasos secuenciales.
- Una identidad no puede ocupar dos slots de revisión incompatibles de un mismo documento/ronda.
- Un principal/editor activo por Plan o grupo/carrera/período cuando así se resuelva institucionalmente.
- `end_date >= start_date`; `sort_order >= 0`; `new_deadline >= original_deadline`; extensiones bajo máximo configurado.
- FKs `RESTRICT`/soft-delete cuando existen artefactos/historial, no `ON DELETE CASCADE` destructivo en documentos firmados.
- Índices de `period_id`, `group_id`, `career_scope_id`, `status`, `assigned_user_id`, `deadline`, `created_at`, `entity_id` de auditoría y trazabilidad.

### 19.5 Snapshots

Conservar snapshot de:

- nombres de Facultad/Carrera/Unidad/Grupo;
- nombre/cargo de cada elaborador/revisor/validador al firmar;
- recursos y medios elegidos;
- template + workflow efectivo;
- actividad, fechas y responsables tal como se aprobaron;
- firma y PDF exactos.

Cambiar un catálogo hoy **no altera** un PDF de otro período.

---
## 20. CONTRATOS API REST Y FLUJOS DE SERVICIO

Rutas **orientativas [EQUIPO]**: si el backend ya tiene un contrato adoptado, mantener compatibilidad o versionar explícitamente. Documentar todas las rutas en OpenAPI con request/response, autorización por ámbito, códigos HTTP y errores. No confiar en un único endpoint gigantesco que hace múltiples transiciones en función de un string inseguro.

| Grupo | Operaciones principales | Reglas de autenticación/autorización |
|---|---|---|
| `/api/v1/auth` | `POST /login`, `/logout`, `/change-password`, `/recover`, `/reset-password`, `/captcha/refresh`, `/me` | CSRF/sesión; rate limits; recuperación anti-enumeración |
| `/api/v1/users` | CRUD lógico, búsqueda/paginación, importación dry-run/confirm, credenciales temporales, carreras y roles | Admin o coordinador dentro de alcance |
| `/api/v1/organization` | facultades/carreras/unidades/grupos, membresías, responsables y asignaciones | Permiso contextual |
| `/api/v1/periods` | listar actual, crear, configurar ventanas, activar/cerrar, feriados, calendario | Administrador |
| `/api/v1/catalogs` | actividades/sugeridas, recursos, medios, tipo de evidencia, activation | Administrador o delegado |
| `/api/v1/document-types` | consultar y habilitar tipos documentales/versión del esquema | Solo tipos publicados |
| `/api/v1/plans` | crear en ámbito; listar/obtener; guardar borrador; actividades; anexos; archivar borrador; generar v2; informe origen | Editor principal para edición |
| `/api/v1/documents` | detalle, estados formales, versiones/rondas, preview, artifact autorizado, historial | Permisos por objeto y estado |
| `/api/v1/templates` | configurador, vista previa, publicar versión, restaurar predeterminada | Admin, sin alterar históricos |
| `/api/v1/workflows` | definiciones, versiones, asignaciones, instancias, tareas activas, reassign excepcional | Admin/Coordinador según acción |
| `/api/v1/review-tasks` | `GET /mine`, detalle, observaciones, `POST /return`, `POST /approve-and-sign` | Solo actor de tarea ACTIVE y artefacto correcto |
| `/api/v1/signatures` | iniciación de firma PKCS#12 y resultado/verificación técnica, slots | Solo titular y etapa válida |
| `/api/v1/files` | carga temporal/confirmación, metadata, preview/download autorizado | Política por tipo/ámbito; no servir storage directo |
| `/api/v1/activities` | seguimiento, requerimientos de evidencia, responsables, deadlines | Plan aprobado y ámbito válido |
| `/api/v1/evidences` | subir PDF/referencia, reemplazar, versionar, consultar historial | Responsable, ventana vigente o prórroga |
| `/api/v1/deadline-extensions` | solicitudes/autorizaciones/historial, límites | Coordinador/autoridad del ámbito |
| `/api/v1/final-reports` | crear desde Plan, prellenar, guardar T2, preview, firmar, consultar | Solo Plan elegible, uno vigente por Plan |
| `/api/v1/notifications` | listar, contar no leídas, marcar leída/todas | Solo destinatario |
| `/api/v1/reports` | plan, comisión, consolidado, historial, descarga | filtros de autorización |
| `/api/v1/audit` | consultar eventos autorizados y timeline | permiso específico, no usuario común |
| `/api/v1/ai/writing` | sugerir texto en campos autorizados | rate limit, consentimiento/política proveedor |
| `/api/v1/admin/operations` | health lógico, exportación/cierre, jobs, parámetros | admin operaciones, protección especial |

### 20.1 Ejemplo de creación de Plan (conceptual)

```json
{
  "groupId": "<uuid>",
  "careerScopeId": "<uuid-o-null-por-unidad>",
  "documentType": "T1_PLAN"
}
```

El backend **deduce** `periodId` activo, `currentUser`, rol/editor autorizado, facultad/unidad y referencias institucionales. **No** acepta `createdBy` arbitrario desde frontend. Si existe un Plan vigente para el ámbito, `409 PLAN_ALREADY_EXISTS` y, cuando esté autorizado, link al Plan existente.

### 20.2 Guardar borrador

DTO versionado con `rowVersion`/ETag y PATCH granular o reemplazo completo controlado; permitir incompleto. Validación de finalización separada de validación de guardado. Las fechas de actividades deben ser strings ISO `YYYY-MM-DD`, no timestamps.

### 20.3 Firma/inicio de revisión

Solicitar la firma usando upload protegido de PKCS#12, password solo transitivo en request, y `Idempotency-Key`. Validar: dueño del slot, estado DRAFT, última versión, integridad/metadata PDF, roles y period; si hay múltiples elaboradores, avanzar al siguiente slot, y al completar el último disparar estado `IN_REVIEW` y tarea inicial. Resultar en `201/200` con referencia al artefacto/estado, **no** con contraseña ni bytes sensibles en JSON.

### 20.4 Aprobar/devolver

Ejemplo `POST /review-tasks/{taskId}/return`:

```json
{
  "roundId": "<uuid>",
  "artifactId": "<uuid>",
  "observations": [
    {"sectionKey": "justificacion", "pageNumber": 2, "text": "Describa el sustento de la actividad."}
  ]
}
```

Rechazar lista vacía, tarea obsoleta, documento equivocado, revisor incorrecto o etapa futura. Devolución `IN_REVIEW → DRAFT`, nueva ronda al firmar corrección, no nueva versión formal. `approve-and-sign`: firma PKCS#12/credencial institucional elegible y cambio de tarea/etapa atómicos en sentido operativo.

### 20.5 Evidencia

`POST /evidences` lleva `activityVerificationMediumId` y archivo PDF o referencia según `evidence_kind` del catálogo. El backend resuelve responsable, deadline y extensión autorizada, no acepta `isValidated=true` ni `completed=true` enviados por cliente.

### 20.6 Errores estándar

```json
{
  "code": "DOCUMENT_NOT_EDITABLE",
  "message": "El documento no puede modificarse en su estado actual.",
  "details": {"documentState": "IN_REVIEW"},
  "traceId": "..."
}
```

Códigos esperados: `400` validación de input, `401` sin sesión, `403` permisos, `404` sin revelar existencia de documento restringido, `409` unicidad/concurrencia/tarea obsoleta, `413` tamaño, `415` tipo no soportado, `422` regla de negocio, `429` rate limit, `503` dependencia temporalmente indisponible. No exponer stack traces ni nombres de rutas del storage.

---

## 21. REQUERIMIENTOS NO FUNCIONALES Y CALIDAD

| ID | Atributo | Exigencia / mecanismo | Evidencia de aceptación |
|---|---|---|---|
| RNF-SEC-001 | Autenticación | Argon2id y sesiones backend seguras, CAPTCHA + rate limiting | Pruebas login, reset, CSRF y brute force |
| RNF-SEC-002 | Autorización | Permisos por documento, etapa, período, grupo, carrera, actor | Tests positivos y negativos IDOR |
| RNF-SEC-003 | Confidencialidad | HTTPS/TLS, archivos privados, secretos solo backend, restricción histórica | Red scan/test acceso directo |
| RNF-SEC-004 | Firma | PKCS#12 transitorio, firma digital incremental verificable, no claves privadas persistidas | Verificación externa compatible según DTIC |
| RNF-INT-001 | Integridad | Hash SHA-256 de archivos y artefactos, inmutabilidad de aprobados, auditoría | Alteración detectada / artefacto invariable |
| RNF-REL-001 | Consistencia | Transacciones DB, staging de storage, idempotencia, outbox y errores recuperables | Doble clic, crash/reintento |
| RNF-REL-002 | Recuperabilidad | Backup coordinado DB+storage, restore periódico probado | Ensayo restauración documentado |
| RNF-PER-001 | Capacidad | Soportar piloto 200 usuarios y diseñar para crecimiento potencial 2000, sin afirmación de concurrencia no medida | Pruebas carga con escenarios concretos |
| RNF-PER-002 | Rendimiento | Paginación, índices, caché prudente, streaming archivos, tiempos definidos por medición/objetivos | Resultados p95 de APIs y cargas de PDF |
| RNF-MNT-001 | Mantenibilidad | Monolito modular, migraciones Flyway, DTOs/versionado API y CI | Build reproducible, código modular |
| RNF-TST-001 | Testabilidad | Unitarias + integración PostgreSQL + E2E Playwright + pruebas firma/PDF | Reportes automatizados y escenarios del dominio |
| RNF-USA-001 | Usabilidad | T1 seis pasos, T2 ocho, accesibilidad, confirmaciones, errores inline | Pruebas de flujo/UX escritorio-tableta |
| RNF-COMP-001 | Compatibilidad | Navegadores escritorio actuales/soportados por institución, PDF A4 verificable | Matrix de navegadores con aprobación institucional |
| RNF-AUD-001 | Trazabilidad | Auditoría append-only con actor, timestamp UTC, request ID y objeto | Recorrido completo de flujo y versiones |
| RNF-PRV-001 | Privacidad | Minimización, control de acceso, no cédula/teléfono por defecto, IA sin textos en logs | Revisión campos y registros |
| RNF-OPS-001 | Despliegue | Único puerto externo 64430 bajo reverse proxy **si DTIC habilita**, DB interna, secretos fuera de Git | Smoke test red/host autorizado |
| RNF-OPS-002 | Operación | Núcleo disponible sin Internet externo; IA/correo con fallo degradado | Desconexión simulada de Internet |
| RNF-DOC-001 | Fidelidad documental | T1/T2 reproducen formatos institucionales con snapshots y firmas | Comparación visual PDF con referencia original |

**Métricas de aceptación:** el equipo debe definir umbrales medibles en un Plan de Calidad revisado (p. ej. p95 de API, concurrencia, tasa de errores, tamaño máximo de documento, tiempos de restore) y probarlos en infraestructura representativa. No inventar que el cliente ya aprobó «p95 ≤ X ms», SLA 99,9 %, RPO/RTO o un número de usuarios concurrentes cuando no constan en las reuniones.

**Normas y referencias adoptadas como guías del proyecto** (no certificaciones): ISO/IEC/IEEE 12207, ISO/IEC 25010/25040, ISO/IEC/IEEE 29148/15289, ISO 31000, gestión del proyecto PMI/PMBOK, OWASP ASVS/Top 10, LOPDP Ecuador y política institucional de firma. Utilizar ediciones corroboradas en la documentación académica si se citan, sin afirmar conformidad integral sin auditoría.

---

## 22. PLAN DE PRUEBAS OBLIGATORIO Y TRAZABILIDAD

### 22.1 Pirámide

- **Unitarias:** validadores de fechas, feriados, unicidad, autorización pura, máquina de estados, cálculo de evidencia/completitud y límites de prórroga, versionamiento.
- **Integración:** servicios + PostgreSQL Testcontainers con Flyway; restricciones únicas parciales, sesiones, concurrencia de firma, storage staging, tareas idempotentes.
- **Contrato/API:** OpenAPI y casos negativos 400/401/403/404/409/422/429; DTOs de fecha, archivos, metadata.
- **PDF:** representación T1/T2, fuentes, A4, matriz horizontal, contador de página, pie, índices, PDF de evidencia concatenado, filenames sin duplicar, firma visible correcta.
- **Firma criptográfica:** PKCS#12 válido/incorrecto, password incorrecta, titular incorrecto, certificado inválido/revocado según política, firma incremental multi-actor, PDF alterado, no persistencia de secretos.
- **E2E Playwright:** entorno levantado con backend real (sin localStorage como único motor), DB aislada/seed DEMO y usuario por rol.
- **Seguridad:** IDOR por IDs ajenos, acceso fuera de período/ámbito, tarea futura, download antes de APPROVED, CSRF, subida maliciosa, MIME falso, path traversal, SSRF en enlaces, XSS en plantillas/textos, logout.
- **Resiliencia:** doble clic, fallo de almacenamiento, fallo de correo/IA, timeout, reintento HTTP y recuperación del job/outbox; sin estados huérfanos.
- **Operación:** backup PostgreSQL+files y restore probado; caída de Internet externo sin afectar núcleo.

### 22.2 Matriz mínima de escenarios de aceptación

| Prueba | Preparación | Secuencia | Resultado esperado |
|---|---|---|---|
| AT-001 | Usuario importado con clave temporal | Login → cambio obligatorio → acceso | No permite usar sistema antes de cambiar clave |
| AT-002 | Múltiples contextos | Usuario cambia grupo/carrera | Identidad no cambia ni aparecen datos no autorizados |
| AT-003 | Grupo, período y carrera ya tienen Plan | Intentar otro Plan con mismo ámbito | Unicidad rechaza sin duplicación |
| AT-004 | Una persona con dos carreras | Crear T1 en ambas | Dos Planes, dos workflows, identidades de carrera aisladas |
| AT-005 | Documento T1 nuevo | Guardar sin justificación | Borrador persiste; finalizar se bloquea |
| AT-006 | Justificación válida + actividades | Configurar actividad completa y avanzar | Matriz reproduce actividad una sola vez |
| AT-007 | Intervalo con feriado intermedio | Guardar actividad | Permite; rechaza extremos en feriado |
| AT-008 | Activity sin responsables | Finalizar T1 | Rechaza con explicación «mínimo uno» |
| AT-009 | Anexos No/Sí | Continuar sin anexos, luego cargar DOCX/imagen en Sí | UX sencilla, PDF no imprime vacío/filename duplicado |
| AT-010 | Borrador completo | Cancelar diálogo de firma | Sin firma, sin transición, campos editables |
| AT-011 | PKCS#12 propio válido | Firmar elaboración | PDF firmado, pasa a primera revisión automáticamente |
| AT-012 | Dos elaboradores obligatorios | Primero firma, después segundo | Revisión inicia **solo cuando todos los obligatorios firmaron**, sin suplantación |
| AT-013 | Revisor A y B | A antes que B | B no ve tarea; al aprobar/firma A, B se activa |
| AT-014 | Revisor devuelve con observación | Abrir devolución, corregir | Estado DRAFT, conserva datos, observación resaltada, edita todo |
| AT-015 | Revisor intenta devolver sin observación | Devolver vacío | Bloqueado 422 |
| AT-016 | Nueva ronda tras devolución | Volver a firmar | Ronda incrementa, formal_version no cambia, firmas previas históricas |
| AT-017 | Revisor B aprueba/firma, final firma | Culminar workflow | Documento APPROVED; bytes final inmutables; descarga autorizada |
| AT-018 | Documento IN_REVIEW | Intentar PATCH T1 | Rechazado, sin mutación |
| AT-019 | Documento APPROVED v1 | Crear v2 formal | v1 intacta; v2 DRAFT enlazada, requiere workflow completo |
| AT-020 | Borrador accidental | Archivar | Queda registro, no ocupa unicidad vigente, sin DELETE |
| AT-021 | Plan aprobado con medios PDF | Cargar PDF por cada medio | Actividad pasa a completa cuando todos requeridos cubiertos |
| AT-022 | Medio referencia | Registrar URL permitida | Cumple regla del medio sin forzar PDF si catálogo lo permite |
| AT-023 | Evidencia reemplazada dentro de plazo | Subir v2 | v1 histórica, v2 vigente, auditoría |
| AT-024 | Evidencia vencida sin prórroga | Subir/reemplazar | Backend bloquea operación |
| AT-025 | Coordinador concede prórroga individual | Usuario beneficiario vuelve a subir | Solo beneficiario habilitado, límite guardado y auditoría |
| AT-026 | Un PDF igual en dos comisiones | Asociar en ambos ámbitos autorizados | No rechazo por hash global; seguridad de ámbito conservada |
| AT-027 | Plan aprobado | Crear Informe Final T2 | Origen prellenado, no muta Plan fuente |
| AT-028 | Plan con Informe Final vigente | Intentar segundo Informe Final | Rechaza duplicado; permite versión de la misma línea si aplica |
| AT-029 | T2 con evidencias PDF y repositorio | Generar PDF T2 | Evidencias PDF al final; enlace como referencia; páginas/índices correctos |
| AT-030 | Usuaria abandona comisión anterior | Intentar ver histórico ajeno | Denegado; autoridad con ámbito autorizado sí consulta |
| AT-031 | Admin cambia plantilla publicada | Abrir artefacto antiguo | No cambia contenido ni bytes del documento histórico |
| AT-032 | Admin cambia flujo futuro | Plan en revisión | Instancia/turnos previos se conservan; no cambio medio semestre |
| AT-033 | Actor es revisor y aprobador del mismo doc | Publicar asignación | Validación la rechaza |
| AT-034 | Fallo durante firma/storage | Reintento con misma key | No doble firma ni `IN_REVIEW` huérfano |
| AT-035 | Sin Internet externo | Editar T1/firmar/generar T2; invocar IA | Núcleo funciona; IA informa indisponibilidad sin borrar texto |
| AT-036 | Cierre de período | Archivar/exportar | Históricos conservados; no purga automática destructiva |
| AT-037 | Archivo .exe / PDF falso / path traversal | Subir | Rechazo con 4xx y sin escritura insegura |
| AT-038 | Prueba de ventana 1366x768 / tablet | Navegar T1, T2, preview | Legibilidad, foco, sin overflow crítico |
| AT-039 | Prueba de acceso/descarga por rol | Preview antes/aprobación | Actor futuro no ve; descarga PDF final solo tras aprobación y con permiso |
| AT-040 | Admin cambia configuración de catálogos | Documento firmado anterior | Snapshot preservado, nueva selección usa catálogo nuevo |

**Reglas E2E adicionales heredadas del prototipo:** no-visible-emojis, fecha institucional sin desfase horario, A4 correcto, no divergencia entre currentUser y activeContext, autosave IA por campo, aislamiento T2/T1, reseteo DEMO independiente de Admin y preview expandido con Escape. Conservar/actualizar pruebas antiguas, nunca eliminar cobertura solo para «PASS».

### 22.3 Evidencias de calidad requeridas

Para cada módulo registrar:

```text
RF ID → pantalla/ruta → endpoint → servicio/entidad → migración → prueba → evidencia
```

Ningún conteo de pruebas de la versión mockup (32/36 E2E históricas) debe presentarse como prueba de backend completo. Ejecutar la suite del repositorio actual y reportar cifras reales. No usar `skip`, mocks vacíos o assertions triviales para falsear éxito.

---

## 23. METODOLOGÍA, GESTIÓN DEL PROYECTO Y DOCUMENTACIÓN

**Metodología elegida explícitamente por el usuario:** **Scrum + Kanban (Trello)**. El desarrollo puede seguir iteraciones/sprints con planificación, tablero, revisión, retrospectiva y entregables verificables; Kanban visualiza estados, prioridades y bloqueos. No confundir Scrum con los cinco grupos clásicos de proceso de PMI; la gestión de alcance, cronograma, interesados, riesgos y cierre se documenta por separado. Si la documentación afirma Scrum formal, registrar roles/ceremonias y sus evidencias en el proyecto.

Artefactos necesarios:

- contexto/alcance, problema, actores y beneficiarios;
- historias/requisitos `RF`, `RNF`, reglas y criterios de aceptación;
- matriz de trazabilidad y matriz de permisos;
- casos de uso por rol; flujos BPMN/UML o equivalentes actualizados;
- diagramas arquitectura, modelo ERD, diccionario de datos y ADRs;
- especificación OpenAPI real, endpoints, auth, errores;
- plantillas institucionales versionadas;
- matriz de riesgos, medidas de seguridad, plan de calidad y pruebas;
- configuración y despliegue Docker; procedimientos backup/restore, monitoreo y actualizaciones;
- manual Usuario/Docente, Revisor, Coordinador, Admin;
- manual técnico y operación; changelog, mantenimiento y aceptación;
- bitácora de decisiones cliente y supuestos configurables;
- registros de reuniones/pruebas piloto y entrega.

**Control de documentos académicos:** antes de entrega pueden mantenerse encabezados/documentación institucional en **versión 1.0** según instrucción del usuario; esto **no** autoriza borrar historiales funcionales de documentos T1/T2 ni reescribir migraciones de DB.

---

## 24. INFRAESTRUCTURA, BACKUPS Y OPERACIÓN

### 24.1 Entorno DEV / pruebas / producción

- **DEV:** Docker Compose, DB y storage separados de producción, fixtures DEMO, mail tester opcional, firma con certificados de prueba claramente etiquetados; secretos nunca en Git.
- **TEST/CI:** PostgreSQL efímero, datos aislados, suite seguridad/firma/PDF, archivos de prueba controlados.
- **PROD:** reverse proxy TLS, backend Java aislado, PostgreSQL privado, storage privado, backups coordinados, jobs/notificaciones, seguridad y logs; sin endpoint de reset DEMO ni identidades ficticias.

### 24.2 Un solo puerto externo

La reunión menciona `64430` habilitable como puerto de aplicación. Recomendar `https://host:64430` o host institucional que DTIC configure; frontend y `/api` comparten el listener/proxy, evitando puertos web adicionales públicos. `587` corresponde a conexión **saliente** SMTP; el protocolo puede variar según el servicio institucional autorizado. Verificar firewall de salida a GroqCloud si se habilita IA.

### 24.3 Backup coherente

- Base de datos y storage forman una unidad lógica: backup de solo PostgreSQL **no** restituye PDFs.
- Copias automáticas/snapshots con ventana consistente, cifrado, almacenamiento seguro y retención aprobada.
- Procedimiento restauración con chequeo de hashes, estado de migraciones, permisos, indexado de PDFs y pruebas funcionales; registrar simulacros.
- RPO/RTO, ubicación y periodicidad definitivos **[PENDIENTE DTIC]**; usar valores DEV etiquetados, no atribuir SLAs falsos a institución.

### 24.4 Observabilidad y mantenimiento

- Health/readiness/liveness, logs estructurados con `traceId`, métricas de endpoints/jobs/storage, alertas de fallos de firma/espacio disco/DB y errores de correo.
- Política de actualización de dependencias, vulnerabilidades, rotación de secretos, monitoreo de certificado TLS, archivado y restore.
- Prueba de no dependencia de Internet externo para funciones núcleo.

### 24.5 Datos necesarios de DTIC

Antes de despliegue solicitar: sistema operativo y soporte, CPU/RAM/disco, tipo/volumen de almacenamiento, dominio/IP, TLS/certificado, proxy/firewall, autorización Docker/SSH, puertos 64430 y 587, salida HTTPS a proveedor IA, repositorio de backups, cuentas de servicio, política de retención, disponibilidad esperada, RPO/RTO, verificación institucional de firmas, OCSP/CRL y sello de tiempo. No detener construcción del backend por desconocer esos valores; parametrizar infraestructura.

---
## 25. PLAN DE IMPLEMENTACIÓN PARA EL AGENTE (ORDEN OBLIGATORIO DE TRABAJO)

### Fase 0 — Auditoría y reconciliación SIN cambios destructivos

**Entregable:** `docs/REPORTE_AUDITORIA_IMPLEMENTACION_VS_ESPECIFICACION.md`.

1. Inspeccionar árbol de repositorio, dependencias, versiones, frontend, backend, Flyway y datos existentes.
2. Comparar **cada RF, RNF, regla y pantalla** de este archivo contra código y tests.
3. Clasificar `IMPLEMENTADO / PARCIAL / FALTA / CONTRADICE / PENDIENTE FUENTE`.
4. Identificar entidades/migraciones actuales con unicidad vieja `(teacher,group,period)`, revisión paralela, estados heredados, reporte T2 1:N y evidencia con validador: **recomendar migración no destructiva** antes de reescribir.
5. Reportar dependencias entre módulos, riesgos para datos y plan de pruebas de regresión.
6. **No** modificar BD productiva, revertir datos o reescribir código amplio sin respaldo y plan aprobado.

### Fase 1 — Fundación y seguridad transversal

- Construir/consolidar monolito modular Java 21 + Spring Boot y PostgreSQL Flyway, configuración perfiles DEV/TEST/PROD, validación/errores estandarizados y OpenAPI.
- Añadir seguridad de sesión/cookie/CSRF, Argon2id, rate limiting, identidad real/contexto autorizado, restricción global por ámbito y auditoría base.
- Tests de migración, auth y autorización. Docker DEV y health checks.

**Salida:** login real, seguridad de sesión, migraciones reproducibles, auth y auditoría mínima; no dependencia de identidades mock.

### Fase 2 — Organización, períodos y catálogos

- Facultades, carreras, unidades, grupos, membresías por período/carrera, roles, responsables editor/jerarquía.
- Admin CRUD + importación de usuarios, primera contraseña, resets, control de carreras múltiples.
- Período actual y feriados; catálogos POA/Plan de Mejoras/Acción de Mejora/Otra, recursos y medios; seeds DEMO etiquetados.
- Congelación de configuración operativa por período y snapshots.

**Salida:** Admin puede configurar dos carreras de una persona, asignaciones y fuentes de actividad sin editar Java.

### Fase 3 — Motor documental y T1

- `documents`, `document_versions`, `plans`, borradores, unicidad por `period+group+career+type`, archivo lógico.
- T1 wizard de seis pasos, integración API, autosave por campo/paso y optimistic locking.
- Actividades como única fuente matriz, responsables, recursos, medios, fechas/feriados; anexos Sí/No y archivos seguros.
- Tests AT-003 a AT-010.

**Salida:** T1 completo en DRAFT, persistente en DB, sin duplicación y con controles.

### Fase 4 — Plantillas, render, visor y artefactos

- Generar T1 canónico backend según DOCX/PDF guía, A4, tabla matriz, pie/firma/historial, orientación y numeración.
- Storage privado con UUID/sha256, staging y protección de descarga; visor paginado de artefacto real.
- Versionar plantillas y snapshot en cada documento.
- Pruebas visuales/automatizadas de composición.

**Salida:** PDF reproducible previo a firma y mecanismos de inmutabilidad.

### Fase 5 — Firma criptográfica y workflow estrictamente secuencial

- PKCS#12 temporal, verificación con política institucional, firma PDF incremental y artefactos por etapa.
- Asignaciones por período/tipo/grupo/carrera, workflow versionado y congelado, única tarea activa, prohibición de doble actor incompatible.
- `Firmar → revisión automática`; todos los elaboradores obligatorios firman primero si corresponden.
- Revisión, observaciones, devolución a DRAFT, nueva ronda, re-firma, aprobación y descarga controlada.
- AT-011–019, AT-032–034, AT-039.

**Salida:** ciclo T1 completo verificable con N firmas sin romper las anteriores.

### Fase 6 — Ejecución y evidencias

- Actividades aprobadas, medios obligatorios PDF/referencia, upload privado, reemplazo versionado, completitud por cobertura, deadlines servidor, mensajes de vencimiento.
- Extensiones individuales autorizadas, límites configurados/pendiente de ratificación, alertas y auditoría.
- AT-021–026, AT-035, AT-037.

**Salida:** ejecución real con seguimiento y restricciones de plazo.

### Fase 7 — Informe Final T2

- Origen Plan válido, relación **1:1** Informe Final, datos prellenados y aislados de T1, wizard 8 pasos.
- Matriz ejecución, narrativas, contactos condicionales, anexos PDFs concatenados y referencias digitales.
- PDF, firma y workflow de T2 por tipo; historial y versiones.
- AT-027–029.

**Salida:** cierre documental completo Plan→evidencias→Informe Final.

### Fase 8 — Administración avanzada, reportería e histórico

- Plantillas versionadas/publicación con permisos, workflow por período y asignaciones, catálogos ampliados, consolidado/reportes y filtros por autoridad.
- Bandeja interna, jobs de recordatorios, auditoría buscable, cierre/exportación segura sin purge físico.
- AT-030–031, AT-036, AT-040.

**Salida:** administración sin hardcodear flujos y reportería trazable.

### Fase 9 — IA opcional y hardening

- Backend `AiProvider` e integración aprobada con GroqCloud si existen credencial y autorización; no almacenar texto de solicitudes.
- Pruebas proveedor no disponible, límites, privacidad, apply/discard, aislamiento campos; no bloquear núcleo.
- Seguridad ASVS priorizada; cargas, certificados, protección PDF, logging, revisiones de dependencias.

**Salida:** IA complementaria y revisión transversal de seguridad.

### Fase 10 — Despliegue, pruebas piloto y aceptación

- DTIC confirma servidor, dominio/puerto, TLS, SMTP, firma confiable, backups y permisos.
- Despliegue reversible con Docker/servicios aprobados, migraciones y restauración probadas.
- Pruebas end-to-end por actor real de ejemplo, aceptación con casos oficiales y capacitación/manuales.
- Soporte de inicio, corrección de incidencias y documentación de entrega.

**Salida:** solución instalada, verificada, con acta de aceptación institucional, sin reclamar conformidad donde faltó decisión.

### 25.1 Dependencias críticas

```text
Identidad/autorización + organización + períodos
    ↓
T1/actividades + catálogos
    ↓
PDF canónico + storage + plantilla
    ↓
Firma real + workflow secuencial
    ↓
Plan APPROVED
    ↓
Ejecución + evidencias + prórrogas
    ↓
T2 Final vinculado + firmas + PDF evidencia
    ↓
Histórico + reportes + cierre/exportación
```

La IA no es dependencia del flujo central; la firma de producción sí necesita decisión de seguridad/validación institucional antes de uso con personas reales.

---

## 26. REGISTRO DE CAMBIOS DE DECISIONES (ANTERIOR → VIGENTE)

Esta tabla evita que el agente reactive reglas del mockup ya reemplazadas.

| Tema | Primera versión / mockup | Decisión posterior que prevalece | Acción en código |
|---|---|---|---|
| Propiedad T1 | Docente+grupo+período | **Titular: comisión/unidad**, docente es responsable/autor | Cambiar modelo/constraint sin perder histórico |
| Unicidad | `teacherId + groupId + periodId` | **grupo + carrera (si aplica) + período + tipo T1** | Migración/versionado + test multicarrera |
| Responsable editor | Docente que creó | Persona principal única del grupo/carrera/periodo; otros elaboradores sin coedición | Separar editor y contribuyentes |
| Paralelismo | Varios revisores en etapa simultánea | **Estricto secuencial**, todos los requeridos firman en orden | Rehacer activation de tasks, sin `ALL` paralelo |
| Estados | DRAFT, review, correction, validated, execution, finished | **Tres documentales DRAFT/IN_REVIEW/APPROVED**, ejecución aparte | Migración mapeo de estados, no perder eventos |
| Corrección | Estado especial `EN_CORRECCIÓN` | Devolución regresa a DRAFT, preserva observaciones | UI indicador derivado, no nuevo estado |
| Firma/enviar | Acciones separadas en primeros protos | **Firmar inicia revisión automáticamente** cuando se completan firmas requeridas | Unificar transición |
| Versiones | Posible v2 por devolución | **Ronda nueva**, v2 solo si v1 aprobada y cambio material | Separar formal_version y review_round |
| Informe Final | Posible T2 independiente o varios informes por Plan | **Un Informe Final por Plan, no se crea sin T1**; otros tipos aparte | Relación 1:1 funcional, versiones propias |
| Evidencias | Aprobación/validación independiente y estados OBSERVADA/VALIDADA | **Completitud por medios acreditados**, sin workflow general extra | No arrastrar validación antigua como gate |
| Teléfono | Responsive móvil genérico | **PC obligatoria, tablet aceptada, móvil no objetivo** | Priorizar layouts correctos |
| Prórrogas | Sin prórroga definida | **Extensión individual por autoridad**, máximo verbal ≈3 días | Motor/configuración; aclarar unidad de plazo |
| Descarga | Preview/descarga indistinta | **Descarga PDF final solo tras APPROVED**, visor restringido para revisión | RBAC endpoints |
| IA | Mock determinista | Credencial institucional de backend para proveedor externo aprobado; sin historial textual | Provider abstraction, privacidad |
| Login | Mitad azul/mitad blanco + reto matemático | Tarjeta central moderna + CAPTCHA alfanumérico | Conservar UI mejorada |
| UI T1 | Siete/ocho pasos incluyendo matriz | **Seis pasos**, actividades unificadas, anexo Sí/No | No recrear matriz capturable |
| Retención | Históricos consultables | Cliente también mencionó eliminación tras exportar | **Conflicto abierto**: archivar/exportar, no purgar hasta autorizar |

---

## 27. CONFLICTOS, DUDAS Y DEPENDENCIAS GENUINAS

**Regla:** no confundir «dato faltante» con «regla faltante». Grupos, nombres, revisores, recursos y fechas se cargan por Admin desde documentación oficial; **no necesitan bloquear el desarrollo técnico**. Las cuestiones siguientes sí pueden cambiar reglas o producir pérdida de información.

| ID | Tema | Qué sabemos | Decisión de implementación hasta resolución | A quién preguntar / prioridad |
|---|---|---|---|---|
| **RET-001** | Borrado al fin del semestre vs acceso histórico | El cliente pidió consultar históricos por Decano/Subdecano y también exportar+«eliminar todo». | Cierre/exportación, archivo y conservación; **sin purga física automática**. | Custodio documental/cliente y DTIC/jurídico; **P0 antes de producción** |
| **SIG-001** | Confianza jurídica y técnica de firma | Se aceptó `.p12/.pfx` y apariencia visible, pero falta definición oficial de emisor/cadena/revocación, PAdES/TSA/verificador y política de suplencia. | Servicio de firma real desacoplado, certificados TEST en desarrollo; **no atribuir validez institucional sin comprobación**. | DTIC/área jurídica; **P0** |
| **SIG-002** | Varios elaboradores y envío automático | Deben firmar los elaboradores de la matriz y «apenas firma va a revisión». | Última firma requerida dispara revisión sin clic extra; mostrar firmas pendientes. | Cliente funcional para caso de elaboradores múltiples; **P1** |
| **DLD-001** | Tope y tipo de días de extensión | Último audio alude a 3 días, menciona también «10 laborables» de forma confusa. | Parámetro, default DEMO 3 días calendario; no presentarlo como regla institucional final. | Coordinador/cliente; **P0 para evidencias reales** |
| **DLD-002** | Carga iniciada antes del límite y terminada después | El límite 23:59 está confirmado, pero momento exacto de aceptación no. | Server-received-complete/commit antes del deadline; documentar y auditar. | Cliente funcional + DTIC; P1 |
| **REP-001** | Porcentaje de ejecución | Cliente exige porcentaje por comisión; T2 original lo contiene; evidencia completa no demuestra por sí sola resultado material. | Mostrar conteos, tasa de evidencias y actividades; etiquetar definición de porcentaje, no automatizar afirmación cualitativa. | Cliente responsable de informes; **P1** |
| **TPL-001** | Versiones oficiales y formatos adicionales | Existen T1/T2 DOCX de referencia y lista de comisiones; prometió otros formatos y tabla de actores. | T1/T2 operativos, otros tipos deshabilitados hasta plantilla, reglas y workflow. | Cliente/custodio documental; **P1** |
| **DOC-001** | Informe no derivado del Plan en T2 oficial | DOCX T2 permite informe no planificado; última reunión dice Informe Final nunca sin Plan. | Informe Final T2 requiere Plan; otros informes como nuevos tipos documentales. | Solo confirmar mapeo de tipos cuando entreguen formatos; P1 |
| **ORG-001** | Un mismo contenido para dos carreras | Cliente dijo «mismo archivo» y «dos flujos diferentes». | Dos instancias/documentos con ámbitos, firmas y rutas separadas; copia de contenido controlada. | Cliente para si desea función de duplicado/copia; P2 |
| **ORG-002** | Designación en ausencia durante semestre | Workflow estructural no cambia durante semestre; Coordinador reemplaza actor. | Mantener pasos congelados, permitir reasignación auditada sin borrar firmas pasadas, bloquear si vacío. | Cliente para autorizaciones excepcionales; P1 |
| **DOC-002** | Criterio objetivo de cambio «sustancial» para v2 | Nueva versión formal solo tras aprobación y cambio material; no se definió lista exhaustiva. | Solicitud explícita con motivo y revisión normal; no clasificar automáticamente por IA. | Custodio del proceso; P2 |
| **PER-001** | Plazos reales elaboración/revisión/cierre | Se mencionó elaboración habitual 5 días, no ventanas por grupo/periodo ni fechas oficiales. | Configuración por período; DEMO etiquetado, ninguna fecha inventada irreversible. | Admin/cliente; P1 antes del piloto |
| **USR-001** | Quién puede crear/importar credenciales | Admin seguro; cliente también propuso Coordinador de ámbito. | Admin global; Coordinador bajo privilegio otorgado y alcance preciso. | Cliente responsable de autorizaciones; P1 |
| **PRV-001** | Política completa datos personales/retención/exportación | Nombres/cargo/carrera/correo sí; teléfono/cédula no. No existe tabla de retención legal ratificada. | Minimizar, cifrar/proteger, auditar, no compartir texto IA por defecto, no purgar. | Institución/DPD/DTIC; P0 producción |
| **INF-001** | Características del servidor institucional | Puerto externo 64430; SMTP 587 de salida a verificar; recursos/SO/dominio no confirmados. | Docker portable y reverse proxy unificado, variables de entorno. | DTIC/infraestructura; P0 despliegue |
| **QIP-001** | QIPOC/procedimiento externo | Aparece en guía de preguntas, no se identificó respuesta posterior inequívoca. | No crear integración/proceso ficticio; feature deshabilitada. | Cliente si corresponde; P2 |
| **ANX-001** | Lista final de extensiones/límites | Anexos «cualquier documento», evidencias PDF; 10 MB y 20 son valores de desarrollo. | Allowlist segura configurable y límites provisionales. | Cliente/DTIC por límites productivos; P2 |

### 27.1 El agente NO debe detenerse por estos datos faltantes

- Listado oficial actualizado de personas y roles; se cargará desde fuente institucional.
- Nombres exactos de revisores y autoridad por cada grupo/carrera/período; ya existe motor de asignación.
- Fechas de períodos futuros y feriados específicos; Admin las introduce.
- Recursos, medios y actividades reales; usar catálogos DEMO bien identificados, no hardcodeados.
- Cronograma escolar/plan de trabajo del equipo y credenciales de GroqCloud; la IA es opcional y el equipo puede seguir con backend núcleo.

### 27.2 Lo que nunca debe decidir arbitrariamente Admin

- Que un documento firmado se pueda sobrescribir;
- Que un actor se autoapruebe;
- Que una firma inválida sea aceptada como válida;
- Que se eliminen históricos/auditoría fuera de política aprobada;
- Que todos los documentos privados sean públicos;
- Que se envíen datos personales a proveedores de IA sin autorización;
- Que se salten revisores o cambios de flujo del semestre en expedientes iniciados.

---

## 28. ACTA DE ACEPTACIÓN DEL PRODUCTO TERMINADO

El software estará completo para piloto **solo cuando** los responsables técnicos puedan demostrar, con datos de prueba realistas y una configuración institucional representativa, estos hitos:

1. **Acceso:** usuario importado, cambio temporal obligatorio, sesión/recuperación alternativa segura, permisos correctos.
2. **Configuración:** Admin carga períodos, grupos/carreras/membresías/catálogos, flujo por tipo y autoridad, plantilla T1/T2 y calendario; cambios no alteran expedientes históricos.
3. **Plan:** un responsable crea T1 con seis pasos, registra actividades/fechas/medios, selecciona anexos, obtiene PDF fiel al T1 oficial y firma.
4. **Revisión:** documento transita en **secuencia** por todos los revisores necesarios; devolución con observación → DRAFT → corrección → nueva ronda → firmas → APPROVED. No permisos de actor futuro.
5. **Seguridad documental:** antes de aprobar no se descarga PDF final; tras aprobación, descarga autorizada y firma verificable con artefacto inmutable.
6. **Ejecución:** cada responsable acredita medios con evidencia PDF/referencia; reemplazo/historial, límite horario/feriados, prórroga individual; progreso calculado.
7. **Informe Final:** único y enlazado al Plan, mismo responsable, matriz de ejecución, anexos de evidencias como páginas PDF, flujo propio y firma; sin mutación de T1.
8. **Gestión:** notificaciones, reportes agrupados, histórico según rol, auditoría, cierre/exportación sin pérdida irreversible.
9. **Resiliencia:** fallo IA/correo no bloquea flujo; backup+restore probados; operaciones críticas idempotentes; sin secretos en logs.
10. **Calidad:** pruebas unitarias/integración/E2E, seguridad, accesibilidad desktop/tableta, documentación de despliegue/manuales, aceptación institucional firmada.

**Bloqueadores de producción que no puede maquillar la demo:** política de firma confiable con DTIC, infraestructura real, permisos institucionales completos, retención, listas/formularios oficiales y pruebas restauración/seguridad.

---

## 29. PROMPT OPERATIVO PARA EL AGENTE (PEGAR JUNTO AL ARCHIVO)

> Lee íntegramente `UTAPED_ESPECIFICACION_MAESTRA_COMPLETA_PARA_AGENTE.md`. Este documento prevalece sobre los resúmenes previos cuando existan decisiones posteriores del cliente. **No programes todavía cambios destructivos**. Primero inspecciona repositorio y genera `docs/REPORTE_AUDITORIA_IMPLEMENTACION_VS_ESPECIFICACION.md` con matriz requisito→archivos→estado→riesgo→prueba, incluyendo todas las discrepancias con la base actual. Separa hechos verificables del código, fuentes del cliente, decisiones de equipo y pendientes. Identifica especialmente unicidad Plan grupo+carrera+período, editor principal, tres estados formales, revisión estrictamente secuencial, todos los elaboradores/revisores firmantes, Firma→Revisión, Informe Final 1:1, evidencia sin workflow adicional, prórrogas, auditoría y retención. Propón cambios/migraciones no destructivos y el orden por dependencia. **Después de revisar y aprobar el plan**, implementa por fases, prueba cada módulo y actualiza OpenAPI/documentación; no cambies el diseño aprobado sin necesidad.

### 29.1 Formato de informe por tarea del agente

```markdown
## Tarea / módulo
- RF/RNF aplicables:
- Fuente funcional/decisión:
- Estado anterior y diferencias:
- Archivos modificados:
- Migraciones nuevas y compatibilidad:
- Endpoints/DTO/errores:
- Reglas de autorización:
- Tests añadidos:
- Comandos y resultados REALES:
- Capturas/snapshots si UI:
- Riesgos abiertos:
- Decisiones institucionales NO asumidas:
- Siguiente dependencia:
```

### 29.2 Reglas de finalización por funcionalidad

```text
[ ] Regla RF y estado de dominio verificados
[ ] Autorización backend y casos negativos probados
[ ] DTO/Bean Validation y errores HTTP consistentes
[ ] Entidad, índices y migración Flyway coherentes
[ ] Auditoría/evento histórico cuando aplica
[ ] Operación atómica/idempotente cuando crítica
[ ] Contrato OpenAPI actualizado
[ ] Unitarias + integración PostgreSQL
[ ] Prueba E2E al cerrar flujo de usuario
[ ] Frontend integrado, accesibilidad y UX mantenidas
[ ] No regresión T1/T2/visor/login aprobados
[ ] No secretos ni datos DEMO en producción
[ ] Documentación y changelog de implementación actualizados
```

---

## 30. SÍNTESIS FINAL / LÍNEA BASE

**La versión objetivo de UTAPED** es una plataforma institucional completa basada en Spring Boot/PostgreSQL/React, cuyo documento Plan T1 pertenece a comisión/carrera/período, es editado por un principal único, y se materializa en PDF firmado, revisado **secuencialmente** por actores obligatorios hasta aprobarse. Sus actividades y medios se acreditan durante ejecución, con evidencia versionada, plazos y extensiones controladas. Al final existe **un Informe Final por Plan**, con anexos/evidencias integrados y trazabilidad. Administración configura identidades, grupos, períodos, catálogos, formatos, flujos y permisos, preservando históricos y evitando hardcodear nombres. La firma privada no se almacena, la IA es opcional y las acciones sensibles se auditan. Los conflictos de firma confiable, retención y algunos límites institucionales quedan declarados, sin impedir desarrollar los módulos técnicamente independientes.

**Fuente de verdad para el agente:** este archivo, más el material oficial posterior que el usuario incorpore expresamente. Una decisión nueva no se aplica en secreto: se actualiza aquí, se registra su fuente y después se modifica el software con migraciones/pruebas.
