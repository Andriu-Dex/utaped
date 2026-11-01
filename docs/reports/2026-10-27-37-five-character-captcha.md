# CAPTCHA de cinco caracteres

Fecha: 27 de octubre de 2026.

El código de acceso pasa de seis a cinco caracteres. El servidor usa la misma longitud para generar y comprobar el reto; el campo tiene mínimo y máximo de cinco. Los caracteres quedan centrados en la imagen, con el tamaño anterior y sin alterar el formato PNG.

Se conserva el alfabeto existente, que excluye `O/0` e `I/1`, la normalización de mayúsculas, el audio del mismo código, la renovación, la caducidad de tres minutos, el uso único, el vínculo con la sesión y los límites de solicitudes e intentos. No se almacena la respuesta en claro ni se añade una vía de acceso para pruebas.

La interfaz conserva «Código de verificación» y el placeholder anterior. Se retira la explicación extensa bajo el campo, sin añadir una indicación visible sobre el número de caracteres. El texto alternativo sigue identificando la imagen y su alternativa sonora.

Las fixtures de autenticación backend, E2E y aceptación usan respuestas conocidas de cinco caracteres únicamente a través de sus bases aisladas. Se añade una prueba de generación que comprueba longitud, alfabeto, dimensiones de imagen y el texto enviado al generador de audio. Los casos de autenticación existentes comprueban minúsculas, sesión, consumo, caducidad, audio y renovación.

## Verificación

- Lint, TypeScript y build correctos; permanece el aviso de tamaño del bundle de Vite.
- Suite backend aislada terminada con código 0: 222 pruebas sin fallos, errores ni casos omitidos, incluida la nueva prueba de generación y las regresiones de autenticación.
- Ocho pruebas E2E correctas, incluidas reproducción, repetición, renovación, teclado, recuperación de errores y preservación de credenciales.
- Prueba aislada de importación y acceso con contraseña temporal: correcta.
- Capturas de CAPTCHA en escritorio y tablet oscura revisadas visualmente.
- La primera preparación E2E se interrumpió por un archivo de Vite bloqueado en Windows durante `npm ci`. Se verificó y detuvo únicamente el proceso local de este proyecto, se reinstalaron las dependencias y se repitió correctamente el lanzador. El servidor local quedó restaurado en el puerto 5173.
- Entorno de desarrollo actualizado conservando datos y credenciales. No se modifican estados de documentos ni artefactos firmados.

[Contrato de acceso](../api/local-access.md). Cierre mediante commit local, sin publicación remota.
