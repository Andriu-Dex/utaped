# Reglas institucionales provisionales, estructura documental y ejecución

Fecha: 2026-10-21. Complementa [circuito documental](document-circuit.md), [períodos](period-control.md) y [autoría/revisión](document-authorship-and-review.md). Decisiones técnicas autorizadas por el propietario; requieren ratificación del cliente antes del uso institucional.

## Documentos y formatos

| Concepto | Tipo interno | Código de plantilla |
|---|---|---|
| Plan de Trabajo | T1 | UTA-SGC-A-2-1-P7-T1 |
| Informe final | T2 | UTA-SGC-A-2-1-P7-T2 |
| Informe de ejecución por corte | T3 | UTA-SGC-A-1-1-P4-T2 |

T3 no es un código institucional nuevo. Su composición parte de `docs/templates/INFORME EJECUCION II E26J26 UPE.docx`. `Informe_ejecucion.pdf` proporciona referencia visual; su índice es menos completo que el DOCX, que incorpora FODA, acciones de mejora, firmas/control. Se conservan originales y se genera A4, con índice, páginas y espacios de firma derivados de la composición real.

Un T3 requiere un T1 aprobado, vigente, accesible y al responsable principal, dentro del período activo. Conserva el ID/artefacto/versión del Plan base. Solo hay un borrador activo por Plan; una repetición abre el mismo borrador. Cada nuevo corte obtiene I/II…; un descarte de borrador no consume numeración. Los cortes aprobados no se versionan formalmente: se crea otro corte. El T2 final mantiene su familia única y no se reemplaza por un T3.

Se seleccionan actividades del Plan aprobado, se declara porcentaje/observaciones y se completan análisis, cuatro factores FODA, conclusiones/recomendaciones. Las mejoras son pares situación→acción; admite enlace HTTPS y anexos PDF. Los textos de instrucciones y nombres de firmantes de la referencia se sustituyen por datos del documento. Firmas, rondas, observaciones, descarga aprobada, adjuntos e historial usan el circuito común, con identidad y bytes independientes.

| Método y ruta bajo `/api` | Operación |
|---|---|
| GET `/execution-reports/options` | Planes autorizados del principal (límite inicial: 50). |
| POST `/work-plans/{base}/execution-report` | Crear corte o abrir borrador existente. |
| GET / PUT `/execution-reports/{doc}` | Consultar/guardar resultados y narrativas; PUT exige rowVersion. |
| PUT `/admin/scopes/{scope}/workflows/T3` | Asignar revisión publicada. En período activado, habilitar T3 por primera vez sin modificar T1/T2. |

El nuevo formato requiere flujo propio publicado/asignado y participantes válidos. En un período ya activado, Administración usa «Preparar flujo de ejecución»; se resuelve/congela la nueva asignación una sola vez. No hay herencia silenciosa del flujo final ni sustitución de tareas ya activas.

## Estructuras publicadas y contenido adicional

Administración → Plantillas → «Editar estructura»: elegir formato, reordenar apartados, agregar secciones con texto inicial/obligatoriedad y publicar. Los apartados oficiales conservan identificador, título y obligatoriedad; firmas/control permanecen al final. Se conservan el encabezado y las tablas institucionales. Máximo inicial: 20 secciones adicionales, 10.000 caracteres por sección.

Cada documento nuevo copia revisión, secciones y valores iniciales. Publicar otra revisión no modifica documentos existentes ni PDFs. Las nuevas versiones formales conservan la estructura de su documento anterior. Los documentos históricos que no tienen estructura permanecen sin actualización retroactiva. Los obligatorios adicionales se comprueban antes de generar un artefacto, pero se admiten borradores incompletos.

El responsable edita valores desde Preparación → «Editar secciones adicionales», con concurrencia y conservación de entradas ante error. Al componer, se reconstruyen índice/numeración y referencias reales; la matriz T1 mantiene A4 horizontal y las narrativas A4 vertical.

| Método y ruta | Operación |
|---|---|
| GET / PUT `/admin/document-structures/{T1|T2|T3}` | Estructura publicada; PUT exige revision y conserva historial/auditoría. |
| GET / PUT `/work-plans/{doc}/structure` | Snapshot y contenido adicional; PUT exige rowVersion y permiso de edición. |

## Acceso, cargos e integrantes

Los borradores descartados manualmente son visibles solo para ADMIN. El archivo automático de un borrador por cierre no lo convierte en descarte. Consultar un Informe descartado no concede acceso a las evidencias privadas de su Plan base: se informa que no están disponibles para esa cuenta.

En períodos cerrados, el acceso documental requiere un cargo activo con `can_view_history`, asignado a la persona en un período activo. Un cargo por carrera se limita a la carrera del documento; uno de facultad cubre la facultad. ADMIN habilita/revoca expresamente ese permiso desde Cargos (`PUT /admin/positions/{id}/history-permission`, enabled + rowVersion). No se amplían permisos por ser administrador técnico, antiguo autor o nuevo integrante. Los ejemplos Decano/Subdecano de la semilla no son designaciones institucionales.

