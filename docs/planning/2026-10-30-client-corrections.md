# Plan de correcciones de la reunión del 30 de octubre de 2026

Revisión documental: 31/10/2026. Estado: **implementación técnica verificada; pendientes institucionales separados**.

El objetivo es completar las decisiones de la reunión utilizando la transcripción corregida manualmente por el propietario. Este plan sustituye las interpretaciones del análisis preliminar basado en el reconocimiento automático; no sustituye el historial de decisiones. El apartado 9 y el reporte de implementación registran las entregas verificadas.

## 1. Fuentes y precedencia

1. Transcripción corregida: `docs/decisions/Todas las reuniones.txt`, L1060–1799. SHA-256 de la revisión analizada: `65ec9f5f101be840557173fdaa1da7156f23cad850962837046fe45ec362b1f6`.
2. Fuentes oficiales en `docs/templates`, especialmente `Actividades.docx`, `Matriz_POA_2026.xlsx`, `PLAN DE MEJORAS - CARRERAS.docx`, propuestas de mejora y formatos T1/T2/T3.
3. Imagen de firma proporcionada por el propietario, para composición visual.
4. Requisitos y decisiones anteriores, cuando no contradigan expresamente esta reunión.
5. Código y pruebas existentes, como evidencia de implementación; no como fuente de nuevas reglas institucionales.

El audio y el video conservan valor como respaldo. No se utilizará una retranscripción automática para desautorizar las correcciones manuales del propietario. Las referencias de línea de este plan pertenecen exclusivamente a la versión corregida identificada arriba.

## 2. Precisiones que cambian el análisis preliminar

- **Todos los participantes personales de elaboración, revisión y aprobación deben firmar.** Debe funcionar con más de un revisor o aprobador (L1496–1505). El mecanismo criptográfico ya existe, pero una configuración que permita finalizar una etapa personal sin firma incumpliría esta decisión.
- **La nota de la matriz debe aparecer siempre y ser editable**, no solamente imprimirse automáticamente con un texto fijo (L1348–1363).
- **El autoguardado es una petición expresa**, orientada a conservar documentos ante interrupciones, no una mera suposición sobre lo ya implementado (L1672–1676).
- **El restablecimiento administrativo debe enviar la contraseña temporal por correo**, manteniendo el cambio obligatorio al entrar (L1612–1625). El correo de recuperación por enlace no satisface por sí solo este caso.
- El mínimo de una selección se confirma en el contexto de las **acciones de mejora de la comisión** (L1200–1206). No se generaliza a todas las categorías ni se marcan todas sus actividades como obligatorias.
- El cliente acepta investigar FirmaEC; no establece que una API externa sea necesaria para leer las fechas del certificado (L1780–1796). La explicación sobre las limitaciones del certificado de prueba no debe convertirse en una limitación del producto.
- Los cambios de proveedor de redacción y la ampliación indiscriminada de tipos de archivo son ofrecimientos del desarrollador. El compromiso verificable es asistencia de redacción, anexos PDF/imagen/URL y posibilidad de tablas; no «cualquier proveedor gratuito» ni «cualquier archivo».

## 3. Inventario y aplicación de los datos oficiales

| Fuente | Contenido comprobado | Tratamiento |
|---|---|---|
| Matriz POA 2026 | 14 actividades, indicador del objetivo estratégico, estrategia, indicador de actividad y cumplimiento trimestral. | Catálogo POA por período y procedencia. No importar presupuesto ni inventar un objetivo distinto del que proporciona la fuente. |
| Plan de Mejoras Carreras | 67 actividades en 24 indicadores, con objetivos institucionales y criterios. | Conservar jerarquía y procedencia. Números de actividad como referencia interna; el cliente no exige mostrarlos en el selector. |
| Actividades | Aplicabilidad POA y relación comisión–indicadores. | Reglas administrables y verificadas de elegibilidad, no solo etiquetas visuales. |
| Propuestas TI | Situaciones y acciones de mejora por comisión. | Conservar origen, comisión/carrera y período. El archivo con `(1)` es idéntico por SHA-256 al que ya existía: no duplicar la importación. |

