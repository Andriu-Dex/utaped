# Acceso local y perfiles — contrato vigente

Fecha: 2026-10-06. Sesiones JDBC, cookie HttpOnly/SameSite y CSRF existentes. Las rutas conservan `/api` hasta la migración versionada; no se afirma `/api/v1` implementado.

| Operación | Contrato / autorización |
|---|---|
| GET `/api/auth/csrf` | Público; crea/obtiene token de la sesión. |
| GET `/api/auth/captcha` | Público; PNG y `X-Captcha-Id` opaco, `Cache-Control: no-store, private`. No devuelve texto de respuesta. Límite 100 emisiones/IP/15 min. |
| POST `/api/auth/login` | Form URL encoded: `username`, `password`, `captcha`, CSRF. Usuario o correo; resultado 204. CAPTCHA inválido/consumido/vencido: 400; credenciales: 401; exceso de intentos: 429. |
| GET `/api/auth/me` | Cuenta autenticada: añade `username`, `firstNames`, `lastNames`, admitiendo null en registros históricos sin completar. Nunca hashes de contraseña. |
| POST `/api/admin/users` | ADMIN + CSRF. Añade `username` opcional, `firstNames`/`lastNames` como pareja. Si se omite username, deriva del prefijo del correo. Colisión: 409, elegir identificador distinto. UI exige nombres/apellidos. |
| PUT `/api/admin/users/{id}` | ADMIN + CSRF + `rowVersion`. Puede completar nombres/apellidos y username. Cambiar username o permiso revoca sesiones; se mantiene correo. |
| GET `/api/admin/users/directory` | ADMIN; busca por nombre, correo o username. Añade `username`, `first_names`, `last_names` a metadata segura. |
| POST `/api/admin/users/{id}/temporary-password` | ADMIN + CSRF. JSON `rowVersion`, `temporaryPassword`. 200 éxito; 400 propia cuenta/contraseña igual/inválida; 404 inactivo/ausente; 409 versión obsoleta. |

Usernames: 1–100 caracteres ASCII; primero alfanumérico y resto alfanumérico, punto, guion o guion bajo. Normalización minúsculas. El correo es independiente. Nombres y apellidos: hasta 80 cada uno, total compuesto máximo 160. No se infieren nombres del texto histórico ni se crean personas a partir del PDF de asignaciones sin datos completos.

Contraseñas: regla vigente de mínimo 12 caracteres/máximo 72 bytes UTF-8. Altas, cambios, recuperación y resets usan Argon2id; bcrypt existente se migra después de login correcto. El límite anterior se conserva por compatibilidad, aunque Argon2id no lo requiere técnicamente.

CAPTCHA no distingue mayúsculas, vence a los 3 minutos y acepta un solo intento, incluso fallido. Para reintentar solicitar otra imagen. El frontend renueva automáticamente al fallar y conserva usuario/contraseña; la renovación borra solo el código. Retos viejos se purgan al emitir nuevos. No se persisten ni registran respuestas en claro.

Las cuentas temporales solo acceden a consulta de cuenta, CSRF, cambio de contraseña y logout. Reset administrativo revoca todos los enlaces/sesiones previos y genera `ADMIN_PASSWORD_RESET` sin registrar la contraseña. La entrega de la contraseña temporal al titular se hace por canal privado autorizado; no se envía automáticamente ni aparece en auditoría.

No se añaden variables de entorno. Flyway ejecuta V12 al arrancar la nueva versión. Antes de actualizar un entorno con datos, realizar el respaldo habitual de BD/archivos y revisar los usernames migrados en el directorio. SMTP real, HTTPS y dominios siguen siendo configuración operativa necesaria para publicación, no para la validación local con Mailpit.
