# Decisiones pendientes y contradicciones

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
