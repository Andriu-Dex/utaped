# Contrato propuesto de firma y revisión T1

Estado: mecanismo CONFIRMADO-PROYECTO; política y flujos PENDIENTES DE VALIDACIÓN INSTITUCIONAL. Preparado el 2026-10-03, actualizado el 2026-10-04. El propietario eligió archivos .p12/.pfx y confirmó que los flujos aún no están ratificados. Autoriza avanzar manteniendo finalización/aprobación bloqueadas. Q-003/Q-018 siguen abiertas; Q-007 queda parcialmente resuelta solo para el mecanismo.

## Decisiones necesarias

1. Mecanismo: archivos .p12/.pfx con contraseña, confirmado por el propietario. No se requiere seleccionar una API DTIC para esta preparación.
2. Política técnica: formato/perfil de firma, identificación del titular, certificados/cadena admitidos, revocación, tiempo de firma/sellado, conectividad y errores. La validez criptográfica no acredita automáticamente confianza o aceptación institucional.
3. Flujos por grupo: etapas y destinatarios, representación de órganos colegiados, quién firma y quién solo decide, revisores obligatorios y condición de avance. La configuración administrativa existente no constituye ratificación institucional.
4. Operación: ventanas, pérdida de pertenencia, corrección/devolución, reintentos y fallos. Q-006/Q-017 permanecen pendientes; no crear excepciones automáticas.

## Invariantes ya exigidas

- Identidad autenticada y asignación explícita, sin sustituir identidad al cambiar contexto.
- Artefacto exacto: conservar los bytes y hash del PDF de entrada; el resultado firmado constituye un nuevo artefacto relacionado, nunca reemplaza la previsualización conservada.
- Conservar revisiones y firmas previas; nueva corrección/ronda no reutiliza firmas como si fueran válidas para contenido distinto.
- Certificado y contraseña solo durante la operación; sin logs, DB, localStorage, serialización de DTO persistido ni almacenamiento permanente. Definir transporte y tratamiento de secretos antes de habilitar una ruta de firma.
- Error, cancelación, ausencia de proveedor o validación fallida no finalizan ni envían el documento.
- Página/ubicación derivada de composición real. El artefacto actual identifica la página de elaboración; la geometría y espacios de revisores aún deben integrarse al PDF correspondiente.

## Contrato técnico propuesto

Entrega 2026-10-04: los nuevos snapshots T1 conservan revisión técnica de flujo y participantes resueltos, sin crear asignaciones. El endpoint de preparación verifica integridad, muestra esa configuración histórica y devuelve siempre signingEnabled=false con bloqueos explícitos. No recibe certificados ni contraseñas. El núcleo criptográfico interno funciona y se prueba con certificados generados en memoria, pero no está conectado a una ruta de firma o transición.

PDFBox 3.0.8 y Bouncy Castle 1.86 generan CMS separado SHA-256 (RSA ≥2048 o EC ≥256), guardado incremental y verificación del resultado. Requiere huella pública vinculada y anclas de confianza explícitas; comprueba vigencia, uso y cadena PKIX offline. Ninguna política institucional de identidad/confianza se ha configurado. Revocación, TSA, perfil PAdES e identidad de cuenta aún requieren contrato; por tanto este núcleo no constituye firma institucional habilitada. Firma invisible y única inicial: geometría visible y firmas múltiples siguen pendientes.

La vinculación futura debe obtener la huella desde un registro confiable del servidor, jamás desde el cuerpo del pedido del firmante. Las raíces no se toman del archivo como autoridades confiables. La [ficha de confirmación](../validation/workflow-signature-confirmation.md) permite avanzar con la institución sin rellenar reglas por suposición.

Preparación contiene targetDocId, artifactId, hash verificado, identificador de la revisión de flujo, identidad autenticada y contador de edición esperado. Estos identificadores se vuelven a validar en el servidor. Una requestKey debe evitar firmas/envíos duplicados al reintentar.

Un adaptador de firma recibe únicamente el artefacto autorizado y el material temporal que permita el mecanismo elegido; devuelve resultado firmado y evidencias verificables. El servidor valida correspondencia con la entrada, integridad, identidad y política configurada antes de persistir o cambiar estados. Respuesta sin firma verificable no se acepta. Nunca incorporar un adaptador que responda éxito simulado.

El snapshot documental debe fijar configuración del flujo, participantes resueltos y metadata de firmas antes de generar el artefacto que se firma. Cambiar configuración, nombres o pertenencias después no reescribe ese artefacto. Antes de iniciar la operación se comprueba vigencia y se solicita regeneración cuando corresponda; la política sobre cambios posteriores debe acordarse.

La transición final debe coordinar persistencia del artefacto firmado, asignación de primera etapa y auditoría/notificaciones sin perder consistencia. El esquema actual de work_plan admite únicamente DRAFT: no ampliar estados ni conceder acceso a revisores hasta definir las reglas.

## Casos de aceptación a implementar tras las decisiones

1. Documento ajeno, contexto alterado, participante futuro o rol revocado: rechazo sin firma ni exposición de archivo.
2. Contenido/artefacto/contador cambiado entre preparación y operación: conflicto, con contenido local preservado.
3. Archivo inválido, contraseña errónea, certificado vencido/no admitido, identidad distinta, proveedor caído o resultado alterado: ningún avance; sin secretos persistidos.
4. Operación repetida con la misma requestKey: mismo resultado o estado recuperable, sin segunda asignación/firma ficticia.
5. Artefactos anteriores recuperables con hash y bytes originales; firma posterior no reexporta el documento editable.
6. Cada revisor ve exactamente el artefacto de su etapa/ronda; etapas futuras no acceden anticipadamente.
7. Paralelismo y órganos colegiados siguen la regla confirmada, sin atribuir una decisión a quien no actuó.
8. Devolución conserva observaciones/firmas anteriores como historia y origina la ronda acordada; no altera versión formal por suposición.

## Referencias técnicas consultadas

[Ejemplo oficial PDFBox de firma](https://github.com/apache/pdfbox/blob/trunk/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateSignature.java) muestra escenarios de firma y firma externa. Es una referencia técnica, no un adaptador ya incorporado ni un contrato DTIC.

[ETSI EN 319 142-1](https://www.etsi.org/deliver/etsi_EN/319100_319199/31914201/01.01.01_60/en_31914201v010101p.pdf) diferencia perfiles PAdES B-B/B-T/B-LT/B-LTA. Seleccionar perfil y servicios asociados requiere la decisión técnica/institucional; este documento no declara cumplimiento PAdES.
