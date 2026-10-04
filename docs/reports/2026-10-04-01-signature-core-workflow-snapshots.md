# Preparación de firma, núcleo criptográfico y snapshots de flujo

Fecha: 2026-10-04. Rama feature/signature-review-preparation, base develop a7ec62b (PR #6 fusionado). Alcance autorizado: .p12/.pfx y preparación técnica con finalización/envío/aprobación deshabilitados porque los flujos institucionales no están confirmados.

## Implementación

1. Snapshot documental con revisión técnica del flujo T1 y etapas/participantes resueltos. Captura consistente REPEATABLE_READ; cambios relevantes vuelven anterior la preparación conservada. Configuración sin órgano personal no inventa firmantes. Los snapshots/PDF históricos conservan sus datos y bytes; campo workflow opcional permite leer artefactos anteriores. No se requiere migración y no se cambian estados ni versión formal.
2. Consulta privada por targetDocId/artifactId con integridad/hash real, configuración congelada y bloqueos actuales. ADMIN no accede a documentos ajenos ni se concede acceso a futuros revisores. UI muestra requisitos pendientes, carga/error/actualización y configuración histórica; signingEnabled siempre false. Sin formulario de certificados/contraseñas ni endpoints de firma.
3. Núcleo interno Pkcs12PdfSigner con PDFBox 3.0.8 y Bouncy Castle 1.86. Firma CMS separada SHA-256 RSA ≥2048 / EC ≥256, guardado incremental que conserva el PDF original como prefijo y verificación posterior de bytes, ByteRange, certificado y criptografía. Huella SHA-256 vinculada y anclas de confianza explícitas obligatorias; vigencia, uso de firma, certificado personal y cadena PKIX offline comprobados. Rechaza contenedores con más de una clave, PDFs cifrados/ya firmados y materiales inválidos. Límites internos iniciales: contenedor 1 MiB y PDF 64 MiB; no son políticas institucionales ni modifican límites de anexos.
4. Certificado/contraseña consumidos solo en memoria y arrays recibidos borrados en finally, tanto en éxito como en fallo. No hay persistencia, logs o referencia permanente al material. JCA puede no permitir destruir claves/copies internas; no se afirma borrado absoluto de memoria JVM. Las pruebas generan claves/certificados exclusivamente en memoria y nunca incorporan .p12/.pfx/contraseñas reales al repositorio.
5. Decisión del propietario, pendientes Q-003/Q-007/Q-018, contrato y ficha por grupo actualizados. [Ficha de confirmación](../validation/workflow-signature-confirmation.md) lista la información concreta que debe validar la institución.

## Límites de la entrega

El núcleo no está conectado a HTTP, DB, asignaciones ni finalización. Su huella vinculada debe provenir en la integración futura de un registro confiable del servidor, nunca del pedido del usuario. No hay política institucional de enrolamiento del titular ni autoridades configuradas. La cadena offline no comprueba revocación y la fecha CMS no es sello TSA. No se declara cumplimiento PAdES ni aceptación institucional. Firma inicial invisible/única; geometría visible, múltiples firmas, perfil, revocación, idempotencia de operación persistida y transacciones de finalización siguen pendientes.

La revisión técnica configurada no ratifica condiciones ANY/ALL, decisiones colegiadas o permisos de ejecución. Ninguna consulta aprueba, firma o cambia DRAFT. T2, revisión/observaciones, ejecución/evidencias y agente de código/Trello continúan pendientes según estado.

## Verificación

- npm --prefix frontend run lint: correcto, sin advertencias después de corregir el reinicio de estado dentro del efecto.
- npm --prefix frontend run build: correcto (TypeScript y Vite).
- Suite completa con PostgreSQL aislado mediante compose.test.yml: 48 pruebas, cero fallos/errores/omitidas (42 de aplicación e integración y 6 del núcleo criptográfico). Se ejecutó la suite final después de corregir CMS y endurecer cobertura ByteRange.
- Casos nuevos: snapshot de flujo/órgano, cambios/nombres/deshabilitación, lectura de snapshot antiguo, historial exacto, acceso ajeno/ADMIN/retiro de pertenencia e integridad alterada. Criptografía RSA/EC real, contraseña/material incorrectos, huella ajena, confianza ausente/no admitida, certificado vencido/futuro/CA/no apto, clave débil/múltiple, PDF inválido/ya firmado y bytes originales/firmados/incrementales alterados o añadidos. Arrays comprobados borrados en éxito y fallo.
- Playwright contra Nginx/backend Docker: 2 escenarios ampliados aprobados (59,7 s). Consulta no-store, bloqueo de firma, ausencia de inputs de secretos, actualización y artefacto anterior tras quitar anexo; escenarios previos de identidad, recuperación, permisos, flujos, auditoría y notificaciones conservados.
- Captura móvil signature-preparation-mobile.png inspeccionada: textos/bloqueos legibles y acción de actualización visible. E2E comprueba ausencia de desbordamiento móvil y navegación de páginas. No se modificó la plantilla visual del PDF ni se afirma revisión institucional del documento.
- git diff --check: correcto; revisión de archivos rastreados y atribución antes de commits. Certificados reales, secretos, logs, capturas y outputs permanecen excluidos.

Catálogo FR-SIGN-001–004 actualizado a PARCIAL con alcance explícito del núcleo interno, manteniendo estado heredado y aceptación pendiente. FR-SIGN-005/ubicación visible y revisión productiva no se declaran completados.

Se detectaron y corrigieron durante desarrollo: tipado del selector CMS de Bouncy Castle y discrepancia entre signingTime CMS y reloj de validación del certificado. La firma ahora usa el mismo instante para metadata CMS/PDF y comprobación PKIX.

## Fuentes y decisión técnica

Se reutiliza PDFBox existente para actualización incremental y se agrega únicamente bcpkix-jdk18on 1.86 (incluye proveedor/utilidades transitivos) para CMS/PKIX, compatible con Java 21. Evita implementar CMS manualmente o incorporar un servicio completo antes de disponer de contrato.

- [Ejemplo oficial PDFBox de la versión 3.0.8](https://github.com/apache/pdfbox/blob/3.0.8/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateSignatureBase.java).
- [Distribución oficial Bouncy Castle Java, versión 1.86 y Java 1.8+](https://www.bouncycastle.org/download/bouncy-castle-java/).
- [Licencia Bouncy Castle](https://github.com/bcgit/bc-java/blob/main/LICENSE.html), mantenida por las dependencias originales; no se copió código de terceros.

## Siguiente implementación

## Publicación

Commits por bloque: 0575af4 (snapshot/consulta y pruebas), c5bcd87 (núcleo criptográfico y pruebas), 3215789 (UI, trazabilidad y contrato). Rama publicada y [PR #7 abierto hacia develop](https://github.com/Andriu-Dex/utaped/pull/7). Este reporte y la continuación de la inspección inicial se incluyen en un commit documental final de la misma rama. CI remoto disparado por push/PR; los resultados locales descritos están verificados y la revisión/fusión no se ejecutan automáticamente. Contenedores aislados retirados sin borrar volúmenes.

## Siguiente implementación

Completar la ficha por grupo y la política de identidad/confianza/revocación/perfil. Con esas decisiones, integrar firma autorizada al artefacto exacto, persistir el nuevo resultado relacionado con reintentos seguros y habilitar finalización/asignación/revisión conforme al flujo confirmado. Hasta entonces mantener los bloqueos actuales. Mockup congelado intacto, main intacta y fusión a develop a cargo del propietario.