Mapeo explícito del documento Actividades: diseño/ajustes 1–7; seguimiento al desempeño 11; UOC/clubes/aulas virtuales 12; eventos 14; titulación 15; graduados 16; vinculación 17 y 21; investigación 18; prácticas 23; redes/laboratorios 29 y 30. POA identifica Titulación, Vinculación y Seguimiento a Graduados. Las equivalencias de nombres y los indicadores no asignados requieren revisión administrativa; no repartirlos por intuición.

Textos iguales en indicadores diferentes son registros distintos. Una clave estable debe distinguir fuente, período, jerarquía e identidad de la actividad, no solo su título. No convertir las actividades descritas —por ejemplo, construir un ServiceDesk— en módulos adicionales obligatorios de UTAPED.

## 4. Registro trazable del trabajo

Actualización de implementación: ver [entrega y verificaciones](../reports/2026-10-31-55-client-corrections-implementation.md). El texto de situación de cada fila conserva el diagnóstico de partida; los estados actuales están en el apartado 9. La aceptación institucional sigue separada de las pruebas técnicas. P0 ordena dependencias funcionales; P1 completa el circuito y la experiencia; P2 corresponde a administración, aclaraciones o integraciones investigables.

| ID | Prioridad | Decisión o tarea | Evidencia corregida | Situación y alcance |
|---|---|---|---|---|
| OCT30-01 | P0 | Catálogos con su jerarquía oficial | L1188–1249 | Categorías ya existentes; faltan objetivo/estrategia o criterio/indicador y procedencia estructurada. |
| OCT30-02 | P0 | Elegibilidad por comisión/carrera/período | L1273–1285; Actividades | Existe catálogo por grupo. Completar relaciones y filtros de servidor y UI. |
| OCT30-03 | P0 | Mínimo de una acción de mejora aplicable | L1200–1206 | Distinguir mínimo de categoría y actividades individualmente obligatorias. |
| OCT30-04 | P1 | Propuestas del Informe hacia acciones futuras | L1179–1182 | Añadir promoción explícita, trazable y revisable; no convertir texto libre en obligación global. |
| OCT30-05 | P0 | Capacidades de elaboración/revisión/aprobación por contexto | L1478–1540 | Ya hay restricción al principal y prohibición de autorrevisión; completar administración y presentación contextual. |
| OCT30-06 | P0 | Firma de todos los participantes personales | L1496–1505 | Reutilizar firma real y tareas múltiples; impedir finalización personal sin firma y probar cada participante. |
| OCT30-07 | P0 | T2 vinculado al Plan y a cortes de ejecución | L1553–1566 | T2 solo referencia directamente al Plan. Añadir relación explícita con T3 y requisitos de existencia. |
| OCT30-08 | P1 | Nota siempre presente y editable | L1348–1363 | Actualmente es un booleano opcional y texto fijo en T1. Convertir a contenido documental. |
| OCT30-09 | P1 | Anexos PDF, imagen, URL y tablas | L1300–1343 | PDF ya se inserta; ampliar sin confundir anexo con evidencia de actividad. Definir el mecanismo de tablas. |
| OCT30-10 | P1 | Sello de firma conforme a la imagen | L1389–1410 | Ya hay QR/nombre/fecha; ajustar leyenda y composición sin simular validaciones externas. |
| OCT30-11 | P2 | Vigencia y estudio de FirmaEC | L1780–1796 | Vigencia/uso de clave ya comprobados; investigar confianza/revocación/contrato e interoperabilidad. |
| OCT30-12 | P1 | Terminología y navegación comprensibles | L1427–1437, L1471–1475, L1508, L1587–1596, L1648–1693 | Sustituir textos ambiguos; diferenciar revisar/aprobar y explicar el contexto institucional con nombres útiles. |
| OCT30-13 | P1 | Búsqueda dinámica inmediata en todos los buscadores | L1580–1584 y aclaración expresa del propietario | Todos los buscadores de todas las interfaces deben actualizar la búsqueda con cada letra/cambio del campo, sin Enter ni clic en Buscar. No limitarlo a módulos seleccionados. |
| OCT30-14 | P1 | Período actual predeterminado | L1599–1605 | Primera entrada en listado con actual; mantener elección histórica explícita. |
| OCT30-15 | P1 | Correo tras restablecimiento administrativo | L1612–1625 | Restablecimiento y cambio obligatorio existen; su ruta actual no envía esa contraseña por correo. |
| OCT30-16 | P1 | Autoguardado real | L1672–1676 | No existe autoguardado periódico general; la previsualización no equivale a persistencia del borrador. |
| OCT30-17 | P1 | Carreras vigentes | L1681–1687 | Software, TI, Telecomunicaciones, Industrial, Automatización y Robótica. Desactivar la antigua para altas; conservar historia. |
| OCT30-18 | P1 | Comisiones específicas de cada carrera | L1695–1705 | Relación ya existente en organizational_scope. Completar datos y presentación, sin reconstruirla desde cero. |
| OCT30-19 | P2 | Transferencia de estructuras JSON | L1712–1749 | Ya hay edición/publicación de secciones; añadir importación/exportación validada y versionada. |
| OCT30-20 | P2 | Consulta asistida de feriados | L1752–1759 | Manual ya disponible. Añadir propuestas con fuente y confirmación; no aceptar fechas de IA como normativa. |
| OCT30-21 | P1 | Desglose de indicadores de reportes | L1767–1773 | Ya existen datos de próximas actividades; comprobar cobertura completa, límites y permisos. No dar por entregado «ver todo» por mostrar seis filas. |
| OCT30-22 | P2 | Sesión de 30 minutos | L1664–1670 | Configuración existente; comprobar comportamiento real y pérdida de borrador. Un reloj permanente no es requisito confirmado. |
| OCT30-23 | P2 | Política de contraseñas coherente | L1628–1642 | Valores expuestos con dudas; backend actual 12 caracteres/72 bytes. Aclarar antes de modificar la política. |
| OCT30-24 | P1 | Estilos administrativos de grupos | Evidencia de código/video del análisis previo | administration.css existe sin importación localizada. Confirmar y corregir el recurso; no atribuirlo a datos vacíos. |
| OCT30-25 | P2 | Alcance real de asistencia de redacción | L1119–1155 | Mantener Groq/OpenAI implementados; Gemini no se deduce como obligación aprobada de una oferta del desarrollador. |
| OCT30-26 | P0 | Matriz institucional de quién valida a quién | L1068–1074, L1258–1261, L1763–1766 | Sigue pendiente como fuente oficial. No deducir autoridades de listas de integrantes o firmas de un ejemplo. |

