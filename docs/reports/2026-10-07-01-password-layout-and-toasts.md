# Revisión del diseño de contraseña y avisos toast

Fecha: 2026-10-07. Rama: `feature/modern-workspace-access`. PR de la entrega: #10 hacia develop, todavía abierto.

## Resultado

Se revisó el diseño aportado por el propietario: tarjeta centrada, ancho adaptable de 440 px, encabezado/icono y botón de ancho completo. Conserva campos, validaciones, bloqueo durante guardado y cierre de sesiones. Registrado por separado en `e174628` (`style: refine password change layout`). Los escenarios E2E se actualizaron al título «Acceso temporal» y comprobaron el cambio efectivo de contraseña.

Se agregó un componente toast reutilizable, sin dependencias nuevas, para los avisos de acceso, seguridad, administración, directorio, certificados, planificación, documentos, matriz, anexos, previsualización, firma, auditoría, seguimiento, flujos y notificaciones.

- Capa común fuera de formularios/fieldset: el cierre sigue disponible mientras un formulario está bloqueado y los avisos no alteran su distribución.
- Error y advertencia permanecen hasta cerrarse o hasta que cambie/desaparezca su estado de origen. Un nuevo intento puede mostrar otra vez el mismo error.
- Confirmaciones de operación se ocultan a los ocho segundos; pasar el puntero o enfocar el aviso pausa el cierre y reinicia el plazo al salir.
- Cierre con botón accesible y teclado; `alert`/`status` según severidad, texto con salto de línea, adaptación móvil y movimiento reducido. La aparición no mueve el foco.
- Tras fallo de login, se enfoca el CAPTCHA después de habilitar el formulario; antes se intentaba enfocar dentro del fieldset todavía deshabilitado.

Las validaciones específicas de campos (por ejemplo «Otro» vacío), estados de carga, bloqueos documentales y controles de comparación de versiones conservan su contexto. Las confirmaciones para descartar cambios o realizar acciones sensibles siguen requiriendo una decisión explícita; un toast no reemplaza esas protecciones.

## Verificación

- Lint frontend y build TypeScript/Vite aprobados.
- Tres escenarios Playwright completos aprobados contra Docker/Nginx y PostgreSQL aislados: identidad/recuperación/documentos/firma, flujos y acceso/administración.
- Regresiones nuevas: toast de error en móvil, cierre con teclado, error repetido, conservación de credenciales/foco, pausa y cierre automático de éxito, contraseñas diferentes y cambio exitoso.
- Capturas revisadas de contraseña en escritorio/móvil y toast móvil; sin desbordamiento horizontal en las comprobaciones.
- URLs de API y Mailpit de las pruebas configurables con `E2E_API_URL` y `E2E_MAIL_URL`, manteniendo sus valores anteriores por defecto. Se usaron puertos aislados 28081/28080/25433/28025/21025 para no interrumpir el Docker del propietario ni modificar su BD.
- No se modificó lógica backend ni se volvió a ejecutar su suite local por este cambio de presentación. Las comprobaciones remotas corresponden al workflow del PR.

## Configuración y alcance

No hay variables nuevas necesarias para la aplicación. Si se usa Vite, basta recargar; para ver la interfaz construida en Docker se debe reconstruir el servicio frontend. No se aplicó despliegue ni fusión a develop.

Los cambios previos del propietario en `.gitignore` y `backend/src/main/resources/application.yml` se conservaron fuera de estos commits. No se modificó Prototipos, no se restauraron reportes eliminados y no se añadieron datos semilla.
