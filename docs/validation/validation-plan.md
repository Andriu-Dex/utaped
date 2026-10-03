# Plan de validación de requisitos

Estado: listo para revisión; respuestas aún no recibidas. [Catálogo trazable](../requirements/catalog.md) conserva 77 IDs FR-* y 45 bloques nuevos UT-SEC-*; son 122 registros fuente, no 122 requisitos atómicos aprobados.

## Paquetes de decisión

| Orden | Participación requerida | Preguntas / entregable | Bloquea |
|---|---|---|---|
| 1 | Propietario y usuarios que validan UX | Q-001, Q-005, Q-013; pasos, formatos vigentes y elegibilidad T2. | Contrato de creación y diseño documental. |
| 2 | Responsables institucionales del proceso | Q-002, Q-003, Q-004, Q-006, Q-017, Q-018; flujo por grupo, actores y excepciones. | Esquema de titularidad y workflow. |
| 3 | DTIC y propietario | Q-007, Q-008, Q-009, Q-010; identidad, firma, stack e infraestructura/archivos. | Integraciones y ADR tecnológico. |
| 4 | Áreas institucionales competentes | Q-011, Q-012, Q-014, Q-015, Q-016; plazos, datos, IA, comunicación y operación. | Políticas y aprobación de producción. |

No se envían comunicaciones ni se atribuyen cargos/validadores definitivos. El propietario coordina quién puede responder con autoridad.

## Registro por respuesta

ID de cuestión; respuesta exacta; alcance/grupos; quién valida y en qué calidad; fecha; documento o evidencia; requisitos afectados; criterios de aceptación; decisión de arquitectura si aplica. Estado: abierto, respuesta propuesta, validado, descartado o sustituido. Respuesta del propietario sobre UX es confirmación del proyecto, no aprobación institucional de normativa.

## Desglose antes de desarrollar

Cada registro compuesto se divide en requisitos verificables conservando ID fuente, por ejemplo un identificador subordinado. Documentar precondiciones, actor, entrada, resultado y errores. Establecer prioridad, dependencia y pruebas; separar configuración de permiso real. No marcar como implementado por existir pantalla DEMO.

## Avance independiente posible

Revisar vocabulario, preparar casos de aceptación, identificar restricciones de integridad y diseñar interfaces conceptuales. Posponer esquema físico de unicidad y workflow hasta respuestas. No elegir silenciosamente SSO, algoritmo de firma o almacenamiento.

## Primer módulo vertical propuesto

Identidad/grupos/períodos, seguido de borrador T1 persistido: persona real, contexto autorizado, pertenencia, datos mínimos, creación/consulta/edición con autorización y concurrencia, auditoría y aislamiento. Sin acreditar firma o aprobación mientras sus contratos estén pendientes.

Criterios propuestos: usuario autorizado recupera su borrador tras nueva sesión; usuario ajeno recibe rechazo al leer/editar con ID conocido; cambio de contexto mantiene identidad; duplicados obedecen decisión validada; edición concurrente produce conflicto identificable; registros no contienen secretos. Ejecutar integración con DB y E2E cuando exista implementación.
