# Entrega amplia del circuito documental T1 y T2

Fecha: 2026-10-12. Repositorio: UTAPED. Rama: `feature/modern-workspace-access`. La entrega incorpora el circuito de desarrollo desde un Plan por ámbito hasta su revisión, ejecución e Informe derivado. Conserva la modernización de interfaz del propietario y usa permisos, persistencia, archivos y firmas reales. No acredita todavía todo el Plan Maestro ni preparación institucional para producción.

## Fuentes y alcance

Se contrastaron `requirements.md`, `implementation-status.md`, preguntas abiertas, Plan Maestro del 2026-10-06, especificación candidata del 2026-10-11, plantillas institucionales y la referencia funcional de Prototipos. La especificación candidata no sustituye las decisiones vigentes cuando hay contradicciones. No se modificó el mockup ni se restauraron reportes eliminados.

La tanda se organiza en seis capacidades relacionadas: organización, T1/revisión, evidencias, T2, versiones/archivo y seguimiento. La creación de pantallas no se considera por sí sola validación de los procesos. El archivo por documento se entrega; el cierre de período/ZIP y otros pendientes de la planificación siguen fuera de esta entrega y se enumeran abajo.

## Organización y autorización

- Carreras y ámbitos de grupo/carrera/período, integrantes con cargo o función, coordinación y responsable principal.
- Nuevos T1 pertenecen al ámbito; creación y edición corresponden al principal. Un solo T1 activo por ámbito, con protección frente a solicitudes duplicadas y ventanas de elaboración.
- Lectura concedida explícitamente a integrantes iniciales. Un alta posterior no hereda silenciosamente documentos previos. La transferencia concede acceso al nuevo principal, conserva historial y no altera identidad ni firmas existentes.
- ADMIN administra y consulta agregados, sin recibir acceso implícito al contenido privado. Revisión y ejecución conservan sus autorizaciones específicas.
- Control de versiones técnicas y bloqueos transaccionales. Se impide retirar desde la pantalla general una pertenencia todavía activa en ámbitos por período.
- Los documentos individuales previos mantienen sus IDs y permisos. No se borraron datos del entorno principal ni se convirtieron automáticamente al modelo nuevo.

## T1 y revisión secuencial

El editor conserva tarjetas, jerarquía y acciones del propietario. Se agregan preparación, firma/envío, revisión histórica, ejecución e Informe derivado. Un lector no genera artefactos ni modifica contenido mediante una ruta alternativa.

La firma del elaborador conserva el PDF e inicia la primera tarea de revisión en la misma transacción. El artefacto contiene la configuración y los ejecutores que se aplicaron. Se completa cada etapa en secuencia; una tarea futura no puede actuar. Una etapa colegiada sin ejecutor individual explícito bloquea el envío, sin inventar representantes.

Los revisores pueden registrar observaciones generales, por página y mediante resaltado rectangular. Devolver exige observación, conserva los bytes y firmas de la ronda anterior y produce DRAFT con ronda nueva. La versión formal no aumenta por esa devolución. Corregir y reenviar produce otro artefacto y firma.

Las etapas que requieren firma usan .p12/.pfx en memoria y agregan una firma incremental sobre el PDF vigente. Se comprueban huella pública vinculada, vigencia, CMS, integridad de firmas anteriores y preservación de bytes. No se guardan certificados privados ni contraseñas. Cadena emisora, revocación, PAdES institucional y sellado de tiempo no se declaran comprobados. La descarga final requiere aprobación.

## Ejecución, evidencias y plazos

Cada medio de verificación recibe un PDF; reemplazarlo conserva sus versiones. Solo responsables de la actividad pueden cargar dentro de su plazo personal sobre un T1 aprobado y no archivado. Validación de archivo, almacenamiento privado y control de versión evitan reemplazos silenciosos.

La cobertura cuenta medios documentados y actividades con todos sus medios completos. No se transforma automáticamente en avance físico. El coordinador del ámbito puede conceder prórroga a un responsable individual: conserva la fecha original, motivo, autorizador e historial; notifica al destinatario. Configuración técnica inicial: tres días calendario adicionales como máximo, dentro del período. Su cómputo institucional sigue pendiente.

## Informe T2

Un T1 aprobado origina una familia de Informe, con identidad independiente y una sola versión activa. Se trabaja con la matriz del artefacto base aprobado, narrativas, porcentaje manual por actividad, observaciones y delegaciones/contactos cuando correspondan. La evidencia incompleta exige explicación; no se inventan resultados.

