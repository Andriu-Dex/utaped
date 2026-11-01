# Separación de Inicio y Reportes — backend

## Alcance

Decisión del propietario: Inicio personal orientado a actuar y retomar documentos; Reportes orientado a analizar. Administración conserva su panel institucional. Frontend delegado por el propietario a Antigravity mediante instruction.md, sin cambios de pantallas en esta entrega.

## Implementación

GET /api/tracking/recent entrega WorkPlanModels.Page con cinco documentos accesibles por actualización descendente y UUID como desempate. Reutiliza consulta, tipos y ACL de WorkPlanService; excluye archivados y descartados antes de contar y limitar. Cache-Control: no-store, filtros UUID opcionales y sesión obligatoria. No devuelve ni calcula métricas analíticas. El contrato anterior /tracking y los endpoints de pendientes, reportería y exportación permanecen compatibles.

## Handoff

instruction.md contiene contratos, archivos afectados, jerarquía de Inicio, manejo independiente de carga/errores, navegación, tema oscuro, accesibilidad y escenarios de aceptación. El archivo se encuentra ignorado por Git según la configuración existente; el contrato permanente está en docs/api/home-and-reporting.md. Frontend pendiente de implementación y revisión visual.

## Validación

Pruebas dirigidas: TrackingIntegrationTests (contrato previo y nuevo endpoint). Se comprueban límite, orden, total, filtros, sesión, caché, ausencia de métricas, exclusión de descartados/archivados y autorización sin privilegio implícito de ADMIN. La primera ejecución detectó una expectativa incorrecta del fixture: ADMIN sin pertenencia al grupo tampoco obtiene documentos por figurar como teacher_id; se corrigió la expectativa para respetar el ACL existente, sin modificar permisos.

Resultado final: tres pruebas de integración aprobadas, sin fallos ni errores; git diff --check correcto. Sin cambios frontend, commits ni push.
