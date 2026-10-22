# Plan: reglas institucionales provisionales e Informe de ejecución

Fecha: 2026-10-20. Rama: `feature/institutional-rules-and-execution-report` desde `develop`. Decisiones en [open-questions.md](../decisions/open-questions.md#decisiones-provisionales-del-equipo--2026-10-20).

Cada fase se entrega con commit propio, pruebas backend PostgreSQL aisladas, E2E/aceptación de los flujos afectados, lint/build, actualización de implementation-status.md y reporte nuevo. Las reglas provisionales quedan como parámetros administrables con valor por defecto documentado, para reemplazarlas sin cambiar código cuando el cliente entregue la información oficial.

## F1. Plazos en días hábiles y reapertura individual de elaboración

- Cómputo común de días hábiles: excluye sábados, domingos y feriados registrados del período.
- Prórroga de evidencias: máximo acumulado de 3 días hábiles desde el vencimiento original, individual por persona/actividad, motivo obligatorio, concedida por el coordinador del ámbito, nunca posterior al fin del período. Sustituye el cómputo calendario actual (`app.planning.max-extension-days`); las prórrogas ya concedidas se conservan.
- Elaboración: ventana normal de 5 días hábiles configurable por período; reapertura individual del Plan/Informe con la misma regla de autorización, motivo e historial.
- Pruebas: fines de semana, feriados en el intervalo, extremos del período, acumulación y concurrencia.

## F2. Fecha de elaboración igual a la finalización

- La fecha se fija al preparar el artefacto de «Firmar y finalizar», porque debe quedar dentro de los bytes que firman los elaboradores; no puede escribirse después de la primera firma sin invalidarla.
- Antes de esa preparación, la vista previa muestra «Fecha provisional». La fila V.1 del control de cambios usa la misma fecha.
- Texto de interfaz: «Firma y finalización». Los artefactos ya firmados no se modifican.

## F3. Cierre de período con exportación y borrado

1. Cierre ACTIVE→CLOSED genera y verifica el ZIP (existente).
2. ADMIN descarga el ZIP; el sistema registra descarga y huella SHA-256.
3. El borrado se habilita solo tras descarga registrada y un período de gracia configurable (30 días por defecto), con confirmación explícita de ADMIN.
4. Se eliminan contenidos, PDFs, anexos y evidencias del período. Se conserva un registro mínimo: inventario de documentos, fecha de cierre, responsable de la descarga, huella del ZIP y auditoría.
- Riesgo registrado: la conservación legal de documentos firmados depende del custodio del ZIP. Pruebas específicas de que no se borra nada de otros períodos ni antes de cumplir las condiciones.

## F4. Flujos por cargo, ejemplo FISEI y delegación al coordinador

- Catálogo de cargos (Responsable UPE, Coordinador de carrera, Subdecano, Decano, Consejo Directivo…) y asignación persona↔cargo por período/carrera.
- Etapas de flujo resueltas por cargo; los participantes se congelan al activar, como ahora.
- Importación CSV de flujos y asignaciones; datos de ejemplo basados en `comisiones.xlsx` y el PDF de unidades, marcados como ejemplo y solo en el entorno de desarrollo.
- Coordinador: alta de miembros y reasignación de revisores dentro de su ámbito, con auditoría, sin acceso global a `/api/admin`.

## F5. T3 Informe de ejecución

Fuente: `docs/templates/INFORME EJECUCION II E26J26 UPE.docx` e `Informe_ejecucion.pdf`.

- Documento independiente derivado de un T1 aprobado y numerado por Plan (I, II…), como sugiere el nombre del ejemplo; el T2 Informe final sigue siendo único.
- Secciones: Resultados del plan de acción (objetivo general, actividades, medios, % de ejecución, observaciones; tabla Plan de mejoras), Análisis de resultados con FODA, Conclusiones y recomendaciones, Acciones de mejora (resultado → acción), Anexos con enlace de evidencias, firmas Elaborado/Validado y control de cambios.
- Plantilla A4 propia con el encabezado institucional compartido; firma, revisión y descarga reutilizan el circuito existente.
- Pendiente de confirmar con el cliente: si el corte es por período parcial o por actividad. Se implementa por corte con selección de actividades, lo que cubre ambos casos.

## F6. Estructura de plantillas administrable

- Versiones de estructura por tipo (secciones, orden, obligatoriedad, texto inicial), con snapshot por documento; los históricos no cambian.
- Se hace después de F5 para modelar tres tipos reales y no inventar un formato genérico.

## F7. Pendientes menores

- Borradores descartados visibles solo para administración autorizada.
- Historial de períodos anteriores para Decano/Subdecano mediante cargo (depende de F4).
- Constancia Quipux: etapa opcional por flujo donde el coordinador adjunta el PDF de constancia; sin ella no avanza. Sin integración con Quipux.
- Indicadores: avance declarado (promedio de porcentajes T2/T3) y cobertura de evidencias, mostrados por separado.
- QR de firma: pendiente hasta definir su contenido; no se genera un QR que aparente validación oficial.

## F8. Revisión UX

Recorrido por rol (docente, revisor, coordinador, administrador) en PC y tablet, con propuestas antes de aplicar cambios.

## Orden

F1 → F2 → F3 son acotadas y desbloquean reglas ya decididas. F4 antes de F5/F7 porque los flujos por cargo los alimentan. F6 al final del bloque documental.

## Cierre de implementación — 2026-10-21

F1–F7 implementadas en el alcance detallado en [el contrato](../api/institutional-rules-and-execution.md); F8 se comprueba mediante recorridos automatizados y revisión visual PC/tablet. [Reporte de estabilización](../reports/2026-10-21-01-institutional-completion-and-stabilization.md).

Precisiones frente al plan inicial: la custodia exige verificar la copia local; la fecha documental es la preparación previa a firmas y cada firma retiene su propia fecha; el QR no afirma validación externa; cinco días hábiles se aplican cuando no se proporciona un fin explícito. T2/T3 usan el período vigente y no requieren una reapertura separada. La habilitación de un T3 ausente en un período activado es explícita y no modifica los T1/T2 congelados. Los descartes manuales se distinguen del archivo automático al cerrar.

La delegación de integrantes ordinarios no habilita cambios globales de contraseña. Sustituir ejecutores de tareas ya activas sigue pendiente de regla institucional; no se cambian los firmantes congelados por deducción. Las secciones oficiales se protegen y las adicionales son configurables; validar institucionalmente las nuevas estructuras antes de publicarlas para uso real.
