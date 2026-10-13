# CAPTCHA accesible y pruebas de carga documental

Fecha: 2026-10-13. Rama: `feature/modern-workspace-access`.

## Resultado

Se incorpora audio español al mismo CAPTCHA visual y se añaden pruebas concurrentes de documentos extensos, evidencias y firmas. No se necesitan variables nuevas, servicio de voz o claves adicionales. Se conservan el mockup congelado, el puerto principal 64430 y las reglas de permisos/documentos firmados.

## Cambios entregados

- Audio WAV producido localmente a partir de 32 clips incluidos. Letras/números hablados, pausas y variación moderada de ritmo/volumen; sin enviar el código a proveedores ni exponer respuesta textual en frontend.
- Endpoint privado por sesión e ID, caducidad tres minutos, un intento compartido y límites de solicitudes. Renovar/consumir elimina el WAV del reto anterior; los vencidos se purgan al emitir nuevos. Flyway V22 añade audio temporal con máximo 1 MiB. No se guardan certificados ni contraseñas.
- “Escuchar código” y controles nativos de audio operables con teclado, foco visible, pausa/repetición/volumen, ayuda breve y toast ante fallo. No hay reproducción automática. Renovar conserva usuario/contraseña y elimina únicamente respuesta/audio anteriores.
- Solicitud PNG con ID de cabecera, cancelación de respuestas tardías y liberación de Blob URLs. CSP de Nginx HTTP/HTTPS permite Blob solo en imágenes y audio del mismo origen; se verificó la compilación servida por Nginx.
- Carga HTTP aislada: tres ámbitos/autores y T1 de 35 actividades con anexo de 12 páginas, paginación real, ocho rasterizados/lectores, bloqueo de preliminares y comprobación de acceso cruzado.
- Tres firmas PKCS12 reales de autores distintos en los servicios transaccionales; una firma/revisión por documento, aprobación explícita y descarga idéntica a los bytes firmados.
- Evidencias: cuatro cargas concurrentes por versión, una escritura y tres conflictos 409, dos versiones conservadas. T2 extenso, doble solicitud con la misma clave de firma, un artefacto firmado, reintento idempotente y conservación íntegra del Plan base. Buffers de material privado vaciados en todos los resultados.
- JSON de métricas por fase, CPU Java, heap muestreado, p95 y estados de todos los intentos. Herramienta limitada a fixtures `utaped_test`, sin acceso a DB principal o llamadas a IA.

## Validación ejecutada

- Suite PostgreSQL aislada completa: **135 ejecuciones / 20 suites**, cero fallos, errores o casos omitidos. El total incluye casos heredados por algunas suites.
- Playwright general: **7 escenarios**, incluidos dos nuevos de audio; todos pasan.
- Playwright ejemplo: **5 escenarios**, incluidos circuito T1/T2, firmas, siete roles, recuperación e importación; todos pasan.
- Los dos escenarios de audio se repitieron sobre el build servido por Nginx en 64431: **2/2 pasan**, comprobando imagen/CSP, reproducción y sesión.
- `npm run lint` y `npm run build`: correctos. TypeScript compilado; 70 módulos.
- Revisión visual de capturas PC 1440×1000 y tableta 820×1180, claro/oscuro, sin desbordamiento horizontal. La reproducción automatizada no sustituye una revisión humana de pronunciación o con lector de pantalla.
- `git diff --check`: correcto; archivos privados, PDFs/capturas y métricas temporales permanecen ignorados.

Se corrigieron durante la validación selectores de pruebas tras actualizar el texto alternativo y la CSP HTTP que inicialmente bloqueaba Blob. La pausa máxima de reintentos del generador de carga se ajustó para contemplar el arranque frío del motor documental; no se cambió ningún límite o comportamiento de concurrencia del producto para hacer pasar las pruebas.

## Mediciones locales

Última suite completa, Java 21.0.12.1, PostgreSQL 18.6, 20 procesadores visibles para la JVM. Máximo ocho trabajadores; motor previamente utilizado dentro de la suite. p95 corresponde a operaciones completas, incluyendo reintentos 503. Estados contabilizan intentos, no solamente operaciones finales.

