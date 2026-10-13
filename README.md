# UTAPED — Gestión Documental Académica

Aplicación productiva modular para FISEI / Universidad Técnica de Ambato. Incluye identidad local, administración por ámbitos, Planes T1, revisión secuencial con firmas, evidencias e Informes T2 derivados, versiones formales, flujos por período, cierre histórico y reportes de gestión. La interfaz conserva una base visual común para PC y tabletas, con modo claro y oscuro.

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

Administración asigna T1/T2 a cada ámbito y activa sus flujos antes de nuevas firmas. El cierre conserva datos y genera un archivo histórico; la descarga respeta los permisos documentales. [Configuración y operación](docs/api/period-control.md).

La entrega permite recorrer el circuito documental de desarrollo, incluyendo referencias HTTPS versionadas y bandeja de revisión paginada. La asistencia de redacción T1/T2 está integrada y deshabilitada hasta [configurar el proveedor](docs/development/writing-configuration.md). El propietario construye el agente de código/Trello por separado.

No acredita todavía todo el Plan Maestro ni la preparación institucional para producción. Siguen pendientes retención/reapertura, sustitución de ejecutores y capacidades administrativas adicionales. [Estado y límites](implementation-status.md). HTTPS, SMTP, respaldo/restauración y aceptación de firma siguen siendo condiciones de operación. [Almacenamiento](docs/development/document-storage.md).
