# Cinco pasos y firma con certificado aportado

Cambios anteriores revisados y guardados en e778dd6, sin push. El propietario autorizó eliminar la vinculación administrativa como requisito y reducir el T1 a cinco pasos. Se mantiene firma real; no se introducen simulaciones.

## Flujo y UI

Información general → Contenido → Actividades y matriz → Anexos y opciones → Revisar y firmar. Los enlaces antiguos step=preparation abren el último paso. La matriz sigue requiriendo actividades para avanzar desde su editor; la revisión final puede mostrar pendientes y no prepara/firma contenido incompleto.

La preparación es automática y reutiliza el artefacto vigente. El documento ocupa el ancho completo; debajo se confirma su revisión y se abre el formulario de firma. Elaboradores, ubicaciones, secciones adicionales, constancias y circuito siguen accesibles en un desplegable. T2/T3 conservan sus pasos y utilizan la nueva firma.

Seleccionar .p12/.pfx + contraseña → inspeccionar → confirmar titular/emisor/vigencia → firmar. Cancelar no aplica firma ni inicia revisión; el formulario y buffers de operación se limpian. Los certificados públicos administrativos quedan como historial de solo lectura en UI, sin condición previa para firmar.

## Firma y trazabilidad

- La inspección verifica apertura, una clave privada, uso de firma, vigencia, algoritmo/fuerza admitida y prueba criptográfica clave/certificado.
- Se compara de nuevo la huella confirmada con el archivo utilizado en la firma. Se conservan sesión, CSRF, HTTPS, permisos por documento/etapa, turnos, idempotencia, hash, posición real e integridad incremental.
- V36 conserva binding_id histórico y permite null para nuevas firmas. Se registra la cuenta ejecutora separada de holder/subject, emisor, serie, huella y vigencia. La apariencia del PDF usa el titular real del certificado. Firmas previas, sus documentos y sus bytes se preservan.
- Claves y contraseñas no se guardan en DB, archivos, logs o almacenamiento del navegador. El endpoint de inspección devuelve solo metadata pública, con no-store, límite de entrada y concurrencia acotada.

## Límites de atribución y confianza

Por decisión explícita del propietario, no hay prueba institucional confiable de correspondencia entre cuenta y titular del certificado. Control de la clave no demuestra esa equivalencia; no se infiere del CN/nombre. La auditoría distingue ambos. La comparación de huella protege la continuidad del certificado confirmado, no acredita identidad civil. No se comprueban confianza de CA, revocación ni sello temporal confiable. La aceptación institucional requiere contrato aparte. Estas limitaciones quedan en docs/, sin avisos de limitación en UI; tampoco se mantienen afirmaciones falsas de identidad vinculada.

## Verificación

Pruebas de criptografía, documentos y revisión: 64 aprobadas. La suite general ejecutó 214 casos; se corrigió una aserción antigua sobre mayúsculas de la portada y se repitieron 9 pruebas de escenarios preparados/criptografía. Los reportes finales suman 214 pruebas, 0 fallos y 0 errores. Se comprueba firma sin vinculación, metadata del titular distinta del usuario, huella confirmada, contraseña incorrecta, vigencia, integridad y limpieza de material. La aceptación incluye cancelación tras mostrar titular, firmas de autor/revisor sin vínculo, devoluciones y T2/T3. Lint/TypeScript/build correctos; aviso de bundle conocido sin cambios de arquitectura.

Implementación sin commit para revisión. Evidencia local ignorada en backend/target/2026-10-27-provided-certificate-*.log y 2026-10-27-five-steps-certificate-*.log.

La inspección queda asociada al contexto activo de documento/revisión: si se abandona ese contexto antes de confirmar, no se abre un diálogo tardío ni se aplica una nueva firma desde esa operación. La navegación detecta el material pendiente durante la comprobación. Se verifican cancelación tras mostrar titular, limpieza de campos y ausencia de firmas nuevas al cancelar.

Cierre de verificación: 24 pruebas de aceptación aprobadas en la última ejecución (2026-10-27-five-steps-certificate-final-guard.log), tras proteger el contexto de la operación. Los reportes backend finales suman 214 casos sin fallos ni errores. Los entornos descartables de backend/aceptación se limpiaron. utaped-sample se actualizó sin restablecer datos y Vite se volvió a iniciar de forma oculta para la revisión local. La nueva implementación permanece sin commit.