El T2 reutiliza el motor de revisión y firmas, conservando su propia historia. Generarlo, firmarlo, devolverlo o actualizarlo no cambia el Plan. Las referencias a evidencias corresponden a sus anexos efectivos.

Se incorpora la plantilla DOCX oficial T2 como recurso de aplicación y se adapta a A4 conservando estructura, gráficos, encabezados, secciones y tablas. Índices, páginas y ubicaciones de firmas se derivan de composición. Se separan los anexos integrados de la hoja de firmas para mantener el orden documental. Los datos de prueba no son resoluciones, autoridades ni documentos institucionales reales.

## Versiones, archivo y seguimiento

Archivar requiere motivo y autorización, conserva datos y PDFs y no actúa durante revisión. Un Plan con Informe activo exige resolver ese Informe primero. Actualizar formalmente un aprobado crea otro ID, incrementa la versión mayor y comienza en DRAFT/ronda 1; la versión aprobada anterior permanece intacta. No copia firmas ni evidencias de ejecución. Un borrador T2 archivado puede reemplazarse conservando el número formal, sin bloquear permanentemente la elaboración ni simular una nueva aprobación.

Las vistas de seguimiento filtran grupo/período, incluyen opcionalmente versiones archivadas y exportan CSV. ADMIN recibe totales institucionales; otros actores, totales autorizados. El CSV protege valores interpretables como fórmulas. Auditoría e historial incorporan las nuevas acciones. Se distinguen estado documental, permiso de edición y condición archivada.

## Interfaz y correcciones integradas

Se preservan los cambios staged del propietario en acceso, administración, directorio y documentos, registrados en un commit separado. Las nuevas vistas usan la misma base visual, tarjetas, métricas, editores por pasos, feedback toast y modo claro/oscuro para PC y tabletas.

La validación detectó y corrigió: respuesta vacía tras firma de revisión; navegación de reportes para usuarios; generación de PDF por lectores; fechas T2 tratadas como preparación T1; formularios administrativos cerrados antes de confirmar guardado; concurrencia inicial de pertenencias/prórrogas; confusión entre estado y permiso; campos de contactos, encabezados largos e índices del T2; numeración de pie superpuesta al código del formato; distribución de anexos respecto a firmas. Se mejoró contraste de métricas y pasos en modo oscuro.

También se corrigió la continuidad tras cambiar contraseña temporal: revoca sesiones anteriores/enlaces y crea una sesión autenticada nueva. El usuario continúa al espacio de trabajo. El cambio voluntario normal mantiene el cierre de sesiones; la recuperación pública no inicia sesión por sí sola.

## Migraciones y configuración

| Migración | Alcance |
|---|---|
| V13 | Carreras, ámbitos, pertenencias, titularidad y acceso explícito. |
| V14 | Rondas, tareas y observaciones de revisión secuencial. |
| V15 | Evidencias PDF y versiones de ejecución. |
| V16 | Identidad y contenido de Informes derivados. |
| V17 | Prórrogas individuales e historial. |
| V18 | Familias de versiones formales y archivo lógico. |

Se mantienen JDBC, sesiones y contratos `/api`; no se afirma una transición completa a JPA/OpenAPI/Testcontainers. Se actualizaron requisitos, estado, permisos, contratos y guías. Detalle: [API del circuito](../api/document-circuit.md).

## Datos de ejemplo y acciones manuales

Desde la raíz:

```powershell
./scripts/start-development-sample.ps1
```

Abre un proyecto separado en http://127.0.0.1:64431, DB `utaped_dev`, siete cuentas ficticias y catálogos de ejemplo. La contraseña se genera en `.env.sample`, ignorado por Git. No restablece usuarios existentes al repetir el arranque. El puerto web principal sigue siendo 64430. [Guía y usuarios](../development/sample-environment.md).

No se requiere publicar claves ni modificar secretos del entorno principal para probar ese escenario. Para usar cuentas reales hay que configurar carreras/ámbitos/principal, publicar el flujo con personas explícitas y vincular sus certificados públicos. El certificado privado y contraseña se aportan únicamente al firmar. El perfil de ejemplo permite HTTP local; eso no configura un despliegue institucional.

El reinicio voluntario del escenario de ejemplo detiene primero la aplicación y verifica el nombre de la base. Solo afecta a sus tablas; no purga almacenamiento ni datos principales. No se ejecutó una limpieza de la base principal.

