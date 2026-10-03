# Seguimiento, notificaciones internas y exportación de auditoría

Fecha: 2026-10-03. Rama feature/tracking-notifications-audit-export desde develop 83edd16, merge del PR #5. Mockup congelado sin modificaciones. Entrega por bloques verificados y commits separados; publicación final mediante push y PR hacia develop, revisión/fusión reservadas al propietario.

## Bloque 1 — Seguimiento personal

Panel con filtros por grupo/período, total de documentos propios disponibles, borradores editables, solo lectura, Planes con previsualización guardada, distribución y cinco recientes. Abrir utiliza targetDocId explícito y autorización real. Las métricas no consideran firma ni aprobación; los PDFs guardados pueden ser anteriores al contenido actual. [API](../api/tracking.md).

Verificación del bloque 1: frontend build/lint, 33 pruebas backend con PostgreSQL aislado y dos E2E ampliados con Nginx/Docker. Panel móvil revisado visualmente y navegación al Plan comprobada. Se corrigieron datos de pruebas que omitían creation_title y repetían un nombre de período único. Un fallo de caché Docker al empaquetar frontend se resolvió reconstruyendo esa imagen sin caché; no se borraron volúmenes ni datos.

## Bloque 2 — Notificaciones internas

Flyway V10, bandeja privada con filtros/paginación y contador de no leídas. Lectura individual/idempotente y colectiva; aviso enlaza al objeto exacto con comprobación vigente de acceso. Se generan por cambios reales de pertenencia y previsualizaciones T1 nuevas, en la misma transacción del evento. Dedupe por origen/destinatario; operaciones sin cambio no generan avisos. Historial permanece aunque el destino deje de estar disponible. No se retrogeneran avisos ni se envían correos/push.

Verificación: frontend build/lint sin avisos; 36 pruebas backend, cero fallos, incluidas privacidad frente a ADMIN, CSRF, paginación, lectura, rollback/dedupe y revocación de ámbito. Dos E2E ampliados con Nginx/Docker: apertura del Plan exacto, marcar todas, filtros y lectura persistida tras recargar. Bandeja móvil revisada visualmente. [API](../api/notifications.md).

## Bloque 3 — Exportación administrativa

CSV de todos los resultados de filtros aplicados, independientemente de la página visible, en un snapshot transaccional consistente. ADMIN obligatorio, caché privada/no-store, nombres históricos cuando existen y límite técnico de 10000 filas ajustable. Superar el límite requiere refinar filtros; no trunca ni registra éxito. AUDIT_EXPORTED atribuye la descarga. Las celdas peligrosas reciben prefijo visible Texto: y las comillas internas se escapan; no hay secretos ni contenidos documentales. [Contrato y referencia OWASP](../api/audit-history.md).

Verificación final: frontend build/lint sin avisos; 40 pruebas backend con PostgreSQL aislado, cero fallos; dos E2E ampliados con Nginx/Docker correctos. Exportación filtrada, filtros no aplicados, descarga y nombre de archivo, vacío/limitación, autorización, timestamps inclusivos, fórmulas/variantes de ancho completo y comillas. Regresiones de T1/PDF inmutable, matriz, anexos, grupos, directorio y flujos conservadas. No se afirma prueba institucional de carga ni aceptación final del formato.

UT-SEC-051/052/053 se registran como PARCIAL en el catálogo; no se cambian reglas heredadas ni se atribuye aceptación institucional.

## Commits y publicación

- 347d47e: seguimiento personal.
- 0542f42: notificaciones internas.
- Tercer commit: exportación y cierre documental; identificable en el historial de la rama y el PR.

Entrega publicada mediante un único push de la rama y PR hacia develop tras verificaciones locales. El propietario revisa y fusiona; no se modifica main ni se despliega. La CI remota se consultará después de publicar y su resultado se comunicará en el PR.

## Siguiente implementación recomendada

Acordar el contrato de firma real (proveedor/DTIC, formato, validación y fallos) y las reglas institucionales de asignación/transición por grupo, incluidas etapas colegiadas y revisores obligatorios. Con Q-003/Q-007/Q-018 definidas, implementar finalización T1, asignación de revisión y bandeja sobre artefactos exactos, conservando decisiones, firmas y rondas. Las notificaciones existentes se ampliarán a esos eventos reales. Después continuar con ejecución/evidencias y T2. No se simulan firmas ni aprobaciones productivas para saltar estos pendientes.