## 5. Diseño de las tandas

### Tanda 0 — Contratos y preparación

Antes de programar: preservar cambios del propietario, trabajar en rama de implementación local y concretar contratos/migraciones. No reiniciar el entorno manual para analizar documentación. Mantener una matriz de escenarios y datos de referencia independiente de cuentas y credenciales reales.

El frontend recibirá capacidades explícitas y DTO de catálogos/selecciones/estados de guardado. Los nombres de rutas o tablas nuevos se definirán en su contrato técnico antes de usarse; este plan no afirma que ya existan endpoints adicionales.

### Tanda 1 — Catálogos y selección de actividades

Backend:

- Modelar las relaciones oficiales por fuente, período y comisión/carrera. POA y Plan de Mejoras no tienen exactamente la misma jerarquía.
- Preparar importación idempotente con vista previa, validación de duplicados/equivalencias y procedencia de fila/tabla. Administración puede mantener catálogos sin editar Word/Excel a mano en cada despliegue.
- Filtrar definiciones en servidor. Rechazar IDs de otra comisión, carrera o período; conservar snapshots de actividades ya incorporadas.
- Aplicar mínimo de una acción de mejora cuando existan opciones aplicables. Si el grupo no tiene opciones, no inventar una ni impedir planificar por ese motivo; registrar el caso para revisión administrativa.
- Conservar obligaciones específicas ya configuradas. No extender el mínimo a POA/Plan de Mejoras sin evidencia.
- Dar seguimiento a propuestas procedentes de Informes, con habilitación administrativa para futuras selecciones; evitar duplicados y cambios retrospectivos.

