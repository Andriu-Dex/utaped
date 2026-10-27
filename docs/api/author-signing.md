# Firma visible del elaborador T1

## Política vigente — 2026-10-27

Por decisión expresa del propietario, la firma de elaboradores y revisores NO requiere una vinculación administrativa previa. El sistema es el producto final, con firma criptográfica real.

1. POST `/api/signing/certificate-preview` (sesión, CSRF, HTTPS; excepción de HTTP únicamente en desarrollo configurado): contenedor binario con longitud de contraseña UTF-8 en 4 bytes big-endian, contraseña y archivo PKCS#12. Máximo 1 MB de contenedor. Devuelve exclusivamente holder, subject, issuer, serial, fingerprint SHA-256 y fechas de vigencia. Comprueba apertura, clave única, uso de firma, fortaleza admitida, vigencia y correspondencia criptográfica clave/certificado; no guarda el material.
2. El usuario confirma el titular mostrado. POST de firma de autor o etapa incorpora además `certificateFingerprint` como parámetro requerido, junto con rowVersion, inputHash y requestKey. El servidor reabre el material y exige que la huella coincida con la confirmada; vuelve a comprobar permiso, turno, estado, hash y ubicación.
3. V36 permite binding_id nulo y guarda el nombre de la cuenta que ejecutó la acción separado del titular/subject, emisor, serie, huella y vigencia del certificado. La apariencia PDF usa el titular real del certificado. Las firmas previas y sus bytes permanecen intactos; su política histórica no se reetiqueta.

**Límite de atribución:** sin un identificador institucional confiable, el sistema NO demuestra que la cuenta autenticada y el titular del certificado sean la misma persona. La posesión de clave y contraseña prueba control de esa clave, no esa asociación. El CN no se compara con el nombre de cuenta ni se usa como evidencia independiente. La auditoría debe distinguir ambos actores/datos. Tampoco se afirma confianza de cadena emisora, revocación o sello de tiempo: permanecen como NOT_CHECKED/no integrados. La aceptación institucional de esos aspectos requiere validación específica.

Las rutas administrativas de certificados se conservan para compatibilidad e historia; el historial visible no registra nuevas vinculaciones ni condiciona la firma. Retirar una vinculación antigua no impide el nuevo flujo ni altera firmas anteriores. Las secciones de entrega 2026-10-05 que siguen describen la implementación histórica y no son requisitos actuales.


Actualización 2026-10-12: en nuevos documentos por ámbito, firmar inicia automáticamente la revisión secuencial y permite firmas posteriores. Los documentos individuales anteriores conservan su comportamiento histórico. El contrato actual y las condiciones de descarga están en [circuito documental](document-circuit.md).

Entrega 2026-10-05. Sesión JDBC/CSRF, propietario activo y pertenencia vigente; ADMIN no obtiene acceso documental por su rol. Todas las respuestas privadas llevan no-store. No hay transición desde DRAFT ni acceso de revisores.

## Vinculación administrativa

Solo ADMIN: GET/POST `/api/admin/users/{userId}/signing-certificates`; DELETE `/{bindingId}` desvincula. POST JSON: fingerprint (64 dígitos hex SHA-256), identityVerified=true, verificationNote (1–500 caracteres), expectedBindingId (UUID activo o null). Solo cuentas activas; una huella activa por cuenta y una cuenta activa por huella. Conflictos de modificación o reutilización: 409. Repetir misma huella no duplica registro/evento. La operación conserva historia y autor administrativo.

Obtener la huella del certificado público X.509 (DER), no del archivo .p12 ni del archivo PEM completo. Puede consultarse con `openssl x509 -in certificado-publico.pem -noout -fingerprint -sha256`; quitar separadores `:` antes de introducirla. Verificar por canal confiable identidad y pertenencia del certificado a la cuenta y registrar referencia de esa comprobación, sin datos privados. No pedir al usuario que envíe su .p12 ni contraseña a administración. Una vinculación equivocada compromete la atribución de la firma; no basta el nombre CN.

## Rutas documentales

Base `/api/work-plans/{targetDocId}`:

| Método/ruta | Resultado |
|---|---|
| GET /artifacts/{artifactId}/signing | preparation, signingEnabled, signingBlockers, signed. Condiciones de firma del elaborador independientes de las de finalización. |
| POST /artifacts/{artifactId}/signing?rowVersion=…&inputHash=…&requestKey=… | Firma única del elaborador sobre artefacto vigente, con hash/contador exactos. |
| GET /signed-artifacts/{signedId}/content | Ruta preliminar bloqueada (403), también para documentos anteriores sin ámbito. La descarga final autorizada usa `GET /reviews/{id}/content` tras aprobación. |
| GET /signed-artifacts/{signedId}/pages/{pageIndex} | PNG del mismo PDF, índice base cero. |

POST usa Content-Type application/octet-stream y cabecera CSRF. Cuerpo: 4 bytes big-endian con longitud UTF-8 de contraseña, contraseña (1–512 bytes), contenedor PKCS12 (hasta 1 MiB). Máximo cuerpo: 1,049,092 bytes. La interfaz limita contraseña a 128 caracteres y limpia campos al iniciar/terminar. No enviar secretos en parámetros, JSON, logs, almacenamiento del navegador o multipart. HTTPS por defecto. SIGNING_ALLOW_HTTP=true solo en entorno local aislado; el Compose E2E lo configura expresamente.

requestKey es UUID de operación por usuario. Reintento con mismo documento/artefacto/hash/contador devuelve resultado conservado sin segunda firma/evento; reutilización con distintos metadatos da 409. La recuperación mantiene autorización e integridad actual, aunque la huella se haya desvinculado. Errores: 400 contraseña/contenedor/certificado inválidos, 403 transporte/CSRF, 404 fuera del ámbito, 409 preparación/contador/hash/concurrencia, 413 límite, 503 firma ocupada. Cualquier error deja DRAFT y no crea resultado parcial persistido.

Resultado firmado contiene identidad histórica, hora del servidor, hashes de entrada/salida, páginas y ubicación efectiva. identityCheck=ADMINISTRATIVE_CERTIFICATE_PIN, issuerChainCheck=NOT_CHECKED, revocationCheck=NOT_CHECKED. El PDF incluye certificado público, no clave privada. No garantiza hora externa, confianza del visor Adobe, TSA, PAdES o validez institucional.

## Operación y alcance

No publicar con SIGNING_ALLOW_HTTP=true. Configurar TLS de modo que request.isSecure refleje HTTPS; detrás de proxy, establecer previamente un contrato de proxies confiables y terminación TLS. No aceptar cabeceras Forwarded de clientes arbitrarios como autorización de transporte seguro. La configuración local HTTP no resuelve este despliegue pendiente.

No activar captura de cuerpos en proxy, APM, depuración o logs. Revisar tratamiento de secretos y memoria/volcados de proceso antes de despliegue institucional. Los arrays propios se limpian, pero no existe garantía de borrar todas las copias internas del runtime o del navegador. El archivo del usuario permanece en su dispositivo.

Conservar volumen privado y backups conforme a la política que se confirme. Edición del borrador y cambios de configuración no regeneran PDFs firmados históricos. Una firma por artefacto/elaborador; múltiples firmantes y rondas se implementarán tras confirmar flujos. La aceptación visual/legal institucional del formato y la política de confianza/revocación siguen pendientes.
