# Correcciones tras presentación al cliente

Fuente: transcripción corregida conservada por el propietario en `instruction.md` y cinco documentos nuevos de `docs/templates`. La transcripción tiene fragmentos poco claros: no convertirlos en asignaciones oficiales.

## Decisiones respaldadas

- 3:29–5:11: todos los elaboradores seleccionados firman en orden; únicamente el responsable principal puede editar. Después de firmar se conserva el artefacto. No convertir a todos los miembros en editores.
- 5:20–8:15: existen procedimientos cuya última etapa interna es «Validado por»; la aprobación se realiza fuera del sistema. El ejemplo `Plan de Tranbajo con validación externa.pdf`, página 8, muestra seis elaboradores y una validación del Subdecano. No tiene firma de aprobación interna.
- 8:37: la siguiente presentación debe hacerse en vivo. No inferir una fecha concreta del término «próximo martes» sin conocer la fecha real de la reunión.

## Orden de trabajo y alcance

### 1. Distinguir el cierre del procedimiento

El flujo tiene un resultado explícito `APPROVED` o `VALIDATED`. La opción histórica sigue siendo aprobación; no reclasificar documentos antiguos ni alterar sus PDFs firmados. El resultado nuevo se conserva en el snapshot del flujo del artefacto, y la ejecución lo toma de ese snapshot, no de la plantilla administrativa actual.

Un flujo de validación interna debe terminar en `VALIDATE` y no incluir aprobación interna. El estado se presenta como «Validado internamente», genera su propio evento/notificación y permite la descarga autenticada del PDF para el trámite externo. No habilita por sí solo ejecución, informes ni indicadores reservados a documentos aprobados. Los reportes y exportaciones deben contar validaciones aparte de aprobaciones.

Decisión del propietario (2026-11-23): por ahora únicamente descargar el PDF validado; no implementar inscripción de resolución externa ni habilitar ejecución por esa vía. Una constancia de Quipux no acredita por sí sola una aprobación externa. No cambiar las reglas existentes de Quipux para hacer esa equivalencia.

### 2. Hacer comprensible la configuración

La administración debe identificar claramente revisores, validadores y aprobadores, el resultado del trámite interno y el orden de etapas publicado. Mantener separados cargo institucional, persona asignada y órgano colegiado. No asignar nombres tomados de los PDFs como cuentas o permisos reales.

No existe una lista definitiva de las cinco unidades con validación externa: UPE está respaldada por el documento; investigación, vinculación, seguimiento y coordinaciones aparecen con fragmentos ambiguos. Configurar cada caso expresamente. No imponer restricciones globales de un único rol por cuenta a partir de la explicación de 2:36–2:45: conservar la incompatibilidad de participantes en cada circuito documental.

### 3. Incorporar variantes de eventos

Referencias editables:

- `uta-sgc-a-2-1-p7-t1_planificacion_especifico_comision de eventos.docx`.
- `uta-sgc-a-2-1-p7-t2_ejecucion_especifico_comision de eventos.docx`.

Ejemplos de contenido y firmas:

- `UTA-SGC-B-2-23-I3-T1_PLANIFICACION EVENTO_3.pdf`.
- `INFORME EJECUCIÓN EVENTO 1 TI-signed.pdf`.

Decisión del propietario (2026-11-23): las dos plantillas DOCX son la referencia de estructura y código; los PDF son ejemplos de contenido y firmas. Discrepancias registradas: el primer PDF tiene un nombre distinto del código mostrado en su pie; el informe de ejecución firmado y su DOCX también muestran un pie T1, aunque el nombre del DOCX lo identifica como T2. Conservar los códigos de los DOCX confirmados, sin derivarlos de los nombres de los PDF.

La planificación específica necesita justificación, objetivo general, objetivos específicos, datos informativos y desarrollo del evento. Los datos informativos contienen tema, tipo (académico, cultural, social, deportivo, investigativo u otro con descripción), fechas, horas, modalidad, ubicación, aforo, asistentes por categoría, recursos logísticos, certificados y difusión. La matriz planificada incluye actividad, fecha, hora inicial/final, responsable y recursos/presupuesto.

La ejecución específica conserva la referencia al evento planificado e incorpora los datos realmente ejecutados, porcentaje declarado de cumplimiento y observaciones por actividad, además de conclusiones/recomendaciones y anexos. No confundir esta variante con un Informe final general ni con todos los cortes T3 del sistema. Definir su identidad, vínculo y reglas explícitamente antes de migrar datos existentes.

Usar los catálogos institucionales existentes cuando correspondan, con «Otro» validado. Conservar borradores incompletos y control de concurrencia, pero bloquear firma si faltan datos requeridos. Validar fechas y horas sin imponer límites institucionales de duración o asistencia no confirmados. El porcentaje es declarado: no deducirlo de anexos o cantidad de evidencias.

El PDF se compone con los bloques y tablas oficiales, A4, índices/páginas/slots reales, todos los elaboradores y las etapas efectivas del flujo. La variante no debe alterar el formato general ni artefactos firmados. El ejemplo de planificación contiene dos elaboradores, dos validadores y un aprobador; la plantilla editable conserva además revisión. Mantener configurables esas etapas, no imponer idéntica ruta a todos los eventos.

### 4. Aceptación en vivo

Verificar dos circuitos independientes: aprobación interna completa y validación interna para trámite externo; después planificación y ejecución de evento. Cubrir firmas sucesivas, un único editor, notificaciones, devoluciones, nueva ronda, permisos de terceros y conservación de PDFs. Verificar reportes, CSV, filtros, tema oscuro y escritorio/tableta. Los tests usan PostgreSQL aislado; nunca reiniciar la base manual para preparar la demostración.

## Estado de esta entrega

Se implementaron el cierre en validación interna, las variantes T1/T2 de evento y el correo al siguiente elaborador. El cierre por validación permite descargar el PDF firmado. La selección de unidades, participantes y etapas corresponde a Administración. Las métricas de evidencias y avance general siguen basadas en las actividades de los planes generales. El informe de evento presenta su cumplimiento por actividad en su propio formato.
