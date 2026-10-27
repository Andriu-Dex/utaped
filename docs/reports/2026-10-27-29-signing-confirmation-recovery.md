# Recuperación de la confirmación de firma

La pantalla manual permanecía en «Firmando…» con la decisión de certificado pendiente. La confirmación existía en el DOM, pero fuera del área visible; no se había registrado ninguna firma nueva. Se recuperó la pantalla cancelando esa decisión, sin firmar ni reenviar la operación.

El diálogo compartido se centra explícitamente respecto de la ventana, conserva los saltos de línea y recupera su capa modal si su nodo abierto se vuelve a insertar en el DOM. La decisión sigue siendo explícita: nunca se confirma automáticamente. Se conserva el cierre previo a devolver el foco al formulario, la cancelación, el tema y el manejo de teclado.

La firma del elaborador distingue «Comprobando certificado…», «Esperando confirmación…» y «Firmando…». No cambia la validación criptográfica, la autorización, la persistencia del artefacto ni los datos del certificado.

La regresión de aceptación desplaza el diálogo pendiente fuera de su capa modal y comprueba su recuperación, visibilidad, foco y cancelación sin firma; después completa el circuito real con un certificado de prueba. Las pruebas se ejecutan exclusivamente en utaped-acceptance.

Validación: lint, TypeScript y build correctos; 24 pruebas de aceptación correctas, incluyendo el circuito real T1/T2/T3 y revisión. Una primera ejecución detectó una regresión de foco al cancelar (23/24); se conservó el cierre explícito antes de resolver la decisión y la repetición completa pasó. Evidencia: `backend/target/2026-10-27-confirmation-recovery-final.log`. Entorno manual desbloqueado y sin firmas nuevas; entornos automáticos eliminados por el lanzador. Cambios sin commit.
