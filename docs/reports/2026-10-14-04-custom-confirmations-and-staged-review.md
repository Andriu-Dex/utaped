# Confirmaciones propias y revisión del staged

Fecha: 2026-10-14. Rama: fix/document-creation-options.

## Solicitud y resultado

El propietario solicita eliminar alertas simples y conservar toasts/modales de confirmación, además de revisar y hacer commit del staged. La captura correspondía a window.confirm. Se sustituyeron sus **41 llamadas** en la aplicación por una confirmación propia asíncrona; no existían llamadas alert/prompt y se verificó que no queden llamadas nativas de los tres tipos en frontend/src. Requisito UT-UI-112.

## Revisión del staged

Archivos recibidos: document-creation.css, ThemeToggle.tsx, action-form.css y styles/theme.css. Se conserva la modernización del propietario: tarjetas/documentos, controles, cabecera y desplazamiento del modal, estados y modo oscuro; botón de tema mediante portal dentro del modal.

La prueba de teclado detectó una regresión: los eventos de un botón montado mediante portal siguen la jerarquía React y podían escapar del manejador de Tab del modal. Se corrigió con captura nativa en el elemento dialog, reutilizable para los nuevos diálogos. El botón de tema elige el último diálogo abierto; se añadió un fallback al color hover no definido. No se incorporaron estilos para celulares.

Validación del staged y reparación: lint/build aprobados, dos escenarios de formularios/teclado aprobados e inspección visual PC/tableta. Commit local **59ceeb2**: style(ui): refine document modal and preserve portal keyboard focus.

## Implementación de confirmaciones

Modal compartido ConfirmationDialog con confirmAction, botones Cancelar/Confirmar, mensaje asociado accesible, foco inicial en Cancelar, Tab/Shift+Tab, Escape y devolución del foco. Se presenta sobre el modal de creación cuando corresponde. No se cierra por un clic accidental en el fondo.

Cada operación espera la respuesta antes de cambiar datos o ejecutar una solicitud. Se conservan valores de select y datos del formulario antes de esperar, evitando depender de eventos React expirados o valores controlados restaurados. Rechazar confirma que no se descarta ni ejecuta la acción. Intentos repetidos durante una decisión pendiente se rechazan sin crear una cola de mutaciones duplicadas. El servicio almacena solamente mensaje/resolución/origen de foco; no recibe contraseñas ni certificados.

| Área | Confirmaciones migradas |
|---|---|
| Navegación y cuenta | Salida con cambios, cambio de espacio, notificaciones y cierre de sesión. |
| Administración | Edición de perfiles/carreras, pertenencias, ámbitos, certificados públicos y recuperación contextual. |
| Planificación | Cambio T1/T2, navegación/recarga de editores, matriz y anexos; retirada de anexos. |
| Catálogos | Descartar edición/cambio de grupo y eliminar feriados. |
| Documentos | Archivo y nueva versión formal. |
| Ejecución/revisión | Salida con evidencias/observaciones pendientes, región de revisión, devolver/completar etapa. |
| Flujos y períodos | Deshabilitar/recargar plantillas, cambiar contexto, activar flujos y cerrar período. |
| Importación | Descartar preparación, salida del importador y confirmación de creación/descarga. |

Los avisos continúan usando Toast. Su host se mantiene dentro del diálogo activo para evitar que el top layer nativo los oculte o impida cerrarlos. El botón de tema también permanece operable. Una desconexión al crear documento conserva el borrador y presenta el toast en español.

## Verificación ejecutada

- Frontend lint y build (TypeScript/Vite): aprobados.
- E2E generales: **7/7** aprobados, incluidas protecciones de navegación, acceso y flujos.
- E2E sample de regresión: **9/9** aprobados, incluidos creación, roles, recuperación, importación y circuito T1/T2 con firmas, devolución, aprobación, evidencias, versiones y cierre.
- Escenarios específicos adicionales: **2/2** aprobados. Barrido del código contra llamadas nativas; confirmación superpuesta, Tab/Shift+Tab, cambio de tema durante la decisión, Escape rechaza sin perder el ámbito, confirmar cambia el tipo, cierre restaura desplazamiento y cero eventos dialog nativos en el escenario.
- Error de red simulado sin enviar creación al servidor: toast visible/cerrable sobre el modal y selección conservada.
- Capturas de staged y confirmación superpuesta oscura revisadas; archivos locales en frontend/test-results, ignorados por Git.
- git diff --check: sin errores. Las pruebas operan los botones reales del modal; no omiten la decisión ni fuerzan cambios mediante el DOM.

Una repetición alcanzó el límite de login de dev.principal; se limpió únicamente su contador identificado en utaped_dev. No se redujeron límites ni se modificaron usuarios de la instancia principal. Backend/base de datos productiva sin cambios; no se repitieron pruebas Java en esta entrega de interfaz.

## Alcance y uso

Frontend de utaped-sample reconstruido, disponible en http://127.0.0.1:64431. Vite refleja estos cambios al recargar. Sin variables nuevas ni migraciones. Commits locales, sin push ni modificación del PR, según instrucción vigente. Documentación de requisitos, estado, AGENTS e índice actualizada.

La protección beforeunload al recargar o cerrar una pestaña con cambios sigue siendo un aviso controlado por el navegador. Una página no puede sustituirlo por un modal asíncrono al salir. Se conserva para evitar perder datos; todas las decisiones internas de UTAPED usan el modal propio. Los permisos del servidor y las confirmaciones del propio navegador ajenas al aplicativo no se alteran.
