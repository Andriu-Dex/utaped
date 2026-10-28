# Panel de gestión y reportes

Se amplían Inicio, el panel administrativo y Reportes de gestión con un componente compartido de indicadores: estados documentales, medios pendientes, actividades próximas/vencidas, avance declarado y cobertura de resultados. Se añaden gráficos de estados y distribución alfabética de actividades por grupo; no se evalúan ni clasifican docentes.

El avance muestra porcentaje y número de actividades informadas sobre planificadas. Sin resultados aprobados se muestra ausencia de resultados, conservando el cero como valor válido. Los datos proceden del servidor y mantienen la separación entre evidencia documental y ejecución declarada. [Definiciones y contrato](../api/management-reporting.md).

Los próximos vencimientos consideran prórrogas, cuenta y pertenencia vigentes. Desde Inicio se puede abrir la ejecución de una actividad autorizada. Administración dispone de totales globales, conservando la autorización documental para cualquier detalle privado.

El CSV conserva filtros, permisos, codificación y protección contra fórmulas, e incorpora cobertura, pendientes y vencimientos. La interfaz tiene filtros visibles, reintento, explicación metodológica desplegable y estados vacíos sin porcentajes inventados. Se revisaron capturas de escritorio y tablet oscura y se ajustaron la densidad de los filtros y el contraste de botones y símbolos.

## Verificación

- Cambios previos del staged cerrados primero en `21dbbc1`, tras tres pruebas de aceptación y lint/build correctos.
- Backend: suite completa con 225 pruebas correctas, incluidos tres casos nuevos de reportería: versiones de evidencia, prórrogas, fechas, acceso privado frente a agregados, filtros, históricos, precedencia T2/T3, cero, cobertura parcial, promedio por actividad y CSV seguro.
- Primera regresión integrada: cuatro pruebas correctas de panel, Administración, circuito T1/T2 y recuperación de consultas.
- Revisión final de contraste oscuro: dos pruebas correctas, con espera de la transición y comprobación del color real de los controles.
- La primera suite general registró 30/32: los dos casos restantes agotaron la cuota común de CAPTCHA de la IP del proxy de pruebas, antes de probar sus funciones. Se ajustó únicamente la fixture positiva del entorno desechable para restablecer ese bucket entre logins; las guardas de entorno, otros límites y el código productivo de autenticación se conservan.
- Frontend: lint, TypeScript y build correctos; permanece el aviso de tamaño del bundle de Vite.

La repetición general terminó con 32/32 casos correctos, incluido el acceso real desde Inicio a ejecución. Se actualizó el entorno local conservando sus datos. No se cambian reglas de aprobación, plazos, datos manuales ni PDFs firmados. Cierre en commit local, sin publicación remota.