Frontend:

- Elegir origen (POA, Plan de Mejoras, Acciones de mejora, Otras). Mostrar únicamente lo aplicable al contexto elegido.
- Mostrar los niveles pertinentes y luego la lista de actividades. Permitir buscar, seleccionar varias y revisar cuántas se incorporarán; no imponer un combo interminable ni mostrar códigos de fila sin valor para el docente.
- Configurar fechas, responsables, recursos y medios como hoy. Preservar denominación colectiva y trazabilidad individual.
- Errores inline con selección conservada; explicar el mínimo aplicable sin obligar a elegir todo.

Cierre: importar sin duplicar las 14 actividades POA y 67 del Plan de Mejoras; comprobar el mapeo oficial y una comisión sin catálogo. Probar la regla mínima por API además de UI, datos inactivos, valores «Otro» y preservación de matrices antiguas.

### Tanda 2 — Participantes y firmas

Backend:

- Resolver capacidades por asignación, período, documento y etapa; conservar USER/ADMIN como permisos generales, sin convertir revisor o aprobador en una prohibición global para toda su cuenta.
- Prohibir que la misma persona elabore y revise/apruebe el mismo documento, o que revise y apruebe ese documento. Mantener la excepción legítima de otra comisión/documento.
- Exigir firma en cada intervención personal nueva. No permitir un bypass mediante COMPLETE directo cuando corresponde firma.
- Reutilizar las listas de participantes, tareas y signatureSlots actuales. Una etapa con dos revisores/aprobadores no se completa con una sola firma. Conservar el orden configurado; no imponer paralelismo como nueva regla institucional.
- Distinguir autores formales de responsables de actividades: no convertir automáticamente a todos los responsables de la matriz en firmantes de elaboración.
- Publicar una nueva revisión del flujo cuando cambie la regla. No reescribir flujos congelados, decisiones concluidas ni PDFs firmados.
- Mantener aislada la decisión anterior sobre registradores de órganos colegiados sin firma personal; confirmar su tratamiento con la matriz institucional antes de cerrar el despliegue. No usar esa excepción para omitir firmas de etapas personales.

Frontend:

- Mostrar creación solo para contextos de elaboración disponibles. Separar «Documentos por revisar» y «Documentos por aprobar», reutilizando el inspector del artefacto firmado.
- Tras la intervención, mostrar «Revisión finalizada» y el siguiente responsable/estado, sin presentar la siguiente aprobación como una acción propia.
- Mostrar certificado, confirmación y firma para cada participante correspondiente. Las etiquetas deben decir revisar/aprobar, no solo «Completar etapa».
- Administración configura participantes y funciones con alcance visible; «Comisiones y unidades» o el término institucional correspondiente sustituye «Grupos» cuando ese sea el significado. «Asignación de comisión, carrera y período» debe explicar el ámbito sin añadir otra entidad equivalente.

Cierre: un elaborador, dos revisores y dos aprobadores firman; cuatro de cinco firmas no habilitan la aprobación final. Verificar turnos, duplicados, cuentas inactivas, devolución, sustitución, llamadas directas y usuario con dos comisiones. La matriz oficial pendiente no impide probar el mecanismo con asignaciones ficticias identificadas como tales.

### Tanda 3 — Informes vinculados

- Mantener Plan aprobado como requisito para un Informe de ejecución.
- Añadir referencias explícitas del T2 a uno o varios Informes de ejecución del mismo Plan, con artefacto/versión/estado de origen conservados.
- Sin Plan aprobado o sin Informe de ejecución elegible, no crear/finalizar el T2 por UI ni API. Mostrar qué falta y el acceso autorizado para resolverlo.
- Para datos históricos, marcar los T2 anteriores sin esa relación como casos a reconciliar; no crear cortes ficticios ni modificar artefactos firmados.
- Confirmar qué estados de T3 son elegibles y qué cortes se consideran antes de cerrar el contrato. Propuesta técnica: utilizar cortes aprobados; **no es todavía una regla institucional ratificada**.
- No sumar porcentajes de distintos cortes ni copiar el último como resultado final por defecto. Conservar la declaración final del elaborador y su relación con los cortes, hasta disponer de una regla de agregación confirmada.
- La UI presenta Plan, cortes disponibles/seleccionados y procedencia del resultado, sin mezclar versión formal, ronda o versión de evidencia.

