# Simplificación de la ejecución local de UTAPED y unificación del entorno

Fecha: 01/11/2026.
Objetivo: Implementar la simplificación de ejecución local especificada en `instruction.md`, unificando el entorno diario en el proyecto Docker `utaped` y base de datos `utaped`, estandarizando en el archivo `.env`, permitiendo el arranque completo con un solo comando sin requerir dependencias en el host, y preservando íntegramente los datos y archivos documentales existentes con verificación SHA-256.

## 1. Motivación y decisiones

1. **Unificación de proyecto y base de datos:** Se eliminó la divergencia entre el proyecto histórico `utaped` y el entorno de desarrollo `utaped-sample`. El entorno diario estándar ahora es el proyecto Compose `utaped` con base de datos `utaped`.
2. **Estandarización de variables de entorno:** El archivo operativo principal es `.env`. La plantilla versionada en el repositorio es `.env.example`. Si `.env` no existe, el script de arranque lo genera automáticamente con contraseñas aleatorias seguras (CSPRNG) para la base de datos y las semillas. Si ya existe, se preservan intactas todas sus configuraciones y claves.
3. **Arranque en un solo comando sin herramientas de host:** `./scripts/start-development.ps1` (Windows PowerShell) y `./scripts/start-development.sh` (Linux / macOS POSIX) levantan por defecto los 4 servicios esenciales: PostgreSQL, Mailpit, Backend Spring Boot y Frontend React compilado en Nginx (puerto 64430). No se requiere tener instalados Node.js, Java ni Maven en la máquina anfitriona.
4. **Flujo de desarrollo interactivo conservado:** Se conserva la opción de ejecutar Vite con HMR mediante el flag `-Vite` (`./scripts/start-development.ps1 -Vite`) y `npm run dev` en `frontend/` (puerto 5173). `vite.config.ts` lee automáticamente `BACKEND_PORT` desde `.env`.
5. **Carga opcional de documentos preparados:** Las semillas institucionales y los 7 usuarios de desarrollo (`dev.admin`, `dev.principal`, etc.) se cargan por defecto con el perfil `development-seed`. La carga de documentos preparados con firmas efímeras permanece opcional bajo el parámetro `-PreparedDocuments`.

## 2. Respaldo y migración verificada de datos

Antes de alterar los contenedores o volúmenes existentes, se procedió a la salvaguarda y validación estricta:

1. **Respaldo con hash SHA-256:**
   - Ejecutado: `python scripts/operations.py backup --project utaped-sample --destination backups/transition-20261101-sample`.
   - Resultado: 8 planes de trabajo, 7 usuarios de aplicación, 16 artefactos, 5 artefactos firmados, 26 archivos físicos (730 MB) y 47 versiones de migración Flyway respaldados.
   - Verificación con `python scripts/operations.py verify`: `verified: true`, sumas SHA-256 de base y archivos 100% coincidentes.
2. **Ensayo de restauración aislado:**
   - Se levantó un proyecto efímero `utaped-restore-verify20261101` para comprobar la viabilidad y fidelidad de la restauración sobre una base limpia. Los 26 archivos y 8 documentos se restauraron de forma idéntica. El proyecto efímero fue eliminado tras la verificación.
3. **Migración a los volúmenes definitivos de `utaped`:**
   - Se crearon los volúmenes `utaped_postgres-data` y `utaped_document-data`.
   - Se extrajo el archivo documental `documents.tar.gz` en `utaped_document-data`.
   - Se restauró `database.dump` mediante `pg_restore` en la base de datos `utaped`.
   - Los volúmenes originales de `utaped-sample` se conservaron intactos como salvaguarda histórica adicional.

## 3. Modificaciones técnicas realizadas

- **`backend/.../DevelopmentSeed.java` y `DevelopmentDocuments.java`:**
  - Se actualizaron las guardas de seguridad para permitir la ejecución en `utaped` además de `utaped_dev` (`Set.of("utaped", "utaped_dev")`), impidiendo la ejecución accidental sobre bases de prueba (`utaped_test`) o producción.
  - Guarda de cookies seguras: `DevelopmentSeed` aborta si `COOKIE_SECURE=true`.
  - Guarda de cuentas administrativas reales: tanto `DevelopmentSeed` como `DevelopmentDocuments` verifican mediante SQL que no existan administradores reales (cuya dirección de correo no pertenezca a `@utaped.example.invalid`) antes de aplicar semillas o documentos de prueba.
  - Se anotó `@Autowired` en el constructor de inyección de `DevelopmentSeed` garantizando compatibilidad con Spring Boot.
