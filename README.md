# UTAPED — Gestión Documental Académica

Sistema modular para la gestión, elaboración, revisión secuencial con firmas digitales y archivo histórico de planes de trabajo (T1) e informes finales (T2) en la FISEI / Universidad Técnica de Ambato.

## Requisitos previos

- **Git**: para clonar el repositorio.
- **Docker** y **Docker Compose**: versión con soporte de Compose v2.
  - Windows: Docker Desktop con motor WSL2 / Linux Containers activo.
  - Linux: Docker Engine y plugin `docker-compose-plugin`.
  - macOS: Docker Desktop para Mac.
- **Puertos locales utilizados por defecto**:
  - `64430`: Aplicación web (interfaz compilada en Nginx).
  - `18080`: Backend API (Spring Boot).
  - `15433`: PostgreSQL 18.
  - `18025`: Interfaz web de Mailpit (buzón local de correos).
  - `11025`: Servidor SMTP local de Mailpit.
  *(Todos los puertos son configurables en `.env`)*.

> [!NOTE]
> Para el uso estándar y la evaluación no es necesario tener instalados Node.js, Java ni Maven en el equipo anfitrión; la aplicación completa se construye y ejecuta dentro de contenedores Docker.

---

## Inicio rápido (Recomendado)

### 1. Clonar el repositorio
```bash
git clone https://github.com/Andriu-Dex/utaped.git
cd utaped
```

### 2. Iniciar la aplicación
En el primer arranque se genera automáticamente un archivo `.env` local con contraseñas criptográficas seguras y se levantan los servicios:

- **En Windows (PowerShell):**
  ```powershell
  ./scripts/start-development.ps1
  ```
- **En Linux o macOS (Bash):**
  ```bash
  ./scripts/start-development.sh
  ```

> [!NOTE]
> **Alternativa directa con Docker Compose:**
> Docker Compose no genera automáticamente el archivo `.env`. Si prefiere no usar los scripts de inicio rápido recomendados, copie primero `.env.example` a `.env` (`cp .env.example .env`) y configure las contraseñas requeridas (`DB_PASSWORD` y `UTAPED_SEED_PASSWORD`) antes de ejecutar `docker compose up -d --build`.

La primera construcción compilará las imágenes de backend (Java 21) y frontend. Al finalizar, la aplicación estará disponible en:
👉 **http://127.0.0.1:64430**

---

## Cuentas de desarrollo y primer acceso

El entorno local se inicializa con el catálogo oficial y siete cuentas de desarrollo para recorrer los diferentes roles del circuito:

| Usuario | Rol del circuito | Función |
|---|---|---|
| `dev.admin` | Administrador | Asignación de flujos, catálogos institucionales y certificados. |
| `dev.principal` | Elaborador | Creación y edición de planes T1 e informes T2. |
| `dev.coordinator` | Coordinador | Coordinación académica y prórrogas. |
| `dev.reviewer` | Revisor | Primera etapa de revisión documental. |
| `dev.approver` | Aprobador | Aprobación final y cierre de etapa. |
| `dev.member` | Integrante | Responsable de actividades asignadas. |
| `dev.outsider` | Sin ámbito | Usuario de prueba para verificar aislamiento de permisos. |

### Contraseña de acceso
En el primer inicio, el lanzador genera una contraseña aleatoria segura que se asigna a todas las cuentas `dev.*`. Para consultarla:
- Abra su archivo local `.env` (ignorado por Git).
- Busque el valor de `UTAPED_SEED_PASSWORD`.
- Inicie sesión en la pantalla de acceso con el usuario deseado, dicha contraseña y el código CAPTCHA de seguridad.

---

## Opciones del entorno

### Documentos preparados de ejemplo (Opcional)
Para incluir planes de trabajo y documentos de prueba ya elaborados y en distintas etapas del flujo, agregue el parámetro `-PreparedDocuments`:
```powershell
./scripts/start-development.ps1 -PreparedDocuments
```
*(En Linux/macOS: `./scripts/start-development.sh --prepared-documents`)*.
Esta opción es **idempotente**: ejecutarla no borra ni duplica planes ya existentes.

### Correo saliente de prueba (Mailpit)
Los correos electrónicos salientes del sistema (como los enlaces y códigos de recuperación de acceso) se capturan localmente en Mailpit para evitar envíos a servidores reales durante el desarrollo. Las notificaciones internas del sistema se gestionan dentro de la propia aplicación mediante sesión web y no utilizan SMTP:
👉 **Bandeja de correo local (Mailpit):** **http://127.0.0.1:18025**
*(Si desea probar entrega externa SMTP real, configure las variables `SMTP_*` en su archivo `.env`)*.

### Detener el sistema conservando los datos
Para detener los contenedores sin perder sus planes, documentos ni configuraciones:
```powershell
./scripts/start-development.ps1 -Stop
```
*(En Linux/macOS: `./scripts/start-development.sh --stop`, o `docker compose stop`)*.

---

## Desarrollo activo de Frontend con Vite (Opcional)

Si desea modificar el código de la interfaz con recarga instantánea en caliente (HMR):
1. Inicie los servicios de backend y base de datos con el flag `-Vite`:
   ```powershell
   ./scripts/start-development.ps1 -Vite
   ```
2. En otra terminal, ingrese a la carpeta frontend con Node 24+ y ejecute:
   ```bash
   cd frontend
   npm ci
   npm run dev
   ```
3. Abra **http://127.0.0.1:5173**. Vite redirigirá automáticamente las llamadas `/api` al backend local.

---

## Pruebas y verificación

El repositorio incluye suites de pruebas completamente aisladas que nunca escriben sobre su base de datos local:
- **Pruebas de integración del backend:** `./scripts/test-backend.ps1` (usa un contenedor descartable `utaped-tests`).
- **Pruebas de aceptación E2E:** `./scripts/test-acceptance.ps1` (usa un entorno temporal `utaped-acceptance`).
- **Verificación frontend:** `npm --prefix frontend run lint` y `npm --prefix frontend run typecheck`.

---

## Documentación del proyecto

- [Requisitos formales](requirements.md) y [Estado de implementación](implementation-status.md).
- [Configuración del entorno local](docs/development/local-setup.md).
- [Referencia y catálogo institucional](docs/development/institutional-reference.md).
- [Auditoría y reportes técnicos](docs/reports/README.md).
- [Operación institucional y puesta en marcha](docs/operations/production-readiness.md).