Cierre: rechazar cortes ajenos/no elegibles, dobles referencias, Plan incompatible y solicitudes concurrentes; comprobar independencia de IDs, firmas, evidencias, datos e historial de Plan/T3/T2.

### Tanda 4 — Documento institucional y anexos

- Separar nota de matriz del indicador de anexos. La nota T1 debe estar presente aunque no haya anexos.
- Administración mantiene texto base; propuesta de UI: el elaborador puede editar la nota de su borrador. Guardarla y congelarla en el snapshot; rechazar texto vacío para el formato que la exige. No imprimir las instrucciones entre paréntesis del formulario oficial como contenido del documento.
- No aplicar automáticamente esta nota a todas las plantillas: T2 tiene instrucciones y párrafos propios. Revisar cada formato antes de extender la regla.
- PDF: continuar insertando sus páginas, no reemplazarlo por un enlace. Imagen: aceptar formatos concretos acordados (propuesta PNG/JPEG), validar contenido, tamaño/orientación y componer páginas legibles sin deformar.
- URL: anexo con título y enlace seguro, visible/clicable en el PDF. No incrustar contenido remoto ni afirmar que el archivo externo está custodiado por UTAPED.
- Tablas: concretar un mecanismo antes de implementar. Propuesta inicial: tabla editable limitada dentro del anexo; importación de Excel/Word no queda comprometida por la palabra «tabla».
- Audio: la conversación contiene una posibilidad, no aprobación definitiva de almacenamiento/transcodificación. Mantenerlo como referencia URL si se confirma su uso; no abrir una carga arbitraria de archivos.
- Preservar orden, títulos, límites, numeración, índices, páginas, posiciones de firma y bytes firmados. Límites nuevos son técnicos y documentados, no reglas institucionales inventadas.
- Ajustar sello a la imagen entregada. Hay una diferencia literal: TXT «Validado únicamente por firmaEC» frente a imagen «Validar únicamente con FirmaEC». Usar la imagen como referencia visual y resolver la cadena exacta en la revisión; no presentar un estado de validación automática que no ocurrió. Conservar fecha real en metadatos/firma aunque cambie su línea visible.

Cierre: composición y navegación con cada tipo de anexo, texto largo de nota, sin anexos, tablas extensas y varias firmas. Comparación visual con fuentes oficiales en A4 y regresión de PDFs anteriores.

### Tanda 5 — Guardado, acceso y experiencia cotidiana

