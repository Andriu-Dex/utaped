# Publicación — Anexos y preparación T1

Fecha: 2026-10-03. Rama: feature/t1-document-preparation. Destino: develop. Base remota verificada: b24b531, merge del PR #3.

El propietario autoriza commit, push y apertura del PR de esta entrega. También establece autorización permanente para repetir esos pasos al completar futuras implementaciones, salvo indicación contraria. La revisión y fusión permanecen a cargo del propietario; main y despliegues quedan fuera de esta autorización. Regla registrada en AGENTS.md y docs/development/git-workflow.md.

Se incluyen código, Flyway V6, referencia institucional DOCX, configuración de almacenamiento y proxy, pruebas y documentación de la entrega. No se incluyen .env, credenciales, datos runtime, cachés, logs, PDFs/PNG de QA ni capturas Playwright. La referencia DOCX es un recurso del motor, no un documento de usuario.

Verificaciones de publicación: git diff --check, revisión de archivos no rastreados y exclusiones, autenticación GitHub disponible, develop remoto actualizado y ausencia de PR previo para esta rama. Las pruebas de implementación ya están registradas en [reporte T1](2026-10-03-07-t1-document-preparation.md): build/lint, 22 pruebas backend y E2E con Vite/Nginx. No se repiten pruebas de aplicación por cambios exclusivos de documentación de publicación.

El commit se identifica en el historial de feature/t1-document-preparation; el PR se abre hacia develop para revisión del propietario. CI remoto debe consultarse en GitHub; no se considera aprobado por las verificaciones locales.
