# Reglas institucionales provisionales e Informe de ejecución

Rama: `feature/institutional-rules-and-execution-report` desde `develop`. Plan: [institutional-rules-and-execution-report.md](../planning/institutional-rules-and-execution-report.md). Decisiones: [open-questions.md](../decisions/open-questions.md#decisiones-provisionales-del-equipo--2026-10-20). Cada fase tiene su propio commit local; no se hizo push ni PR.

## F1. Plazos en días hábiles y reapertura individual de elaboración

- `BusinessCalendar` cuenta días hábiles excluyendo sábados, domingos y feriados registrados.
- Prórroga de evidencias: el máximo acumulado (`PLANNING_MAX_EXTENSION_DAYS`, 3 por defecto) pasa de días calendario a días hábiles desde el vencimiento original. Las prórrogas existentes no se modifican; se conserva el nombre de la variable.
- Reapertura de elaboración por ámbito: el coordinador del ámbito, en un período activo, amplía el cierre de elaboración hasta el mismo máximo de días hábiles y nunca después del fin del período. Motivo obligatorio, concurrencia por versión, historial (`preparation_extension_history`) y auditoría. La fecha del período no cambia.
- El plazo efectivo (`GREATEST` entre el cierre del período y la ampliación del ámbito) se aplica a edición, creación, firma, nuevas versiones y contadores de seguimiento.
- Interfaz: «Mi organización» muestra el cierre vigente, la ampliación y su historial; el formulario abre solo por acción explícita, con confirmación y protección de cambios.
- Migración V25.

## F2. Fecha de finalización y sello visible de firma

- La fecha de elaboración se fija al generar el artefacto que firman los elaboradores; queda en sus bytes y no cambia al validar. La vista previa de edición usa la fecha del día como provisional y el editor indica «Se fija al preparar la firma».
- Título del paso: «Firma y finalización».
- Sello visible en el lugar asignado por la composición: código QR a la izquierda y, a la derecha, «Firmado electrónicamente por:», el nombre en mayúsculas y la fecha y hora de Ecuador. El QR contiene firmante, razón (acción del espacio de firma), localización, fecha ISO y «VALIDAR CON: www.firmadigital.gob.ec». La validez sigue dependiendo de la firma CMS incrustada, no de la imagen.
- Nueva dependencia `com.google.zxing:core` 3.5.3 para codificar el QR.

## F3. Copia de custodia y eliminación de documentación del período

- ADMIN descarga el ZIP interno completo como copia de custodia; se registra quién y cuándo.
- La eliminación exige período cerrado, copia de custodia descargada y período de gracia cumplido (`PERIOD_PURGE_GRACE_DAYS`, 30 por defecto), además de confirmación y motivo.
- Se eliminan documentos, revisiones, firmas, anexos, evidencias, prórrogas, avisos, accesos y el ZIP interno del período. Los bytes se borran solo después del commit. Se conservan el inventario (`purge_inventory`), la huella SHA-256 del ZIP, el motivo y la auditoría. Si un documento de otro período depende de este, no se elimina nada.
- Tras la eliminación, la descarga histórica responde 410 indicando que se solicite la copia de custodia.
- Migración V26.

## Verificación

F1–F3: `scripts/test-backend.ps1` con PostgreSQL aislado, 157 pruebas sin fallos ni errores. Incluye cómputo de días hábiles con feriado, reapertura (permisos, máximo, concurrencia, edición reabierta), fecha fijada al generar el artefacto, decodificación del QR del sello firmado (nombre con tildes, razón y hora de Ecuador) y eliminación del período (sin copia, antes de la gracia, borrado de filas y bytes, otro período intacto, inventario, auditoría, 410 posterior). Frontend: `npm run lint` y `npm run build` sin errores. No se ejecutaron E2E ni aceptación para estas fases ni revisión visual en navegador.
