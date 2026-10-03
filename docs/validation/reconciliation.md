# Reconciliación de requisitos y referencia

Fecha: 2026-10-02. Revisión estática, sin pruebas de navegación. [Evidencias y huellas](../reference/reconciliation-evidence.md).

| Hallazgo | Fuente / referencia observada | Tratamiento productivo |
|---|---|---|
| RC-001: asistente T1 | Línea base §7 y estado heredado §3 describen siete pasos. src/App.tsx:1582 contiene seis: Información general, Contenido, Actividades, Anexos, Previsualización, Firma y Finalización. Step3Matriz es parte del paso Actividades. | Q-001 pendiente de decisión explícita; no añadir un paso por asumir que el documento está más actualizado. |
| RC-002: estados documentales | Fuente propone estados con guiones bajos; types.ts incluye etiquetas con espacios, LISTO PARA FIRMA y EN VALIDACIÓN FINAL; separa estado operativo. | Preparar catálogo canónico y tabla de transiciones, distinguiendo etiqueta UI y estado técnico. FINALIZADO no se acredita por enumeración del motor revisado. |
| RC-003: aprobación de etapa | FR-REV-002 exige todos los revisores obligatorios. workflow.ts:advanceStage admite AL MENOS UNO y marca pares APROBADO sin decisión individual. | Validar si basta uno en alguna etapa. No registrar aprobación ficticia de quien no actuó; distinguir satisfecho/no requerido de aprobado por actor. Q-018. |
| RC-004: identidad DEMO | workflow.ts normaliza alias de usuarios DEMO y usa elaborador predeterminado al configurar flujo. | Identidad de servidor y asignación reales; no trasladar alias ni valores por defecto al backend. |
| RC-005: firma | types.ts contempla credentialMode/isDemo y metadata de firma. | No equivale a PDF firmado criptográficamente. Q-007 bloquea firma real; revisión debe descargar bytes persistidos exactos. |
| RC-006: T2 | WizardInformeView define ocho pasos, incluida tabla de contactos. Fuente exige contactos cuando aplique y contempla Informe independiente condicionado. | Validar condición, elegibilidad del Plan y multiplicidad de Informes. Q-005; UI existente no aprueba regla institucional. |
| RC-007: formatos oficiales | Existen candidatos DOCX T1/T2 en Documentos_guia; también ejemplo PDF firmado. | Confirmar vigencia antes de comparar contenido/maquetación. No se copiaron datos personales. Q-013. |
| RC-008: stack | Estrategias heredadas nombran Spring Boot y PostgreSQL on-premise; solo FR-ARCH-DB-001 define DB explícitamente en catálogo. | Separar antecedente, requisito y ADR aceptado. Q-009; no declarar versiones elegidas. |
| RC-009: estado heredado | implementation-status del prototipo describe siete pasos; referencia visual revisada muestra seis. | Estado histórico no sirve para medir producto ni certificar UI actual; actualizar solo documentos nuevos, manteniendo mockup congelado. |

Se recomienda mantener seis pasos para continuidad con la referencia, condicionado a aprobación del propietario; esto no cambia la estructura T1. Ningún hallazgo se considera resuelto por esta recomendación.

La revisión no constituye auditoría exhaustiva de código ni certificación de funcionalidad. No se revisaron todos los módulos o formatos internos DOCX.
