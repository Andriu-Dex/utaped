# Requisitos no funcionales

Estado PROPUESTO, excepto reglas heredadas señaladas.

| ID | Requisito | Verificación y pendientes |
|---|---|---|
| NFR-SEC-001 | Autorización backend por usuario, ámbito y objeto. | Accesos cruzados rechazados; no confiar en rol enviado por cliente. |
| NFR-INT-001 | Transacciones y control de concurrencia; prevenir doble envío/firma. | Conflictos no sobrescriben silenciosamente ni duplican decisiones. |
| NFR-DOC-001 | Artefactos firmados íntegros y preservados. HEREDADO. | Descarga/revisión corresponde a bytes persistidos, no regenerados. |
| NFR-DATA-001 | Persistencia relacional y migraciones versionadas. | Instalación y actualización verificables. PostgreSQL es dirección heredada. |
| NFR-OPS-001 | Respaldo/restauración conjunta de DB y archivos. | Ensayo de recuperación; RPO/RTO, retención y frecuencia pendientes. |
| NFR-UX-001 | Español, teclado, foco, errores asociados y A4 legible. | Revisión manual; nivel formal y navegadores pendientes. |
| NFR-PERF-001 | Paginación y límites de consultas/cargas. | Medir con volumen/concurrencia acordados; no inventar SLA. |
| NFR-OBS-001 | Diagnóstico correlacionado sin secretos. | Revisar logs de error, firma y recuperación. |
| NFR-MOD-001 | Módulos con responsabilidades y contratos explícitos. | Reglas críticas fuera de UI; dependencias revisadas. |
| NFR-TIME-001 | Distinguir instantes de fechas civiles; zona institucional configurable. | Casos de límite horario; zona definitiva pendiente. |

Faltan valores institucionales de concurrencia, volumen, tamaño PDF, disponibilidad, tiempos de respuesta y retención.
