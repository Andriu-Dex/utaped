# Revisión de entrega frontend y cierre técnico — 30/10/2026

## Alcance

Revisión de todos los cambios pendientes de la rama fix/integral-product-audit: carga de reportes, bandeja e inspector de revisión, persistencia de ubicación y filtros en URL, copias multicarrera, sustitución de participantes, auditoría, custodia y documentos de cierre. Se conservaron las mejoras visuales del propietario.

## Correcciones de revisión

- La copia liberaba la vista de resultados sin liberar el bloqueo de envío: ahora permite cerrar, abrir una copia o finalizar correctamente.
- Selección masiva limitada a diez destinos; los destinos que dejan de ser elegibles después de recargar se conservan identificados y requieren retirarse expresamente. Los candidatos de sustitución también se revalidan contra las opciones actualizadas.
- Claves de idempotencia conservadas para reintentos del mismo contenido; su renovación ocurre al enviar contenido diferente, fuera de actualizadores funcionales de estado.
- Filtros sincronizados al recargar y navegar por el historial, incluyendo la selección vacía. Seleccionar otra vez el mismo ámbito ya no deja el indicador de carga sin consulta. Se limpia el ámbito al cerrar sesión.
- La sustitución queda bloqueada mientras haya una observación o firma con cambios pendientes. El texto del modal evita detalles internos de slots de firma.
- Avisos en una fila propia dentro de modales de acción: conservan acceso al cierre y no tapan Cancelar. Los anchos específicos de los nuevos diálogos prevalecen sobre el ancho genérico.
- Pruebas de frontend actualizadas a las etiquetas, clases y contratos reales; los títulos de pruebas describen la cobertura efectiva. Se añadieron comprobaciones de límite de selección, estado de resultado y navegación con filtros vacíos.

## Validación

Frontend: lint y typecheck correctos; compilación productiva correcta. Vite mantiene una advertencia de tamaño del bundle principal (aproximadamente 697 kB sin comprimir); no se alteró su umbral ni se presenta como un fallo funcional.

Backend: 14 pruebas de integración dirigidas aprobadas (PlanCopyIntegrationTests 3, ReviewReassignmentIntegrationTests 3, ReportingIntegrationTests 8). Se comprobaron migración, permisos, idempotencia, concurrencia de sustitución, integridad del PDF, copias independientes y cálculo de reportes en PostgreSQL aislado.

Aceptación de navegador: 25 escenarios aprobados en la ejecución final (1,7 minutos): 11 de copias/sustitución y navegación, 2 de administración contextual, 7 de reportes, 1 de circuito documental completo y 4 de revisión. Los nuevos escenarios de presentación de copias/sustitución utilizan respuestas controladas de API; no sustituyen las pruebas backend. El circuito documental multirrol existente utiliza la aplicación y API reales en un entorno descartable. Las capturas de componentes con documentos ficticios no constituyen aprobación del formato institucional.

Registros locales: backend/target/review-delivery-backend.log y backend/target/review-delivery-verified.log. Capturas locales de copia completada, sustitución en tablet, carga de reportes y flujo documental. No se guardan credenciales ni material de firma privado en Git.

## Límites

La aceptación del cliente, configuración institucional e infraestructura final continúan según la checklist. Esta entrega no acredita una nueva auditoría exhaustiva de todos los módulos ni garantiza ausencia de defectos fuera de los escenarios ejecutados. Solo commits locales; sin push ni cambios en producción.
