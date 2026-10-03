# Seguimiento personal de documentos

GET /api/tracking: filtros groupId y periodId opcionales. Devuelve documents, editableDrafts, readOnlyDocuments, withPreview, distribución groups/periods y recent (hasta cinco documentos).

Solo incluye Planes propios, con grupo activo y pertenencia vigente. ADMIN no obtiene documentos ajenos. Los filtros no autorizados devuelven cero resultados, coherentemente con la lista documental. Borrador editable usa la ventana inclusiva, zona y configuración vigentes; solo lectura es la diferencia con el total disponible. withPreview cuenta Planes con algún artefacto persistido, aunque sea anterior al contenido actual; no acredita firma/aprobación ni actualidad del PDF.

El panel navega mediante un identificador documental explícito y el servidor revalida permisos al abrir. No contiene ranking, sanciones, porcentajes institucionales supuestos ni métricas de estados todavía no implementados.
