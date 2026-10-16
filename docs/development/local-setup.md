# Entorno local

Anexos y previsualizaciones requieren volumen documental persistente además de DB: [configuración y respaldo](document-storage.md). Docker incluye fuentes para el conversor T1; sin Docker instalar Liberation Sans/DejaVu Sans.

## Entorno diario recomendado

Desde raíz: `./scripts/start-development.ps1`; después `cd frontend` y `npm run dev` (primera instalación npm ci). URL http://127.0.0.1:5173. Para el compilado Docker: `./scripts/start-development.ps1 -Web`, URL 64431. Mismos datos. [Guía y credenciales](sample-environment.md). No iniciar otra copia local de la aplicación.

## Configuración base sin semillas (alternativa histórica)

Requisitos: Docker Desktop con contenedores Linux y Compose. No es necesario instalar Java/Maven para este modo.

1. En la raíz, copiar .env.example a .env y sustituir DB_PASSWORD y BOOTSTRAP_ADMIN_PASSWORD por valores únicos. Elegir correo del administrador; no publicar .env.
2. Para usar interfaz Docker establecer APP_PUBLIC_URL=http://localhost:64430.
3. Ejecutar `docker compose --profile web up --build -d`.
4. Abrir http://localhost:64430, ingresar con el correo/usuario configurado, contraseña y CAPTCHA; cambiar la contraseña temporal. Username inicial se deriva del prefijo del correo, que también sirve como alias.
5. Retirar los valores BOOTSTRAP_ADMIN_* del entorno después de provisionar. No son contraseña permanente: solo se usan con DB vacía.

El backend aplica Flyway al arrancar. El volumen conserva DB y sesiones. `docker compose down` detiene servicios conservando datos; no borrar volúmenes para solucionar errores ordinarios.

V12 agrega username/perfil y CAPTCHA sin borrar cuentas. No hay variables nuevas obligatorias; revisar identificadores migrados en el directorio. Las pruebas CAPTCHA fijan una respuesta únicamente mediante la BD aislada, sin bypass de aplicación. [Contrato](../api/local-access.md).

Puertos locales: web 64430, API 8080, DB 5433, bandeja Mailpit 8025. Todos vinculados a localhost. Compose es entorno local HTTP; no receta de publicación institucional. Producción exige HTTPS, COOKIE_SECURE=true, secretos externos, SMTP institucional, restricciones de red y plan de respaldo.

## Desarrollo frontend

Node 24. En `frontend`: `npm ci` (primera instalación) y `npm run dev`. Vite abre http://127.0.0.1:5173 y reenvía `/api` al backend. Solo iniciar Vite no arranca Java, PostgreSQL ni SMTP.

El destino se configura automáticamente y conserva los puertos de Docker:

1. `API_TARGET` de la terminal o de `frontend/.env.local`, si se establece explícitamente.
2. `http://127.0.0.1:<BACKEND_PORT>` usando únicamente BACKEND_PORT de `.env.sample`.
3. `http://127.0.0.1:18082` si no existe ese archivo.

El .env histórico de la raíz ya no selecciona la API de Vite. No se exponen secretos al navegador. Para Java directo, usar API_TARGET explícito (ejemplo http://127.0.0.1:8080); reiniciar Vite tras cambiarlo. Ante ECONNREFUSED, ejecutar primero start-development.ps1 y comprobar que no haya un API_TARGET antiguo en terminal/frontend/.env.local. El script ajusta APP_PUBLIC_URL al modo Vite/Docker para recuperación de acceso.

## Desarrollo backend sin Docker

Java 21; Maven Wrapper incluido. Configurar DB_URL, DB_USER, DB_PASSWORD, COOKIE_SECURE=false para HTTP local y SMTP_HOST/SMTP_PORT. DB del Compose local: jdbc:postgresql://localhost:5433/utaped.

En backend: `./mvnw spring-boot:run` o `./mvnw.cmd spring-boot:run` en Windows. No se verificó este modo directamente porque Java no está instalado en el host; compilación/ejecución se verificaron en Docker.

## Verificación

- Frontend: `npm run lint` y `npm run build`.
- Backend: `./scripts/test-backend.ps1`; DB utaped_test aislada sin puertos públicos. Prepara la caché Maven externa y limpia los recursos desechables al finalizar.
- E2E: `./scripts/test-e2e.ps1`; proyecto utaped-e2e y DB nueva por ejecución. Puertos 28081, 25433, 28025 y Vite 15173.
- Aceptación por roles/circuito: `./scripts/test-acceptance.ps1`; proyecto utaped-acceptance con frontend 24431. No escribe en el entorno manual.
- Los tres lanzadores limpian al finalizar y admiten -KeepOnFailure para diagnóstico. Resultados bajo frontend/test-results y backend/target. Nunca ejecutar las suites contra una base manual o institucional.

No hay envío hacia buzones reales en pruebas: Mailpit captura SMTP. SMTP institucional todavía debe configurarse para publicación.

AUDIT_EXPORT_MAX_ROWS limita filas de CSV administrativo (10000 por defecto, ajuste válido 1–100000). Superar el límite exige refinar filtros; no hay truncamiento silencioso. Notificaciones internas se consultan por sesión y no usan SMTP. No se habilita correo/push de avisos por esta configuración. La revisión documental se ejecuta con las asignaciones explícitas configuradas; sus datos no constituyen ratificación institucional.

## Firma local

La firma del elaborador requiere HTTPS por defecto y vinculación administrativa previa de su huella pública. Para desarrollo HTTP aislado puede habilitarse SIGNING_ALLOW_HTTP=true en el entorno local, nunca en un despliegue institucional. E2E lo configura en su entorno privado. No use certificados personales reales en pruebas. [Vinculación, límites y transporte](../api/author-signing.md).
