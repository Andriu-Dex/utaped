# Elaboradores, decisiones de revisión y encabezados institucionales

Fecha: 2026-10-18. Rama: `feature/document-authorship-and-review`. Implementación: `85ff0be`.

## Resultado y alcance

Se implementa la tanda autorizada tras la auditoría de reuniones. Incluye varios elaboradores con un único editor, firmas sucesivas T1/T2, registro de decisiones colegiadas por una persona designada y corrección del encabezado documental. Se conserva Prototipos como referencia externa sin modificarlo.

| Brecha de la auditoría | Resultado de esta entrega |
|---|---|
| A02 — Varios elaboradores | Selección independiente por documento; principal primero y coautores activos del ámbito en orden de selección. Solo el principal edita. |
| A03 — Aprobar con observaciones | Servidor y UI bloquean completar o firmar una etapa si existen observaciones de la ronda. Corresponde devolver y corregir; rondas anteriores no bloquean la nueva. |
| A04 — Órgano colegiado | Administración asigna un registrador por período. Actúa en secuencia y queda en auditoría; el PDF muestra el órgano, sin atribuir firma personal al registrador. No hay aprobación automática. |
| A05 — Cargos | Se congelan y representan cargos de elaboradores y participantes. El catálogo institucional completo y la resolución automática por cargo siguen pendientes. |
| A14 — Denominación | Paso «Firma y Finalización», conservando el envío automático tras la última firma. |
| Encabezados T1/T2 | Composición compartida, logo original, columnas legibles de unidad/carrera, universidad, título y fecha, conservando A4 y el carácter formal del documento. |

## Firmas y conservación documental

La primera firma bloquea el contenido, matriz, anexos y selección de elaboradores. Mientras faltan firmas el documento conserva DRAFT, con indicación de recopilación en la interfaz. El siguiente coautor recibe un aviso privado y solo puede firmar en su turno con su identidad y certificado vinculados.

Cada firma se añade al PDF anterior de forma incremental, con hash de entrada vigente y comprobación de integridad/firmas previas. La última activa la revisión en la misma transacción. Los slots y páginas se calculan a partir del documento compuesto; las filas de firma no se dividen entre páginas. Un Informe mantiene identidad, elaboradores y firmas independientes de su Plan base.

Se conservan idempotencia, concurrencia, permisos de servidor y descarga final restringida. Certificados privados y contraseñas solo se utilizan temporalmente, sin persistencia. No se afirma validación de cadena, revocación o TSA.

## Cambios técnicos

- Migración aditiva V24: tabla de coautores, orden de firmas y admisión de AUTHOR_SIGNATURE_REQUESTED en las notificaciones. Se corrigió durante la validación una restricción SQL que impedía emitir ese aviso.
- API GET/PUT `/api/work-plans/{targetDocId}/authors`, con versión y validación del ámbito. Límite técnico de 19 coautores adicionales.
- Selección mediante formulario inicialmente cerrado; cargos visibles y avisos Toast. Cambios pendientes protegen la navegación y evitan mutaciones simultáneas desde otros formularios.
- Snapshot de nombres/cargos y selección; las nuevas versiones formales conservan la selección como punto de partida. No se convierten automáticamente responsables de actividades en elaboradores.
- Motor T1/T2 actualizado a engine5. No se modificaron los DOCX oficiales ni los PDFs firmados históricos. Las nuevas previsualizaciones incorporan el encabezado corregido.
- Preparación de firma y acción de firma consultan los mismos requisitos para evitar instrucciones contradictorias a coautores.

El contrato completo está en [document-authorship-and-review.md](../api/document-authorship-and-review.md).

## Verificaciones

- Suite backend completa realizada durante la tanda: 150 pruebas, sin errores ni fallos. Precede a los ajustes finales del aviso, filas indivisibles y ampliación del escenario T2.
- Comprobación backend final de esos ajustes: 4 pruebas aprobadas; firmas sucesivas de dos elaboradores en Plan e Informe, conservación del Plan aprobado, aviso al siguiente firmante, registrador colegiado sin firma personal, bloqueo por observaciones y T2 extenso con evidencias/paginación.
- Aceptación completa realizada durante la tanda: 15 pruebas aprobadas; configuración de coautores, permisos, creación, revisión, evidencias, Informe y versiones formales.
- Aceptación focal final sobre el código actual: 2 pruebas aprobadas (configuración de elaboradores y circuito completo Plan/Informe), después de unificar los requisitos de preparación/firma.
- E2E generales: 8 pruebas aprobadas.
- Lint y compilación TypeScript/Vite aprobados. Existe aviso de tamaño del bundle JavaScript (515,55 kB; 137,89 kB gzip); no es un error de compilación.
- Revisión visual de PDFs T1/T2 renderizados y de la tarjeta de elaboradores. Encabezados legibles, logo conservado y columnas sin la compresión mostrada en el reporte inicial.
- Pruebas en entornos desechables aislados. La preparación de sesiones positivas limpia únicamente el límite de la cuenta de prueba en aceptación; no cambia el rate limiting del producto.

Los logs y capturas de validación quedan en directorios ignorados `backend/target` y `frontend/test-results`; no forman parte de los commits. Los avisos de fuentes del conversor PDF continúan presentes; los documentos revisados se renderizaron y sus comprobaciones de contenido/páginas pasaron. La revisión visual no certifica todas las combinaciones posibles de títulos, cargos y contenido.

## Uso y límites

No se necesitan variables de entorno nuevas. Se actualizó el backend del entorno manual utaped-sample con `scripts/start-development.ps1`, sin reiniciar datos: V24 aplicada correctamente, endpoint CSRF disponible y siete cuentas conservadas. Los proyectos externos a UTAPED y los volúmenes manuales no se modificaron. Vite puede continuar en http://127.0.0.1:5173. Para documentos sin firma, generar una nueva previsualización incorpora el encabezado; los ya firmados conservan sus bytes y apariencia.

Los flujos históricos colegiados sin registrador necesitan una asignación explícita antes de producir nuevas firmas. No se inventan representantes ni nombramientos. Las etapas personales conservan su configuración actual de firma; la tabla institucional definitiva de obligatoriedad sigue pendiente.

No se implementan en esta tanda plantillas estructurales administrables, catálogo completo de funciones, Quipux, purga/retención, excepciones individuales de elaboración ni nuevas delegaciones administrativas. El siguiente bloque recomendado es versionar la estructura permitida de T1/T2 y cerrar los permisos/contextos pendientes, conservando snapshots e históricos.

La transcripción fuente permanece intacta y sin commit, con SHA-256 `50182038cda38f0fdb8cbe877d3c904e8aae4a751ce014efc8cce9f60c3b4a34`. La auditoría del 17 de octubre conserva su carácter histórico; este reporte registra las brechas resueltas después.

## Entrega en Git

Commit local de implementación `85ff0be` y commit separado de documentación. Sin push ni actualización de pull request, conforme a la instrucción vigente. Solo se conserva sin rastrear el TXT aportado por el propietario; no se incluyeron secretos, PDFs de prueba, capturas ni archivos temporales.
