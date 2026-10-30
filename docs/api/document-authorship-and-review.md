# Elaboradores y decisiones de revisión

## Elaboradores por documento

`GET /api/work-plans/{targetDocId}/authors` devuelve rowVersion, editable, authors y candidates. Cada persona incluye id, name y position; los cargos proceden de la pertenencia del ámbito. El principal siempre ocupa el primer lugar y conserva la edición. Los responsables de actividades son una selección independiente.

`PUT /api/work-plans/{targetDocId}/authors` recibe `{rowVersion, coauthorIds}`. Requiere sesión, CSRF, permiso del principal, borrador editable, versión vigente y coautores activos del mismo ámbito. Máximo técnico: 19 coautores además del principal, sin duplicados. Concede acceso documental explícito a los elegidos. No concede edición ni un rol administrativo. Un coautor retirado de la selección ya no puede firmar un artefacto nuevo por su antigua selección; el acceso de lectura previamente concedido sigue sujeto a las reglas del ámbito.

La configuración empieza cerrada, mediante Configurar elaboradores. El orden es principal primero y después orden de selección de coautores. Se puede volver a dejar únicamente al principal. Configurar elaboradores invalida la preparación anterior por rowVersion; no modifica artefactos anteriores.

## Recopilación de firmas

Las firmas usan la ruta existente `/artifacts/{artifactId}/signing` y el material temporal p12/pfx. Cada usuario solo firma en su turno con el certificado aportado, inspeccionado y confirmado en la operación, sin vinculación administrativa obligatoria (decisión vigente del 2026-10-27; véase author-signing.md). La respuesta de estado incluye pendingAuthors, actorSigned, firmas conservadas y el hash exacto de entrada para la siguiente firma.

1. El artefacto congela nombres, cargos y slots de todos los elaboradores.
2. La primera firma bloquea edición, matriz, anexos y gestión del contenido. El documento sigue en DRAFT mientras reúne firmas, con una indicación específica en la UI; no se introduce un estado documental nuevo.
3. Cada firma siguiente se añade incrementalmente al PDF anterior, comprobando firmas/bytes previos. El hash enviado por el cliente debe corresponder a la cadena vigente.
4. La última firma inicia revisión en la misma transacción. Con un solo elaborador, esto ocurre en su primera firma, como antes.
5. Una devolución habilita una nueva ronda y conserva todas las firmas/artefactos anteriores. La nueva preparación vuelve a requerir firmas para sus propios bytes.

Notificación privada AUTHOR_SIGNATURE_REQUESTED al siguiente elaborador. La revisión no recibe tareas antes de completar la autoría. Bloqueo de fila y versión evitan firmas concurrentes incompatibles, edición durante recopilación y envíos duplicados. Idempotencia por actor/requestKey conserva el resultado de la operación original. Se mantiene bloqueo de descarga final antes de APPROVED.

Flyway V24 incorpora document_author, signature_order y el tipo de notificación AUTHOR_SIGNATURE_REQUESTED. No reescribe archivos firmados ni rellena coautores de documentos históricos. Nuevas versiones formales conservan la selección inicial; el principal puede ajustarla antes de firmar. T1/T2 disponen de selección independiente; no se convierte a cada responsable de actividad en elaborador.

## Órganos colegiados y cargos

Decisión del propietario del 2026-10-17: Administración designa a una persona que registra la decisión colegiada por período. No es un firmante personal del órgano. Se utilizan los participantes congelados al activar el período.

Una etapa COLLEGIATE con requiresSignature=false exige exactamente un assigneeId: el registrador. Debe ser una cuenta activa; actúa en la secuencia y queda identificado en auditoría. En la matriz PDF se muestra el órgano (recipientLabel), sin atribuirle firma ni mostrar al registrador como firmante. Una etapa colegiada con firma personal sigue requiriendo representantes explícitos y slots reales. No se asigna ni aprueba automáticamente un órgano sin registrador.

Los flujos admiten positionLabel opcional (máximo 160 caracteres), capturado como cargo de sus participantes en el snapshot y representado en T1/T2. Los datos históricos sin cargo conservan su ausencia; no se inventa un nombramiento. Esto no entrega todavía un catálogo institucional completo de cargos ni autoriza reglas de excepción de firma no ratificadas.

## Observaciones de revisión

Una observación vigente de la ronda impide COMPLETE y firma/completado de etapa, mediante validación del servidor y comprobación en UI. La devolución (RETURN) exige al menos una observación vigente o un mensaje explícito, abriendo una nueva ronda de corrección. No se bloquea una nueva ronda por observaciones de rondas anteriores ni se permite «marcar resuelta» como atajo de aprobación.

Se habilitan la actualización y el retiro explícito de observaciones propias mientras la etapa asignada esté ACTIVE y la ronda permanezca IN_REVIEW:
- `PUT /api/reviews/tasks/{taskId}/observations/{observationId}`: actualiza el texto y opcionalmente la geometría/página manteniendo la identidad original y versionamiento por `rowVersion`. Registra evento de auditoría `REVIEW_OBSERVATION_UPDATED` e historial en `review_observation_history`.
- `DELETE /api/reviews/tasks/{taskId}/observations/{observationId}?rowVersion={rowVersion}`: retiro lógico (establece `deleted_at` y `deleted_by`), desvincula la nota como bloqueador de aprobación o justificación de devolución, registra evento `REVIEW_OBSERVATION_DELETED` e historial de retiro en `review_observation_history`.
- Preservación estricta de asociación de página: navegar entre páginas del PDF no altera la página vinculada a la observación salvo acción explícita del usuario («Asociar a página actual» o trazado de nueva zona). Convertir a observación general activa la detección de cambios pendientes (`isFormDirty`) y «Cancelar edición» solicita confirmación previa con salvaguarda de datos antes de descartar cambios.
- Archivo y custodia institucional: los registros de `review_observation_history` se incorporan dentro del manifiesto histórico de cada documento (`documents/{id}/manifest.json`) en el ZIP de custodia del período antes de cualquier purga.
- Normalización de presentación: las etiquetas heredadas exactas «Revisión de ejemplo» (REVIEW) y «Aprobación de ejemplo» (APPROVE) se presentan dinámicamente como «Revisión documental» y «Aprobación del documento», preservando intactos los registros históricos persistidos.

## Documento institucional

El encabezado T1/T2 utiliza la misma composición: logo oficial conservado, universidad, título, unidad/carrera en columnas legibles y fecha. Se eliminan el espaciado excesivo y las proporciones heredadas que comprimían textos y estiraban la fila de fecha. El título no repite el prefijo automático. Los cargos y elaboradores se dibujan antes de calcular páginas/slots.

La versión del motor cambia para nuevas preparaciones. Regenerar una previsualización de borrador incorpora el formato corregido; un PDF ya firmado conserva su apariencia y bytes históricos. A4, matriz horizontal, paginación dinámica y anexos originales se mantienen.
