# Entrega ampliada — Flujos, administración y trazabilidad

Fecha: 2026-10-03. Rama: feature/workflow-administration-audit desde develop, merge 68119c4 del PR #4, con CI previo correcto. Trabajo organizado en bloques verificados y commits separados; un único push/PR al finalizar. Revisión y fusión reservadas al propietario.

## Bloque 1 — Configuración de flujos

Flyway V7 y módulo administrativo para definiciones T1/T2 independientes por grupo. Etapas ordenadas, personas múltiples, roles de pertenencia y órganos colegiados sin firmante individual cuando no requieren firma personal. Borrador editable, validación de destinatarios, concurrencia, revisión técnica inmutable, historial y deshabilitación. Ninguna configuración concede acceso a archivos ni inicia revisión/firma documental.

Validación del bloque: frontend build/lint; 25 pruebas backend con PostgreSQL aislado, sin fallos; dos escenarios Playwright con Nginx/Docker, incluido flujo administrativo, falta de destinatarios, orden, historia y separación T1/T2. Q-003/Q-007/Q-018 siguen pendientes para ejecutar transiciones institucionales. [API](../api/workflow-configuration.md).

## Bloques siguientes

Administración de grupos/usuarios y consulta de auditoría/historial: en implementación; se registrarán resultados efectivos antes de publicar la entrega.