## Validaciones

- Frontend: `npm run lint` y `npm run build`, correctos; incluye TypeScript.
- Backend/PostgreSQL aislado: 80 casos comprobados, sin fallos en los resultados finales. La corrida completa detectó un marcador de composición T2 afectado por el ajuste de texto; después de corregirlo se reejecutaron los 25 casos documentales/revisión, todos correctos. Los otros 55 habían pasado en la corrida completa. No se ocultó ni deshabilitó el caso fallido.
- Playwright de acceso/administración/tema/flujos: 5 casos correctos, proyecto `utaped-e2e` con puertos distintos de los del propietario.
- Playwright del circuito: 1 escenario completo correcto en `utaped-sample`, con devolución/ronda nueva, firmas sucesivas reales, aprobación, evidencia, T2 con delegaciones, descarga CSV y actualización formal del T2; comprueba permisos de miembro/ADMIN y conservación del Plan.
- Revisión visual de organización, revisión, Informe y reportes, incluyendo tableta oscura. Se ajustaron contraste y capturas antes de verificar la presentación final.
- PDF T2: se revisaron las cinco páginas renderizadas del escenario con delegación y evidencia, incluyendo portada, índices, contactos, anexo y firmas. Pies sin superposición, columnas sin desbordamiento y orden documental correcto. Los casos de paginación larga T1 y firmas incrementales están cubiertos por las pruebas documentales.
- Arranque del script de ejemplo comprobado; sin reiniciar sus datos ni la base principal. `.env.sample`, `.env.e2e`, certificados, logs y builds permanecen ignorados.
- `git diff --check` correcto para los cambios de implementación. Los espacios dobles de Markdown en la especificación del propietario son saltos de línea intencionales y se conservaron.

La revisión de PDF utilizó el motor productivo Java/docx4j/FOP y PDFBox. El renderer DOCX independiente del host no pudo ejecutarse por dependencias ausentes; no se afirma validación independiente en Word/LibreOffice ni aceptación institucional de la plantilla. El comando de reinicio destructivo del escenario de ejemplo no se ejecutó como verificación de esta entrega.

Las pruebas de firma utilizan certificados temporales de pruebas. El CAPTCHA se resuelve mediante fixtures directas de las bases aisladas; no se añadió un bypass a la aplicación. Capturas, logs y PDFs de comprobación permanecen en directorios ignorados.

## Pendientes y siguiente implementación

Esta entrega permite comprobar un circuito integrado de desarrollo. Todavía falta trabajo funcional, además de la operación institucional:

1. Cierre ACTIVE/CLOSED, historia por período y exportación ZIP verificable.
2. Configuración y bloqueo de flujos por ámbito/período, continuidad ante bajas y cambio de responsables.
3. Administración completa de carreras/ámbitos/cargos, historial visible de prórrogas, permisos delegados y recuperación por coordinador.
4. Referencias/repositorios externos como evidencias, anexos no PDF, importaciones y avisos automáticos de vencimiento.
5. Asistencia de redacción y agente de inspección de código/Trello, con proveedor y accesos configurados.
6. Paginación de revisión, alternativa accesible al CAPTCHA visual, más escenarios documentales largos y pruebas de carga.
7. Ratificación de plazos T2, cómputo de prórrogas, múltiples elaboradores y asignaciones oficiales; HTTPS, SMTP, respaldos/restauración y retención.

Siguiente tanda recomendada: cierre de períodos, configuración temporal de flujos y administración pendiente. Después, las integraciones de asistencia/agente y la campaña de aceptación integral. Los pendientes no se consideran resueltos por los datos semilla.

## Control de entrega

Solo commits locales en la rama actual, según instrucción vigente del propietario. Sin push, actualización de pull request ni fusión a develop.

- `d87921c`: modernización de interfaz y documentación que el propietario había dejado en staged.
- `64afeaa`: API, migraciones V13–V18, revisión, firmas sucesivas, evidencias, T2, versiones y entorno de ejemplo.
- `c9f817b`: vistas modernas del circuito y pruebas de navegador, incluyendo comprobación del contraste tras el cambio de tema.
- La documentación de esta entrega se registra en un commit separado con este reporte y su índice.

Los tres commits de interfaz/API corresponden a los cambios verificados arriba. Los archivos privados de entorno, certificados temporales, capturas y resultados de pruebas permanecen fuera del seguimiento de Git. No se modificaron las credenciales ni se vació la base principal del propietario.
