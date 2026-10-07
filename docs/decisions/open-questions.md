# Decisiones pendientes y contradicciones

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
