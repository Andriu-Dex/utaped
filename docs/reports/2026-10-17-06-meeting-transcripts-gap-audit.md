# Auditoría de reuniones frente a UTAPED productivo

Fecha: 2026-10-17. Código revisado: `1d502cb`, rama `feature/realistic-development-identities`. Alcance: análisis estático y documentación; sin modificar código, datos, configuración ni firmas.

## Resultado

**Sí hay decisiones que todavía no están implementadas, capacidades parciales y diferencias concretas de comportamiento.** La transcripción es útil como evidencia primaria y refuerza requisitos ya recogidos en el Plan Maestro; no es información inútilmente duplicada. Tampoco demuestra aceptación del producto actual: el propietario aclara que en esas reuniones se mostraron exclusivamente Prototipos.

Los problemas más urgentes son poder completar una etapa después de registrar observaciones y el bloqueo de un flujo con órgano colegiado sin personas firmantes. Después deben completarse plantillas estructurales, coautores y cargos, permisos administrativos/contextuales y excepciones de elaboración. La implementación actual permite revisar un circuito inicial; no equivale a cubrir íntegramente lo solicitado en reuniones.

## Método y límites de la fuente

Se leyó completo `docs/decisions/Todas las reuniones hasta la del 6-10-2026.txt`: 1053 líneas, SHA-256 `50182038cda38f0fdb8cbe877d3c904e8aae4a751ce014efc8cce9f60c3b4a34`. Se contrastó con el Plan Maestro, requisitos, decisiones pendientes, estado y componentes backend/frontend correspondientes. La fuente original permanece intacta y sin añadir a staging.

Los bloques reinician sus marcas de tiempo y no tienen fecha individual fiable. Hay repeticiones, errores de transcripción y cambios de criterio: por ejemplo, descarga exclusiva del validador frente a descarga autorizada después de aprobar, y conservación histórica frente a eliminación semestral. No se cuenta una repetición como una confirmación adicional ni se convierte una demostración del presentador en una regla aprobada por el cliente. El Plan Maestro vigente y las decisiones posteriores explícitas del propietario conservan precedencia; se registran las diferencias en vez de reabrir automáticamente lo ya resuelto.

Las referencias Lx indican líneas del TXT; los enlaces de código señalan el punto principal de evidencia. Las brechas identificadas por inspección no se presentan como fallos reproducidos mediante pruebas nuevas.

## 1. Brechas verificadas y prioridad propuesta

### A01. Plantillas: estructura y tipos documentales — P1, faltante ya previsto

