# Hallazgos de auditoría integral

Base: 06c3b53; rama fix/integral-product-audit; inicio 2026-10-27. Los casos se registran con evidencia nueva, no por el recuento histórico.

## IA-01 Recuperación del detalle de revisión — P2

Fuente: NFR-UX-001, plan fase H. Revisor abre una tarea cuya consulta GET /reviews/{id} devuelve 503. Se esperaba error y reintento sin acciones; se observó Toast junto a «Cargando revisión…» permanente y sin recuperación propia. Causa: el componente solo distinguía state nulo/presente y no el fallo de carga. Afecta Inbox, Documentos e Historial porque comparten ReviewInspector.

Corregido: carga/fallo explícitos, estado remoto descartado ante error, reintento y acciones suspendidas durante consulta. Se conserva el mensaje local pendiente. Regresión: integral-audit.spec.ts, review detail failures. Fallo reproducido en integral-baseline-acceptance.log; repetición dirigida integral-recovery-regression.log: aprobada.

## IA-02 Estado de trámite no conocido — P2

Fuente: UT-QPX-130, NFR-UX-001. Al fallar GET formal-submission, el componente anunciaba «Documento aprobado, pendiente de trámite externo» incluso sin conocer estado/requisito. Causa: branch de error compartido con trámite pendiente. Afecta preparación T1/T2/T3 y ejecución.

Corregido: error de consulta independiente, sin inferir aprobación ni requisito, con reintento; los errores al guardar conservan formulario y estado conocido. Regresión an unavailable institutional submission: falla antes y pasa después. Evidencias: integral-submission-reproduction.log e integral-recovery-regression.log. La primera versión de la prueba usaba tamaño de página inválido (100 frente a máximo 50); se corrigió la fixture para crear su propio documento. Ese fallo de fixture no se atribuye al producto.

## IA-03 Contratos contradictorios — P2 documental

Fuentes: decisiones 2026-10-17/27. document-authorship-and-review aún exigía certificado vinculado y la guía de aceptación descartaba múltiples elaboradores. El contrato inicial de preparación indicaba módulos hoy entregados como pendientes. Reconciliados con las decisiones actuales, conservando identificadas las entregas históricas y enlaces a los contratos vigentes.

## Hipótesis descartadas

- Bajo contraste de botones secundarios: la medición real del control habilitado y las capturas claro/oscuro no reprodujeron la sospecha inicial. No se cambió la paleta por esa hipótesis.
- Diferente tamaño de hoja en el original: fuentes mezclan A4/Carta; la salida A4 es una excepción expresamente autorizada, no un defecto.

## IA-04 Plazos e integrantes sin recuperación — P2

Fuente: NFR-UX-001, administración contextual. El 503 de plazo dejaba carga indefinida y el de miembros no ofrecía recuperación; fallos de permiso técnico se confundían con ausencia de capacidad de gestión. Corregidos con error local, reintento y distinción del 403 legítimo. Se mantienen los datos pendientes del plazo cuando existe información previa. Reproducción roja: integral-scope-reproduction.log. Regresión verde: integral-last-regression.log y aceptación global.

## IA-05 Recorrido E2E de certificado obsoleto — P2 de cobertura

La suite E2E esperaba «Vinculación de certificados» y vencía tras 180 s. La decisión vigente eliminó esa alta manual. Se actualizó el recorrido para verificar el historial y la ausencia del botón de vinculación; no se recupera la exigencia antigua para hacer pasar la prueba. La firma real sigue cubierta en el circuito de aceptación y backend. Primera ejecución: 7 correctas/1 fallida; segunda: 8 correctas. Se repite tras el último lote administrativo.

## IA-06 Consultas administrativas auxiliares — P2

Fuente: NFR-UX-001 y reglas de errores sin falso vacío. Se reprodujeron ausencia de reintento en cargos, catálogos, estructuras y avisos; cargos además mostraba «No hay cargos registrados» tras un 503. Se añadieron estados iniciales de carga/error, reintentos y bloqueo de recarga sobre ediciones pendientes. La prueba auxiliary administrative queries recoge todos los fallos con aserciones soft para no ocultar los demás tras el primero. Evidencia roja: integral-auxiliary-reproduction.log; repetición global integral-acceptance-final-verified.log: 29/29 correctas. La red se restaura únicamente al recibir el clic real del botón; se corrigió una carrera de fixture con consultas tardías, sin omitir aserciones.

## Ensayo operativo y fixtures

La recuperación del respaldo histórico V33 pasó, pero no contenía PDF aprobado y el transporte terminó con JSONDecodeError: no es un resultado TLS aprobado. Se añadió diagnóstico de precondición y opción de credenciales del origen recuperado. La nueva opción PreparedDocuments del lanzador de aceptación espera el evento de fin de la semilla antes de probar o respaldar, igual que el lanzador manual; una primera captura anterior al commit de la semilla contenía cero documentos y no se usó como evidencia final.

Se repitió con respaldo de la versión V36: cuatro documentos y diez archivos verificados. Restauración y HTTPS pasaron, incluida sesión Secure, identidad, descarga del aprobado por SHA-256 y cuarenta lecturas de salud. No se envió correo real; no acredita SLA institucional. Se conservaron respaldos locales y se limpiaron solo los recursos aislados creados para el ensayo.

## Seguimiento de cierre

Cierre técnico: 215 pruebas backend, dos regresiones ampliadas T2/T3, 8 E2E y 29 de aceptación finales correctas; frontend lint/TypeScript/build, cinco guardas de aislamiento y once pruebas operativas correctos. Documentos y capturas revisados; respaldo/restauración y HTTPS actuales aprobados. No quedan P0/P1 identificados abiertos en los escenarios ejecutados. Aceptación institucional, accesibilidad con usuarios y contratos operativos reales siguen separados, según el reporte 34 y el guion.
