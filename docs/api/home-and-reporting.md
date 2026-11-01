# Inicio personal y Reportes de gestión

Decisión del propietario, 2026-10-30: Inicio reúne pendientes y documentos recientes; Reportes conserva analítica, filtros y exportaciones. El panel administrativo mantiene su propósito institucional.

## GET /api/tracking/recent

Consulta de sesión autenticada, Cache-Control: no-store. Parámetros groupId y periodId opcionales UUID. Respuesta WorkPlanModels.Page: items, total, page=0, size=5. Orden updated_at descendente y UUID como desempate. total incluye todos los documentos autorizados que cumplen filtros, antes del límite. Se excluyen archivados y descartados. No representa últimos accesos: representa actualizaciones documentales.

Los elementos reutilizan WorkPlanModels.Summary y sus permisos calculados. ADMIN no obtiene acceso implícito a documentos privados. UUID inválido devuelve 400; sin sesión, 401; consulta autorizada vacía devuelve 200 con items vacío. No modifica estados ni fechas. No calcula indicadores de gestión.

Inicio reutiliza GET /api/pending-work para acciones personales. counts corresponde a categorías antes de paginar/filtrar; la suma cuenta acciones, no documentos únicos. No se agregan indicadores duplicados ni otra consulta de resumen.

GET /api/tracking sigue compatible. GET /api/reporting y /api/reporting/export permanecen sin cambios para Reportes. Frontend pendiente de reorganización; consultar instruction.md en el entorno de trabajo para la entrega visual.
