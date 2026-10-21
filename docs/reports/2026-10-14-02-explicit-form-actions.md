# Formularios mediante acciones explícitas

Fecha: 2026-10-14. Rama: `fix/document-creation-options`.

## Objetivo y fuente

Aplicar la decisión del propietario: los formularios de creación, edición y acciones deben comenzar cerrados y abrirse con un botón. La aclaración del propietario conserva visibles los formularios de acceso y los filtros. Requisito confirmado: UT-UI-110. El proyecto productivo conserva su diseño moderno; Prototipos permanece como referencia funcional sin modificaciones.

## Implementación

Componente compartido `ActionForm`: botón con icono, panel identificado, enfoque del primer control, Cancelar y Escape. Si hay cambios, solicita confirmación; rechazarla conserva los datos. Aceptarla descarta exclusivamente el borrador de esa acción y devuelve el foco al botón. Los campos cerrados no se montan, incluidos los archivos y contraseñas transitorios de firma. Los errores de operación conservan el formulario y sus datos; no se alteran permisos ni validaciones del servidor.

| Área | Apertura explícita aplicada |
|---|---|
| Documentos | Crear documento, con selector T1/T2 y nombres propuestos dentro del panel. La bandeja y los filtros continúan visibles. |
| Administración institucional | Crear grupo/período/carrera/ámbito, asignar integrantes, designar principal, editar/desactivar carrera y vincular certificado público. |
| Administración contextual | Restablecer cuenta del integrante autorizado. |
| Catálogos y calendario | Crear/editar elementos y actividades, denominación colectiva, feriados y restricciones. Editar un registro en la lista abre su panel. |
| Flujos y períodos | Editar plantilla de flujo, configurar asignaciones del ámbito y preparar cierre. |
| Elaboración | Agregar actividad a la matriz y agregar anexo; la edición de anexos ya dependía del botón Editar. |
| Firma y revisión | Preparar firma del elaborador/revisor y agregar observación. Seleccionar una región visual abre el formulario de observación asociado. |
| Ejecución | Registrar/reemplazar evidencia PDF o URL y conceder extensión individual. |
| Ciclo de vida | Actualización de versión formal y archivo con motivo detrás de una acción; historial consultable conservado. |

Los editores T1/T2 y sus pasos ya se abren mediante una acción documental. Se conservan las aperturas existentes del directorio, importador y restablecimiento de contraseña. Login, recuperación, cambio de contraseña de acceso, filtros de búsqueda y consultas de historial permanecen visibles. No se agregan aperturas redundantes a los asistentes existentes.

Los paneles pueden permanecer abiertos tras un guardado para continuar una sesión iniciada expresamente; volver a entrar al módulo no los abre automáticamente. El estilo usa los temas existentes y ajustes para PC/tabletas, sin reglas nuevas para celulares.

## Verificación ejecutada

- `npm --prefix frontend run lint`: aprobado.
- `npm --prefix frontend run build`: aprobado, incluyendo TypeScript y compilación Vite.
- Playwright general: **7/7** aprobados; acceso, recuperación, administración, flujos, contexto, CAPTCHA y temas.
- Playwright sample: **8/8** aprobados; dos escenarios específicos nuevos y regresión de creación T1/T2, siete roles, administración contextual, importación y circuito documental completo.
- Cancelación por teclado: rechazar conserva el texto; aceptar limpia el borrador; reapertura vacía y foco restaurado comprobados.
- Circuito: firma, devolución, observación visual, aprobación, evidencias, T2, extensión, versión formal y cierre con las nuevas aperturas explícitas.
- Capturas revisadas de administración en PC/claro y documentos en tableta/oscuro, con formulario cerrado y filtros disponibles. Capturas locales en `frontend/test-results/`, ignoradas por Git.
- `git diff --check`: sin errores de espacios.

Las pruebas se adaptaron para pulsar los botones reales mediante un auxiliar reutilizable; no omiten controles de permisos ni fuerzan apertura mediante manipulación del DOM. Los ensayos usan entornos aislados y datos ficticios. Se limpiaron únicamente contadores de inicio de sesión de cuentas de prueba para repetir la validación, sin bajar límites ni cambiar datos de la instancia principal.

## Entorno y entrega

Se reconstruyó exclusivamente el frontend de `utaped-sample`, disponible en `http://127.0.0.1:64431`. No requiere variables nuevas, migraciones o cambios manuales. Backend y base de datos principal sin cambios; las pruebas Java de la entrega anterior no se repitieron en esta corrección de interfaz.

Implementación y pruebas en el commit local `1c403a4` (`fix(ui): open mutation forms through explicit actions`), con un commit documental separado en la misma rama. Sin push ni modificación del pull request, conforme a la instrucción vigente del propietario. AGENTS.md, requisitos, estado e índice actualizados para mantener esta regla en futuras pantallas.

## Límites

La validación cubre los escenarios indicados y la inspección de formularios del código actual; no equivale a aceptación institucional final ni a una auditoría completa de accesibilidad. Se mantienen las decisiones institucionales y operativas pendientes ya documentadas.
