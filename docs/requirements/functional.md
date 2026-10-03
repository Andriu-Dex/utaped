# Requisitos funcionales y aceptación

Estado HEREDADO salvo indicación contraria. Las referencias § remiten a [baseline-requirements.md](../reference/baseline-requirements.md). Cada fila reúne requisitos relacionados sin eliminar IDs originales. Prioridades propuestas.

| Área / prioridad | Fuente | Comportamiento y aceptación observable |
|---|---|---|
| Acceso P0 | §3 FR-AUTH-* | Autenticar, cambiar contraseña temporal y recuperar acceso; cambiar contexto conserva identidad. Sesión expirada impide operaciones. Mecanismo local/SSO pendiente. |
| Grupos P1 | §4 FR-GRP-* | Pertenencia múltiple y roles por grupo; ámbito autorizado. Unicidad docente/grupo/período y excepciones por confirmar. |
| Períodos P1 | §5 FR-PER-* | Configurar ventanas y fechas; rechazar rangos inválidos. Cierre y excepciones pendientes. |
| Plan T1 P1 | §6–26 FR-T1-*, FR-MAT-*, FR-ANX-* | Borrador, datos institucionales, justificación y objetivo únicos, actividades/matriz, anexos, previsualización y finalización. Fecha congelada al finalizar. Orden del asistente por reconciliar. |
| Actividades P1 | §10–17 FR-ACT-*, FR-DATE-*, FR-RESP-*, FR-REC-*, FR-MED-* | Obligatorias no eliminables, opcionales y Otra; fechas y feriados configurables; mínimo un responsable, selección múltiple y seleccionar todos. Denominación colectiva conserva identidades internas. Catálogos y Otro con texto obligatorio, error inline y conservación del formulario. Fuente editable y Elaborado por derivado. Nota de datos personales por condición explícita. |
| Motor documental P0 | §19–25, §41, §61 FR-DOC-A4-001, FR-TPL-* | T1/T2 fieles a formatos A4; páginas/índices/slots reales. Snapshot de plantilla fija versión, orden y visibilidad; administración no modifica artefactos previos. |
| Firma P0 | §27 FR-SIGN-* | Usuario asignado firma artefacto exacto; secretos temporales; error o cancelación no avanza estado. Contrato institucional pendiente. Simulación no cumple producción. |
| Revisión P1 | §28–34 FR-REV-*, FR-FLOW-* | Etapas por grupo, paralelismo cuando aplique y espera de todos los obligatorios. Observaciones propias, generales o sobre zona; decisión congela historial. Devolución permite corregir y reenviar en ronda nueva, conservando artefactos y firmas anteriores. No incrementa versión formal. |
| Informe T2 P1 | §35–40 | Derivar del Plan sin mutarlo, importar actividades/medios/datos; registrar ejecución y observaciones. Antecedentes, desarrollo, conclusiones, mejoras, contactos cuando aplique, anexos, firmas e historial. Omitir notas instructivas del formato. Informe independiente pendiente. |
| Ejecución P1 | §42–48 FR-EVI-* | Consultar/filtrar actividades. Solo responsables cargan/reemplazan. Un PDF vigente por medio, tamaño configurable; reemplazo versiona y reinicia validación. Revisor observa/valida. Completo no equivale a validado. Límite horario exacto por confirmar. |
| Administración P1 | §49–50 | Usuarios/importaciones, grupos/miembros, períodos, catálogos, feriados, plantillas y flujos. Errores de importación identificables. Cédula no obligatoria sin validación. |
| Notificaciones P1 | §51 | Bandeja por usuario y navegación al objeto correcto bajo permisos. Canales y entrega pendientes. |
| Auditoría e histórico P0 | §52–55 | Eventos atribuibles; documentos, versiones, actividades, firmas y evidencias preservados. Histórico cerrado solo lectura. Reportes documentales/de estado, sin ranking ni sanciones. Cierre pendiente. |
| Interfaz P1 | §56–59; instruction.md | Español y azul institucional; cargando/vacío/error/éxito/permiso. Iconos con nombre accesible y tooltip, teclado, resoluciones medianas y legibilidad A4. Login como referencia; CAPTCHA DEMO no acredita seguridad. |
| Asistencia IA P2 | §9 FR-T1-CONT-*, §61 FR-ARCH-AI-001 | Mostrar original/sugerencia, aplicar/descartar explícito; fallo no bloquea edición. Proveedores backend configurables; habilitación y privacidad pendientes. |

## Escenarios mínimos

1. T1: crear, finalizar, revisar, devolver, corregir y aprobar con personas distintas.
2. T2: derivar y firmar sin modificar Plan o artefactos anteriores.
3. Evidencia: cargar, observar, reemplazar y validar; rechazar usuario ajeno y acción fuera de plazo.
4. Documentos cortos/largos: comparar A4, índices, firmas y contenido con originales oficiales.
5. Cambiar plantilla/flujo sin alterar snapshots previos.
6. Consultar histórico/auditoría bajo permisos y comprobar revocación de sesión/pertenencia.

Antes de cada módulo, desglosar requisitos atómicos y permisos. No convertir pendientes en pruebas de una normativa inventada.
