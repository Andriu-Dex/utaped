# Administración de perfiles y directorio

GET /api/admin/users/directory: query literal por nombre/correo, active opcional, page desde 0 y size 1–100 (20 por defecto). Devuelve items, total, page y size, sin hashes ni secretos. GET /api/admin/users/{id}: perfil editable y contador técnico.

PUT /api/admin/users/{id}: rowVersion, displayName y systemRole (ADMIN/USER). No cambia correo ni contraseña. Cambiar permisos revoca sesiones. PATCH /active acepta active y rowVersion; contador opcional para compatibilidad con clientes previos. El último administrador activo no puede desactivarse ni degradarse.

GET /api/admin/groups incluye activos e inactivos y row_version. PUT /api/admin/groups/{id}: rowVersion, name, groupType, active. Desactivar conserva documentos/pertenencias y bloquea su ámbito de acceso; reactivar restituye acceso según pertenencias. Nuevas asignaciones requieren grupo activo. No existe eliminación física ni excepción de ventanas institucionales.

Todas las mutaciones requieren ADMIN, sesión y CSRF. Conflictos 409 conservan el formulario local. Búsqueda paginada también sirve para asignar integrantes y participantes de flujos más allá de los primeros 200 usuarios.
