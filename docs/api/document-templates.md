# Configuración del texto inicial T1

Alcance: justificación predeterminada de nuevos Planes de Trabajo. No administra archivos DOCX, diseño A4, firmas ni el contenido de documentos existentes. Actualmente solo existe `T1`.

## Acceso y contrato

Todas las rutas requieren sesión ADMIN; las mutaciones requieren además CSRF. Los usuarios ordinarios reciben 403. Un tipo inexistente produce 404.

| Método | Ruta | Contenido |
|---|---|---|
| GET | `/api/admin/document-templates` | Lista de configuraciones. |
| GET | `/api/admin/document-templates/T1` | Configuración actual. |
| PUT | `/api/admin/document-templates/T1` | `{rowVersion, justification}`. |
| POST | `/api/admin/document-templates/T1/reset` | `{rowVersion}`; restaura el texto inicial incorporado en el proyecto. |

La respuesta incluye documentType, name, description, justification, rowVersion, updatedAt, updatedBy y updatedByName. `justification` es obligatorio, no vacío y máximo 50 000 caracteres. `rowVersion` es obligatorio, entero no negativo. Las mutaciones bloquean la fila y verifican su versión; un conflicto devuelve 409 y conserva los datos para recargar/reconciliar. Registrar `INSTITUTIONAL_TEMPLATE_UPDATED` o `INSTITUTIONAL_TEMPLATE_RESET` en auditoría no concede acceso al contenido privado de Planes.

## Persistencia y uso

Flyway V23 crea `institutional_document_template` e incorpora el texto inicial T1. Al crear un nuevo Plan se almacena una copia de la justificación vigente dentro del documento. La lectura posterior devuelve el contenido realmente almacenado, incluso si está vacío. La configuración también se expone como `defaultJustification` en las opciones autorizadas del editor para la acción explícita de restauración.

Cambiar la configuración afecta solo creaciones posteriores. Restaurarla desde Administración tampoco modifica Planes ya creados. Restaurar desde el editor sí modifica el borrador local elegido, solicita confirmación cuando corresponde y requiere guardar normalmente; no genera una firma ni guarda automáticamente.

No se necesita una variable de entorno nueva. Un backend actualizado aplica V23 al arrancar con la configuración habitual de Flyway. No ejecutar SQL manual ni resetear la base para incorporar esta migración.

El texto suministrado por el propietario se conserva como contenido editable; sus referencias normativas no se consideran verificadas por esta entrega.
