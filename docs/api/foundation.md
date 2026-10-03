# Contrato API inicial

El módulo documental inicial tiene su contrato en [Borradores de Plan de Trabajo](work-plan-drafts.md).

Origen común frontend/backend; JSON UTF-8 salvo login form-urlencoded. UUID como identificadores. Errores de aplicación: objeto message en español, sin secretos. 401 sesión inválida; 403 permisos/CSRF/contraseña temporal; 400 validación; 409 duplicado/conflicto; 429 límite de intentos.

| Método / ruta | Acceso | Entrada / salida |
|---|---|---|
| GET /api/auth/csrf | Público | headerName y token; conservar cookie y enviar cabecera en mutaciones. |
| POST /api/auth/login | Público + CSRF | username=email y password form-urlencoded; 204 y cookie. |
| GET /api/auth/me | Sesión activa | id, email, displayName, systemRole, active, mustChangePassword. |
| POST /api/auth/logout | CSRF | 204; invalida sesión y cookie. |
| POST /api/auth/change-password | Sesión + CSRF | currentPassword, newPassword; 200; revoca todas las sesiones. |
| POST /api/auth/forgot-password | Público + CSRF | email; respuesta genérica sin indicar existencia; procesamiento SMTP asíncrono. |
| POST /api/auth/reset-password | Público + CSRF | token, newPassword; token de 43 caracteres, aleatorio, de un solo uso. |
| GET /api/groups | Sesión sin contraseña temporal | Grupos activos propios, o todos para ADMIN; id,name,group_type,active,membership_role. |
| GET /api/groups/{id}/members | Miembro de grupo o ADMIN | id,display_name,membership_role; 404 para grupo ajeno. |
| GET /api/periods | Sesión sin contraseña temporal | id,name y seis fechas ISO civiles con nombres snake_case. |
| GET /api/admin/users | ADMIN | Hasta 200 usuarios; id,email,display_name,system_role,active,must_change_password. No hash. |
| POST /api/admin/users | ADMIN + CSRF | email,displayName,systemRole=ADMIN/USER,temporaryPassword; devuelve id. |
| PATCH /api/admin/users/{id}/active | ADMIN + CSRF | active; desactivar revoca sesiones; conservar al menos un admin activo. |
| POST /api/admin/groups | ADMIN + CSRF | name,groupType=COMMISSION/UNIT/CLUB/OTHER; devuelve id. |
| POST /api/admin/groups/{id}/members | ADMIN + CSRF | userId,membershipRole=MEMBER/COORDINATOR; asigna/actualiza pertenencia. |
| DELETE /api/admin/groups/{id}/members/{userId} | ADMIN + CSRF | Retira pertenencia; no borra usuario. |
| POST /api/admin/periods | ADMIN + CSRF | name,startsOn,endsOn,preparationStartsOn,preparationEndsOn,reviewStartsOn,reviewEndsOn; devuelve id. |

POST administrativos devuelven 200 en esta entrega. Cambiar contexto en frontend no altera identidad ni crea permisos. Windows/procesos futuros no están implementados por registrar fechas: cierre, elaboración documental y revisión requieren módulos posteriores.

Las respuestas de consultas institucionales conservan snake_case de SQL en este contrato inicial; DTOs públicos uniformes podrán introducirse mediante cambio explícito y pruebas. No asumir un contrato documental futuro a partir de estas tablas.
