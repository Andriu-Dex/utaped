# Trabajo personal y comparación de rondas

Decisión del propietario: 2026-10-28.

## Mis pendientes

GET `/api/pending-work?kind=&page=0&size=10`, sesión autenticada. Respuesta privada `no-store`: items, total filtrado, page, size, counts por CORRECTION/REVIEW/SIGNATURE/EVIDENCE. Tamaño 1–50, página 0–1000000; categoría desconocida devuelve 400. Identidades y permisos se resuelven en servidor; ser ADMIN no otorga tareas documentales ajenas.

- Corrección: documento DRAFT devuelto, con responsable principal habilitado para edición.
- Firma: siguiente elaborador pendiente del artefacto preparado en la versión actual. También se muestra cuando existen bloqueos, con acceso a consultar requisitos; no se habilita la firma por mostrar una tarea.
- Revisión: asignación ACTIVE de la ronda IN_REVIEW, en período activo y con acceso documental vigente. No listar etapas futuras, completadas o canceladas.
- Evidencia: actividad asignada al usuario con al menos un medio sin respaldo; solo T1 aprobado vigente, sin trámite formal bloqueante. Se respeta el plazo personal extendido; una actividad vencida permite consultar, sin habilitar carga fuera de plazo. Una versión de evidencia cubre el medio y no crea tareas por cada versión histórica.

Primero plazos vencidos; luego correcciones, decisiones, firmas y evidencias, con orden de plazo/título/identificador estable. Filtros por tipo, paginación y actualización. Las acciones apuntan al identificador explícito del documento y al espacio correspondiente. Resolver una tarea se deriva de los datos actuales; leer o descartar una notificación no la resuelve. Notificaciones conservan su finalidad de aviso.

## Comparación

GET `/api/reviews/{id}/comparison`: autoriza el documento de la ronda solicitada y el de su ronda anterior. Solo compara la inmediatamente anterior del mismo documento; la primera devuelve previousRound/previousReviewId nulos. Devuelve identificadores/números de ronda y apartados modificados con textos anterior/actual. Sin descarga previa a aprobación ni acceso a documentos ajenos.

Se leen snapshots conservados, no borradores ni PDF regenerados. Se comparan información general, justificación/objetivo, matriz, anexos, elaboradores, flujo, nota de datos, informes T2/T3, referencias y secciones adicionales. Es comparación de contenido por apartado, no un diff de píxeles ni una certificación de equivalencia. Firmas y fecha de preparación se consultan en los PDF históricos. Cambios en archivos/configuración pueden mantener el mismo resumen de texto: la interfaz lo identifica.

La comparación se abre explícitamente en el inspector de revisión y mantiene un único visor PDF. Secciones desplegables con antes/ahora, teclado, scroll de textos extensos y tema oscuro.

## Comprobación previa a firma

No introduce una validación institucional nueva. Presenta readiness y signingBlockers actuales en grupos apropiados para T1/T2/T3. En T1, T2 y T3 permite ir al apartado pendiente, pasando por la guarda de cambios sin guardar; para bloqueos de turno, administración, período o certificado no ofrece una edición que el usuario no puede realizar. Conserva confirmación de revisión del PDF, contraseña/certificado transitorios y verificación del servidor al firmar. No permite firmar por haber marcado visualmente una lista.

La consulta personal compone candidatos autorizados y reutiliza los servicios vigentes de evidencia/firma; no mantiene una copia paralela de tareas. No se ha dimensionado para carga institucional masiva en esta entrega. El lanzador backend admite -TestPattern para regresiones Maven dentro de la misma BD desechable y con limpieza habitual.