- **`compose.yml` y `compose.production.yml`:**
  - En `compose.yml`, `SPRING_PROFILES_ACTIVE` por defecto queda vacío (`${SPRING_PROFILES_ACTIVE:-}`).
  - En `compose.production.yml`, se anula explícitamente cualquier perfil de desarrollo: `SPRING_PROFILES_ACTIVE: ""`, `UTAPED_SEED_PASSWORD: ""`, `APP_DEVELOPMENT_DOCUMENTS: "false"`.
  - Se retiró el perfil `web` para que el servicio `frontend` inicie por defecto en `docker compose up`. Se mapearon puertos con variables y valores por defecto seguros para evitar colisiones con servicios locales preexistentes (`DB_PORT:-15433`, `BACKEND_PORT:-18080`, `MAIL_UI_PORT:-18025`, `MAIL_SMTP_PORT:-11025`, `WEB_PORT:-64430`).
- **`.env` y `.env.example`:**
  - Se configuró `SIGNING_ALLOW_HTTP=true` para permitir el flujo de firma local bajo HTTP en el entorno de desarrollo.
  - Se preservaron las claves y contraseñas del usuario, definiendo `UTAPED_SEED_PASSWORD`, `SPRING_PROFILES_ACTIVE=development-seed`, `UTAPED_SEED_DOCUMENTS=false`, `SMTP_HOST=mail` y `SMTP_PORT=1025` (Mailpit).
- **`scripts/start-development.ps1` y `scripts/start-development.sh`:**
  - Administran el proyecto `utaped`, generan `.env` automáticamente a partir de `.env.example` con contraseñas seguras si no existe, soportan `-Vite`, `-PreparedDocuments`, `-ResetData`, `-Force` y `-Stop`.
  - El flag `-ResetData` fue reforzado integralmente:
    - Verificación estructurada de la configuración efectiva de Docker mediante `docker compose config --format json` (analizando directamente el objeto deserializado en PowerShell para evitar la trampa de evaluación booleana de colecciones en `-notmatch`, asegurando `SPRING_PROFILES_ACTIVE=development-seed` y rechazando `COOKIE_SECURE=true`).
    - Solicitud interactiva de confirmación (escribir `CONFIRMAR`) antes de realizar el truncamiento destructivo, evitable únicamente con el flag explícito `-Force` / `--force`.
    - Preservación estricta de las 7 cuentas de desarrollo y sus contraseñas: se truncan todas las tablas operativas de `public` excepto `app_user` y `flyway_schema_history`, y se eliminan únicamente los usuarios adicionales no semilla (`DELETE FROM app_user WHERE id NOT IN (...)`). Las credenciales personalizadas de los 7 usuarios semilla se conservan intactas.
    - Guarda en base de datos PostgreSQL: verifica pertenencia a `utaped`/`utaped_dev`, aborta si existen administradores que no pertenezcan al dominio de ejemplo ficticio (`@utaped.example.invalid`), y aborta si no existe la cuenta semilla `admin@utaped.example.invalid` (garantizando que la base sea efectivamente un entorno de desarrollo).
  - El script `scripts/start-development.sh` fue normalizado con finales de línea LF (Unix) e incorporado en el índice de Git con modo ejecutable `100755` (`git ls-files --stage`).
- **`scripts/clean-development-data.py`:**
  - Exige coincidencia estricta del proyecto de respaldo (`manifest["sourceProject"] == project`) para impedir la limpieza o restauración accidental con un respaldo ajeno.
- **`frontend/vite.config.ts`:** Actualizado para leer `BACKEND_PORT` primero de `../.env` y en su defecto de `../.env.sample`.
- **`README.md`, `docs/development/local-setup.md`, `docs/development/sample-environment.md`:**
  - Documentación completamente alineada al nuevo estándar; el `README.md` especifica Node.js 24+ y aclara el comportamiento de `docker compose up`.
  - Distinción en `README.md` entre captura SMTP saliente (Mailpit) y notificaciones internas in-app (consultadas por sesión HTTP).
  - Eliminación de referencias contradictorias en `local-setup.md` y `sample-environment.md` sobre vinculación administrativa previa de certificados, alineándolas con el flujo real donde el elaborador/revisor aporta directamente su archivo `.p12`/`.pfx` en el diálogo de firma.
  - Corrección en `sample-environment.md` para instruir respaldos sobre `--project utaped` en lugar de `utaped-sample`.

