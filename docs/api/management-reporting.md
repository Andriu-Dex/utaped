# Panel de gestión y reportería

Fuente: Plan Maestro §§54–55 y decisión del propietario del 28 de octubre de 2026.

`GET /api/reporting` mantiene `groupId`, `periodId` y `archived`. Sus filas por grupo/carrera/período y su resumen alimentan Inicio, el panel administrativo y Reportes de gestión. No se escriben estados ni se reconstruyen artefactos.

## Indicadores

- Estados: borradores, en revisión y aprobados, sobre todos los T1/T2/T3 visibles en los filtros. Planes e informes se cuentan por separado.
- Actividades y cobertura documental: actividades del T1; una actividad tiene respaldo completo cuando tiene al menos un medio y todos sus medios tienen evidencia. Las versiones de una misma evidencia no multiplican medios ni actividades. Se aceptan PDF o referencia HTTPS según el medio configurado.
- `pendingEvidence`: medios sin evidencia de Planes aprobados, no archivados, con período y grupo/ámbito activos.
- `dueSoonActivities` y `overdueActivities`: actividades elegibles con medios pendientes y algún responsable vigente cuyo plazo está próximo o vencido. Se comprueba pertenencia, cuenta activa y acceso actual. Se usa el plazo individual ampliado si existe. Una actividad con varios responsables puede aparecer en ambos conteos si sus plazos son distintos; no deben sumarse como categorías excluyentes.
- Horizonte: hoy y los siguientes siete días, ajustable mediante `app.reporting.due-soon-days` (entre 1 y 30). Es un parámetro técnico, no una nueva regla institucional. Zona horaria: `app.planning.time-zone`.
- `declaredExecution`: media simple de porcentajes aprobados por actividad. Se conserva la precedencia del T2 final sobre cortes T3 y la selección más reciente por fecha de creación del informe dentro del tipo. No se incluyen resultados en borradores, informes archivados, porcentajes nulos ni actividades ajenas a la matriz del Plan. Cero es un resultado válido; ausencia de datos es `null`.
- `declaredActivities` y `declarationCoverage`: actividades con resultado aprobado y su proporción sobre todas las actividades planificadas de los T1 incluidos. El promedio global se calcula con los puntos de cada actividad, no promediando porcentajes de grupos. No se convierte falta de resultado en cero ni se afirma cumplimiento integral con cobertura parcial.

`upcoming` contiene hasta seis actividades próximas, ordenadas por fecha y con IDs estables para abrir ejecución. Para varios responsables muestra el vencimiento próximo más cercano. La lista aplica siempre los permisos documentales; el permiso de ADMIN para agregados no permite consultar detalles privados. Una lista vacía con totales globales positivos no se presenta como ausencia institucional de vencimientos.

## Exportación

`GET /api/reporting/export` conserva CSV UTF-8 con BOM y los mismos filtros y permisos. Añade cobertura de resultados, medios pendientes, próximos vencimientos y plazos vencidos a los campos anteriores. La ausencia de avance aprobado se exporta como celda vacía. Se conserva la protección contra fórmulas en nombres y las cabeceras de descarga sin caché. No exporta nombres de responsables ni detalles privados por el mero permiso administrativo.

## Presentación

Inicio muestra indicadores, gráficos con leyendas numéricas y distribución alfabética por grupo, sin clasificaciones de desempeño personal. El detalle y la exportación permanecen en Reportes de gestión. La explicación metodológica es desplegable, con modo oscuro, teclado y distribución PC/tablet. Los fallos de carga ofrecen reintento y no se convierten en ceros o estados vacíos ficticios.
