# Recuperación de acceso y entrega de correo

Fecha: 2026-10-28. Rama: `fix/integral-product-audit`.

## Resultado

Después de solicitar recuperación, «Revisa tu correo» sustituye al formulario. El título recibe el foco; las acciones permiten iniciar sesión o corregir el correo anterior. Los errores conservan los datos y la confirmación no revela si existe una cuenta. Se mantienen tema oscuro, PC/tablet y regreso compacto.

La prueba completa descubrió un 401 tardío de una solicitud anterior al logout: remontaba el acceso cuando el usuario ya estaba recuperando su contraseña. Ahora una señal de expiración sin cuenta activa se ignora, y una carga institucional de una identidad anterior no aplica errores después de cerrar sesión. La expiración de sesiones activas conserva su comportamiento.

## Diagnóstico de entrega

El correo mostrado por el propietario no corresponde a una cuenta registrada. La respuesta genérica es deliberada y no acredita envío. El entorno manual usa Mailpit en `http://127.0.0.1:18027`. Se comprobó entrega SMTP para una cuenta ficticia registrada, sin cambiar su contraseña manual. No se creó una cuenta para la dirección de la captura.

El circuito E2E aislado sí creó su propia cuenta, recibió el mensaje en Mailpit, abrió el enlace, restableció la contraseña e inició sesión con ella. No se almacenaron certificados, contraseñas ni enlaces de recuperación en este reporte. La entrega a Gmail no está configurada ni verificada; el despliegue productivo existente admite SMTP externo y requiere sus datos privados y una URL pública correcta. No se cambió esa configuración.

## Verificación

- Lint, TypeScript y build correctos; permanece el aviso previo de tamaño de bundle.
- Ocho E2E correctos, incluido recuperación/cambio de contraseña y logout. La primera ejecución detectó la interferencia de sesión tardía; se corrigió antes de repetir.
- Aceptación específica: error recuperable, respuesta para correo ausente, foco, regreso, correo conservado y señal tardía de expiración.
- Capturas PC y tablet oscura revisadas: confirmación legible, sin desbordamiento horizontal.

El launcher E2E añade `-SkipInstall` opcional para reutilizar dependencias/Chromium ya instalados y evitar borrar archivos de Vite abiertos en Windows. El valor predeterminado conserva instalación y la limpieza del entorno aislado. No se detuvieron los servidores Vite manuales.

Los cambios ajenos en `DocumentCreation.tsx` y `document-creation.css` quedan fuera de este commit. Sin push.
