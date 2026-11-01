# Pendientes personales, comparación de rondas y comprobación de firma

Fecha: 2026-10-28. Rama: fix/integral-product-audit. Sin commit por instrucción del propietario.

## Implementación

Inicio documental incorpora «Mis pendientes»: filtros, contadores personales, paginación, actualización y acciones sobre el documento explícito. Se consultan correcciones, siguiente turno de elaboración, revisión activa y medios de evidencia sin respaldo. Reutiliza permisos y estados vigentes; los pendientes se resuelven con las operaciones reales, sin confundirlos con notificaciones leídas. Considera prórrogas personales, períodos cerrados, aprobación, trámite formal y documentos archivados. También muestra firmas con requisitos bloqueantes para que no desaparezcan de la bandeja.

El inspector de revisión incorpora comparación explícita con la ronda inmediatamente anterior del mismo documento. Usa snapshots congelados, muestra apartados cambiados y texto antes/ahora en secciones desplegables. No añade otro visor PDF ni altera originales, firmas, versiones formales o rondas. La comparación es de contenido y configuración por apartado; los cambios puramente visuales de plantilla o firmas se consultan en los PDF históricos.

La lista previa a firma agrupa los requisitos reales del servidor con estado legible y acceso al apartado que requiere atención. T1/T2/T3 conservan sus pasos, controles, turnos, confirmación de revisión y firma real. Los requisitos administrativos o del certificado no ofrecen acciones de edición indebidas.

Contrato: [trabajo personal y comparación](../api/personal-work-and-round-comparison.md).

## Revisión visual del propietario

Se revisaron las modificaciones staged y de trabajo vigentes durante la tarea: navegación administrativa agrupada, tarjetas/indicadores, tablas de grupos, formularios, acceso y reportería. Se reutilizan jerarquía, tarjetas con bordes suaves, iconos, colores semánticos y tema oscuro; sin sobreescribir los archivos visuales que el propietario continúa editando.

Lint, TypeScript y build correctos durante esta revisión, y navegación administrativa real en cuatro tamaños/temas correcta. Esto no cierra una aprobación definitiva de todos los cambios que el propietario sigue realizando. La nueva UI se verificó con capturas de escritorio y tablet oscura. Se corrigió un bloqueo al repetir el filtro seleccionado y textos en singular.

## Verificación

- Batería backend general: 225 pruebas sin fallos.
- Regresión backend dirigida: cuatro casos correctos; tres nuevos escenarios y el caso existente de firma secuencial ampliado. Comprueba asignaciones/aislamiento, devolución, consulta de requisitos, cierre, cobertura de evidencia, comparación y conservación exacta del PDF firmado anterior.
- Aceptación: bandeja, comparación y checklist T1 correctos; reportería y exportación correctas; navegación administrativa en PC/tablet/temas correcta; circuito T1 → devolución → firmas → evidencias → T2 correcto.
- Una primera ejecución de los tests nuevos detectó dos supuestos incorrectos de la fixture: selector ambiguo y objetivo ya completado en la semilla. Se ajustaron antes de repetir. El circuito completo requiere entorno sin documentos preparados: su expectativa de historial de certificados vacío no aplica a la semilla preparada; se repitió por separado sin esa opción y pasó.
- Cierre de plazos: dos regresiones ampliadas correctas para firma bloqueada visible y prórroga individual de evidencia.
- Cierre UI: cuatro pruebas nuevas correctas, incluidos enlaces de revisión de requisitos en T2/T3 sin modificar el Plan base.
- Sigue el aviso previo de tamaño de bundle. Sin push, commit ni cambios de reglas institucionales.

Capturas y logs locales en frontend/test-results y backend/target/2026-10-28-work-assistance-*. Se mantienen fuera de Git datos y material de firma. Los servicios manuales quedaron actualizados mediante start-development.ps1 -Web; no se borran datos ni se ejecutan pruebas contra su BD.

Cierre autorizado el 2026-10-29: commits locales separados para interfaz y asistencia documental; revisión adicional y puerto Vite en [reporte 44](2026-10-29-44-interface-review-and-local-commits.md).
