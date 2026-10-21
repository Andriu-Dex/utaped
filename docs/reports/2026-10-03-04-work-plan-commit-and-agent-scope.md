# Commit de borradores T1 y nuevo alcance del agente

Fecha: 2026-10-03. Rama: `feature/work-plan-drafts`. Fuente: autorización del propietario para commit y solicitud de evaluar un agente que inspeccione proyecto/Trello y sugiera mejoras.

## Entrega y verificación

Preparación del commit de la implementación descrita en [reporte de borradores T1](2026-10-03-03-work-plan-drafts.md), con asunto `feat: persistir borradores de planes de trabajo`. Se incluyen código, migración V4, configuración de ejemplo, pruebas y documentación. Se preserva la identidad Git configurada; sin push ni PR en esta tarea.

Revisados estado Git, cambios, lista de archivos nuevos y reglas de exclusión. La validación de la implementación realizada en la entrega anterior consta de build/lint, 15 pruebas backend y E2E con Vite/Nginx; no se repitieron pruebas de aplicación en esta tarea porque no se modificó código. Se verifica el diff antes del commit y el estado final después de crearlo. Los secretos locales y artefactos generados deben permanecer ignorados.

## Impacto del agente

El nuevo requisito exige ampliar la documentación y delimitar permisos/fuentes; no obliga a reconstruir UTAPED ni cambiar React/Spring Boot/PostgreSQL. Se registran FR-AGENT-001/002/003 en [requisitos del agente](../requirements/project-advisor-agent.md), enlazados desde requirements.md, y Q-019 en decisiones pendientes. El agente aún no está implementado y no se ha conectado ninguna cuenta Trello.

El propietario confirmó que revisará código y planificación del desarrollo de UTAPED. Recomendación: herramienta interna separada de la aplicación institucional, con lectura de fuentes autorizadas y sugerencias con evidencia. No cambia el flujo documental de los usuarios institucionales.

La API Trello permite autorización de lectura: [fuente oficial consultada](https://developer.atlassian.com/cloud/trello/guides/rest-api/authorization/). La elección concreta de autenticación, proveedores de IA y framework se pospone hasta definir alcance, datos permitidos y presupuesto.

Pendientes: delimitar rutas/revisiones, tableros, usuarios y criterios de mejora. Después, definir aceptación y diseño antes de implementar el agente. La implementación documental puede continuar sin alterar los borradores existentes.
