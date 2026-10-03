# Reporte — Base productiva y acceso local

Inicio: 2026-10-02. Finalización: 2026-10-03. Rama: feature/project-foundation, creada desde develop. Repositorio: C:\Documentos\D-Proyectos\Git\utaped.

## Resultado

Implementación de la base técnica y primer módulo de identidad/acceso. Stack seleccionado por delegación del propietario: React 19.2.8, TypeScript 6.0.2, Vite 8.3.2, Java 21, Spring Boot 4.1.1, PostgreSQL 18.6 y Flyway. Monolito modular con frontend separado; Spring JDBC, Spring Security y sesiones JDBC. [ADR con fuentes y alternativas](../architecture/adr/0001-production-foundation.md).

Se investigaron documentación oficial y registros de paquetes; se verificó compatibilidad mediante compilación, migraciones y ejecución real. Maven/Java se ejecutan en Docker porque no están instalados en el host. Las versiones directas de npm y su lockfile están fijadas. No se incorporan microservicios, Redis, JWT, ORM o servicios de pago como requisitos del primer módulo.

## Funcionalidad entregada

- Autenticación local real, sesión persistida en DB, cookie HttpOnly/SameSite, CSRF y logout. Secure por defecto en backend; desactivado únicamente en Compose local HTTP.
- Bcrypt con prefijo de algoritmo y validación de longitud; cambio obligatorio de contraseña temporal; cambio/reset invalidan sesiones anteriores.
- Bootstrap administrativo explícito con secretos externos, solo con DB vacía; alta y activación/desactivación de usuarios; protección del último administrador activo.
- Separación entre permisos globales ADMIN/USER y pertenencia MEMBER/COORDINATOR; cambio de contexto conserva identidad. Verificación actualizada de usuario y rol en servidor.
- Grupos, asignación/retiro de integrantes, consulta restringida al ámbito del usuario; alta/consulta de períodos y validación de rangos/fechas civiles.
- Recuperación SMTP asíncrona con respuesta genérica, token aleatorio, hash persistido, expiración y un solo uso. Mailpit captura mensajes locales; ninguna prueba envía correo a buzones reales.
- Límites de intentos persistidos, limpieza periódica de tokens/contadores expirados y auditoría de accesos, contraseñas y cambios administrativos. Pertenencias auditadas incluyen usuario afectado y grupo.
- UI española, errores y estados vacíos, formularios administrativos y adaptación desktop/móvil.

## Archivos y operación

backend contiene módulos identity, institution, audit y shared; tres migraciones versionadas. frontend organiza identity, institution y shared. Dockerfiles y Compose permiten ejecutar backend/DB/correo y frontend Nginx con origen común. .env.example no contiene credenciales reales; .env.e2e y artefactos de prueba permanecen ignorados.

Se actualizaron AGENTS.md, requisitos, catálogo y estado. Se añadieron contrato API, guía local, decisión tecnológica, flujo Git, scripts y workflow de verificación para PRs a develop/main. Se preservó el mockup sin modificaciones.

## Validación ejecutada

- Backend: 13 pruebas exitosas (12 de integración HTTP/PostgreSQL y una de contexto), cero fallos. Migraciones comprobadas sobre DB aislada utaped_test, incluyendo V3.
- Casos: CSRF, login erróneo, identidad, cookie HttpOnly, logout, aislamiento por grupo, retiro de pertenencia, restricciones administrativas/contraseña temporal, cambio y revocación de sesiones, baja, último admin, duplicados, rangos, reset usado/expirado, SMTP y límites de intentos.
- Frontend: TypeScript/build y lint exitosos; npm audit de dependencias de producción sin vulnerabilidades reportadas. Esto no certifica ausencia de vulnerabilidades del sistema.
- Playwright: escenario integral exitoso con servidor Vite y con imagen Docker/Nginx: alta de usuario/grupo/período, asignación, contraseña temporal, contexto, recarga de sesión, logout y recuperación por correo.
- Inspección visual de capturas desktop/móvil y comprobación de ausencia de desbordamiento horizontal en 390 px. Se corrigieron codificación UTF-8, nombres accesibles de selects y navegación por fragmento al recuperar acceso.
- Docker: imágenes frontend/backend construidas y arranque local verificado. Workflow validado con actionlint; aún no ejecutado en GitHub.
- Revisión final de enlaces documentales, JSON, exclusión de secretos/artefactos y Git. No se efectuaron commit, push, PR o despliegue público.

## Límites y siguiente entrega

Esta entrega completa la base técnica acordada, no todo UTAPED. T1/T2, firma real, revisión, ejecución/evidencias, importación masiva, visor de auditoría y administración avanzada siguen pendientes. Listado inicial de usuarios limitado a 200; roles de pertenencia personalizados y edición general todavía no están implementados.

SMTP institucional, HTTPS, respaldo/restauración, red, retención y política final requieren preparación antes de producción. La cola de recuperación es acotada y no durable; ante interrupción se solicita nuevo enlace. Límites por dirección remota requieren evaluar proxy confiable y capacidad institucional. No se implementa CAPTCHA simulado como medida productiva.

Siguiente módulo propuesto: borrador T1 persistido con aislamiento por documento; resolver titularidad/duplicados y pasos antes de cerrar su contrato. Mantener PR hacia develop; main se reserva para una versión terminada.
