# Panel administrativo de supervisión

Fecha: 2026-10-30. Rama: fix/integral-product-audit. Implementación autorizada tras revisar instruction.md; los cinco archivos staged previos también pueden integrarse y modificarse.

## Resultado

Se sustituyen ocho tarjetas y la cuadrícula redundante de accesos por contexto de período, cuatro indicadores, alertas, gráficos compactos y auditoría categorizada. La navegación lateral y Reportes conservan su función. Reportes recibe el período seleccionado.

Los indicadores muestran personal con acceso activo, documentos, medios de evidencia pendientes y avance declarado con cobertura. El gráfico de respaldo representa actividades con todos sus medios cubiertos, separado de ejecución física. Se priorizan hasta cinco ámbitos con pendientes/plazos vencidos. Anillo por estado y distribución T1/T2/T3 en SVG/CSS, sin librerías nuevas. Listas y cantidades siguen siendo accesibles sin interpretar colores.

Las ventanas de elaboración y revisión se muestran independientemente y admiten solapamiento. Se distingue período activo de vigente por fecha. No se selecciona arbitrariamente un período si hay varios vigentes; se usa el resumen institucional. Los días mostrados son calendario; la nota remite las extensiones individuales a sus ámbitos/actividades.

GET /api/admin/overview devuelve un snapshot agregado de solo lectura, con rol ADMIN y no-store. Reutiliza ReportingService; no devuelve títulos privados ni su lista de próximos documentos. Alertas de ámbitos sin coordinador activo, revisión fuera de ventana y evidencia/plazos pendientes; la ausencia de avisos no se presenta como validación universal. Auditoría conserva nombres históricos y fallback para eventos antiguos. [Contrato](../api/administrative-overview.md).

## Mejoras staged del propietario

Revisadas e integradas las mejoras de certificados, catálogos, planificación y flujos: cabeceras, estados vacíos, tablas e iconos. Se mantienen handlers, permisos, confirmación de descarte y acceso a formularios. Se corrige un texto que limitaba la descripción de flujos a T1/T2 aunque también se configura Informe de ejecución. No se cambian documentos institucionales, reglas de firma o asignaciones.

## Verificación

- Ocho pruebas backend dirigidas correctas: siete de ReportingIntegrationTests y una de ReviewExecutionIntegrationTests. Incluyen permiso agregado sin acceso documental implícito, tipos y totales, ausencia de resultados, ventanas independientes/día cero, múltiples períodos vigentes, coordinadores inactivos, revisión vencida/resuelta y nombres históricos de auditoría. No se repite la batería Java general anterior porque el cambio se concentra en supervisión y filtros.
- Seis pruebas de aceptación correctas: nuevo panel, navegación administrativa PC/tablet/temas, recuperación de consultas, reportería/exportación, permisos del panel y plantillas. Repetición visual de panel/navegación: dos casos correctos; cierre de selección repetida verificado en el caso específico. Las repeticiones no se suman como escenarios únicos.
- Ocho E2E correctos; actualizado el contrato visual de Usuarios registrados a Personal institucional.
- Lint, TypeScript/build y diff --check correctos. Aviso de bundle mayor de 500 kB preexistente; no se añaden dependencias.
- Capturas PC y tablet oscura inspeccionadas. Alertas en dos columnas en escritorio y una en tablet; sin desbordamiento global, con leyendas y texto legibles.

Los primeros chequeos detectaron una inferencia TypeScript demasiado amplia de destinos administrativos; se corrigió antes de las pruebas. Fixtures de ventanas usan fechas coherentes. Entornos de prueba desechables separados; entorno manual actualizado sin borrar sus datos. No se envió correo ni se publicaron ramas. Logs/capturas locales bajo backend/target/2026-10-30-executive-overview-* y frontend/test-results.

Cierre mediante commits locales separados para mejoras visuales previas y nuevo panel, sin push. No se afirma aceptación institucional ni dimensionamiento de carga.