- Fuente: [L17–18](<../decisions/Todas las reuniones.txt#L17>), L38, L476–480, L794; Plan Maestro §22.
- Se solicita poder configurar secciones, orden, obligatoriedad y contenido opcional; también se presentó la posibilidad de añadir tipos documentales.
- Actual: [DocumentTemplateModels.java:27](../../backend/src/main/java/ec/edu/uta/utaped/planning/DocumentTemplateModels.java#L27) solo acepta versión y justificación; el administrador cambia el texto inicial T1. T1Template/T2Template mantienen composición específica y no existe un editor de estructura ni alta genérica de formatos.
- Falta versionar una estructura permitida, conservar su snapshot por documento y adaptar formulario/renderizado sin cambiar históricos. Añadir otros tipos requiere sus plantillas reales: no se deben inventar formularios por la mera mención de «eventos».
- No es necesario retirar la justificación administrable: es una entrega parcial válida, no la administración completa de plantillas.

### A02. Varios elaboradores, un único editor — P1, faltante ya documentado

- Fuente: [L603–611](<../decisions/Todas las reuniones.txt#L603>), L700–701; Plan Maestro §§8.3 y 20.
- Actual: [T1PdfEngine.java:89](../../backend/src/main/java/ec/edu/uta/utaped/documents/T1PdfEngine.java#L89) crea un único slot ELABORADO_POR asociado al principal. T1Template:122 y T2Template:89 muestran una sola persona elaboradora; AuthorSignatureService trabaja con ella y después inicia revisión.
- Falta registrar el conjunto de elaboradores, distinguirlo de responsables de actividades, permitir sus firmas sucesivas y comenzar revisión una vez cumplidas las firmas requeridas, conservando un solo editor principal. Seleccionar tres responsables de una actividad no cubre este requisito.

### A03. Aprobar con observaciones de la ronda — P0 funcional, diferencia localizada

- Fuente: [L8](<../decisions/Todas las reuniones.txt#L8>) y L422–428: si existen observaciones para corregir, devolver; aprobar cuando no las haya.
- Actual: [ReviewService.java:140](../../backend/src/main/java/ec/edu/uta/utaped/workflow/ReviewService.java#L140) exige observación al devolver, pero COMPLETE no comprueba las registradas. El método sign tampoco lo hace antes de advance. ReviewWorkspace:80 bloquea la decisión por texto local sin guardar, pero después de guardar la observación ese texto queda vacío y no utiliza las observaciones persistidas para impedir aprobación.
- Falta una política explícita de observaciones pendientes/resueltas y validación en servidor antes de completar o firmar. No se debe bloquear por observaciones de rondas anteriores que ya se corrigieron. Probar ambas rutas: con firma y sin firma.

### A04. Órgano colegiado sin nombre ni firma personal — P0 para esos flujos, incompatibilidad interna

- Fuente: [L251–268](<../decisions/Todas las reuniones.txt#L251>): hay destinos como Consejo Directivo sin firmante individual; el detalle por comisión se entregaría posteriormente.
- Actual: WorkflowModels/WorkflowService permiten COLLEGIATE sin firma y sin assigneeIds. Sin embargo, [ReviewService.java:48](../../backend/src/main/java/ec/edu/uta/utaped/workflow/ReviewService.java#L48) bloquea cualquier etapa sin participantes. Además, T1Template:135 dibuja filas por participante y no representa por sí sola un destino colegiado vacío.
- Falta definir y soportar cómo se registra la decisión colegiada: constancia, actor habilitado para registrar, o destino externo según la política aprobada. No resolverlo inventando una persona firmante ni contando una aprobación automática. La configuración visible hoy no garantiza que ese caso sea ejecutable.

### A05. Cargos institucionales y matriz de firmas — P1, capacidad parcial

- Fuente: [L244](<../decisions/Todas las reuniones.txt#L244>), L615–626; Plan Maestro §24.
- Actual: [WorkflowModels.java:15](../../backend/src/main/java/ec/edu/uta/utaped/workflow/WorkflowModels.java#L15) resuelve PERSON o GROUP_ROLE limitado a MEMBER/COORDINATOR. DocumentWorkflowSnapshot.Participant conserva nombre/estado, no cargo. T1Template:122 y :137 dejan vacía la celda de cargo; T2Template tampoco recibe una asignación de cargo por firmante.
- Falta catálogo/asignación de funciones institucionales por período/carrera, resolver sus personas y congelar cargos en la matriz documental. Etiquetar una etapa «Decano» y elegir manualmente una persona no equivale a disponer de esa relación funcional.

### A06. Borrador descartado visible solo a administración autorizada — P1, diferencia con requisito vigente

- Fuente: [L591–601](<../decisions/Todas las reuniones.txt#L591>); Plan Maestro §15.
- Actual: DocumentLifecycleService archiva sin borrar y permite crear otro borrador, pero [WorkPlanService.java:20](../../backend/src/main/java/ec/edu/uta/utaped/planning/WorkPlanService.java#L20) no restringe el acceso por archived_at; su list tampoco excluye estos descartados. Ser ADMIN no concede por sí mismo lectura documental.
- Falta una política específica para borradores descartados y su bandeja administrativa autorizada. No mezclarla con versiones aprobadas archivadas, que sí deben conservar el historial de su familia. Una corrección no debe abrir todos los documentos privados a todo administrador.

### A07. Coordinador: altas, comisiones y reasignaciones — P1, faltante conocido

- Fuente: [L195–202](<../decisions/Todas las reuniones.txt#L195>), L658–663, L867–874; Plan Maestro §§28 y roles.
- Actual: [SecurityConfig.java:63](../../backend/src/main/java/ec/edu/uta/utaped/identity/SecurityConfig.java#L63) reserva /api/admin a ADMIN; OrganizationService exige ADMIN para membresías/principal. El coordinador dispone de recuperación delegada y prórrogas de evidencia, pero no de alta/importación ni de toda la administración de su comisión. Tampoco hay sustitución completa de un revisor ya congelado tras una baja.
- Falta delegación acotada por ámbito/período con auditoría, catálogo de permisos y reasignación sin reescribir acciones previas. No convertir al coordinador en administrador global. Debe confirmarse quién concede cada permiso y qué pasa con tareas ya activas.

### A08. Plazo de elaboración y reapertura individual — P1, parcial

- Fuente: [L728](<../decisions/Todas las reuniones.txt#L728>): normalmente cinco días para elaborar, bloqueo posterior y habilitación autorizada.
- Actual: [WorkPlanService.java:50](../../backend/src/main/java/ec/edu/uta/utaped/planning/WorkPlanService.java#L50) aplica una ventana de elaboración común configurada por período. Puede configurarse a cinco días; no falta el bloqueo básico. EvidenceService:88 implementa prórrogas por actividad/persona, no por elaboración de un Plan. Las semillas usan todo el semestre, deliberadamente para pruebas.
- Falta excepción individual para elaboración, con motivo, plazo anterior/nuevo y autorización. Confirmar si los cinco días son calendario o laborables, desde qué fecha se cuentan y quién autoriza; no trasladar automáticamente los tres días de evidencia a esta regla.

### A09. Historial de autoridades y límites para otros usuarios — P1, parcial/política por completar

- Fuente: [L876–887](<../decisions/Todas las reuniones.txt#L876>); Plan Maestro §52.
- Actual: no existe un alcance de autoridad Decano/Subdecano para consultar el historial institucional. WorkPlanService.ACCESS también conserva acceso por tareas propias ACTIVE/COMPLETED/RETURNED, sin un filtro explícito de «solo período actual». La transferencia de principal concede acceso a los documentos del ámbito.
- Falta resolver cargos/authority_scope y conciliar continuidad del responsable, historial de quienes actuaron y restricción del cliente por período. No se comprobó una exposición arbitraria: existe autorización por documento/tarea; lo pendiente es si coincide con la política institucional esperada.

### A10. Firma visible con QR y texto — P2, diferencia de presentación; contrato pendiente

- Fuente: [L756–792](<../decisions/Todas las reuniones.txt#L756>): la conversación termina pidiendo texto junto a la representación de firma.
- Actual: [Pkcs12PdfSigner.java:179](../../backend/src/main/java/ec/edu/uta/utaped/signing/Pkcs12PdfSigner.java#L179) genera firma criptográfica y apariencia de texto con nombre/fecha; no dibuja QR. No significa que falte la firma real.
- Falta acordar el contenido/propósito del QR y la apariencia esperada. No generar un QR institucional falso ni afirmar validación oficial por mostrar un cuadrado escaneable. No hace falta almacenar certificados para resolverlo.

### A11. Porcentaje de ejecución del Plan — P2, indicador parcial

- Fuente: [L930–936](<../decisions/Todas las reuniones.txt#L930>).
- Actual: [ReportingService.java:13](../../backend/src/main/java/ec/edu/uta/utaped/planning/ReportingService.java#L13) compara número de actividades por grupo y porcentaje de actividades con evidencia completa. ReportService recoge porcentaje declarado por actividad en el T2. No hay agregado institucional definido de ese porcentaje físico por Plan en el reporte general.
- Falta diferenciar y, si procede, mostrar avance físico del Plan frente a cobertura documental. Confirmar fórmula y ponderaciones; no sustituir uno por otro ni inventar una media como fórmula aprobada. El conteo/comparación de actividades y exportación CSV sí están presentes.

### A12. Unidad académica/administrativa elegible — P2, alcance por precisar

- Fuente: [L51–68](<../decisions/Todas las reuniones.txt#L51>) y L120–128: hay cambios de criterio sobre ubicación y tipo de unidad; se menciona selección desde catálogo.
- Actual: WorkPlanService:130 fija FISEI al crear; WorkPlanEditor:213 muestra unidad en solo lectura para ámbitos. T1Template:83 conserva el rótulo combinado académica/administrativa. Sí existe carrera por ámbito, incluidas varias carreras para la misma persona.
- Falta, si se mantienen unidades administrativas en el alcance, un modelo/catálogo y selección explícita del tipo de unidad y su representación por plantilla. Validar contra el formato oficial vigente y el alcance FISEI; no declarar incorrecto todo el encabezado por una frase aislada anterior.

### A13. Firma requerida por cada persona — P1, configuración y reglas parciales

- Fuente: [L698–701](<../decisions/Todas las reuniones.txt#L698>); Plan Maestro §20, considerando la excepción colegiada.
- Actual: el motor firma revisores cuando requiresSignature=true, pero permite etapas de personas sin firma. Las semillas crean etapas sin firma obligatoria y WorkflowAdministration permite elegirlo libremente.
- Falta traducir la tabla oficial de flujos a políticas obligatorias y controlar excepciones autorizadas. No afirmar que «no hay firmas de revisores»: sí están implementadas. Lo que no está garantizado es la obligatoriedad institucional, además de los coautores de A02.

### A14. Término «Firma y Finalización» — P2, ajuste de interfaz pendiente

- Fuente: [L338–345](<../decisions/Todas las reuniones.txt#L338>) y L534–541; Plan Maestro §10.
- Actual: WorkPlanEditor:139 muestra «Firma y envío». La firma del principal sí inicia revisión atómicamente (AuthorSignatureService:96), por lo que la conducta principal está cubierta.
- Falta armonizar los textos con la denominación acordada sin volver a separar firma/envío ni introducir otro clic obligatorio.

## 2. Contradicciones y decisiones que no se deben aplicar automáticamente

| Tema | Evidencia en reuniones | Tratamiento para UTAPED |
|---|---|---|
| QIPOC/QIPUX, probablemente Quipux | L451–472 propone aprobación por coordinador con constancia externa obligatoria; L476 pide cambiar el flujo. | No está implementada una constancia que bloquee aprobación. Es más específico que una mención casual, pero el Plan Maestro §34 mantiene ese procedimiento pendiente. Confirmar vigencia, casos y actores. No presupone integración API: podría ser carga/registro manual de constancia. |
| Descargar solo el último validador | L16 y L424–428. | L794–802 amplía a usuarios autorizados una vez aprobado; Plan Maestro §34 conserva esa regla general. No revertir a descarga exclusiva del último revisor. El código bloquea PDF final antes de aprobación. |
| Exportar y borrar todo al finalizar | L919–929. | Contradice L876–887 sobre historial de autoridades; el Plan Maestro §53 prohíbe purga automática hasta ratificación. Conservar cierre/archivo actual y acordar retención antes de eliminar. |
| Fecha de elaboración al finalizar o al crear | L75–80 frente a L124 y L246. | Actualmente preparation_date se almacena al crear y se congela en el artefacto. No cambia al validar. Confirmar solo si se pretende distinguir fecha de creación y fecha de finalización; no cambiarla en cada visualización. |
| Tres días de extensión | L753–754 es ruidoso respecto a laborables; L810–813 fija máximo tres. | El código usa máximo configurable de tres días calendario desde vencimiento original y dentro del período. La duración está sustentada; falta ratificar cómputo y extremos. |
| Firma exclusivamente local | L764–766 dice que la firma «nunca sale de aquí». | El producto recibe temporalmente p12/pfx por HTTPS y firma en backend, sin persistir secretos. Confirmar si «local» significaba no almacenar o exigía que nunca abandone el PC. La decisión posterior del propietario autorizó el flujo temporal actual; no sustituirlo silenciosamente por otro. |
| Compartir contraseña para que otro cargue | L848–850, comentario de un escenario. | No es una funcionalidad a implementar ni una autorización técnica delegada. Mantener identidad individual y resolver sustituciones mediante permisos. |
| Crear cuenta genérica para proveedor/correo | L952–970; SMTP corregido a 587 en L1008–1010. | Es tarea de operación: custodio, cuentas y secretos institucionales; no dejar claves en código ni prometer disponibilidad de servicios gratuitos. El puerto web 64430 es distinto de salida SMTP. |

## 3. Decisiones ya cubiertas o con base implementada

| Decisión | Fuente TXT | Evidencia de implementación / límite |
|---|---|---|
| Un Plan por comisión, carrera y período; continuidad por principal | L582–606, L664–673 | Ámbitos y principal en WorkPlanService/OrganizationService; autoría técnica no cambia la pertenencia institucional. No se crea automáticamente un segundo Plan: se selecciona el ámbito de cada carrera. Confirmar si se desea un atajo de copia/creación doble, no compartir un único artefacto entre flujos. |
| Un editor, miembros sin coedición | L605–611 | is_editor/principal y controles del servidor. Coautores firmantes siguen faltando, A02. |
| Contenido antes de matriz | L327–337, L478–480 | Wizard T1: general → contenido → actividades/matriz → anexos → preparación → firma. |
| Justificación precargada y editable/eliminable | L138–146 | Plantilla T1 administrable, copiada solo al crear; sin sustitución silenciosa de blancos guardados. |
| Actividades opcionales, categoría general y agregar otra | L1, L297–320, L484–486 | ActivityService/MatrixEditor; catálogo más actividad libre. No se fuerza POA/plan/acción de mejora en todos los casos. |
| Uno/varios/todos los responsables y representación colectiva | L2–3, L174–212 | IDs individuales y Seleccionar todos; denominación colectiva elegible. El automatismo exacto de mostrarla al seleccionar todos puede afinarse en UX; la capacidad existe. |
| Fuente editable y elaborado por comisión automático | L214–224 | ActivityService devuelve matrix source y groupName; ArtifactService/T1Template los incorporan. |
| Recursos/medios administrables y Otro obligatorio | L230–240 | Catálogos, validación servidor/inline de texto no vacío, conservación de selecciones. |
| Fechas completas y feriados en extremos | L147–172, L735–739 | ActivityService valida rango/período y extremos según configuración; calendario administrable. Configuración institucional debe habilitar la restricción si esa es la regla aplicable. |
| Anexos opcionales y No aplica | L7, L43, L243 | AttachmentService y motores documentales; sin anexos se representa su ausencia. |
| PDF A4, páginas/índice, orden y formato institucional | L7, L269–288, L794 | Motores T1/T2 y navegación por página. La inspección confirma base técnica, no aceptación visual exhaustiva contra Word: falta validación institucional de tipografía, márgenes y notas de cada plantilla. |
| Firma temporal y envío directo, documento bloqueado | L8, L680–691 | AuthorSignatureService y revisión atómica; material privado temporal y artefactos conservados. |
| Secuencia obligatoria y una persona no cubre dos etapas | L636–656 | ReviewService resuelve tareas secuenciales, bloquea duplicidades; instancias congeladas por período. |
| Devolver con observación y nueva ronda, sin nueva versión formal | L638–639, L703–724 | ReviewService retorna DRAFT y aumenta review_round; versiones formales solo desde aprobado. |
| Resaltar con comentario y navegación por página | L354–384, L549–578 | ReviewWorkspace/ReviewService, zonas y observaciones conservadas por ronda. No requiere herramientas complejas de dibujo. |
| Evidencia por cada medio, plazo inclusivo y versión al reemplazar | L730–733, L810–832 | EvidenceService, un medio/archivo vigente, cierre al comenzar el día siguiente en America/Guayaquil, reemplazo versionado. No existe aprobación separada de evidencias. |
| Reutilización de PDF sin prohibición global por hash | L815–824 | No hay rechazo global por hash en EvidenceService. Se exige permiso en cada documento; no se comparte acceso automáticamente entre comisiones. |
| Informe final único derivado, actividades importadas y anexos consecutivos | L835–859 | ReportService exige Plan aprobado, copia matriz, mantiene identidad T2 y anexa PDFs; soporta referencias HTTPS. No crea un T2 automáticamente al cerrar el semestre. |
| Credenciales locales, nombres/apellidos, sin cédula/teléfono y recuperación | L861–915, L1025–1034 | UserAdminController, SecurityConfig, recuperación SMTP y restablecimiento administrativo; username/correo separados. Convención del usuario institucional debe configurarse en altas reales. |
| Auditoría exportable, dashboard y reportes de actividades | L18–27, L930–936 | AuditQueryService exporta CSV, panel y ReportingService; porcentaje físico agregado pendiente A11. |
| PC/tablet, interfaz moderna | L938–950, L1048–1053 | Decisión vigente implementada en UI; no reintroducir diseño móvil ni clonar la estética del mockup. |
| Puerto 64430 y correo | L978–1034 | Override HTTPS/operación y SMTP configurables; apertura real de red pendiente de infraestructura. |
| Sin conservar comparación de textos de redacción | L971–974 | Propuesta explícita y efímera; no hay historial dedicado de diffs de redacción. La auditoría técnica no necesita borrarse por ello. |

## 4. Preguntas concretas para cerrar lo realmente pendiente

1. ¿Sigue vigente la constancia Quipux antes de aprobar? ¿Para qué tipos/grupos, quién la registra, qué PDF/dato debe aportar y qué ocurre si no llega o se devuelve? ¿Se autoriza descargar alguna versión firmada antes de APPROVED para enviarla externamente? Resolver esa posible excepción al bloqueo general.
2. ¿Quiénes figuran como elaboradores por documento y cuáles deben firmar? ¿Hay un orden? ¿Qué ocurre cuando uno no firma o deja la comisión? Mantener siempre un único editor principal.
3. Para Consejo Directivo u otro órgano sin firma individual, ¿quién deja constancia de la decisión, con qué respaldo y permisos, y en qué punto continúa el flujo?
4. Entregar tabla completa grupo/carrera/tipo/período → cargo revisor/aprobador → persona asignada y excepciones de firma. El PDF de unidades no sustituye toda esa tabla.
5. ¿Coordinador puede crear usuarios, comisiones, importar miembros y sustituir revisores? ¿Solo de su carrera o de qué ámbitos? ¿Quién habilita estos permisos y cómo se reemplaza a un actor de una revisión ya iniciada?
6. ¿Cinco días de elaboración desde qué fecha y con qué cómputo? ¿Quién concede extensión individual y hasta qué máximo? Para evidencias, ¿tres días calendario o laborables?
7. ¿Qué historial pueden leer antiguos participantes, responsables sustitutos y Decano/Subdecano? ¿Cómo se accede a borradores descartados? Acordar retención y custodia antes de purgar.
8. ¿Qué significa porcentaje de ejecución del Plan: actividades completas, promedio de porcentajes declarados o ponderación aprobada? ¿Diferenciarlo de cobertura de evidencias?
9. ¿Qué unidades administrativas entran en este producto FISEI y cuál es el encabezado vigente? ¿Qué representa el QR? ¿«Firma local» exige que la clave privada nunca abandone el dispositivo?
10. Infraestructura real: SO/recursos, hostname/TLS, puerto de entrada 64430, salida SMTP al proveedor elegido, custodios de correo/proveedor, límites y respaldo. Las referencias a unos 70 usuarios (L242) no acreditan concurrencia ni capacidad sostenida.

## 5. Orden recomendado para próximas implementaciones

1. Corregir A03 y A04 con pruebas específicas antes de extender el circuito.
2. Completar coautores, cargos/asignaciones y política de firma; mantener snapshots e identidad individual.
3. Entregar estructura versionada de plantillas y armonizar PDF/UX con los formatos y decisiones consolidadas.
4. Completar permisos contextuales, archivo de descartados y excepciones individuales de elaboración.
5. Implementar el circuito externo solo si se ratifica su contrato; después indicadores y ajustes de presentación.
6. Hacer aceptación con el cliente usando escenarios y documentos reales autorizados; no declarar finalizado todo el producto por pasar el circuito actual.

No se ejecutaron pruebas de aplicación en esta auditoría: solo lectura/análisis del TXT, requisitos y código. No se alteró el entorno manual, ni se hizo commit/push/PR de la transcripción o de este análisis. No se restauraron reportes eliminados.
