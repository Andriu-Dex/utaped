# Consulta de auditoría e historial

GET /api/admin/audit-events requiere ADMIN. Parámetros: action exacta (hasta 80 caracteres), actorId/targetId UUID opcionales, from/to ISO-8601 con zona (límites inclusivos), page desde 0 y size 1–100 (20 por defecto). Fechas invertidas y valores inválidos se rechazan. Orden occurredAt DESC, id DESC; filtros SQL parametrizados. GET /actions devuelve códigos observados, no un catálogo de permisos.

GET /api/work-plans/{targetDocId}/history valida identidad propietaria, grupo activo y pertenencia vigente exactamente como el Plan. ADMIN no concede acceso a un Plan ajeno. Devuelve solo eventos documentales del identificador indicado, paginados. No mezcla autenticación, otros Planes, versiones formales ni rondas.

Respuesta: items, total, page, size. Evento: id, actorId, actorName, historicalActorName, action, targetId, subjectUserId, occurredAt. No incluye contenidos documentales, contraseñas, correos, certificados ni archivos. La vista administrativa permite consultar identificadores para investigación; no habilita descarga ni navegación privilegiada hacia objetos.

Flyway V9 añade actor_display_name e índices de consulta. Los eventos nuevos capturan el nombre al registrar la acción. Los eventos previos no se rellenan retroactivamente: usan el nombre actual y historicalActorName=false; la interfaz explica esta limitación. Las fechas del filtro se interpretan en zona local del navegador y se envían como instantes UTC; el servidor conserva TIMESTAMPTZ.

No hay endpoint de edición/eliminación, política automática de purga, firma de logs ni garantía contra administradores directos de la base de datos. Retención institucional y exportación siguen pendientes. Los fallos de login siguen sujetos a la cobertura de eventos existente; este visor no introduce auditoría exhaustiva de cada lectura o rechazo.

## Exportación de auditoría

GET /api/admin/audit-events/export exige ADMIN y acepta exactamente action/actorId/targetId/from/to del visor. Descarga todas las filas filtradas, independientemente de su página, con orden estable. El botón usa los filtros aplicados; ediciones pendientes no cambian la descarga. Snapshot transaccional REPEATABLE_READ, CSV UTF-8 con BOM, cabecera fija y Content-Disposition; Cache-Control privado/no-store.

Límite técnico AUDIT_EXPORT_MAX_ROWS=10000, ajustable entre 1 y 100000. Si se supera devuelve 409 para refinar filtros; no trunca silenciosamente ni registra exportación exitosa. Un archivo descargado registra AUDIT_EXPORTED, sin guardar filtros ni datos sensibles adicionales. No incluye cuerpos documentales, correos, certificados ni hashes de contraseña. No altera retención de auditoría.

CSV destinado a consulta administrativa, no a reimportación exacta: comillas dobles escapadas, todos los campos entrecomillados y prefijo visible Texto: para celdas que comienzan con operadores de fórmula, incluidas variantes de ancho completo y espacios/control iniciales. Este prefijo modifica la representación exportada, conservando el valor original en la DB. No se basa solo en comillas o apóstrofes; el riesgo y las diferencias entre aplicaciones se consultaron en [OWASP CSV Injection](https://community.owasp.org/attacks/CSV_Injection). Ningún CSV sustituye controles del consumidor al editar/importar datos.