- **Autoguardado:** solo borradores editables y por targetDocId. Cola de escritura por documento, espera breve configurable, versión de concurrencia y respuesta asociada a la revisión local enviada. Una respuesta tardía no debe borrar edición más reciente.
- Mostrar estados discretos de guardando/guardado/error/conflicto. Reintentar fallos transitorios sin sobrescribir cambios ajenos; un 409 exige reconciliación, no repetición ciega.
- Cubrir contenido, matriz, notas y secciones de T1/T2/T3; anexos/firma siguen operaciones explícitas. Los borradores pueden estar incompletos y la validación final sigue bloqueando firma. Esperar el guardado antes de preparar/firmar.
- Mantener el bloqueo de firma durante cambios pendientes y recuperación ante sesión vencida. No almacenar contraseñas, certificados ni información documental sensible indiscriminadamente en localStorage.
- **Correo administrativo:** conservar cambio obligatorio y revocación de sesiones/tokens. Enviar después de confirmar el restablecimiento; no guardar ni registrar contraseña temporal en texto plano. Validar entrega a Mailpit y fallos de SMTP; no anunciar «recibido» cuando solo fue aceptado para envío. Definir manejo de fallos y recuperación administrativa sin exponer secretos en auditoría.
- **Sesión:** conservar 30 minutos. Comprobar el comportamiento servidor/cliente; un aviso preventivo y continuar sesión son mejoras de UX propuestas, no un reloj permanente exigido por el cliente.
- **Buscadores:** inventariar todos los campos de búsqueda de todas las interfaces, incluidos modales, administración, Documentos, Revisiones/Aprobaciones, usuarios y selectores de Plan/catálogos. Cada letra o cambio —incluidos borrar, pegar y vaciar el campo— debe actualizar inmediatamente la búsqueda. No esperar una pausa de escritura, una longitud mínima, Enter ni un clic en Buscar. Cuando los datos sean locales, filtrar de inmediato; cuando la consulta dependa del servidor, iniciarla en cada cambio, cancelar/descartar solicitudes obsoletas y conservar paginación/filtros y foco. La latencia de red se representa con un estado de carga, sin añadir una espera intencional ni mostrar como vigentes resultados de un texto anterior. La optimización no puede introducir excepciones al comportamiento solicitado.
- **Períodos:** seleccionar actual en primera entrada; no sobrescribir una selección histórica o enlace/filtro explícito. Si no existe actual, mostrar un estado claro, no elegir arbitrariamente el más reciente.
- **Terminología/estilos:** corregir importación administrativa, nombres de acciones, filtros y asignaciones; revisar PC/tablet y ambos temas. No inventar nuevos menús que dupliquen funciones existentes.

Cierre: recuperar datos guardados tras recarga y fallo de red; dos pestañas concurrentes; edición rápida; cambio de documento; 401/409; retorno de navegación; cero falsos «guardado». Verificar correo sin registrar secretos, búsquedas cruzadas y vista de grupos con/sin datos.

### Tanda 6 — Administración y detalle de reportes

- Actualizar carreras vigentes y desactivar la carrera antigua para nuevas altas, preservando historia. Resolver comisiones por carrera/período usando organizational_scope existente.
- JSON: exportar un contrato versionado de la configuración admitida; importar con validación, vista previa de diferencias y publicación explícita. Proteger encabezados, tablas, firmas y secciones institucionales necesarias. «Cualquier cosa» dicho en una demostración no autoriza romper el formato oficial ni eliminar bloques críticos.
- Feriados: consulta asistida por localidad/año con fuentes y revisión humana. No sobrescribir fechas manuales ni guardar sugerencias de IA automáticamente.
- Reportes: comprobar detalle de próximas a vencer y demás indicadores con filtros, cobertura y permisos coherentes. Actualmente upcoming tiene LIMIT 6: mostrar que es un resumen y permitir consultar el conjunto autorizado cuando haya más registros. Totales institucionales no conceden acceso implícito al contenido privado.
- Aclarar política de contraseñas. Mantener provisionalmente 12 caracteres y máximo 72 bytes UTF-8; la explicación dubitativa de 8/54 no confirma que deba reducirse la política. Formularios y mensajes deben reflejar la regla efectiva.
- Mantener asistencia de redacción disponible para justificación y objetivo. No prometer Gemini, LLM como proveedor concreto o gratuidad ilimitada; incorporarlos requeriría una decisión y un contrato propios.
- Aclarar el propósito de Certificados administrativos frente a inspección del certificado de firma; no reinstaurar vinculación manual obligatoria como condición para firmar.

### Tanda 7 — Validación e interoperabilidad de firma

- Verificar mediante regresión que Pkcs12PdfSigner.inspect comprueba contraseña/clave, notBefore/notAfter y uso permitido. Los certificados autofirmados de prueba también contienen fechas; no atribuir a su condición de prueba la ausencia de esos campos.
- Separar estas comprobaciones de confianza de emisor, revocación e interoperabilidad con FirmaEC. Investigar servicios oficiales/disponibilidad en la infraestructura institucional y formalizar contrato antes de integrar.
- No enviar claves privadas o archivos .p12 a un servicio remoto por deducción. No asumir API pública gratuita ni credenciales disponibles. Si la validación oficial pasa a ser requisito obligatorio, definir fallo cerrado y respuesta frente a indisponibilidad de red.
- Comprobar documentos con firmas sucesivas y sustituciones en un validador acordado, usando certificados autorizados para la aceptación. Una imagen QR y pruebas con certificados sintéticos no equivalen a esa aceptación.
- Conservar la separación usuario ejecutor/titular del certificado y la documentación vigente de confianza; no inventar correspondencias de identidad ni copiar firmas de las referencias oficiales.

