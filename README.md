# UTAPED — Gestión Documental Académica

Aplicación productiva modular para FISEI / Universidad Técnica de Ambato. Incluye identidad local, administración por ámbitos, Planes T1, revisión secuencial con firmas, evidencias e Informes T2 derivados, versiones formales y reportes de gestión. La interfaz conserva una base visual común para PC y tabletas, con modo claro y oscuro.

## Arranque

Copiar .env.example a .env y configurar secretos únicos. Ejecutar `docker compose --profile web up --build -d` y abrir http://localhost:64430. El administrador inicial debe cambiar su contraseña temporal. [Guía completa](docs/development/local-setup.md).

El acceso admite usuario o correo y exige CAPTCHA generado por servidor. El directorio permite consultar los usernames migrados y restablecer cuentas administrativamente. No hay registro público. [Contrato de acceso](docs/api/local-access.md).

## Stack

React/TypeScript/Vite; Java 21/Spring Boot; PostgreSQL/Flyway; sesiones JDBC y Spring Security. [Decisión y fuentes](docs/architecture/adr/0001-production-foundation.md).

## Documentación

- [Requisitos](requirements.md) y [estado real](implementation-status.md).
- [Plan Maestro vigente](docs/decisions/PLAN_MAESTRO_AGENTE_DESARROLLO_GESTION_DOCUMENTAL_ACADEMICA.md) y [base de interfaz](docs/design/interface-foundations.md).
- [Índice documental](docs/README.md) y [contrato API](docs/api/foundation.md).
- [Pruebas y entorno](docs/development/local-setup.md).
- [Flujo Git](docs/development/git-workflow.md) y [reportes](docs/reports/README.md).

El mockup permanece congelado fuera del repositorio en `C:\Documentos\Documentos\Universidad UTA\Ingenieria De Software\Semestre 7\Gestion de Proyectos\Proyecto\Prototipos`.

Para probar con usuarios y catálogos ficticios sin cambiar la base principal: `./scripts/start-development-sample.ps1`. [Guía de ejemplo](docs/development/sample-environment.md) y [contrato del circuito documental](docs/api/document-circuit.md).

La entrega permite recorrer el circuito documental de desarrollo; no acredita todavía todo el Plan Maestro ni la preparación institucional para producción. Siguen pendientes cierre de períodos/ZIP, políticas temporales de flujos, capacidades administrativas adicionales, evidencias por referencias externas y servicios de asistencia/agente. [Estado y límites](implementation-status.md). HTTPS, SMTP, respaldo/restauración y validación de firma siguen siendo condiciones de operación. [Almacenamiento](docs/development/document-storage.md).
