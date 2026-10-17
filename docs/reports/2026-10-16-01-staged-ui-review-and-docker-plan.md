# Revisión visual del staged y propuesta Docker

Fecha: 2026-10-16. Rama: fix/document-creation-options. Fuente: solicitud del propietario de revisar/validar/confirmar el staged y definir si son necesarios los entornos Docker actuales.

## Cambios revisados

El staged recibido contenía 32 archivos, con modernización de estilos y componentes de navegación, administración, auditoría, planificación, matriz, firma, preparación y revisión documental. Además incluía recuperación de pantalla/documento/paso mediante URL y un escenario E2E nuevo de recarga: no era exclusivamente un cambio visual.

Se conserva el diseño del propietario: jerarquía, tarjetas, indicadores de estados, iconos, filtros, matriz con selección por chips, delegaciones, cabeceras y superficies de lectura documental, modos claro/oscuro y ajustes de escritorio/tableta. No se modificó la composición backend de los PDFs institucionales.

## Correcciones durante la revisión

- ReportEditor notificaba el paso nuevo y luego el anterior al volver al panel de edición; la URL podía quedar un paso atrasada. Se transmite el paso elegido explícitamente.
- El callback inline de App para seleccionar documento cambiaba de identidad en cada render y provocaba recargas innecesarias del documento. Se estabilizó; cambiar el paso ya no requiere volver a cargarlo.
- El manejador popstate original omitía la protección de cambios pendientes. Se incorpora confirmación, conservación del borrador al cancelar y restauración de la URL correspondiente.
- Los editores sincronizan cambios de sección recibidos desde el historial, incluso cuando se conserva el mismo documento, sin reiniciar el borrador local.
- Al abrir un Informe desde un Plan o crear una versión formal nueva, el editor y la URL deben usar el nuevo ID. Se conectaron esas operaciones con la selección documental superior.
- Se retiró el cast `as any` de la sección inicial T1 y se valida contra las secciones admitidas.

Las rutas no conceden permisos: los datos siguen obteniéndose mediante los endpoints autorizados. Cerrar sesión limpia la ruta documental. Recargar recupera los datos guardados en el servidor; no se añadió autosalvado ni persistencia de contraseñas/certificados en el navegador.

## Verificación ejecutada

- Frontend lint y build (TypeScript/Vite): aprobados.
- Suite E2E general: **8/8** aprobados, incluido el escenario de recarga incorporado por el propietario.
- Recarga ampliada y repetida: aprobada para lista, Información general, Contenido e Inicio, con cancelación de Atrás ante cambios pendientes, texto preservado y URL restaurada. Incluye captura PC/claro y tableta/oscuro y comprobación de ausencia de desbordamiento horizontal.
- Sample: **10 escenarios** aprobados en la suite y circuito documental repetido por separado hasta aprobar el escenario restante: **11 escenarios únicos aprobados**. Incluye roles, catálogos, importación, confirmaciones propias, creación, firma, devolución/aprobación, evidencias, T2, nueva versión y cierre.
- El circuito incorpora verificación de ID/paso de T2 tras guardar, recarga de Oportunidades de mejora con contenido conservado, y recarga de la versión formal nueva con su ID correcto.
- Capturas revisadas del Plan en escritorio/tableta y del circuito documental; evidencias locales en frontend/test-results, ignoradas por Git.
- git diff --check: sin errores. Sin llamadas nuevas a alert/confirm/prompt ni atribuciones automáticas.

En las primeras ejecuciones de los escenarios ampliados se ajustaron localizadores de las áreas de texto a su rol/nombre accesible. Una repetición del login alcanzó 12 intentos de la cuenta ADMIN ficticia de E2E; se limpió únicamente su clave de contador en el contenedor/base aislados de pruebas. No se redujeron límites de seguridad ni se modificaron credenciales. Backend sin cambios: no se repitieron pruebas Java.

## Análisis Docker

Se inspeccionaron Compose, los proyectos/contenedores existentes, políticas de reinicio seleccionadas y una muestra de consumo. No es necesario mantener todos los entornos activos para trabajar diariamente. La separación para pruebas/restauraciones protege datos, pero tener varias copias persistentes encendidas no aporta valor al desarrollo cotidiano.

La [propuesta de organización](../development/environment-layout-proposal.md) recomienda un único entorno persistente de desarrollo/revisión, reutilizando por ahora sample; pruebas y restauraciones desechables, activas solo durante su ejecución; despliegue institucional separado cuando corresponda. Con Vite local bastan DB/backend/Mailpit; el frontend Docker es opcional para probar la distribución completa.

Hallazgo operativo: la suite sample actual escribe en la base utilizada para revisión manual y deja registros ficticios. La siguiente reorganización debe aislarla; no basta cambiar nombres de proyectos. La propuesta detalla conservación de datos, respaldo, volúmenes, conexiones y limpieza controlada.

La reorganización Docker queda **propuesta**, sin modificar Compose, renombrar volúmenes, detener o borrar entornos. Se reconstruyó únicamente el frontend de sample para verificar las mejoras. Las pruebas usaron los entornos ya disponibles; no se crearon nuevas copias de restauración ni se tocaron proyectos ajenos a UTAPED.

## Entrega

Las mejoras staged del propietario, reparaciones relacionadas y pruebas ampliadas están en el commit local **1628a31** (`feat(ui): modernize workspaces and preserve document navigation`). Requisitos/estado y estos documentos se conservan en un commit documental separado de la misma rama. Sin push ni actualización del PR, conforme a la instrucción vigente. No se requieren variables nuevas ni migraciones para los cambios de interfaz.
