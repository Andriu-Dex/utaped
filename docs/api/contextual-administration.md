# Administración contextual y alertas internas

Entrega: 2026-10-12. Fuente: Plan Maestro §§6.6, 8, 39–40 y 56; autorización del propietario para continuar. Es alcance productivo inicial, sin nuevas designaciones institucionales.

## Carreras e historial

- `PUT /api/admin/careers/{id}`: ADMIN, CSRF, `code`, `name`, `active`, `rowVersion`. Conflicto de versión/duplicado: 409. Desactivar impide seleccionar la carrera para nuevos ámbitos y Planes; no elimina ámbitos ni documentos anteriores.
- `GET /api/organization/scopes/{id}/history`: ADMIN o integrante activo del ámbito, incluido un período cerrado. Devuelve `memberships` y `principals` con identidades, función, estado, autorización de recuperación, autor y fecha del cambio.
- `PUT /api/admin/organization/scopes/{id}/members` añade `passwordResetAuthorized` opcional. Solo ADMIN concede la autorización; solo tiene efecto para COORDINATOR activo. Retirar o cambiar a MEMBER la revoca. Omitir el campo conserva la autorización existente mientras siga siendo coordinador activo.
- La migración V20 registra una línea base de pertenencias/principales existentes, expresamente marcada `BASELINE`. Su fecha corresponde a la migración y no a una designación institucional anterior. Los cambios posteriores se agregan como `CHANGE`; no sobrescriben el historial.
- Los nuevos ZIP de cierre incluyen ambos historiales. Un ZIP generado antes de V20 sigue conservando sus bytes originales; no se reconstruye.
- `GET /api/work-plans/{doc}/execution/extensions/history`: autorización documental habitual. Muestra destinatario, plazo anterior/nuevo, motivo, autorizador y fecha. La UI de ejecución presenta el historial por actividad.

## Recuperación por coordinador

`GET /api/organization/scopes/{scope}/accounts` devuelve únicamente cuentas recuperables (`id`, nombre, username, versión). Requiere pertenencia al ámbito; sin delegación válida devuelve una lista vacía.

`POST /api/organization/scopes/{scope}/accounts/{user}/temporary-password`: sesión y CSRF, JSON `rowVersion`, `temporaryPassword`.

Condiciones comprobadas en servidor:

1. Período ACTIVE, ámbito/grupo activos.
2. Actor activo, COORDINATOR activo del ámbito y autorización explícita concedida por ADMIN.
3. Destinatario activo, USER e integrante activo del mismo ámbito.
4. No cuenta propia, no cuenta ADMIN, no acceso cruzado a otro ámbito.
5. Versión de cuenta actual; mínimo 12 caracteres y máximo 72 bytes UTF-8; contraseña diferente.

Se bloquean período, cuentas y ámbito durante la operación, en el mismo orden compatible con la creación documental. Se guarda solo Argon2id, se revocan sesiones/enlaces, se obliga cambio y se incrementa la versión de cuenta. Los cambios voluntarios y mediante enlace también incrementan la versión para invalidar resets preparados sobre datos antiguos.

La recuperación modifica el acceso global de esa cuenta aunque la autorización del actor sea contextual. La interfaz advierte este efecto y pide confirmación. El coordinador introduce la contraseña temporal y la entrega por un canal privado; no se envía automáticamente. Se vacía el formulario tras éxito. No se guarda la contraseña en navegador, historial ni auditoría.

El destinatario recibe un aviso interno `COORDINATOR_PASSWORD_RESET` o `ADMIN_PASSWORD_RESET`, destino ACCOUNT, visible/abrible solo para su cuenta. Abrirlo lleva a seguridad de la cuenta. Una cuenta temporal debe completar primero el cambio obligatorio.

## Avisos de vencimiento

Programador del backend con primera ejecución y comprobaciones posteriores cada hora por defecto. Solo períodos ACTIVE cuya fecha actual esté dentro de sus límites, Planes T1 APPROVED no archivados, grupos/ámbitos activos y responsables activos con acceso documental vigente. No se conceden permisos mediante notificaciones.

Se comprueba cada medio de verificación; cualquier versión existente cuenta como PDF vigente. Con todos los medios cubiertos no se avisa. Se usa el plazo personal de `deadline_extension`, conservando el original.

- `ACTIVITY_DEADLINE_NEAR`: una vez por actividad, responsable y plazo, dentro de los tres días previos, incluido el día de vencimiento.
- `EVIDENCE_MISSING`: después del plazo, una vez por intervalo configurable, inicialmente diario. No se reitera fuera de las fechas del período ni después de su cierre.
- Si una prórroga cambia el plazo, la nueva fecha tiene su propia clave de entrega. Un aviso histórico no se borra al completar evidencia; ya no genera nuevas reiteraciones.
- Registro persistente `deadline_notice_delivery`, restricción única y transacción compartida con auditoría/notificación. Repetir o ejecutar concurrentemente la comprobación no duplica el aviso del intervalo. El bloqueo compartido del período coordina el envío con su cierre.

Configuración técnica inicial, no frecuencia institucional ratificada:

| Variable opcional | Valor inicial |
|---|---|
| `DEADLINE_NOTICES_ENABLED` | `true` |
| `DEADLINE_NOTICE_NEAR_DAYS` | `3`, rango 0–30 |
| `DEADLINE_NOTICE_REPEAT_DAYS` | `1`, rango 1–30 |
| `DEADLINE_NOTICE_SCAN_MS` | `3600000` |

Zona: `PLANNING_TIME_ZONE`, inicialmente America/Guayaquil. Los intervalos de reiteración se agrupan desde la fecha de referencia Unix; no desde la última hora de apertura de la aplicación. Si se cambia la frecuencia puede cambiar el intervalo y producir un nuevo aviso.

`GET /api/admin/deadline-notices`: configuración segura. `POST /api/admin/deadline-notices/scan`: ADMIN + CSRF, comprobación manual con fecha del servidor y número de avisos nuevos. Deshabilitar el programador no impide esta comprobación manual explícita. La UI está en Períodos académicos; la bandeja interna funciona sin SMTP.

## Operación y límites

Recompilar/reiniciar backend y frontend; Flyway aplica V20 sin borrar datos. No son necesarias variables nuevas: todos los valores tienen defaults. El administrador debe conceder la autorización de recuperación en Carreras y ámbitos; ningún coordinador la obtiene automáticamente.

Pendientes: alta/importación delegada, catálogo completo de cargos y fechas de vigencia de asignaciones, permisos de Decano/Subdecano, sustitución de ejecutores congelados, avisos por correo y procesamiento de grandes volúmenes. Esta entrega no inventa esas políticas ni declara finalizado el software.

Referencia técnica del programador: [Spring Scheduled](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/scheduling/annotation/Scheduled.html). La generación transaccional reside en un servicio separado del componente programador, de acuerdo con [las transacciones mediante proxies de Spring](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html).
