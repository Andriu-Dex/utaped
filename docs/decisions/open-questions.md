# Decisiones pendientes y contradicciones

## Firma — precisión del propietario, 2026-10-27

La firma real usa el certificado aportado y confirmado en la operación, sin vinculación administrativa obligatoria. No existe una fuente institucional confiable que permita demostrar la correspondencia entre cuenta y titular: se registran por separado y no se afirma equivalencia. Cadena emisora, revocación y tiempo confiable siguen pendientes de contrato institucional. Esta decisión sustituye el requisito anterior de pin administrativo, preservando su historia. [Contrato vigente](../api/author-signing.md).

## Precisiones de implementación — 2026-10-21

- La copia local de custodia debe verificarse realmente por SHA-256; una respuesta HTTP de descarga no prueba su conservación. Sigue siendo necesaria la custodia fuera del servidor, antes de cualquier purga manual.
- Fecha de elaboración = preparación del PDF para firma, no fecha de la última firma. No se puede cambiar después sin invalidar firmas; fecha/hora de cada firmante se conserva por separado.
- El QR contiene metadatos; se elimina la referencia no contratada a un validador público. No afirma confianza institucional, TSA ni validación de revocación.
- Historia cerrada: ADMIN habilita permiso sobre un cargo y lo asigna en un período activo, por carrera o facultad. Decano/Subdecano se precargan únicamente como ejemplos de desarrollo. Ser ADMIN técnico no concede lectura de aprobados históricos. Descartados manuales son distintos del archivo automático al cerrar.
- Quipux se representa mediante constancia PDF opcional por flujo congelado, registrada por coordinación. Se bloquea avance a ejecución/informes derivados y cierre si falta; el aprobado se puede descargar para efectuar el trámite externo.
- La estructura adicional se publica para documentos nuevos y se congela por documento. Las secciones institucionales se conservan; validar con el cliente cualquier reordenamiento antes de usarlo institucionalmente.
- `Informe_ejecucion.pdf` muestra cuatro entradas de índice; el DOCX incorpora además acciones de mejora, firmas/control y FODA. Se usa el DOCX más completo como fuente de composición y el PDF como referencia visual. No se modifica ningún original de templates.
- La ventana inicial de cinco días hábiles se calcula cuando el fin de elaboración se deja vacío; una fecha final explícita configura otra ventana. La reapertura individual se aplica al ámbito del T1; T2/T3 disponen del período vigente. No se reabren períodos cerrados.

El corte/periodicidad definitiva del Informe de ejecución, las tablas reales de cargos/firmas, la reasignación de tareas ya activadas y las políticas institucionales de custodia/retención/operación requieren validación. [Contrato](../api/institutional-rules-and-execution.md).

## Decisiones provisionales del equipo — 2026-10-20

El propietario autoriza resolver provisionalmente las contradicciones de reuniones mientras el cliente no entregue información oficial. Son reglas configurables, no normativa institucional ratificada. [Plan de implementación](../planning/institutional-rules-and-execution-report.md).

- Descarga final: solo después de APPROVED y para participantes autorizados del documento. Coincide con el comportamiento actual; no se restringe al último validador.
- Fin de semestre: se elimina la documentación del período después de exportar y verificar el ZIP, registrar su descarga por ADMIN y cumplir un período de gracia configurable. Se conserva un inventario mínimo y la auditoría. Supera la prohibición anterior de purga en este alcance.
- Fecha de elaboración: la de finalización, fijada al preparar el artefacto de «Firmar y finalizar» y congelada en los bytes firmados.
- Prórrogas: máximo acumulado de 3 días hábiles (sin fines de semana ni feriados registrados), individual, con motivo, concedida por el coordinador del ámbito y dentro del período. La ventana de elaboración usa 5 días hábiles con la misma regla de reapertura individual.
- Puerto: 64430 es el único puerto de entrada; SMTP 587 es salida y requiere confirmación de red, con restablecimiento ADMIN como alternativa.
- Comisiones, cargos y flujos: datos de ejemplo de FISEI/UTA administrables e importables hasta recibir la tabla oficial.
- Nuevo formato: `INFORME EJECUCION II E26J26 UPE.docx` se trata como T3 Informe de ejecución, distinto del T2 Informe final.

