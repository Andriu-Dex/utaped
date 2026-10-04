# Preparación de firma y revisión — decisiones pendientes

Fecha: 2026-10-03. Base develop a7ec62b, merge del PR #6 confirmado en GitHub. Rama feature/signature-review-preparation. Trabajo en curso, no entrega funcional terminada.

Se releen requisitos, AGENTS.md productivo, estado, Q-003/Q-007/Q-018, matriz de permisos y motor T1. La firma/finalización debe aplicar una firma real al artefacto exacto y continuar según un flujo validado; no es admisible simular éxito o inventar decisiones institucionales.

Preparado [contrato propuesto y aceptación](../decisions/signature-review-contract.md). Inspección estática: los artefactos conservan bytes/snapshot/hash y página de elaboración; falta integrar flujo/participantes al snapshot de firma, geometría/espacios aplicables y proveedor real. El esquema documental actual solo admite DRAFT. No se modificaron estados, permisos, PDF ni datos.

Se solicitan al propietario el mecanismo de firma disponible y la existencia de flujos/reglas validados. Esas respuestas son información requerida para implementar comportamiento dependiente; no se consideran respondidas por transcurrir tiempo ni por existir una opción preseleccionada.

Validaciones realizadas: PR #6 MERGED, actualización fast-forward de develop, lectura de fuentes/código y consulta de referencias oficiales PDFBox/ETSI. No se ejecutaron pruebas de aplicación porque aún no hay cambios de comportamiento. No se afirma haber implementado firma/revisión ni cerrado las cuestiones.

No se realiza commit/push/PR de una implementación incompleta. Tras las respuestas, continuar en esta rama, implementar el alcance confirmado, verificar frontend/backend/E2E y publicar para revisión del propietario. Mockup congelado sin modificaciones.

## Continuación 2026-10-04

El propietario eligió .p12/.pfx y confirmó que todavía no dispone de flujos ratificados. Aceptó avanzar en la preparación técnica manteniendo bloqueadas las transiciones. La entrega posterior y sus verificaciones se registran en [nuevo reporte](2026-10-04-01-signature-core-workflow-snapshots.md); este reporte conserva la inspección inicial y no se usa como evidencia de firma institucional habilitada.
