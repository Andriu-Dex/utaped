# Acceso local y perfiles — contrato vigente

Actualización 2026-10-12: el cambio de contraseña temporal revoca las sesiones previas y enlaces de recuperación, crea una sesión autenticada nueva y permite continuar sin volver al login. El cambio voluntario de contraseña y la recuperación pública siguen cerrando las sesiones.

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

## Recuperación contextual — 2026-10-12

Se incorpora recuperación por coordinador con delegación expresa, sin permiso global. [Contrato](contextual-administration.md). Los cambios de contraseña también incrementan row_version; los resets se rechazan si se prepararon antes de ese cambio. ADMIN y coordinador generan un aviso interno privado al titular. La renovación del CAPTCHA queda deshabilitada mientras la imagen está cargando para evitar peticiones simultáneas sobre la misma sesión.


## CAPTCHA visual y audio — 2026-10-13

El propietario autorizó una alternativa sonora en español al mismo reto visual. `GET /api/auth/captcha` sigue devolviendo PNG y `X-Captcha-Id`; `GET /api/auth/captcha/audio?id=<UUID>` devuelve `audio/wav` solo a la sesión que recibió ese ID, mientras esté vigente y sin consumir. Ambos usan `Cache-Control: no-store, private` y `Vary: Cookie`. ID desconocido, otra sesión, reto renovado/consumido/expirado o un reto antiguo sin audio: 404 con mensaje genérico. UUID inválido: 400. La lectura tiene su propio límite de 100 solicitudes por IP en 15 minutos; no renueva, consume ni prolonga el reto.

El código sigue siendo seis caracteres de `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, sin distinción de mayúsculas, tres minutos y un solo intento. Flyway V22 incorpora el WAV a la fila temporal del reto, con máximo 1 MiB. Se conserva únicamente el hash de respuesta como texto; el WAV contiene su representación audible y se elimina junto con el reto al consumir/renovar o al purgar vencidos en nuevas emisiones. No se conserva la respuesta textual ni se envía a proveedores. El audio no es una contraseña ni material de firma; tampoco se registra en auditoría o logs. Los WAV del alfabeto incluidos en el servidor son recursos compartidos y no retos emitidos.

El navegador solicita el PNG, obtiene el ID de su cabecera y muestra un objeto Blob local; no contiene la respuesta en alt, JSON o etiquetas. Nginx permite `blob:` únicamente para imágenes y audio del mismo origen. “Escuchar código” muestra controles nativos de reproducción, pausa, repetición y volumen, enfocados con teclado; no hay reproducción automática. Renovar elimina el audio anterior y vacía únicamente el código, manteniendo credenciales. Se descartan respuestas tardías y se liberan las URLs temporales. Si falla la imagen, el audio permanece utilizable; si falla o expira el audio se informa con toast y se ofrece renovar.

No se necesitan variables nuevas, cuenta de voz, clave o instalación de sintetizador en el servidor. Para actualizar Docker: reconstruir backend y frontend del entorno elegido; migración automática al arrancar. Antes de actualizar datos reales, seguir el respaldo habitual. Prototipos permanece congelado.

Esta alternativa mejora el acceso al reto visual; no acredita conformidad WCAG integral ni resistencia a reconocimiento automático. Se conservan CSRF, comprobación de credenciales, sesión y límites de acceso. Falta aceptación de pronunciación/usabilidad con usuarios destinatarios, y definir asistencia institucional para quienes no pueden resolver ninguna de las dos modalidades. Referencias: [W3C G143](https://www.w3.org/WAI/WCAG21/Techniques/general/G143), [W3C G144](https://www.w3.org/WAI/WCAG21/Techniques/general/G144.html).
