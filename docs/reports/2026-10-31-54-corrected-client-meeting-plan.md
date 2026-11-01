# Plan tras la transcripción corregida del cliente

Fecha: 31/10/2026. Reunión: 30/10/2026.

Se releyó íntegramente el segmento corregido L1060–1799 de `docs/decisions/Todas las reuniones.txt` y se contrastó con el código, las fuentes oficiales ya inspeccionadas, las decisiones vigentes y el análisis preliminar. La transcripción corregida sustituye las interpretaciones dependientes de errores del reconocimiento automático.

Resultado: [plan de correcciones](../planning/2026-10-30-client-corrections.md) con 26 identificadores OCT30, ocho tandas de preparación/implementación y escenarios de aceptación. `instruction.md` apunta al plan y delimita la siguiente intervención.

## Precisiones relevantes

- Todos los participantes personales de elaboración, revisión y aprobación deben firmar, incluidos varios revisores/aprobadores. Reutilizar la firma implementada y cerrar configuraciones/vías personales sin firma. Confirmar por separado el tratamiento colegiado anterior.
- Nota de matriz siempre presente y editable; autoguardado y correo tras restablecimiento administrativo son peticiones explícitas.
- Mínimo de una acción de mejora aplicable por comisión; no obligar a elegir todas ni extender el mínimo por deducción a otros orígenes.
- Informe final relacionado con Plan y cortes de ejecución; falta confirmar elegibilidad y agregación de cortes.
- Capacidades por contexto, con distinta participación en dos comisiones; conservar controles existentes de principal y autorrevisión.
- Revisar términos, búsqueda general al escribir, período actual, carreras, JSON, anexos y detalle de reportes.
- Aclaración posterior del propietario: OCT30-13 comprende **todos los buscadores de todas las interfaces**, con actualización inmediata por cada letra/cambio, sin pausa intencional, Enter ni clic en Buscar. Corregidas las expresiones «pertinentes» y «espera breve» del plan de búsqueda; conservar cancelación/descarte de respuestas obsoletas.
- Vigencia del certificado ya comprobada; integración externa con FirmaEC se investiga separadamente. Las cifras de contraseñas y ofertas de proveedores expresadas con dudas no se convierten en reglas técnicas definitivas.

## Comprobaciones realizadas

- Lectura de la transcripción corregida y contraste estático de WorkPlans, editores, catálogo, estructuras, firma, workflow, ReportService, ReportingService, PasswordService y recursos administrativos.
- Confirmada la base actual: categorías de actividad, scopes, firma criptográfica, tareas múltiples, restricción al principal, prevención de autorrevisión y cambio de contraseña obligatorio ya existen en distintos grados.
- Confirmados puntos pendientes: categorías planas sin jerarquía oficial, T2 sin relación explícita con T3, opciones personales de flujo sin firma, nota booleana/fija, restablecimiento administrativo sin ese envío de correo, búsqueda por envío y resumen de próximos vencimientos limitado a seis filas.
- Referencias documentales locales comprobadas y diff revisado para evitar cambios de implementación.

No se ejecutaron suites de aplicación, renderizados nuevos de documentos, pruebas SMTP, llamadas FirmaEC ni aceptación con el cliente. No se cambió código, configuración, datos operativos o Templates. No se hicieron commits ni push. Los checks del plan son futuros; no están declarados entregados.
