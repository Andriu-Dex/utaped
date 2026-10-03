# Notificaciones personales internas

Flyway V10 crea personal_notification. Bandeja por identidad autenticada: GET /api/notifications con read opcional, page desde 0 y size 1–100 (20 por defecto); orden createdAt DESC, id DESC. Incluye items, total y unread (de toda la bandeja, independiente del filtro). GET /unread-count devuelve el contador propio.

POST /{id}/read marca de forma idempotente; POST /read-all afecta únicamente avisos propios existentes. Ambos requieren sesión/CSRF. POST /{id}/open verifica destinatario y acceso vigente, marca leído y devuelve type/id. WORK_PLAN usa propiedad/pertenencia del Plan; GROUP usa los mismos permisos de grupo disponibles. ADMIN no puede leer ni operar avisos de otro usuario. Objetos revocados muestran disponibilidad falsa, sin enlaces habilitados, y la apertura directa devuelve 404.

Avisos nuevos: asignación/cambio de rol de pertenencia, retiro de pertenencia y previsualización T1 realmente generada. Evento y aviso se guardan en la misma transacción; deduplicación por source_event_id/recipient_id. Repetir una asignación sin cambiar rol o retirar una pertenencia inexistente no genera evento/aviso nuevo. Generaciones reutilizadas del mismo artefacto no crean avisos repetidos. No se fabrican notificaciones retroactivas de auditoría.

Los avisos conservan título del objeto como metadata personal; no incluyen archivos, correos ni secretos. Al perder permiso permanecen como historial propio. No proporcionan autorización documental y la navegación revalida acceso. El navegador consulta el contador al entrar, volver al foco y cada 60 segundos mientras la página es visible; ajuste técnico inicial, no política institucional de frecuencia.

Esta entrega habilita únicamente el canal interno. Asignaciones/revisión/devolución de documentos, observaciones de evidencias, vencimientos, SMTP/push, preferencias y retención institucional siguen pendientes de sus módulos/reglas (Q-015 y Q-017). No hay avisos de acciones simuladas ni ejecución de firma/revisión.