## Decisión colegiada y autoría confirmadas — 2026-10-17

El propietario confirma un registrador designado por Administración para cada período, identificado en auditoría y sin firma personal en nombre del órgano. Esta decisión resuelve quién registra; no inventa la tabla de órganos/casos institucionales. Autoriza completar coautores/cargos y corregir observaciones de la ronda y encabezados. [Contrato de la tanda](../api/document-authorship-and-review.md). Siguen pendientes el catálogo completo de funciones y asignaciones oficiales, Quipux, retención y excepciones no incluidas en esta entrega.

## Evidencia de reuniones auditada — 2026-10-17

Se incorpora análisis del TXT «Todas las reuniones hasta la del 6-10-2026», sin convertir demostraciones de Prototipos en aceptación del software actual. [Auditoría completa](../reports/2026-10-17-06-meeting-transcripts-gap-audit.md).

El procedimiento QIPOC/QIPUX se describe en L451–472 con constancia obligatoria y registro por coordinador; esta evidencia merece confirmación expresa, pero el Plan Maestro §34 continúa siendo la regla vigente que lo mantiene pendiente. Aclarar también cualquier descarga previa requerida para ese trámite. No implementar una integración externa por deducción.

La designación del ejecutor colegiado se resuelve en la decisión anterior. Siguen abiertas la tabla oficial de cargos/firmas por ámbito, las delegaciones de coordinador, la excepción individual del plazo de elaboración y el cálculo de ejecución del Plan. Hay evidencia de cinco días de elaboración y tres días de evidencia, pero no debe confundirse su alcance ni dar por ratificado el cómputo laborable/calendario. El QR y la interpretación de «firma local» necesitan contrato concreto. La conservación del historial y la eliminación semestral se contradicen; no habilitar purga automática.

La tanda de autoría resuelve coautores y el bloqueo de aprobación con observaciones. Continúan pendientes las plantillas estructurales y el archivo restringido de descartados identificados en la auditoría. Las secciones históricas siguientes conservan su fecha y no son un inventario actualizado completo.


## Selector y denominación — 2026-10-14

El propietario autorizó recuperar Plan/Informe en creación y proponer nombres institucionales automáticamente. Se mantiene Informe Final derivado de Plan aprobado y único. El patrón de identificador individual (por ejemplo el nombre `TI-JD2026-UTIT-PLAN-V01-signed.pdf`) aún no tiene regla completa confirmada: los códigos de plantilla T1/T2 no sustituyen un consecutivo documental. No generar ni presentar códigos individuales como oficiales por deducción. [Contrato actualizado](../api/document-circuit.md).


## Accesibilidad y carga — 2026-10-13

El propietario autorizó y se incorpora audio español al mismo CAPTCHA con controles de teclado y protecciones de sesión/expiración/consumo. Se añaden escenarios concurrentes de PDFs extensos, evidencia y firma con datos aislados. Quedan aceptación con usuarios (incluidas necesidades que no cubre imagen/audio), prueba sostenida/SLA/concurrencia institucional y carga de transporte de certificados por HTTPS. [Alcance y reproducción](../testing/captcha-and-document-load.md). Las menciones históricas a ausencia de alternativa o de pruebas de carga quedan superadas exclusivamente en este alcance acotado.

## Preparación operativa y aceptación — 2026-10-12

Se entregan override HTTPS en 64430 con SMTP externo configurable, preflight, backup de DB/volumen/imágenes y restauración verificada en un destino aislado. Faltan hostname/certificado/CA y SMTP reales, condiciones de red, custodio/cifrado/frecuencia/RPO/RTO y aceptación institucional. Se amplió aceptación por siete identidades y T2 de 18 páginas; no equivale a dimensionamiento bajo carga documental. [Guía](../operations/production-readiness.md).

## Redacción y agente externo — 2026-10-12

