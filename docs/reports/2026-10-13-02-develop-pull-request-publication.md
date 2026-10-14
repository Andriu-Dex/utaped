# Publicación del circuito documental hacia develop

Fecha: 2026-10-13. Autorización expresa del propietario: push y pull request hacia develop.

## Alcance

Se publica la rama `feature/modern-workspace-access` y se actualiza el PR existente [#10](https://github.com/Andriu-Dex/utaped/pull/10) hacia `develop`, evitando duplicados. La descripción se reescribe para representar toda la entrega acumulada: acceso local y CAPTCHA visual/sonoro, separación administrativa, interfaz moderna PC/tableta con temas/toast, ámbitos y revisión secuencial, T1/T2 y firmas reales, ejecución/evidencias/versiones, cierre e historial, importación, redacción asistida y preparación operativa.

Revisión previa: árbol de trabajo limpio; `origin/develop` es antecesora de la rama (0 commits exclusivos de develop, 35 commits de implementación antes de este reporte). No hace falta resolver divergencias. Se publica también este registro y el índice de reportes. No se incluyen variables privadas, claves, certificados o salidas de pruebas ignoradas.

## Validación de la implementación

Se conserva la validación completada antes de publicar: 135 ejecuciones backend / 20 suites, siete escenarios E2E generales, cinco de ejemplo, dos escenarios de audio repetidos sobre Nginx, lint/build y revisión visual. No se repiten pruebas por cambios exclusivos de documentación/publicación. [Reporte técnico y límites](2026-10-13-01-accessible-captcha-and-document-load.md).

La carga de firma fue comprobada en servicios transaccionales; el transporte de certificados por HTTPS bajo carga y dimensionamiento institucional sostenido siguen pendientes. La versión funcional se presenta para revisión/aceptación, sin declararla aprobada para producción institucional.

## Publicación y revisión

Destino: `origin/feature/modern-workspace-access`; PR #10, base `develop`. El propietario revisa y fusiona. No se modifica `main`, no se realiza merge automático ni despliegue. Se comprueba el resultado remoto del push y la actualización del PR; el estado de CI se consulta después de publicar y puede seguir en ejecución.

Pendientes: aceptación del cliente, reglas institucionales aún abiertas, infraestructura/SMTP/certificado y operación real según la documentación vigente.
