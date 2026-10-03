# UTAPED — Gestión Documental Académica

Aplicación productiva modular para FISEI / Universidad Técnica de Ambato. Primera entrega: identidad local y administración básica de usuarios, grupos, pertenencias y períodos.

## Arranque

Copiar .env.example a .env y configurar secretos únicos. Ejecutar `docker compose --profile web up --build -d` y abrir http://localhost:8081. El administrador inicial debe cambiar su contraseña temporal. [Guía completa](docs/development/local-setup.md).

## Stack

React/TypeScript/Vite; Java 21/Spring Boot; PostgreSQL/Flyway; sesiones JDBC y Spring Security. [Decisión y fuentes](docs/architecture/adr/0001-production-foundation.md).

## Documentación

- [Requisitos](requirements.md) y [estado real](implementation-status.md).
- [Índice documental](docs/README.md) y [contrato API](docs/api/foundation.md).
- [Pruebas y entorno](docs/development/local-setup.md).
- [Flujo Git](docs/development/git-workflow.md) y [reportes](docs/reports/README.md).

El mockup permanece congelado fuera del repositorio en `C:\Documentos\Documentos\Universidad UTA\Ingenieria De Software\Semestre 7\Gestion de Proyectos\Proyecto\Prototipos`.

T1/T2, ejecución, firma real y aprobación documental se entregarán en módulos posteriores. Esta base no certifica despliegue institucional: HTTPS, SMTP institucional, respaldos y políticas operativas requieren configuración y validación.