## 6. Decisiones pendientes acotadas

| Decisión | Qué se puede avanzar | Qué no debe inventarse |
|---|---|---|
| Matriz oficial de participantes y tratamiento colegiado | Motor y UI multiactor con firmas personales y escenarios de prueba | Autoridades/asignaciones oficiales o supresión de excepciones colegiadas históricas |
| Estados/cortes T3 elegibles para T2 y agregación | Relación explícita, procedencia y bloqueo si no existen cortes | Aprobación de T3 obligatoria, promedio/suma de cortes o número fijo universal |
| Indicadores/comisiones sin correspondencia expresa | Importar jerarquía y mapeos suministrados, marcar no asignados | Asignar todas las actividades a todas las carreras |
| Tabla y audio como anexos | PDF, imagen y URL; prototipo de tabla limitada como propuesta | Importación de todos los Office, subida de audio o transcodificación obligatorias |
| Texto literal del sello | Ajustar composición, mantener metadatos reales | Afirmar «validado» por FirmaEC sin comprobación |
| Política de contraseñas | Alinear explicaciones con política actual | Reducir longitud/composición por una respuesta expresamente insegura del presentador |
| Contrato FirmaEC y fuente de feriados | Investigación técnica y configuraciones revisables | Inventar proveedor, credenciales, calendario o disponibilidad del servicio |

No se vuelve a dejar pendiente lo ya explícito: firma de todos los participantes personales, nota presente/editable, mínimo de una acción de mejora aplicable, correo del restablecimiento, autoguardado, búsquedas al escribir, período actual y vínculo T2 con Plan/cortes.

## 7. Verificación y aceptación

Las tareas se ejecutarán por tandas revisables. Actualizar contratos, checklist y estado únicamente con evidencia de entrega. Preservar pruebas existentes; ejecutar lint, TypeScript/build, integraciones PostgreSQL aisladas y recorridos Playwright adecuados a cada cambio. Nunca usar utaped-sample como entorno automático de pruebas.

Escenarios mínimos del cierre:

- [ ] Importación oficial idempotente; POA/Plan de Mejoras/propuestas sin mezcla ni duplicados; reglas por comisión/carrera.
- [ ] Mínimo de una acción de mejora y caso sin opciones; validación también por API.
- [ ] Usuario que elabora en A y revisa en B; prohibición de autorrevisión/aprobación y revisión+aprobación del mismo documento.
- [ ] Elaboración + dos revisores + dos aprobadores, todas sus firmas; ninguna vía personal sin firma; devolución y nueva ronda.
- [ ] T2 sin cortes rechazado; T2 con Plan/cortes elegibles; referencias históricas conservadas e independientes.
- [ ] Nota siempre presente/editable, PDF/imagen/URL/tabla y variación de páginas e índices; artefactos firmados intactos.
- [ ] Autoguardado con fallos, concurrencia y cambio de documento; firma solo después de guardar contenido actual.
- [ ] Restablecimiento por ADMIN, correo temporal, cambio obligatorio, revocación y fallo de correo sin mensajes falsos.
- [ ] Inventario completo de buscadores de todas las interfaces. En cada uno, primera letra, escritura continua, borrado, pegado y campo vacío actualizan inmediatamente la búsqueda sin pausa, Enter ni Buscar; una respuesta antigua nunca reemplaza la consulta actual. Período actual/histórico, terminología, estilos PC/tablet y temas.
- [ ] JSON válido/inválido, conservación de versiones; feriados revisados; detalle completo de reportes según autorización.
- [ ] Inspección de certificados válidos, caducados, aún no vigentes, contraseña incorrecta y usos incompatibles; validación institucional cuando exista contrato.
- [ ] Nueva demostración con Plan → ejecución → Informe final, múltiples firmas y los datos actualizados. El video anterior no acredita este nuevo alcance.

