# Cierre backend y entrega de frontend — 30/10/2026

## Entrega

Se implementaron la sustitución autorizada de revisores no disponibles y la copia explícita de borradores T1 entre carreras de un ámbito unipersonal. Ambos procesos comprueban permisos en servidor, versión concurrente e idempotencia, y registran auditoría. La migración V38 incorpora sus registros; debe aplicarse antes de integrar las nuevas pantallas.

La sustitución conserva el artefacto y las firmas anteriores. La nueva firma identifica la sustitución; el nombre originalmente impreso en la tabla no se reescribe. Este tratamiento requiere aceptación institucional. Solo se considera indisponibilidad por cuenta inactiva o pertenencia expresamente inactiva; no se inventa una regla para ausencias temporales.

Las copias son borradores independientes, con identificadores de actividades nuevos. No heredan firmas, revisiones, anexos ni evidencias. La operación sobre varios destinos es transaccional y no modifica el Plan fuente. Auditoría y custodia incluyen ambos procesos.

## Verificación

Se aprobaron 19 escenarios dirigidos: tres de copias (incluido HTTP, sesión y CSRF), tres de sustitución, dos de cierre/custodia, tres de revisión existente y ocho de firma PKCS12. Se ejecutaron en entorno aislado. La última ejecución conjunta tuvo un fallo en la detección de bloqueo de la propia prueba de concurrencia; corregida la consulta de diagnóstico, ese escenario se ejecutó nuevamente y pasó (1 prueba, 0 fallos, 0 errores). Los otros 18 habían pasado en la ejecución conjunta.

Registros locales: `backend/target/closure-final-checks.log`, `backend/target/closure-concurrency.log` y Surefire. No constituye una nueva aceptación integral ni una revisión visual de frontend. Las advertencias de composición FOP presentes en los datos de prueba no equivalen a aceptación del formato documental; se conserva la revisión visual en el protocolo final.

## Pendientes y entrega

- [Contrato API](../api/review-reassignment-and-career-copies.md).
- Instrucciones detalladas entregadas en `instruction.md` en la raíz (ignorado por Git); el contrato permanente está enlazado arriba.
- [Decisiones para confirmar con el cliente](../validation/client-closeout-decisions.md).
- [Protocolo de aceptación final](../validation/final-acceptance-checklist.md).

RC014 y RC051 permanecen parciales hasta integrar frontend y validar los escenarios correspondientes. No se alteraron los archivos frontend del propietario, ni se hicieron commits o push. La migración se verificó en pruebas aisladas; no se aplicó al entorno manual durante esta entrega.
