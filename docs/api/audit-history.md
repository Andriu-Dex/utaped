# Consulta de auditoría e historial

GET /api/admin/audit-events requiere ADMIN. Parámetros: action exacta (hasta 80 caracteres), actorId/targetId UUID opcionales, from/to ISO-8601 con zona (límites inclusivos), page desde 0 y size 1–100 (20 por defecto). Fechas invertidas y valores inválidos se rechazan. Orden occurredAt DESC, id DESC; filtros SQL parametrizados. GET /actions devuelve códigos observados, no un catálogo de permisos.

GET /api/work-plans/{targetDocId}/history valida identidad propietaria, grupo activo y pertenencia vigente exactamente como el Plan. ADMIN no concede acceso a un Plan ajeno. Devuelve solo eventos documentales del identificador indicado, paginados. No mezcla autenticación, otros Planes, versiones formales ni rondas.

Respuesta: items, total, page, size. Evento: id, actorId, actorName, historicalActorName, action, targetId, subjectUserId, occurredAt. No incluye contenidos documentales, contraseñas, correos, certificados ni archivos. La vista administrativa permite consultar identificadores para investigación; no habilita descarga ni navegación privilegiada hacia objetos.

Flyway V9 añade actor_display_name e índices de consulta. Los eventos nuevos capturan el nombre al registrar la acción. Los eventos previos no se rellenan retroactivamente: usan el nombre actual y historicalActorName=false; la interfaz explica esta limitación. Las fechas del filtro se interpretan en zona local del navegador y se envían como instantes UTC; el servidor conserva TIMESTAMPTZ.

No hay endpoint de edición/eliminación, política automática de purga, firma de logs ni garantía contra administradores directos de la base de datos. Retención institucional y exportación siguen pendientes. Los fallos de login siguen sujetos a la cobertura de eventos existente; este visor no introduce auditoría exhaustiva de cada lectura o rechazo.
