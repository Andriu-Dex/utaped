# Cierre histórico y flujos por ámbito/período

Fecha: 2026-10-12. Rama: `feature/modern-workspace-access`. Entrega posterior al circuito T1/T2. Se implementan cierre sin purga, archivo histórico privado, lectura autorizada y asignaciones de flujos independientes por ámbito y período. No se declara terminado todo el producto.

## Fuentes y decisiones

Se revisaron AGENTS.md, requisitos, estado, decisiones abiertas y Plan Maestro §§3.9, 9, 29, 52–53. Se conserva la modernización de la interfaz del propietario. Prototipos sigue congelado; no se restauraron reportes eliminados.

La activación de flujos es una operación explícita de configuración, obligatoria antes de nuevas firmas por ámbito. La indicación de período actual por fecha no designa autoridades ni activa/cierra automáticamente un semestre. No se añaden reglas de reapertura o purga.

## Implementación

- Flyway V19: ACTIVE/CLOSED, concurrencia, fecha/actor/motivo del cierre, activación y asignaciones T1/T2 por ámbito, registro de ZIP privado. Los períodos existentes permanecen ACTIVE; sus ámbitos reciben como selección inicial la revisión general publicada disponible. PDFs y snapshots anteriores se mantienen intactos.
- Plantillas generales y asignaciones separadas: ADMIN selecciona una revisión publicada para el grupo/tipo correctos; guardado de T1/T2 en una transacción, con control de versiones. Las revisiones de otros grupos/tipos y las solicitudes obsoletas se rechazan.
- Activación de todos los ámbitos activos de grupos activos: principal y ejecutores individuales activos, sin autorrevisión ni duplicados. Se congelan identidades y definición. Cambiar la plantilla general no cambia el flujo activado. Nuevos ámbitos y cambios de asignación quedan bloqueados para ese período.
- Cierre administrativo con motivo y confirmación de conteos pendientes. Bloqueo compartido/exclusivo del período: una escritura ya iniciada termina antes de construir el archivo; una nueva encuentra el período cerrado.
- Archivo de documentos sin borrar ni alterar su estado. IN_REVIEW conserva tareas y PDF, pero no admite acciones; APPROVED mantiene sus bytes y descarga final. Borradores, matrices, anexos, evidencias, versiones, prórrogas y cambios organizativos quedan de solo lectura.
- ZIP interno con manifiestos por UUID, hashes/tamaños y metadatos históricos, snapshots, firmas públicas, rondas/tareas/observaciones, evidencia versionada, asignaciones y auditoría. Incluye archivos históricos para conservación, sin contraseñas, sesiones ni material privado de firma.
- Verificación de cada archivo al leerlo. Falla de integridad/almacenamiento o tamaño excesivo revierte todo el cierre; no queda un período cerrado con archivo incompleto.
- ZIP descargable filtrado por permiso documental actual. ADMIN no obtiene acceso por su rol. Se conserva el bloqueo de PDFs preliminares por ámbito: la descarga incluye finales aprobados, evidencias y anexos vigentes, más metadatos históricos autorizados. `download.json` enumera archivos incluidos; el manifiesto puede registrar otros conservados solo internamente. Respuesta privada con hash del ZIP entregado y evento de auditoría.
- Nuevas tarjetas de control de período y asignación T1/T2, métricas, activación, cierre con motivo, estado cerrado y hash interno. Selector de histórico y descarga desde Documentos. Revisión pendiente cerrada se presenta como solo lectura. PC/tableta y modo oscuro, sin nuevos estilos de celular.
- Semillas de desarrollo asignan revisiones sin activar ni cerrar períodos automáticamente. Puerto principal sigue 64430; ejemplo aislado 64431.

## Verificaciones realizadas

