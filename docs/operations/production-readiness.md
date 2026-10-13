# Preparación operativa: HTTPS, SMTP y recuperación

Entrega 2026-10-12. Configuración preparada y probada en entornos aislados; no se desplegó el proyecto principal ni se declaró aprobación institucional.

## Datos que debe entregar el operador/DTIC

- Nombre DNS o IP de acceso autorizado y certificado HTTPS válido para esa identidad, cadena completa y clave privada correspondiente. No confundir este certificado del servidor con los .p12/.pfx personales de firma.
- Permiso de entrada TCP 64430 hacia el servidor y condiciones de salida hacia SMTP/proveedor de redacción. No se requieren puertos públicos de PostgreSQL, backend o Mailpit.
- SMTP real: host, puerto, modalidad STARTTLS o SSL implícito, autenticación/remitente autorizado y credenciales si aplica. No se envió correo a terceros durante esta entrega.
- Ubicación protegida de respaldos, custodio, cifrado/ACL, copia fuera del servidor y frecuencia/retención. RPO/RTO y retención son decisiones operativas/institucionales pendientes; no se inventa una política definitiva.

## Archivo privado de producción

Copiar `.env.production.example` a `.env.production` en la raíz de UTAPED y sustituir los ejemplos. No editar `.env`/`.env.sample` para convertir los entornos habituales en producción. Python 3.11+, Docker Compose 2.24.4+ y OpenSSL son requisitos de las herramientas operativas; Python no interviene en el runtime del aplicativo.

```dotenv
DB_PASSWORD=<secreto privado de al menos 20 caracteres>
WEB_PORT=64430
WEB_BIND_ADDRESS=0.0.0.0
APP_PUBLIC_URL=https://<nombre del servidor>:64430
TLS_CERTIFICATE_FILE=C:/private/utaped/fullchain.pem
TLS_PRIVATE_KEY_FILE=C:/private/utaped/private.key
SMTP_HOST=<host autorizado>
SMTP_PORT=587
SMTP_USERNAME=<usuario si aplica>
SMTP_PASSWORD=<clave si aplica>
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_SSL=false
MAIL_FROM=<remitente autorizado>
```

Para SSL implícito, configurar el puerto autorizado —frecuentemente 465—, `SMTP_STARTTLS=false` y `SMTP_SSL=true`. El preflight exige una única modalidad cifrada; no verifica autenticación ni entrega SMTP. En Windows usar rutas absolutas con `/`; proteger la clave HTTPS mediante ACL. Nginx necesita una clave disponible sin solicitar contraseña al arrancar. La CA debe ser confiable en los PCs/tabletas institucionales.

La redacción es opcional: copiar las variables del proveedor solo si está autorizada en ese entorno. [Guía](../development/writing-configuration.md). No incorporar secretos a React, imágenes o Git.

## Validación antes del arranque

```powershell
python scripts/production-preflight.py --env-file .env.production
```

Es de solo lectura: comprueba URL/puerto 64430, ausencia de puertos públicos internos, cookies/firma HTTPS, configuración SMTP, exclusión de semillas, credenciales necesarias y pareja certificado/clave. Comprueba identidad del certificado y al menos siete días hasta su expiración como umbral técnico inicial. No garantiza confianza de CA/cadena, fecha inicial de validez, DNS, firewall, entrega de correo ni recuperación real. No imprime claves ni la configuración completa de Compose.

Primer arranque de una DB nueva: configurar temporalmente BOOTSTRAP_ADMIN_EMAIL y BOOTSTRAP_ADMIN_PASSWORD, ejecutar preflight con `--allow-bootstrap`, arrancar, cambiar la contraseña inicial y retirar ambas variables. En bases existentes/restauradas usar las cuentas y procedimientos existentes, sin inventar un administrador nuevo. Tras retirar las credenciales ejecutar preflight normal y recrear backend.

## Arranque y actualización controlados

El siguiente comando modifica el entorno que se elija; no se ejecutó sobre el proyecto principal en esta entrega:

```powershell
docker compose --env-file .env.production -p utaped -f compose.yml -f compose.production.yml --profile web up -d --build
```

Antes de adoptar este comando sobre una instalación existente, confirmar que `-p utaped` identifica los volúmenes/datos correctos y que DB_PASSWORD coincide con la contraseña de esa DB. Cambiarla en el archivo de entorno no modifica automáticamente una DB ya inicializada.

El override publica HTTPS 64430→443, retira los puertos de DB/backend y excluye Mailpit del arranque normal. El frontend monta certificado/clave en solo lectura y sustituye sus cabeceras de proxy con las calculadas en Nginx. Spring reconoce el transporte HTTPS desde ese proxy interno. Cookies Secure, firma sin HTTP y HSTS inicial de un día, sin preload/subdominios. `https://<servidor>:64430/healthz` informa estado sin detalles privados. No existe listener público adicional en 80/443; escribir HTTPS en la URL.

