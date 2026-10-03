# Entorno local

Anexos y previsualizaciones requieren volumen documental persistente además de DB: [configuración y respaldo](document-storage.md). Docker incluye fuentes para el conversor T1; sin Docker instalar Liberation Sans/DejaVu Sans.

## Arranque completo con Docker

Requisitos: Docker Desktop con contenedores Linux y Compose. No es necesario instalar Java/Maven para este modo.

1. En la raíz, copiar .env.example a .env y sustituir DB_PASSWORD y BOOTSTRAP_ADMIN_PASSWORD por valores únicos. Elegir correo del administrador; no publicar .env.
2. Para usar interfaz Docker establecer APP_PUBLIC_URL=http://localhost:8081.
3. Ejecutar `docker compose --profile web up --build -d`.
4. Abrir http://localhost:8081, ingresar con los valores configurados y cambiar la contraseña temporal.
5. Retirar los valores BOOTSTRAP_ADMIN_* del entorno después de provisionar. No son contraseña permanente: solo se usan con DB vacía.

El backend aplica Flyway al arrancar. El volumen conserva DB y sesiones. `docker compose down` detiene servicios conservando datos; no borrar volúmenes para solucionar errores ordinarios.

Puertos locales: web 8081, API 8080, DB 5433, bandeja Mailpit 8025. Todos vinculados a localhost. Compose es entorno local HTTP; no receta de publicación institucional. Producción exige HTTPS, COOKIE_SECURE=true, secretos externos, SMTP institucional, restricciones de red y plan de respaldo.

## Desarrollo frontend

Node 24. En frontend: `npm ci`, `npm run dev`. Vite en http://localhost:5173 comunica /api con localhost:8080. Para recuperar acceso en ese modo establecer APP_PUBLIC_URL=http://localhost:5173 en backend y recrearlo.

## Desarrollo backend sin Docker

Java 21; Maven Wrapper incluido. Configurar DB_URL, DB_USER, DB_PASSWORD, COOKIE_SECURE=false para HTTP local y SMTP_HOST/SMTP_PORT. DB del Compose local: jdbc:postgresql://localhost:5433/utaped.

En backend: `./mvnw spring-boot:run` o `./mvnw.cmd spring-boot:run` en Windows. No se verificó este modo directamente porque Java no está instalado en el host; compilación/ejecución se verificaron en Docker.

## Verificación

- Frontend: `npm run lint` y `npm run build`.
- Backend: desde raíz, `docker compose -f compose.test.yml up --abort-on-container-exit --exit-code-from tests --attach tests`; después `docker compose -f compose.test.yml down`. DB utaped_test aislada y sin puertos públicos; credenciales del archivo son ficticias de pruebas.
- Windows: scripts/test-backend.ps1 ejecuta y limpia contenedores de pruebas.
- E2E: scripts/test-e2e.ps1 genera .env.e2e local e inicia proyecto utaped-e2e separado, instala Chromium y ejecuta frontend/tests/e2e. Puertos 18080, 15433, 18025 y 15173. Los datos E2E son ficticios, pueden conservarse entre ejecuciones; el script borra solo contadores de intentos de esa DB de pruebas.

No hay envío hacia buzones reales en pruebas: Mailpit captura SMTP. SMTP institucional todavía debe configurarse para publicación.

AUDIT_EXPORT_MAX_ROWS limita filas de CSV administrativo (10000 por defecto, ajuste válido 1–100000). Superar el límite exige refinar filtros; no hay truncamiento silencioso. Notificaciones internas se consultan por sesión y no usan SMTP. No se habilita correo/push de avisos ni firma/revisión institucional por esta configuración.
