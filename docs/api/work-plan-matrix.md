# Actividades, catálogos y matriz T1

Complementa [borradores](work-plan-drafts.md). Todas las rutas usan sesión y permisos del producto; las mutaciones requieren CSRF. Los catálogos empiezan vacíos: no se insertan actividades, recursos, denominaciones ni feriados ficticios.

## Matriz del propietario

GET y PUT `/api/work-plans/{targetDocId}/matrix`. El propietario debe conservar pertenencia al grupo activo. Terceros, incluyendo ADMIN sin titularidad, reciben 404. Fuera de ventana de elaboración se consulta en solo lectura y se rechaza PUT con 403.

GET devuelve `rowVersion`, `source`, `elaboratedBy` (nombre del grupo), `collectiveLabel`, rango del período, política de feriados, actividades, definiciones del grupo, catálogos, integrantes activos y feriados configurados.

PUT recibe `rowVersion`, `source` (máximo 500) y hasta 100 actividades. Cada fila tiene UUID `id`, `catalogId` nullable, título (500), categoría, fechas completas Desde/Hasta, `responsibleIds` (1–200, distintos), `collective`, `resources` y `means` (1–50 cada uno). Categorías: `POA`, `IMPROVEMENT_PLAN`, `IMPROVEMENT_ACTION`, `OTHER`. Una actividad libre solo puede ser `OTHER`.

Cada selección de recurso/medio identifica `catalogId`, o usa `catalogId=null` y `other` obligatorio no vacío (máximo 500). Se permite un solo Otro por conjunto. Etiquetas y clasificación de catálogo se derivan en servidor; no se confía en `label`, `mandatory`, título ni categoría enviados para entradas catalogadas.

Las obligatorias activas del grupo aparecen seleccionadas en GET mientras el borrador es editable; su configuración incompleta se representa con fechas null y listas vacías hasta que el usuario complete la matriz. Guardar exige completar todas las filas y conservar las obligatorias. GET no escribe datos ni incrementa versiones. En solo lectura se muestran exclusivamente las filas guardadas; no se incorporan obligaciones nuevas desde el catálogo.

Al guardar se conserva un snapshot de título/categoría/obligatoriedad de actividades y etiquetas de recursos/medios seleccionados. Cambios posteriores del catálogo no reescriben las selecciones ya guardadas. Las nuevas obligatorias activas se añaden a los borradores al consultar: no aplica aún a documentos finalizados ni artefactos firmados, que no existen en esta entrega. Una obligación ya guardada no se elimina automáticamente al desactivarla o reclasificarla en catálogo.

Las fechas deben estar ordenadas y dentro del período. Si `restrictHolidayEndpoints=true`, no pueden empezar/terminar en un feriado configurado; atravesar uno sí está permitido. No se define una lista institucional automática de feriados.

Responsables: usuarios activos que pertenecen al grupo. Se conservan sus UUID individuales; representación colectiva requiere todos los integrantes aplicables y una etiqueta configurada no vacía. Retirar pertenencias o desactivar cuentas exige corregir selecciones antes de volver a guardar; no elimina silenciosamente las selecciones del borrador.

Toda la matriz se guarda atómicamente en `work_plan_matrix` (JSONB tipado y validado). Comparte `work_plan.row_version` con información general/contenido: modificación obsoleta de cualquier sección recibe 409, sin cambios parciales. Guardar matriz incrementa el contador técnico y registra `WORK_PLAN_MATRIX_SAVED`; no cambia versión formal ni crea una ronda de revisión.

## Administración

| Rutas bajo `/api/admin/planning` | Métodos / cuerpo |
|---|---|
| `/catalogs`, `/catalogs/{id}` | GET listado; POST alta; PUT edición de etiqueta/estado manteniendo tipo. `kind` RESOURCE/MEANS, `label`, `active`. |
| `/groups/{groupId}/activities` | GET catálogo de actividades del grupo. |
| `/activities`, `/activities/{id}` | POST alta; PUT edición manteniendo grupo. `groupId`, `title`, `category`, `mandatory`, `active`. |
| `/groups/{groupId}/collective-label` | PUT `{label}`; vacío deshabilita representación colectiva. |
| `/holidays`, `/holidays/{date}` | GET; POST `{date,label}` crea/actualiza por fecha; DELETE elimina fecha configurada. |
| `/periods/{periodId}/holiday-policy` | PUT `{enabled}` obligatorio; activa/desactiva restricción en los extremos de las actividades. |

ADMIN obligatorio, validación de referencias y auditoría para todas las mutaciones. Los errores de selección, fechas, Otro o responsables devuelven 400. No existe eliminación física de catálogos; se desactivan para nuevas selecciones.

## Límites

No hay PDF T1 oficial, firma, finalización, anexos, nota de datos personales ni evidencia PDF en esta entrega. Recursos, medios del Plan y evidencias futuras son entidades diferentes. Q-001/Q-002 siguen abiertos; este editor no fija el número final de pasos ni cambia la unicidad provisional.
