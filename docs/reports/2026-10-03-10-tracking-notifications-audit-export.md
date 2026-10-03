# Seguimiento, notificaciones internas y exportación de auditoría

Fecha: 2026-10-03. Rama feature/tracking-notifications-audit-export desde develop 83edd16, merge del PR #5. Mockup congelado sin modificaciones. Entrega por bloques verificados y commits separados; publicación final mediante push y PR hacia develop, revisión/fusión reservadas al propietario.

## Bloque 1 — Seguimiento personal

Panel con filtros por grupo/período, total de documentos propios disponibles, borradores editables, solo lectura, Planes con previsualización guardada, distribución y cinco recientes. Abrir utiliza targetDocId explícito y autorización real. Las métricas no consideran firma ni aprobación; los PDFs guardados pueden ser anteriores al contenido actual. [API](../api/tracking.md).

## Bloques siguientes

Bandeja de notificaciones persistentes por usuario y exportación administrativa filtrada, en implementación. Se registrarán únicamente verificaciones realizadas.

Verificación del bloque 1: frontend build/lint, 33 pruebas backend con PostgreSQL aislado y dos E2E ampliados con Nginx/Docker. Panel móvil revisado visualmente y navegación al Plan comprobada. Se corrigieron datos de pruebas que omitían creation_title y repetían un nombre de período único. Un fallo de caché Docker al empaquetar frontend se resolvió reconstruyendo esa imagen sin caché; no se borraron volúmenes ni datos.

## Bloque 2 — Notificaciones internas

Flyway V10, bandeja privada con filtros/paginación y contador de no leídas. Lectura individual/idempotente y colectiva; aviso enlaza al objeto exacto con comprobación vigente de acceso. Se generan por cambios reales de pertenencia y previsualizaciones T1 nuevas, en la misma transacción del evento. Dedupe por origen/destinatario; operaciones sin cambio no generan avisos. Historial permanece aunque el destino deje de estar disponible. No se retrogeneran avisos ni se envían correos/push.

Verificación: frontend build/lint sin avisos; 36 pruebas backend, cero fallos, incluidas privacidad frente a ADMIN, CSRF, paginación, lectura, rollback/dedupe y revocación de ámbito. Dos E2E ampliados con Nginx/Docker: apertura del Plan exacto, marcar todas, filtros y lectura persistida tras recargar. Bandeja móvil revisada visualmente. [API](../api/notifications.md).
