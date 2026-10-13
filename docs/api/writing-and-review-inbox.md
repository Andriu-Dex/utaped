# Redacción asistida y bandeja de revisión

Entrega: 2026-10-12. Sesión local, cambio obligatorio de contraseña completado y CSRF en POST. No hay acceso público ni clave de proveedor en React.

## Redacción

`GET /api/writing/configuration` devuelve `available`, `provider` y `maxInputCharacters`. La disponibilidad requiere habilitación, modelo explícito y clave del proveedor elegido. No devuelve modelo, credenciales ni configuración privada.

`POST /api/work-plans/{documentId}/writing`

```json
{
  "rowVersion": 3,
  "field": "JUSTIFICATION",
  "tone": "CLEAR",
  "text": "Texto existente seleccionado por el usuario.",
  "consent": true
}
```

T1: JUSTIFICATION / OBJECTIVE. T2: BACKGROUND / CONCLUSIONS / IMPROVEMENTS. Estilos: CLEAR / FORMAL / CONCISE. Texto no vacío, límite técnico inicial 8000 caracteres; la edición manual conserva su límite de 50000. No genera contenido desde un campo vacío ni transmite anexos, matriz, contactos, PDF o documento completo. El contenido del campo puede contener datos personales: el usuario debe revisar el texto antes de autorizar su envío.

Solo quien tenga permiso efectivo de edición en ese borrador puede solicitar asistencia. El período, archivo, estado, ventana y versión se comprueban antes y después de consultar al proveedor. No se mantienen transacciones/bloqueos documentales durante la llamada externa. ADMIN no obtiene permiso documental por su rol global.

Devuelve `{text, field, rowVersion}`, con `Cache-Control: no-store`. Nunca guarda la propuesta ni modifica el documento. Aplicar actualiza únicamente el formulario local; guardar usa los endpoints existentes y sus controles de versión. Si cambió el texto local o la versión, se deshabilita aplicar esa propuesta. Descartar/cerrar conserva el texto del formulario. Las propuestas desaparecen al salir de la sección; no se serializan en almacenamiento del navegador.

Errores: 400 campo/tamaño/consentimiento inválido; 404 documento ajeno; 403 borrador no editable; 409 versión obsoleta; 429 límite por usuario o capacidad ocupada; 503 asistencia deshabilitada/fallo/timeout/respuesta incompleta. Ninguno impide continuar editando manualmente. El sistema no reintenta automáticamente llamadas facturables.

Auditoría: WRITING_REQUESTED / WRITING_SUGGESTED / WRITING_FAILED, con actor y documento; sin texto original, propuesta, prompts, claves o cuerpo de errores del proveedor. No se registra una supuesta aplicación en servidor cuando solo se modificó el formulario local.

## Proveedores y límites

La interfaz `AiProvider` separa el servicio documental del transporte HTTP. El adaptador soporta Groq Chat Completions y OpenAI Responses; no existe respuesta simulada en producción ni sustitución automática entre proveedores. URLs HTTPS fijas, sin redirecciones. Claves exclusivamente en variables backend.

OpenAI: `instructions`, `input`, `max_output_tokens`, `store:false`; se leen los bloques `output_text` de los mensajes, únicamente en respuestas completadas. Groq: mensajes system/user, `max_completion_tokens`, sin streaming; solo se acepta `finish_reason=stop`. Rechazos, JSON inválido, salida vacía/truncada y respuestas mayores de 128 KiB se descartan. [Responses](https://developers.openai.com/api/docs/guides/migrate-to-responses), [API Groq](https://console.groq.com/docs/api-reference).

`store:false` evita conservar el objeto de respuesta en OpenAI, pero no equivale a ausencia total de retención del proveedor. Las condiciones y controles de tratamiento de datos deben revisarse en la cuenta institucional elegida. [Datos OpenAI](https://developers.openai.com/api/docs/guides/your-data), [datos Groq](https://console.groq.com/docs/your-data).

Configuración inicial ajustable por instancia: 30 segundos, 2000 tokens de salida, dos solicitudes concurrentes y 15 intentos por usuario en 15 minutos. El contador se conserva en PostgreSQL usando la infraestructura existente. La concurrencia se limita por instancia; una futura operación con varias réplicas necesita presupuesto/límite agregado externo. Los intentos que encuentran capacidad ocupada también cuentan. No son políticas institucionales definitivas.

## Bandeja

`GET /api/reviews/inbox?query=&status=&documentType=&page=0&size=20`

Respuesta: `{items, total, page, size, metrics:{active, completed, returned}}`. `items` conserva la estructura de Task del circuito documental. La selección de documento se mantiene independiente de los filtros/página; al volver se actualiza la bandeja conservando esos filtros.

- query: hasta 200 caracteres, coincidencia literal sin distinción de mayúsculas en título, grupo y período; `%`/`_` no actúan como comodines.
- status: vacío / ACTIVE / COMPLETED / RETURNED.
- documentType: vacío / T1 / T2.
- page: base cero, entre 0 y 1000000. size: 1–100; UI ofrece 10/20/50.

Solo tareas propias en grupos activos y estados visibles. Las PENDING/CANCELLED no aparecen; tampoco se adelanta el acceso de un aprobador a su etapa futura. Los indicadores son totales personales sin filtros, mientras `total` cuenta el conjunto filtrado. Lectura con snapshot REPEATABLE_READ; orden estable por tareas activas primero, fecha de revisión descendente, posición e ID. Cambios entre solicitudes pueden mover filas al volver a consultar; no se garantiza un snapshot duradero entre páginas.

El endpoint anterior `/reviews/tasks` se conserva para compatibilidad con su límite de 200. La interfaz usa exclusivamente el nuevo endpoint paginado y accede a tareas posteriores a ese límite. Se comprobó el recorrido de 236 asignaciones sin duplicados en un conjunto estable. La consulta no modifica estados, firmas, rondas ni autorizaciones del inspector.