Coordinación puede buscar personas por nombre (2–100 caracteres, hasta 20 resultados), agregar/retirar integrantes ordinarios del propio ámbito activo, sin designar principal/coordinador ni agregar administradores. La búsqueda vacía no devuelve directorio y un usuario ordinario no puede usarla. Las pertenencias delegadas se registran con su origen. Esa alta no habilita el restablecimiento global de su contraseña; los permisos de recuperación existentes siguen requiriendo autorización administrativa independiente.

## Constancia Quipux

Administración puede exigirla en la definición del flujo antes de publicar/activar. Se copia al workflow del artefacto y los cambios posteriores no afectan documentos aprobados. Después de aprobar, el PDF final sigue disponible para efectuar el trámite externo. Mientras falte la constancia se bloquean cargas de evidencias, creación de informes derivados y cierre del período.

La coordinación del ámbito registra un PDF privado de hasta 10 MiB mediante acción explícita. No se altera el PDF firmado ni se invoca Quipux. El resultado es único y auditado; no se permite cargar/reemplazar silenciosamente una constancia existente. No se afirma verificar remotamente su autenticidad.

- GET / POST `/work-plans/{doc}/formal-submission`: estado/carga multipart `file`.
- GET `/work-plans/{doc}/formal-submission/content`: PDF autorizado, sin caché.

## Custodia antes de eliminar

1. Cerrar período con motivo, conservando estados, archivos y ZIP verificado.
2. ADMIN descarga `/admin/periods/{period}/custody` y conserva el ZIP fuera del servidor.
3. «Verificar copia de custodia» envía la copia local a POST `/admin/periods/{period}/custody-verification` como `application/octet-stream`. Se calcula SHA-256 en streaming y se exige coincidencia íntegra; no se persiste una segunda copia. Una descarga registrada por sí sola no habilita purga.
4. Después de la verificación y de `PERIOD_PURGE_GRACE_DAYS` (30 días por defecto desde el cierre), ADMIN puede confirmar «Eliminar documentación», con motivo obligatorio. No hay borrado programado.

Se retienen inventario, huella, fechas/responsables y auditoría; se eliminan filas/archivos del período y el ZIP interno, después del commit. Las dependencias con otros períodos bloquean la purga. La descarga histórica posterior responde 410. No sustituye un respaldo restaurable ni una política de archivo legal.

El ZIP tiene límite técnico `PERIOD_ARCHIVE_MAX_BYTES` (128 MiB por defecto, hasta 512 MiB). Solo la ruta de verificación admite en Nginx cuerpos de hasta 512 MiB sin buffering de petición; el backend aplica su límite configurado. Las cargas documentales ordinarias mantienen 12 MiB de transporte y 10 MiB por archivo. Al cambiar el límite institucional, revisar también capacidad de memoria/exportación y proxy.

## Fechas e indicadores

- Prórrogas: máximo configurable `PLANNING_MAX_EXTENSION_DAYS` (3), días hábiles acumulados desde el vencimiento original, sin sábados/domingos/feriados registrados; por responsable y actividad. No superar fin de período ni alterar prórrogas anteriores.
- Elaboración T1: si `preparationEndsOn` se omite al crear el período, se calculan cinco días hábiles contando el inicio si es hábil. Una fecha explícita configura otra ventana. Reapertura individual por ámbito y coordinador, con motivo/historial/concurrencia. T2/T3 usan el período vigente; no se reabre uno cerrado.
- Fecha documental: preparación del PDF para firma, antes de la primera firma; cada firmante conserva su propia fecha/hora. El QR repite metadatos de la firma CMS incrustada; no remite a un validador externo no contratado.
- Cobertura documental: actividades con todos sus medios respaldados / actividades requeridas.
- Avance declarado: media simple de porcentajes de resultados aprobados por actividad. T2 final prevalece sobre T3; entre cortes se toma el último. Se informa cuántas actividades entran en la media. Sin datos aprobados, se muestra ausencia de declaración, no un 0% ficticio. No es una fórmula institucional ponderada.

## Desarrollo y pendientes

Migraciones V28–V33: origen delegado, T3, verificación de custodia, estructuras versionadas, permisos históricos/constancia y descarte explícito. V25–V27 de las fases previas completan plazos, purga y cargos. No se deben editar migraciones aplicadas ni restaurar la DB de ejemplo para actualizar código.

La semilla conserva credenciales/datos personalizados y agrega ejemplos faltantes; T1/T2/T3 se asignan únicamente donde el período permite configuración. En períodos ya activados, habilitar T3 desde Administración. No hacen falta claves externas nuevas. Mantener respaldo conjunto de DB/volumen/imágenes antes de migrar el entorno persistente.

Pendiente: periodicidad definitiva del corte, tabla oficial de cargos/firmas/órganos y facultades, política de sustitución de tareas activas, custodia/retención institucional, tratamiento de datos y ensayo operativo real HTTPS/SMTP. El agente de código/Trello queda fuera por decisión del propietario.
