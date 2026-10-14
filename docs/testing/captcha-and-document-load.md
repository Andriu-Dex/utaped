# CAPTCHA accesible y concurrencia documental

Entrega autorizada el 2026-10-13. Complementa [aceptación por roles](role-and-document-acceptance.md); no fija un SLA o dimensionamiento institucional.

## Acceso

- Misma respuesta alfanumérica visual/sonora, sesión e ID explícitos, tres minutos y consumo único, incluso con intentos concurrentes.
- WAV privado repetible mientras el reto está vigente; otra sesión, renovación, consumo y vencimiento lo invalidan en el servidor.
- 32 clips españoles empaquetados, PCM válido, duración/tamaño acotados y muestras no silenciosas. No hay proveedor externo, texto de respuesta en el DOM ni variables adicionales.
- Navegador: teclado, foco en controles, reproducción/pausa/repetición, credenciales conservadas al renovar, respuesta tardía descartada y toast ante fallo. PC/tableta y modo oscuro; no se incorporan estilos para teléfonos.
- Se prueba Vite y la compilación servida por Nginx con CSP. La comprobación automatizada de reproducción no sustituye aceptación de pronunciación con usuarios ni acredita WCAG integral. Usuarios que no pueden resolver imagen/audio necesitan una vía institucional todavía por definir; no existe un bypass de autenticación.

## Carga acotada

`ReviewExecutionIntegrationTests` contiene dos escenarios específicos:

1. Tres autores ficticios con ámbitos distintos, principal y flujos T1/T2 asignados/activados. Cada T1 incluye 35 actividades y anexo de 12 páginas. Se generan simultáneamente PDFs extensos (al menos 20 páginas reales), se renderizan ocho páginas concurrentes, se comprueba aislamiento y se rechazan ocho descargas preliminares. Tres firmas reales PKCS12 temporales de autores distintos se ejecutan concurrentemente, con reintento ante ocupación. Tras aprobar sus tareas asignadas, ocho descargas HTTP conservan exactamente sus bytes firmados. Una firma y una revisión por documento; los PDFs fuente no se alteran.
2. Plan aprobado: ocho descargas HTTP idénticas; cuatro cargas simultáneas para cada una de dos versiones de evidencia. En cada versión se conserva una escritura y tres conflictos 409; todos los PDFs/versiones permanecen íntegros. Un T2 con narrativa extensa y evidencia de 12 páginas recibe dos solicitudes de firma con la misma clave de idempotencia: una firma/revisión, conflicto o devolución idempotente, reintento seguro y material privado borrado de cada buffer. El T1 conserva bytes y versión.

Generación, rasterizado, cargas y descargas atraviesan HTTP real, sesiones, CSRF y PostgreSQL. La concurrencia de firma usa el servicio transaccional/criptográfico con el parámetro de transporte seguro de fixture; no constituye prueba de carga de subida de certificados por HTTPS. Se conserva la prueba de transporte HTTPS separada de la tanda anterior. No se modifica el permiso de firma por HTTP del producto.

Los semáforos actuales aceptan una generación, dos renderizados y una firma criptográfica por instancia; al estar ocupados devuelven 503. El generador de carga registra todos los intentos y reintenta únicamente 503, como máximo 60 veces con pausas de 500 ms. No altera esos límites ni añade reintentos automáticos a la interfaz. Conflictos 409 y prohibiciones 403 se verifican según el escenario; un 500 o respuesta inesperada hace fallar la prueba.

Cada fase sincroniza el inicio y registra estados finales/por intento, duración total, p95 de las operaciones incluyendo reintentos, bytes, CPU del proceso Java y pico de heap muestreado cada 25 ms. Hasta ocho trabajadores; solicitudes HTTP con timeout de 60 s y futuros de 90 s. Heap no equivale a RSS del contenedor; CPU excluye PostgreSQL, Docker, navegador y Nginx. Muestras cortas y orden/calientamiento influyen en los valores: no extrapolar usuarios máximos, percentiles estables o infraestructura recomendada.

## Ejecución reproducible

```powershell
docker compose -f compose.test.yml run --rm tests mvn -B -q '-Dtest=ReviewExecutionIntegrationTests#boundedHttpLoadGeneratesAndRendersLongDocumentsForThreeIsolatedAuthors+concurrentEvidenceAndReportSignaturesPreserveVersionsAndSignedPlan' test
docker compose -f compose.test.yml run --rm tests mvn -B -q test
```

La suite exige `current_database() = utaped_test` antes de limpiar sus fixtures. No usar `.env` ni `compose.yml` para ejecutar pruebas destructivas. No se accede a la base principal ni a IA/correo institucional. Las semillas/browser fixtures de aceptación se restringen a sus proyectos aislados; la aplicación no expone respuestas o bypass de CAPTCHA.

Resultados JSON locales ignorados por Git: `backend/target/load-qa/long-documents-http.json` y `evidence-and-signing.json`. Capturas ignoradas: `frontend/test-results/captcha-audio-desktop.png` y `captcha-audio-tablet-dark.png`.

```powershell
cd frontend
npm run lint
npm run build
$env:E2E_MAIL_URL="http://127.0.0.1:28025"
npx playwright test --config=playwright.config.ts
npx playwright test --config=playwright.sample.config.ts
# Audio con build/Nginx del entorno ficticio:
$env:E2E_BASE_URL="http://127.0.0.1:64431"
npx playwright test --config=playwright.config.ts captcha-audio.spec.ts
Remove-Item Env:E2E_BASE_URL
```

Arrancar previamente los entornos aislados de [desarrollo](../development/sample-environment.md). Requiere Docker, dependencias npm/Chromium instaladas y configuración privada de fixtures; no copiar credenciales en reportes.

## Aceptación pendiente

Cliente: claridad del audio, facilidad de uso con teclado/lector de pantalla y necesidades que excedan ambas modalidades. DTIC: concurrencia/volumen máximos, SLA, documentos representativos autorizados y recursos del servidor. Repetir una prueba sostenida y el transporte de firma bajo HTTPS en esa infraestructura antes de dimensionar. La presente entrega comprueba integridad y recuperación de ocupación local, sin declarar capacidad institucional.
