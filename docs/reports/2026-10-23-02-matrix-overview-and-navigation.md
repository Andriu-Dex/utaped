# Matriz: resumen y navegación directa a Anexos

Fecha: 2026-10-23. Rama: `fix/institutional-document-spacing`.

## Entrega

El resumen tabular ya existía al final del paso 3. Se coloca antes del formulario de detalle para revisar de un vistazo actividades, fechas, responsables, recursos y medios. Se incorporan numeración, categoría/obligatoriedad, indicación de valores pendientes y diferenciación entre cambios locales e información guardada.

Cada fila permite ir al detalle de su actividad mediante un botón iconográfico con nombre accesible y tooltip. El foco de teclado acompaña el desplazamiento; se respeta la preferencia de movimiento reducido. La tabla admite desplazamiento dentro de su contenedor, encabezados fijos y ajuste de títulos largos, sin ampliar horizontalmente toda la pantalla. Utiliza los colores y temas existentes, para PC y tablet.

Se añade **Siguiente: Anexos**. Si hay cambios, utiliza las validaciones existentes y guarda antes de continuar; una selección de actividad aún no agregada debe agregarse o cancelarse. Los errores conservan el borrador y no cambian de paso. Un conflicto de versiones requiere resolver la comparación existente. Sin cambios, se permite continuar sin una escritura adicional; la navegación también está disponible en solo lectura. El Plan se vuelve a consultar antes de abrir Anexos para usar su versión actual.

No se cambian reglas institucionales, catálogos, API, permisos ni generación documental. Crear actividades sigue requiriendo una acción explícita. Los cuatro archivos que pertenecían al staged del propietario permanecen separados de esta entrega.

## Verificación

- Lint y compilación TypeScript/Vite aprobados sobre el código final. Permanece el aviso no bloqueante de bundle superior a 500 kB.
- Suite de aceptación: 16 pruebas aprobadas, incluido el circuito T1 → devolución/revisión → firmas → evidencias → T2 → versión formal.
- Tras ajustar los estilos: dos pruebas de referencia institucional aprobadas en un entorno reconstruido con el código final. Verifican resumen, campos pendientes, foco al abrir detalle, bloqueo por fechas incompletas, responsables colectivos, guardado, fallo simulado HTTP 500 que conserva la fuente local y continuación correcta a Anexos al reintentar.
- Comprobación de ausencia de desbordamiento de la página en tablet y modo oscuro. Inspección visual de `frontend/test-results/matrix-summary-tablet-dark.png`: títulos largos envueltos dentro de su columna y desplazamiento horizontal contenido.
- `git diff --check` aprobado. Los entornos de aceptación fueron eliminados con sus volúmenes; no se alteraron datos del entorno manual ni se ejecutaron pruebas backend, al no modificarse servicios ni contratos.

Log de la suite, ignorado: `backend/target/matrix-usability-acceptance.log`. Capturas y resultados de Playwright permanecen en las carpetas ignoradas de frontend.

## Uso

Con Vite, recargar el editor y abrir el paso 3. El resumen aparece al agregar actividades. No requiere variables de entorno, migraciones ni reconstruir el backend. Un frontend Docker compilado necesita reconstruirse para incorporar estos cambios.

Solo commit local; sin push ni pull request.