| Fase | Operaciones | Estados por intento | p95 (s) | Pico heap (MiB) |
|---|---:|---|---:|---:|
| Generar T1 de tres autores | 3 | 200: 3, 503: 12 | 5.700 | 409.7 |
| Renderizar ocho páginas | 8 | 200: 8, 503: 12 | 1.737 | 488.1 |
| Bloquear descargas preliminares | 8 | 403: 8 | 0.015 | 294.4 |
| Firmar tres autores (servicio) | 3 | 200: 3, 503: 3 | 1.148 | 447.4 |
| Descargar T1 extensos firmados | 8 | 200: 8 | 0.024 | 466.4 |
| Descargar Plan aprobado | 8 | 200: 8 | 0.021 | 369.4 |
| Cargar evidencia v1 | 4 | 200: 1, 409: 3 | 0.143 | 428.4 |
| Cargar evidencia v2 | 4 | 200: 1, 409: 3 | 0.029 | 432.4 |
| Firma T2 duplicada (servicio) | 2 | 200: 1, 409: 1 | 0.115 | 216.8 |

Los 503 indican ocupación controlada de los semáforos (una generación, dos renderizados, una operación criptográfica por instancia). Todas las operaciones permitidas terminan con 200 mediante reintento acotado. Los 403/409 son rechazos esperados; no aparecieron 500 en las fases verificadas. El generador registra reintentos, sin agregarlos automáticamente al producto. En un ensayo separado con motor frío y otros builds locales en marcha, tres generaciones tardaron aproximadamente 23.9 s de p95, frente a 5.7 s en la última suite; estos valores ilustran la sensibilidad al calentamiento/carga local, no un SLA.

Las firmas concurrentes usan servicios reales con el indicador seguro de la fixture; **no se midió carga de subida de certificados por HTTPS**. Heap no es memoria total/RSS y CPU Java no incluye DB, Nginx, Docker o navegador. El tamaño de muestra no acredita capacidad sostenida ni usuarios máximos institucionales.

Resultados ignorados locales: `backend/target/load-qa/long-documents-http.json`, `evidence-and-signing.json`, logs bajo `backend/target` y capturas bajo `frontend/test-results`. [Reproducción y límites](../testing/captcha-and-document-load.md).

## Operación y pasos manuales

No cambiar `.env` ni `.env.sample`. Para ver la versión nueva en el entorno principal Docker, realizar el respaldo habitual y reconstruir backend/frontend:

```powershell
# Desde la raíz utaped; usar el archivo de entorno/proyecto que ya corresponde a tu instalación.
docker compose --profile web up -d --build backend frontend
```

No ejecutar `down -v`, reset de semillas o fixtures de prueba sobre datos principales. V22 se aplica automáticamente al iniciar el backend. En esta entrega se reconstruyeron únicamente entornos ficticios de aceptación; no se desplegó el sistema principal o institucional. En desarrollo con Vite, reiniciar/recompilar el backend para disponer del endpoint de audio.

## Pendientes y siguiente etapa

1. Recorrido de aceptación con el cliente por roles y documentos, incluyendo pronunciación del CAPTCHA, teclado/lector de pantalla y personas que no pueden utilizar imagen/audio. No se declara WCAG integral ni protección infalible frente a reconocimiento automático; siguen vigentes credenciales, CSRF y throttling.
2. DTIC: concurrencia/volumen/SLA, prueba sostenida y carga de transporte HTTPS de certificados en infraestructura acordada. No aumentar semáforos sin medir CPU/memoria y comportamiento transaccional.
3. Confirmar reglas institucionales pendientes (bajas de ejecutores, permisos contextuales, ventanas/múltiples elaboradores) y formatos adicionales antes de implementar políticas definitivas.
4. Hostname, certificado/CA, SMTP y condiciones operativas de respaldos reales continúan pendientes según [preparación operativa](../operations/production-readiness.md).

Commits locales de implementación: `4d6f94b` (CAPTCHA sonoro) y `756dd43` (carga documental). Documentación/estado se registran en un commit posterior. Sin push, actualización de PR o merge, según instrucción vigente del propietario.

Fuentes: autorización del propietario del 2026-10-13, [requisitos](../../requirements.md), [contrato de acceso](../api/local-access.md), [W3C G143](https://www.w3.org/WAI/WCAG21/Techniques/general/G143), [W3C G144](https://www.w3.org/WAI/WCAG21/Techniques/general/G144.html), [recursos de voz y preparación](../../backend/src/main/resources/captcha-audio/README.md).