Asistencia T1/T2 entregada mediante propuestas explícitas y proveedores Groq/OpenAI configurables en backend, deshabilitada por defecto. Falta configurar cuenta/modelo/clave y validar tratamiento de datos, costos y calidad con contenido autorizado. [Configuración](../development/writing-configuration.md). El propietario construye por separado el agente de código/Trello; no pertenece a las implementaciones pendientes de este producto. Bandeja paginada y búsqueda/filtros entregados; las menciones históricas a redacción/bandeja sin implementar quedan superadas en ese alcance.

## Administración contextual — 2026-10-12

Entregados: edición de carreras, historial de cargos/principales/prórrogas, recuperación por coordinador con delegación explícita de ADMIN y alertas internas configurables de vencimiento/evidencia pendiente. Las menciones históricas a estos bloqueos quedan superadas en el alcance del [contrato](../api/contextual-administration.md). No se concede alta/importación global ni permisos de Decano/Subdecano. Frecuencias iniciales técnicas ajustables, no normativa institucional.

## Actualización del 2026-10-12

Cierre ACTIVE→CLOSED sin purga, archivo histórico verificable y asignaciones T1/T2 por ámbito/período están entregados en alcance inicial. La activación es explícita y obligatoria antes de nuevas firmas; congela participantes y revisiones. ADMIN puede operar el cierre, pero su rol no concede lectura del ZIP completo. El ZIP descargable respeta también el bloqueo de preliminares por ámbito. [Contrato](../api/period-control.md).

Siguen pendientes retención/reapertura, permisos institucionales de Decano/Subdecano, sustitución de ejecutores tras bajas y exportación operativa de grandes volúmenes. Las menciones a cierre y configuración temporal pendientes en los párrafos siguientes describen el estado previo de la tanda amplia.

Ámbitos, titularidad, tareas secuenciales, firmas sucesivas, evidencias PDF, T2 derivado y archivo/versiones cuentan ya con implementación inicial. Las filas históricas que indican ausencia de esas tablas o motores quedan superadas por [el estado vigente](../../implementation-status.md).

Continúan pendientes las asignaciones oficiales, múltiples elaboradores, políticas de baja de revisores, configuración temporal por ámbito/período, cierre de períodos/ZIP y aceptación operativa. Prórroga técnica: máximo configurable de tres días calendario, individual y limitada al fin del período; no se presenta como cómputo institucional ratificado. Ventana T2 inicial: período académico completo. Evidencia incompleta exige explicación y no produce avance físico automático.

No se incorporan correos o permisos reales desde el PDF de unidades/comisiones sin alta y designación explícitas. Los usuarios de desarrollo son ficticios. Asistencia de redacción y agente de código/Trello requieren su propia configuración y siguen sin implementar.


## Reconciliación vigente — 2026-10-06

El [Plan Maestro](PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md) sustituye las preguntas históricas que ya resuelve. La tabla original siguiente se conserva como historial, no como bloqueo vigente universal.