## 4. Verificaciones ejecutadas

- **Registro y verificación de `scripts/start-development.sh`:** Comprobado mediante `git ls-files --stage scripts/start-development.sh` (modo `100755`). Verificado que sus finales de línea sean LF. Comprobada la sintaxis (`bash -n`) y ejecución de argumentos dentro del contenedor Linux `debian:bookworm-slim`.
- **Generación de `.env` en clon limpio:** Comprobado mediante prueba automatizada en Python. Genera contraseñas criptográficas seguras, asigna `SIGNING_ALLOW_HTTP=true` y `SPRING_PROFILES_ACTIVE=development-seed`.
- **Arranque de contenedores:** Los 4 contenedores (`utaped-db-1`, `utaped-mail-1`, `utaped-backend-1`, `utaped-frontend-1`) levantaron y alcanzaron estado saludable (`healthy`).
- **Endpoint de backend y CSRF:** `http://127.0.0.1:18080/api/auth/csrf` respondió HTTP 200 con token CSRF válido.
- **Frontend compilado:** `http://127.0.0.1:64430/` respondió HTTP 200 con el HTML de la aplicación.
- **Ciclo de parada y re-arranque:** `./scripts/start-development.ps1 -Stop` detuvo los contenedores; el segundo arranque fue inmediato, conservó datos y contraseñas.
- **Entrega de correo local en Mailpit:** Se disparó la solicitud de recuperación de contraseña en `/api/auth/forgot-password` para `principal@utaped.example.invalid`, verificando en la API de Mailpit (`http://127.0.0.1:18025/api/v1/messages`) la recepción del correo ("Recuperación de acceso a UTAPED") con código de verificación válido.
- **Aptitud para firma bajo HTTP:** Se verificó el endpoint `/api/work-plans/{plan_id}/artifacts/readiness` con sesión autenticada como `dev.principal`. El endpoint reportó `canSign: true` y 0 bloqueadores por falta de HTTPS gracias a `SIGNING_ALLOW_HTTP=true`.
- **Linter y construcción de frontend:**
  - `npm --prefix frontend run lint` (oxlint): 0 errores, 0 advertencias en 133 archivos.
  - `npm --prefix frontend run build` (tsc -b && vite build): compilación exitosa sin errores.
- **Pruebas unitarias de scripts:** `python -m unittest scripts/tests/test_development_cleanup.py` (5 pruebas aprobadas).
- **Prueba de `-ResetData` en contenedor desechable aislado:**
  - Se levantó un contenedor PostgreSQL efímero (`utaped-reset-disposable-test`) restaurando las 58 tablas del esquema oficial de UTAPED.
  - Se insertaron los 7 usuarios de desarrollo, un usuario temporal ajeno (`extra_custom@other.invalid`) y un período académico operativo.
  - Se modificó la contraseña de `dev.principal` estableciendo un hash personalizado (`CUSTOM_CHANGED_HASH_TEST_2026`).
  - Se ejecutó el bloque SQL de `-ResetData`:
    - Tablas operativas truncadas (períodos pasaron de 1 a 0).
    - Usuario temporal eliminado exitosamente (usuarios pasaron de 8 a 7).
    - Las 7 cuentas de desarrollo sobrevivieron y el hash personalizado de `dev.principal` permaneció intacto (`CUSTOM_CHANGED_HASH_TEST_2026`).
    - Guardas de seguridad validadas: inserción de administrador real y eliminación de `dev.admin` provocaron el aborto inmediato del reinicio con excepción.
  - Contenedor efímero destruido automáticamente tras el ensayo.
- **Suite de integración backend:** Ejecutada mediante `./scripts/test-backend.ps1` sobre base de datos aislada `utaped_test` en Docker. Resultado: **368 pruebas ejecutadas, 0 fallos, 0 errores (100% aprobadas)**. Limpieza automática de recursos efímeros verificada.
- **Delimitación de entornos de prueba:** Las pruebas de scripts PowerShell y Docker Compose se ejecutaron sobre el host Windows 11; la verificación de sintaxis y compatibilidad POSIX de `start-development.sh` se ejecutó sobre un contenedor Linux Debian; las pruebas backend e integración se ejecutaron en contenedores Docker Linux (`utaped-tests-tests-1`). No se realizaron ejecuciones en hardware macOS físico.