- Suite completa del backend: **98 casos ejecutados, cero fallos, errores u omisiones**, con PostgreSQL del proyecto `utaped-tests` y base `utaped_test`. Incluye casos heredados ejecutados en las clases documentales; no significa 98 escenarios institucionales diferentes.
- Nuevos casos de cierre, permisos, preservación de bytes y estados, concurrencia real, rollback por archivo corrupto, separación de plantillas/asignaciones, guardado atómico, falta de configuración y exclusión de preliminares de la descarga. También cierre con revisión activa y rechazo de decisiones/observaciones.
- Frontend: lint, TypeScript y build correctos.
- Playwright de acceso/administración/tema/configuración: **5 casos correctos**, backend aislado `utaped-e2e` y Vite de pruebas. Se actualizó el selector de la plantilla de flujo conservando sus comprobaciones de historial y destinatarios.
- Playwright de circuito en `utaped-sample`: **1 escenario completo correcto** con asignación y activación desde UI, devolución, firmas sucesivas reales, evidencia, T2, versión formal, cierre con borrador pendiente, ZIP autorizado y ADMIN sin acceso. El PDF aprobado conserva el mismo SHA-256 antes/después del cierre.
- Las incidencias iniciales de navegador se corrigieron: selector del menú, recarga del catálogo después de fixtures y etiqueta accesible explícita de cada revisión asignada. Se reconstruyó el servicio frontend con el perfil `web` para verificar la versión actual.
- Capturas de control/activación/cierre en escritorio y tableta oscura revisadas; sin desbordamiento horizontal del cuerpo. No se modificaron los formatos institucionales en esta tanda.
- Entornos, certificados, logs, capturas, builds y PDFs de prueba permanecen ignorados. Solo se limpiaron registros de throttle de las bases de pruebas/ejemplo para repetir fixtures. No se vació la base principal ni se modificaron credenciales del propietario.

No se realizó aceptación institucional, prueba de carga, fallo de energía durante escritura, respaldo/restauración integral ni despliegue productivo. Las pruebas de firma usan certificados temporales de prueba y los fixtures resuelven CAPTCHA solo en las bases aisladas; no se añadió bypass a la aplicación.

## Operación y variables

No se necesitan credenciales nuevas. Reconstruya/reinicie backend y frontend para aplicar V19; no elimine volúmenes ni reinicie datos. En el entorno de ejemplo puede utilizar `./scripts/start-development-sample.ps1` sin `-ResetData`.

Antes de nuevas firmas: ADMIN → Períodos académicos → elegir período → revisar principales y asignaciones T1/T2 → guardar → Activar flujos del período. Las selecciones migradas son iniciales, no ratificación institucional; revíselas antes de activar. No existe desbloqueo estructural automático.

`PERIOD_ARCHIVE_MAX_BYTES` es opcional: defecto 134217728 bytes (128 MiB) sin comprimir, incluyendo metadatos y archivos repetidos entre documentos. Rango técnico 1–512 MiB. La generación actual usa memoria; dimensionar o migrar a streaming antes de exportaciones mayores. Ante exceso no se cierra el período. No constituye política de retención.

Para cerrar: revisar pendientes, introducir motivo y confirmar. Desde Documentos, elegir el período cerrado y descargar su archivo autorizado. El ZIP de aplicación no es un respaldo completo restaurable de PostgreSQL. [Contrato completo y pasos](../api/period-control.md).

## Pendientes y siguiente implementación

1. Retención/reapertura institucional (`PENDIENTE_REAL_RETENCION`), exportaciones en streaming y operación de grandes volúmenes.
2. Sustitución autorizada de ejecutores ante bajas y autoridades históricas de Decano/Subdecano por ámbito. No se concedieron permisos globales nuevos.
3. Administración completa de carreras/cargos, permisos delegados y recuperación por coordinador; avisos automáticos de vencimiento.
4. Evidencias externas/anexos no PDF, asistencia de redacción y agente código/Trello, aceptación integral y operación HTTPS/SMTP/respaldos.

Siguiente tanda recomendada: administración contextual pendiente y avisos de vencimiento. Las decisiones institucionales abiertas se mantienen separadas de las capacidades técnicas ya entregadas.

## Control de entrega

Solo commits locales en la rama actual. Sin push, actualización de PR ni fusión.

- `5e8fc07`: API, V19, control de períodos, flujos congelados, archivo privado, permisos y pruebas PostgreSQL.
- `2a41daf`: control visual de períodos, descarga autorizada, revisión de solo lectura y pruebas de navegador.
- Documentación y reporte en un commit separado, con requisitos, estado, contrato y pasos de operación actualizados.

Los cambios fueron revisados antes del commit; no se introdujeron claves privadas, archivos de entorno reales ni atribuciones de autoría adicionales. La base y los contenedores principales del propietario no se reiniciaron ni se modificaron durante esta validación.
