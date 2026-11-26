# Validación interna y documentos de eventos

Administración puede publicar procedimientos cuyo último paso sea una validación interna. El cierre y sus participantes se conservan junto con el artefacto. El documento validado queda disponible para descarga autenticada y tiene su propio estado, aviso, auditoría y conteo en reportes.

La planificación T1 y el informe de ejecución T2 de eventos emplean los DOCX oficiales indicados en el [registro de decisiones](../planning/2026-11-23-internal-validation-and-events.md). El informe parte del plan aprobado y requiere declarar los resultados reales. Las firmas se vinculan al artefacto generado y el PDF original del plan se conserva.

Se conserva un único editor principal. Cada turno de firma genera un aviso interno y un envío SMTP pendiente. Los envíos se procesan en segundo plano con reintentos. El correo enlaza al documento bajo sesión autenticada.

La configuración oficial de unidades y autoridades queda a cargo de Administración. El alcance del trámite externo es la descarga. Los indicadores de evidencias y avance general se calculan con la matriz general, mientras que el cumplimiento de eventos se consigna por actividad en su informe.

## Verificaciones

- Lint, comprobación de TypeScript y compilación de frontend.
- Pruebas aisladas de PostgreSQL para validación, descarga, flujos, documentos de evento y compatibilidad de creación.
- Mailpit recibió el enlace al documento. Se verificó la deduplicación, el rollback y el reintento ante caída de SMTP.
- Playwright con respuestas controladas para edición de evento, autoguardado y modal de actividades, con capturas de escritorio y tableta.

El pie del DOCX de ejecución dice UTA-SGC-A-2-1-P7-T1 aunque el archivo se identifica como T2. Se conservó el código impreso del DOCX confirmado y la variante de informe sigue siendo T2 dentro de UTAPED.