| Cuestión histórica | Decisión vigente / trabajo restante |
|---|---|
| Q-001 | T1 de seis pasos; actividades y matriz comparten fuente. Falta completar el asistente productivo. |
| Q-002 | Plan de comisión/grupo, carrera y período; responsable principal único. Falta migrar titularidad, ámbitos, unicidad y familias de versiones. |
| Q-003/Q-018 | Revisión secuencial, todos obligatorios; no AL MENOS UNO ni paralelismo. Falta tabla de cargos/personas por ámbito y motor de tareas/rondas. |
| Q-005 | Informe final derivado, uno por Plan; nunca independiente. Queda precisar relación con versiones formales y continuidad de evidencias. |
| Q-007 | .p12/.pfx temporal en memoria; firmas acumulativas. Continúan pendientes aceptación operativa del certificado/perfil y política institucional; no se exige inventar una CA o TSA. |
| Q-008 | Local: username, correo separado, Argon2id, CAPTCHA servidor y dos vías de recuperación. Se entrega ADMIN; coordinadores esperan ámbitos autorizados. |
| Q-009 | Monolito modular Java/Spring, React/TS y PostgreSQL. Transición incremental JDBC→JPA, API v1/OpenAPI y Testcontainers aún pendientes. |
| Q-010/Q-013 | Almacenamiento privado; T1/T2 institucionales adaptados a A4. Anexos de varios formatos: falta decidir conversión/representación y alcance de firma. |
| Q-006/Q-011 | Zona America/Guayaquil, cierre ACTIVE→CLOSED sin purga, prórrogas individuales. Precisar los ~3 días, cómputo y cruce del cierre. |
| Q-012/Q-017 | No pedir cédula/teléfono por defecto. Pertenencias por carrera/período/cargo; conservar historial. Falta completar catálogo y autorizaciones. |
| Q-014/Q-019 | Asistencia de redacción con proveedor backend; agente código/Trello sigue como alcance separado. Proveedor, permisos, presupuesto y tablero pendientes. |
| Q-004 | El Plan Maestro no ratifica expresamente la integración QIPOC heredada; no activarla por suposición. |

El PDF de unidades/comisiones julio–diciembre 2026 aporta nombres, carreras y algunos cargos; no contiene correos, usuarios, todos los responsables principales ni la secuencia completa de aprobación. No convierte automáticamente a cada coordinador listado en administrador o firmante. Quedan por precisar también múltiples elaboradores, descarga de preliminares/anexos, elegibilidad del T2 con evidencias faltantes y fórmulas de indicadores. Estos puntos no bloquean el acceso ni el rediseño entregados.

Accesibilidad del CAPTCHA: falta acordar una alternativa equivalente para personas que no pueden resolver un reto visual; el texto alternativo describe el reto sin revelar su respuesta. No se declara conformidad WCAG integral.

## Registro histórico anterior al Plan Maestro

2026-10-04: Q-007 parcialmente resuelta por el propietario: firma mediante archivos .p12/.pfx. Falta definir vinculación de titular/cuenta, autoridades confiables, revocación, perfil y tiempo. Q-003/Q-018 siguen pendientes: el propietario confirma que los flujos no están ratificados y acepta avanzar con preparación técnica manteniendo finalización/envío/aprobación bloqueados. [Contrato](signature-review-contract.md) y [ficha por grupo](../validation/workflow-signature-confirmation.md).

2026-10-03: Q-010 parcialmente resuelta por el propietario: almacenamiento local privado persistente, 10 MB/anexo y 20 anexos como configuración técnica ajustable. Retención, restauración operacional y límites institucionales definitivos siguen pendientes. Q-013 parcialmente resuelta: páginas T1 en A4 conservando estructura y estilos, incluida matriz horizontal; anexos conservan su formato original. Aceptación institucional final y T2 pendientes. [ADR-0002](../architecture/adr/0002-t1-document-preparation.md).

El propietario coordinará validación con institución/DTIC; no asignar autoridades ni respuestas definitivas por suposición.

Entrega 2026-10-03: Q-001/Q-002 continúan pendientes. El editor inicial contiene información general y contenido, sin definir el asistente completo. La unicidad docente/grupo/período está preparada como política configurable, deshabilitada provisionalmente; no constituye decisión institucional. Unidad/carrera permanecen manuales hasta disponer de catálogos y vinculación institucional. Las ventanas existentes se aplican inclusivamente en zona configurable America/Guayaquil; Q-006/Q-011 siguen pendientes para excepciones y validación institucional.

