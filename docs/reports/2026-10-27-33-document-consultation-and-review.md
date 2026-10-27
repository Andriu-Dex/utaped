# Consulta y revisión separadas de elaboración

Fecha: 27 de octubre de 2026.

Al abrir un documento ajeno desde Documentos se mostraban los pasos de elaboración, aunque el usuario solo pudiera consultar o revisar. Se separó esa entrada según la identidad del responsable principal y se conservó la autorización del servidor.

El responsable mantiene los editores T1/T2/T3. Los demás usuarios acceden a consulta sin formularios de creación. Cuando existe una ronda, se reutiliza el inspector del PDF firmado conservado, con observaciones, resaltados, etapas y acciones disponibles según la asignación vigente. La consulta conserva acceso a rondas, historial y ejecución/evidencias del T1 aprobado.

Los coautores mantienen acceso a la preparación disponible y al componente de firma ordenada. La revisión del contenido precede a la firma; la cancelación protege los datos pendientes. Tras una devolución, la consulta de coautor no reutiliza la preparación anterior como si correspondiera a la corrección. No se generan artefactos al entrar como lector ni se amplían permisos del servidor.

## Validación

- TypeScript, lint y build del frontend correctos.
- 24 pruebas de aceptación finales correctas, ejecutadas con el lanzador aislado y sus recursos descartables.
- Cobertura de acceso de lector sin editor, permisos de escritura existentes, acceso del revisor desde Documentos, PDF firmado, vuelta a la bandeja, turnos de coautor, cancelación con cambios y flujo real de devolución/corrección/firma.
- Revisadas las capturas de escritorio (1440 px) y tablet (820 px, tema oscuro); sin navegación de creación ni desbordamiento horizontal del documento de revisión.
- Los escenarios del circuito documental mantienen firmas, rondas y evidencias. Backend y motor documental sin cambios en esta entrega.

Registro local de la ejecución final: `backend/target/2026-10-27-document-consultation-final.log` (24 passed). Capturas en `frontend/test-results/reviewer-document-consultation-desktop.png` y `reviewer-document-consultation-tablet-dark.png`; son resultados locales, no archivos versionados.

Entrega mediante commit local; sin push ni cambios en los datos del entorno manual.