Para actualizar: realizar respaldo completo y comprobado, conservar imágenes/versiones, validar configuración y ejecutar la versión revisada. Flyway aplica migraciones pendientes; si cambia el esquema, revertir imágenes únicamente no revierte la DB. Recuperar el conjunto anterior en un destino separado y validar antes de cambiar el servicio institucional. Nunca usar `down -v` para actualizar o reparar.

## Respaldar y comprobar recuperación

Desde la raíz, indicando explícitamente el proyecto origen:

```powershell
python scripts/operations.py backup --project utaped --destination backups/2026-10-12-institucional
python scripts/operations.py verify --backup backups/2026-10-12-institucional
```

Cada destino debe ser nuevo. La herramienta requiere la topología estándar: un DB, backend, frontend y volumen privado `/app/runtime-data`. Detiene los escritores de ese proyecto, exporta DB con pg_dump custom, archiva el volumen, guarda imágenes de backend/frontend y comprueba metadata/hashes. Reinicia únicamente los servicios que estaban activos incluso ante fallos. No respalda archivos .env ni claves TLS/personales; custodiarlos por separado. Las contraseñas hash y datos documentales sí están en el dump: proteger/cifrar el respaldo. El archivo histórico del período no sustituye este respaldo.

Las imágenes se guardan por ID, sin tags de los proyectos activos. La prueba de recuperación verifica contenido y hashes, no autenticidad de un respaldo recibido de terceros; restaurar únicamente conjuntos confiables. El script no programa respaldos ni los cifra por sí mismo. Con varios escritores/réplicas, almacenamiento externo u otras aplicaciones escribiendo en la DB, diseñar una estrategia conjunta antes de usarlo; no asumir que este procedimiento de instancia única los detiene.

Restauración exclusivamente a un proyecto nuevo cuyo nombre empiece por `utaped-restore-`:

```powershell
python scripts/operations.py restore --project utaped-restore-ensayo --backup backups/2026-10-12-institucional --web-port 64432 --start-application
```

Rechaza proyectos/volúmenes existentes, respaldo corrupto y rutas/enlaces inseguros. El puerto debe estar libre. No borra ni sobrescribe el origen; si falla queda un destino parcial para diagnóstico, sin eliminación automática. La recuperación usa PostgreSQL 18, comprueba conteos/Flyway/metadata y cada archivo contra SHA-256; carga las imágenes originales. Secretos de la DB recuperada se generan privadamente y permanecen en su configuración Docker, no en el reporte.

El aplicativo recuperado se sirve solo en loopback, con semillas/IA/programador de avisos deshabilitados y firma por HTTP deshabilitada. Para comprobarlo, usar un contexto de navegador separado y cuentas conservadas en el respaldo; visualizar páginas y descargar un aprobado autorizado. No es el cambio del servidor institucional a producción. Se conservan sesiones/credenciales del conjunto original; no se distribuyen a usuarios nuevos.

## Ensayo reproducible del transporte

Solo sobre una recuperación ficticia proveniente de `.env.sample`:

```powershell
python scripts/test-operations-transport.py --restored-project utaped-restore-ensayo --port 64433
```

Usa certificado local temporal, la plantilla HTTPS productiva y las imágenes del respaldo sin reconstruirlas. Comprueba preflight con configuración ficticia, sesión Secure, login, identidad, hash de un PDF firmado y 40 lecturas /healthz con ocho trabajadores. No envía correo, no consulta IA, no instala la CA en Windows y no accede a la DB principal. La fixture de CAPTCHA/login se limita a `utaped_restore`. Retira los servicios temporales y restaura los escritores que estaban activos; los volúmenes y datos recuperados permanecen. No usar este ensayo contra copias institucionales de usuarios reales.

## Evidencia y pendientes

[Reporte de aceptación](../reports/2026-10-12-06-role-acceptance-and-operational-readiness.md). Se verificaron respaldos/restauración de datos ficticios, HTTPS de prueba y un PDF firmado recuperado. Para operar faltan los insumos reales anteriores, prueba de envío a un destinatario autorizado, acuerdo de retención/RPO/RTO y ensayo institucional con datos autorizados. No se acredita capacidad de carga por las lecturas acotadas de /healthz.

Referencias técnicas: [Nginx HTTPS](https://nginx.org/en/docs/http/configuring_https_servers.html), [merge de Compose](https://docs.docker.com/reference/compose-file/merge/), [pg_restore 18](https://www.postgresql.org/docs/18/app-pgrestore.html).
