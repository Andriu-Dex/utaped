# Firma visible del elaborador T1

Fecha: 2026-10-05. Base: develop bde162d (PR #7 fusionado). Rama: feature/visible-author-signing. Mockup externo congelado, sin modificaciones. Entrega limitada a firma del artefacto; ninguna regla de aprobación institucional se presupone.

## Resultado

El elaborador selecciona su .p12/.pfx, introduce contraseña y pulsa Firmar. Se produce una firma criptográfica CMS real y visible en la tabla institucional del PDF; visor y descarga muestran el resultado conservado. Una vinculación administrativa previa de la huella pública identifica el certificado autorizado para la cuenta. No se almacena contenedor privado, contraseña o clave privada.

El PDF firmado es independiente e inmutable. Se conserva el PDF de entrada íntegro como prefijo de la revisión incremental y se comprueba la firma antes de guardar. El Plan sigue DRAFT: editar el borrador no modifica el PDF firmado y exige otro artefacto para firmar contenido nuevo. No habilita finalización, envío, asignaciones, revisión ni aprobación.

## Cambios

- Geometría de firma derivada de marcadores de composición dentro de la celda real; limpieza de marcadores y versión de motor engine2. Página, dimensiones y cantidad de páginas derivadas del PDF, incluso con anexos.
- Vinculación de certificado público por ADMIN, comprobación manual de identidad y referencia obligatoria; unicidad, control de modificación, desvinculación e historial. Huella del servidor, nunca proporcionada por el firmante para autorizarse.
- Rutas de firma/consulta/descarga/imagen con IDs explícitos, sesión, CSRF y autorización actual por documento. Administración no obtiene acceso documental adicional.
- Solicitud binaria acotada en memoria: hasta 1 MiB PKCS12 y 512 bytes UTF-8 de contraseña. Sin multipart temporal, DTO de secretos, URL de secretos o persistencia en navegador. Limpieza de arrays y campos tanto en éxito como en error.
- Comprobación de identidad por huella, vigencia, certificado personal no CA, uso de firma, RSA ≥2048/EC ≥256 y firma matemática. El modo PKIX anterior conserva anclas explícitas; la ruta actual usa confianza directa por huella.
- Resultado privado e independiente con hashes, nombre histórico y fecha; reintentos idempotentes, limpieza transaccional de archivos en fallo y auditoría de firma/vinculación/desvinculación.
- HTTPS requerido por defecto; SIGNING_ALLOW_HTTP=true solo para desarrollo aislado y E2E. Documentación del transporte, operación y contrato API actualizada.
- Catálogo FR-SIGN-001–005 actualizado como PARCIAL: elaborador entregado, aceptación institucional y múltiples firmantes pendientes.

## Validación

Validación local final: 51 pruebas backend con PostgreSQL aislado sin fallos; frontend lint/build correctos; 2 escenarios Playwright E2E correctos contra Docker/Nginx. Suite completa repetida tras el ajuste final de apariencia y el caso de reutilización de requestKey. Pruebas cubren firma visible y preservación exacta, ubicación/paginación con anexos, contraseña errónea, otro certificado, transporte inseguro, contador desactualizado, huella desvinculada, idempotencia y reutilización conflictiva de requestKey. Comprueban autorización por documento/pertenencia y ausencia de nuevos archivos/resultados ante error. Certificados de pruebas generados expresamente; no se usan certificados personales reales.

E2E verifica vinculación administrativa, limpieza de campos ante contraseña errónea, firma exitosa, imagen del PDF firmado y preservación de su descarga tras modificar anexos. El fixture OpenSSL usa configuración temporal explícita para funcionar tanto en Windows como en CI Linux. Un primer intento E2E falló por configuración OpenSSL ausente; corregido y repetido correctamente. El esquema PostgreSQL aislado utaped_test se reinicializó después de una modificación de V11 todavía no publicada para evitar un checksum local obsoleto; no se borraron volúmenes ni datos del entorno de aplicación.

Inspección visual del PDF rasterizado: firma dentro de su celda y página efectiva, encabezado/pie/tabla preservados y sin marcadores. La primera etiqueta resultaba demasiado larga y se acortó a “Firma electronica”; se renderizó y revisó de nuevo. Inspección de captura móvil del visor: texto y acciones legibles, estado de verificación explícito y navegación consistente.

## Límites y siguiente paso

Cadena emisora y revocación NO COMPROBADAS y mostradas expresamente. No se declara validación jurídica, TSA, confianza automática de visores o cumplimiento PAdES. La hora procede del servidor. La calidad de la vinculación depende de la comprobación administrativa por canal confiable. El PDF conserva la firma y certificado público necesarios para verificarlo.

Arrays propios se limpian, pero navegador/JVM/librerías pueden mantener copias temporales; no se garantiza borrado absoluto de memoria. La operación productiva debe excluir captura de cuerpos y definir HTTPS/proxies confiables, memoria/volcados, custodia y retención. El entorno Compose HTTP no es despliegue institucional.

Después de revisión/fusión del PR, confirmar Q-003/Q-018 mediante la ficha de flujos por grupo para implementar finalización y revisión sin inventar decisiones institucionales. Política de confianza, revocación, sellado de tiempo, múltiples firmas y aceptación visual institucional siguen pendientes. [Contrato](../decisions/signature-review-contract.md), [API](../api/author-signing.md), [ficha](../validation/workflow-signature-confirmation.md).

## Referencias técnicas

[PDFBox 3.0.8: firma visible](https://github.com/apache/pdfbox/blob/3.0.8/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateVisibleSignature2.java). [Spring MVC: lectura de cuerpos](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html). Son referencias de implementación, no acreditan aceptación institucional.
