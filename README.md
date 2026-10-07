# UTAPED — Gestión Documental Académica

Aplicación productiva modular para FISEI / Universidad Técnica de Ambato. Incluye espacio de trabajo rediseñado, identidad local, administración inicial, borradores T1, matriz, anexos privados, previsualización documental y firma criptográfica del elaborador.

## Arranque

Copiar .env.example a .env y configurar secretos únicos. Ejecutar `docker compose --profile web up --build -d` y abrir http://localhost:8081. El administrador inicial debe cambiar su contraseña temporal. [Guía completa](docs/development/local-setup.md).

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

T1 completo/T2, ejecución, revisión, aprobación y firmas de todos los participantes se entregarán en módulos posteriores. La firma actual del elaborador conserva un PDF independiente y todavía no ejecuta el envío a revisión exigido por el Plan Maestro. Carreras, pertenencias por período y titularidad de comisión requieren migración; el modelo actual de borradores individuales no se presenta como definitivo. [Almacenamiento y respaldo](docs/development/document-storage.md). HTTPS, SMTP, respaldos y políticas operativas requieren configuración y validación antes de publicación institucional.
