# SMTP externo en desarrollo

Fecha: 2026-10-28.

El compose base fijaba Mailpit e ignoraba las variables privadas SMTP. Ahora transmite la configuración completa y admite los alias SMTP_USER/SMTP_PASS conservando prioridad de SMTP_USERNAME/SMTP_PASSWORD. Mailpit sigue siendo el valor predeterminado; producción conserva su override explícito.

Se completaron únicamente en .env.sample ignorado las opciones de autenticación, STARTTLS, SSL y remitente para las credenciales suministradas. Backend manual recreado sin reset de datos. Confirmada configuración efectiva del contenedor sin imprimir secretos. Autenticación SMTP correcta y mensaje técnico de prueba aceptado por el servidor, dirigido a la propia cuenta remitente. No se afirma recepción en bandeja ni se modificaron cuentas para habilitar recuperación. El correo debe pertenecer a una cuenta activa registrada.

Verificación: renderizado compose E2E/aceptación conserva Mailpit sin autenticación; entorno manual arrancó correctamente; .env y .env.sample siguen ignorados. Sin cambios de lógica Java/React ni necesidad de nuevas pruebas de compilación. Cambios ajenos de interfaz quedan fuera del commit. Sin push.
