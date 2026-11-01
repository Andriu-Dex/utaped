# Panel administrativo de supervisión

Decisión del propietario: 2026-10-30.

GET `/api/admin/overview?periodId=<uuid>&allPeriods=false`, sesión ADMIN, solo lectura y `Cache-Control: no-store`. Usuario institucional sin ADMIN: 403; cuenta inactiva: 401. No amplía la autorización documental.

## Contexto y fechas

Si no se indica período y existe exactamente uno activo cuya fecha incluye hoy, se selecciona ese período. Con varios o ninguno vigente se devuelve el resumen institucional, sin elegir por orden de registros. `allPeriods=true` fuerza el agregado global; combinarlo con periodId devuelve 400. Un periodId que ya no está activo devuelve 404, con recuperación desde la interfaz. Los períodos activos pueden ser futuros o haber terminado sus fechas sin cierre administrativo: la UI distingue activo de vigente.

La respuesta incluye fecha actual en la zona configurada, momento de actualización, períodos activos y sus ventanas de elaboración/revisión. Cada ventana es OPEN, UPCOMING, CLOSED o UNCONFIGURED según sus propias fechas. Puede haber varias abiertas simultáneamente. Los días restantes son calendario y cero significa cierre hoy; no redefinen el cómputo institucional de prórrogas. Las extensiones individuales permanecen en los ámbitos/actividades y se indican como tales.

## Indicadores y supervisión

Personal/cuentas y grupos activos son totales institucionales. Documentos, estados, tipos y avance declarado reutilizan ReportingService y su selección de período; en agregado global se conserva el alcance de Reportes sin archivados. Nunca se convierten resultados ausentes en cero. El avance conserva número de actividades informadas y cobertura.

Alertas, siempre sobre períodos administrativamente activos y limitadas por el período seleccionado:

- Ámbitos/grupos activos sin pertenencia COORDINATOR activa de una cuenta activa. No se infiere coordinador desde el responsable principal.
- Documentos distintos IN_REVIEW, no descartados/archivados, con ronda IN_REVIEW y etapa ACTIVE después de la fecha configurada de revisión. Es monitoreo de la ventana, no una prohibición nueva ni una fecha por persona inventada.
- Medios sin respaldo y actividades con evidencia/plazo vencidos según ReportingService, respetando responsables/prórrogas. No es ejecución física.

El estado verde solo informa «Sin alertas en los controles consultados». No afirma validación universal ni aparece al fallar la consulta.

Las filas por ámbito conservan las agregaciones existentes; el gráfico prioriza plazos vencidos y medios pendientes, hasta cinco filas. La proporción representa actividades con todos sus medios respaldados / actividades planificadas, con cantidades explícitas. No promedia porcentajes de grupos ni interpreta el respaldo como avance físico.

La distribución por tipo distingue T1/T2/T3 del mismo conjunto no archivado. El anillo muestra cantidades por estado y una leyenda textual; cero no genera porcentajes inválidos. La auditoría muestra cinco eventos globales con categorías, nombre conservado del actor y fallback para registros históricos. No se presenta como streaming en tiempo real.

## Privacidad y navegación

El endpoint excluye la lista de documentos próximos a vencer, incluso si el administrador tuviera pertenencias documentales: esta pantalla expone agregados, no títulos privados. Las alertas abren módulos administrativos autorizados; Reportes recibe el período seleccionado y conserva sus filtros/exportación. No se accede al PDF desde el agregado. No se añaden herramientas de gráficos, almacenamiento local de datos ni llamadas externas.