| ID | Pregunta | Evidencia necesaria / impacto |
|---|---|---|
| Q-001 | Pasos T1: fuente enumera siete, UI puede agrupar matriz/actividades. | Ratificar UX contra pantallas vigentes, conservando estructura documental. |
| Q-002 | Unicidad docente/grupo/período y excepciones. | Titularidad, cardinalidad e índice. |
| Q-003 | Flujos por grupo, paralelismo y órganos colegiados. | Participantes, condiciones, decisiones y firmas. |
| Q-004 | QIPOC y validación externa. | Quién descarga/carga/valida y cuándo se aprueba. |
| Q-005 | Informe independiente y derivación. | Elegibilidad, multiplicidad y tipos habilitados. |
| Q-006 | Cierre, reapertura, corrección posterior y prórrogas. | Regla institucional; no crear excepciones automáticas. |
| Q-007 | Firma real mediante .p12/.pfx; flujo simple y controles básicos confirmados por el propietario. | Huella pública administrativa implementada para el elaborador; aceptación institucional, cadena/revocación, perfil, tiempo y múltiples firmas pendientes. |
| Q-008 | Identidad, local/SSO, sesiones, recuperación y CAPTCHA. | Política de acceso y correo disponible. |
| Q-009 | Stack e infraestructura. | Ratificar versiones, Spring Boot/React/PostgreSQL, red y migraciones. |
| Q-010 | PDFs/evidencias. | Almacenamiento local y límites técnicos iniciales confirmados; retención, restauración y política institucional pendientes. |
| Q-011 | Fechas/feriados. | Zona institucional y significado exacto de límite 23:59, reloj servidor. |
| Q-012 | Datos personales y nota institucional. | Campos, finalidades, condición de nota y acceso. |
| Q-013 | Formatos oficiales T1/T2. | Versión vigente y reordenamiento permitido frente a fidelidad. |
| Q-014 | IA. | Habilitación, datos permitidos, modelos verificados y presupuesto. |
| Q-015 | Notificaciones/importación. | Canales, frecuencia, formatos CSV/Excel, errores y duplicados. |
| Q-016 | Operación/accesibilidad. | Volumen, concurrencia, SLA, RPO/RTO, navegadores y objetivos. |
| Q-017 | Bajas/cambio de miembros y catálogos. | Permisos, asignaciones vigentes y preservación de historia. |
| Q-018 | Regla AL MENOS UNO frente a todos los revisores obligatorios. | workflow.ts permite alternativa; validar etapas aplicables y representar participantes que no actuaron sin aprobación ficticia. |
| Q-019 | Agente de inspección del código y planificación del desarrollo de UTAPED (alcance confirmado); tableros Trello y destinatarios pendientes. | Delimitar rutas/revisiones, permisos, criterios de mejora, frecuencia, proveedor/presupuesto y retención antes de implementar. [Requisitos nuevos](../requirements/project-advisor-agent.md). |

Q-001: diferencia confirmada mediante inspección estática; la UI enumera seis pasos y la línea base siete. La elección productiva sigue pendiente. [Detalle de reconciliación](../validation/reconciliation.md).

Actualización 2026-10-02: Q-008 resuelta para mecanismo de acceso: credenciales locales por instrucción del propietario. Se implementan recuperación SMTP y sesiones; configuración de correo institucional y política formal de acceso siguen pendientes. Q-009 resuelta para stack según [ADR-0001](../architecture/adr/0001-production-foundation.md); infraestructura de publicación, respaldo y operación institucional siguen pendientes. Las restantes cuestiones permanecen abiertas.

[Plan de validación](../validation/validation-plan.md) y [matriz de permisos para revisar](../validation/permission-matrix.md).

Para cerrar: respuesta, persona/área validadora, fecha y evidencia verificable; actualizar requisitos, ADR y casos afectados.

2026-10-05: firma visible del elaborador entregada con confianza directa por huella pública administrativa y comprobación de vigencia/integridad. El usuario mantiene archivo + contraseña + Firmar. No se valida cadena/revocación y se informa expresamente. Q-003/Q-018 permanecen pendientes; no se habilitan finalización, envío o aprobación. [API y operación](../api/author-signing.md).

2026-10-12: importación CSV exclusiva de ADMIN y evidencia URL entregadas según el Plan Maestro. Alta/importación delegada, cargos oficiales con vigencia y políticas de acceso/retención de repositorios siguen pendientes. La descarga única de credenciales y limpieza de prevalidaciones son decisiones técnicas iniciales documentadas en [el contrato](../api/user-imports-and-external-evidence.md).