Este plan es la base de la siguiente implementación, no autorización para publicar, desplegar, enviar correos reales ni modificar documentos firmados.


## 9. Avance de implementación — 31/10/2026

Decisiones posteriores del propietario: T2 selecciona exclusivamente cortes T3 **aprobados del mismo Plan**. Si la matriz de participantes no aparece en Templates, aún no se ha recibido: no inferir autoridades de los ejemplos.

| Identificadores | Entrega actual | Límite o validación pendiente |
|---|---|---|
| 01–03 | 128 referencias oficiales con procedencia, jerarquía, aplicabilidad, importación administrativa idempotente y selección múltiple; mínimo de mejora al preparar/avanzar. | Administración debe revisar equivalencias no explícitas e incorporar las selecciones al catálogo de cada contexto. No importar todo indiscriminadamente. |
| 04 | Propuesta explícita desde T2 aprobado; Administración acepta/rechaza para un período futuro, sin alterar el Informe firmado. | No equivale a aprobación automática de una obligación institucional. |
| 05–06 | Creación según capacidades; bandejas de revisar/aprobar y etapas personales con firma obligatoria. Ensayo de dos revisores y dos aprobadores. | Matriz oficial pendiente; no reasignar retrospectivamente tareas de artefactos firmados. |
| 07 | Referencias inmutables a cortes aprobados del mismo Plan, validadas en servidor y congeladas en T2. | No sumar automáticamente porcentajes de distintos cortes: fórmula no acordada. |
| 08–10 | Nota de matriz editable, anexos PDF/PNG/JPEG/URL/tablas y sello con QR/nombre/leyenda de referencia. Original de imagen conservado por separado; custodia incluye ambos. | QR no certifica confianza/revocación. Anexos y evidencias siguen siendo entidades distintas. |
| 11 | Vigencia y clave se comprueban en la firma existente; composición visual ajustada. | Aceptación con certificados institucionales/FirmaEC, confianza, revocación y tiempo confiable pendientes de contrato. |
| 12–15 | Textos contextuales, búsquedas por cada cambio con descarte de respuestas obsoletas, período actual si es único y correo tras restablecimiento administrativo. | Entrega SMTP real depende del proveedor/configuración. Un envío aceptado por SMTP no prueba llegada a la bandeja. |
| 16 | Autoguardado de T1/matriz/T2/T3 tras 1,2 s, conserva escritura durante petición y errores recuperables. | No guardar formularios inválidos ni certificados. Filas incompletas de matriz continúan pendientes; no prometer recuperación tras cerrar sin guardar esas filas. |
| 17–18 | Cinco carreras vigentes, Sistemas histórica inactiva; elegibilidad conserva grupo/carrera/período. | No inventar comisiones/asignaciones ausentes en las fuentes recibidas. |
| 19–21 | Transferencia JSON validada/versionada, propuestas de feriados revisadas antes de aplicar, detalle paginado de próximas/vencidas/evidencias pendientes con permisos. | IA no es fuente normativa; sin proveedor configurado sigue disponible el calendario manual. |
| 22 | Sesión de 30 minutos con marcador de actividad funcional; el sondeo de notificaciones no lo renueva. Prueba de renovación/expiración/invalidez posterior aprobada. | No es un detector de teclado/ratón: peticiones funcionales y autoguardados cuentan como actividad. |
| 23 | Política existente preservada. | Confirmación institucional pendiente; no reducirla a partir de una cifra dudosa de transcripción. |
| 24–25 | Hoja administrativa cargada y asistencia Groq/OpenAI conservada. | Gemini no se incorpora como requisito no confirmado. |
| 26 | Configuración y ejemplos separados de asignaciones oficiales. | Bloqueado institucionalmente por ausencia de matriz, según respuesta expresa del propietario. |

Los ensayos se realizan en entornos desechables, no sobre los datos manuales. Informe de pruebas y límites: [reporte de implementación](../reports/2026-10-31-55-client-corrections-implementation.md).
