# Entorno local

Anexos y previsualizaciones requieren volumen documental persistente además de DB: [configuración y respaldo](document-storage.md). Docker incluye fuentes para el conversor T1; sin Docker instalar Liberation Sans/DejaVu Sans.

## Inicio rápido (entorno unificado)

Desde la raíz del repositorio, ejecutar:

- En Windows (PowerShell):
  ```powershell
  ./scripts/start-development.ps1
  ```
- En Linux / macOS (Bash):
  ```bash
  ./scripts/start-development.sh
  ```

El script genera automáticamente el archivo `.env` a partir de `.env.example` con contraseñas seguras si aún no existe, levanta PostgreSQL (`utaped`), Mailpit, backend y frontend compilado en Docker bajo el proyecto Compose `utaped`.

- **Web (Frontend compilado):** http://localhost:64430
- **API Backend:** http://localhost:18080 (Health / CSRF: http://localhost:18080/api/auth/csrf)
- **Bandeja Mailpit:** http://localhost:18025
- **Base de datos PostgreSQL:** `localhost:15433` (BD: `utaped`, usuario: `utaped`)

Para iniciar sesión, utilice cualquiera de las cuentas de desarrollo preconfiguradas con la contraseña `UTAPED_SEED_PASSWORD` (indicada en la consola y registrada en `.env`). [Guía y credenciales](sample-environment.md).

### Desarrollo frontend con Vite (opcional)

Si se desea modificar la interfaz React con recarga en caliente (HMR):

```powershell
./scripts/start-development.ps1 -Vite
```

Y en otra terminal:
```powershell
cd frontend
npm run dev
```

Vite abrirá en http://localhost:5173 y reenviará las llamadas `/api` al backend Docker (leyendo `BACKEND_PORT` automáticamente desde `.env`).

### Parámetros adicionales del lanzador

- `-PreparedDocuments`: Arranca cargando el lote de documentos preparados (Planes e Informes en distintos estados de avance y firma).
- `-ResetData`: Trunca las tablas operativas de la base de datos conservando las 7 cuentas de desarrollo y sus credenciales (incluso si fueron modificadas), eliminando usuarios temporales adicionales y regenerando catálogos semilla. Solicita confirmación interactiva previa.
- `-Force`: Omite la confirmación interactiva al reiniciar datos con `-ResetData` (útil para automatización).
- `-Stop`: Detiene todos los contenedores del entorno de desarrollo conservando datos y volúmenes.

## Configuración base sin semillas (producción o mínima)

Requisitos: Docker Desktop con contenedores Linux y Compose. No es necesario instalar Java/Maven para este modo.

1. En la raíz, configurar `.env` estableciendo `SPRING_PROFILES_ACTIVE=` (vacío) y contraseñas seguras.
2. Ejecutar `docker compose up --build -d`.
3. Abrir http://localhost:64430, ingresar con el usuario administrador inicial provisionado.
4. Retirar los valores `BOOTSTRAP_ADMIN_*` del entorno tras el primer arranque.

El backend aplica Flyway al arrancar. El volumen conserva DB y sesiones. `docker compose down` detiene servicios conservando datos; no borrar volúmenes para solucionar errores ordinarios.

Puertos locales por defecto: web 64430, API 18080, DB 15433, Mailpit web 18025 / SMTP 11025. Todos vinculados a localhost para evitar colisiones. Compose es entorno local HTTP; no receta de publicación institucional. Producción exige HTTPS, COOKIE_SECURE=true, secretos externos, SMTP institucional, restricciones de red y plan de respaldo.

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

La firma electrónica del elaborador y de las etapas de revisión utiliza certificados criptográficos reales y requiere HTTPS por defecto. La firma no requiere vinculación administrativa previa: el usuario aporta directamente su contenedor `.p12`/`.pfx` con su contraseña en el diálogo de firma. Para desarrollo HTTP aislado se habilita `SIGNING_ALLOW_HTTP=true` en el entorno local (`.env`), nunca en un despliegue institucional. E2E lo configura en su entorno privado. No use certificados personales reales en pruebas. [Límites criptográficos y transporte](../api/author-signing.md).
