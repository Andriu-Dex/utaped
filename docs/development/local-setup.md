# Entorno local

Anexos y previsualizaciones requieren volumen documental persistente además de DB: [configuración y respaldo](document-storage.md). Docker incluye fuentes para el conversor T1; sin Docker instalar Liberation Sans/DejaVu Sans.

## Arranque completo con Docker

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
2. `http://127.0.0.1:<BACKEND_PORT>` usando `BACKEND_PORT` del `.env` de la raíz (o del entorno del proceso).
3. `http://127.0.0.1:8080` si no hay configuración, igual que el puerto predeterminado de Compose.

Con `BACKEND_PORT=18080` en el `.env` principal basta ejecutar `npm run dev`, sin exportar variables cada vez. Para un backend Java iniciado directamente en otro puerto, copiar `frontend/.env.example` a `frontend/.env.local` y establecer, por ejemplo, `API_TARGET=http://127.0.0.1:8080`. Este archivo local permanece ignorado; no usar el prefijo `VITE_` ni colocar secretos en él. Reiniciar Vite después de cambiar estos valores.

Si aparece `ECONNREFUSED`, verificar primero que el backend esté iniciado y que su puerto coincida. Desde la raíz: `docker compose up -d db mail backend`. Para recuperación de acceso desde Vite, configurar `APP_PUBLIC_URL=http://127.0.0.1:5173` en backend y recrearlo; el puerto de la interfaz Docker/institucional es independiente del proxy de desarrollo.

## Desarrollo backend sin Docker

Java 21; Maven Wrapper incluido. Configurar DB_URL, DB_USER, DB_PASSWORD, COOKIE_SECURE=false para HTTP local y SMTP_HOST/SMTP_PORT. DB del Compose local: jdbc:postgresql://localhost:5433/utaped.

En backend: `./mvnw spring-boot:run` o `./mvnw.cmd spring-boot:run` en Windows. No se verificó este modo directamente porque Java no está instalado en el host; compilación/ejecución se verificaron en Docker.

## Verificación

- Frontend: `npm run lint` y `npm run build`.
- Backend: desde raíz, `docker compose -f compose.test.yml up --abort-on-container-exit --exit-code-from tests --attach tests`; después `docker compose -f compose.test.yml down`. DB utaped_test aislada y sin puertos públicos; credenciales del archivo son ficticias de pruebas.
- Windows: scripts/test-backend.ps1 ejecuta y limpia contenedores de pruebas.
- E2E: scripts/test-e2e.ps1 genera .env.e2e local e inicia proyecto utaped-e2e separado, instala Chromium y ejecuta frontend/tests/e2e. Puertos 28081, 25433, 28025 y 15173 (web Docker opcional 24430). Los datos E2E son ficticios, pueden conservarse entre ejecuciones; el script borra solo contadores de intentos de esa DB de pruebas.

No hay envío hacia buzones reales en pruebas: Mailpit captura SMTP. SMTP institucional todavía debe configurarse para publicación.

AUDIT_EXPORT_MAX_ROWS limita filas de CSV administrativo (10000 por defecto, ajuste válido 1–100000). Superar el límite exige refinar filtros; no hay truncamiento silencioso. Notificaciones internas se consultan por sesión y no usan SMTP. No se habilita correo/push de avisos por esta configuración. La revisión documental se ejecuta con las asignaciones explícitas configuradas; sus datos no constituyen ratificación institucional.

## Firma local

La firma del elaborador requiere HTTPS por defecto y vinculación administrativa previa de su huella pública. Para desarrollo HTTP aislado puede habilitarse SIGNING_ALLOW_HTTP=true en el entorno local, nunca en un despliegue institucional. E2E lo configura en su entorno privado. No use certificados personales reales en pruebas. [Vinculación, límites y transporte](../api/author-signing.md).
