# Documentos preparados para revisión manual

Fecha: 2026-10-23. Rama: `fix/institutional-document-spacing`.

## Resultado

Se cargaron cuatro documentos adicionales en `utaped-sample`, accesibles mediante `dev.principal`:

- Plan de Trabajo: Comisión Revista Científica de Facultad - Ejemplo editable.
- Plan de Trabajo: Unidad de Planificación y Evaluación (UPE) - Ejemplo revision, en revisión con tarea activa para `dev.reviewer`.
- Plan de Trabajo: Comisión Plan de Contingencia - Ejemplo aprobado, con evidencia PDF.
- Informe: seguimiento académico - Ejemplo, en borrador, derivado del Plan anterior y con contenido/resultados/previsualización preparados. Sus anexos opcionales están desactivados inicialmente.

El período es Julio–Diciembre 2026. El Plan original de Comisión de CTT y Talleres Tecnológicos se conserva íntegro: comparación de la huella de su fila antes/después, sin modificaciones. Las siete cuentas y credenciales también conservan su huella conjunta. Resultado: cinco documentos en total, diez archivos privados, una evidencia y una tarea de revisión activa.

## Implementación y límites

Carga opcional mediante `./scripts/start-development.ps1 -PreparedDocuments` (también admite `-Web`). Sin variables nuevas que el propietario deba escribir, sin migraciones y sin rutas HTTP nuevas. Perfil `development-seed` más base `utaped_dev` obligatorios; desactivada por defecto. El cargador espera a las semillas de referencia y utiliza los servicios normales para crear, validar, componer, firmar, revisar, aprobar y registrar evidencia.

Se usan ámbitos disponibles y catálogos existentes sin sobrescribir documentos. Se activan los flujos del período de desarrollo si faltaba ese paso, conservando el congelamiento normal de asignaciones. No se inventan nombramientos institucionales.

Los títulos y narrativas identifican los ejemplos. Los anexos/evidencias llevan una advertencia de falta de validez institucional. Dos Planes se firman criptográficamente con un certificado autofirmado de ejemplo generado solo en memoria; no son firmas institucionales ni proceden de los archivos oficiales. Se revoca su vinculación temporal al acabar y se restaura una vinculación pública previa si existía. No se persistieron certificados privados ni contraseñas. La base manual termina sin vinculaciones activas de ejemplo.

Un marcador de auditoría impide repetir la carga o resetear documentos editados; bloqueo transaccional evita carreras. Los fallos provocan rollback, incluyendo limpieza de archivos registrados durante la transacción. La carga posterior no reconstruye documentos eliminados o cambiados.

## Verificación

- Compilación backend aprobada.
- Primera prueba integral de preparación aprobada; después, cuatro pruebas finales aprobadas entre DevelopmentDocumentsTests, DevelopmentDocumentsGuardTests y DevelopmentSeedTests, sin errores/fallos.
- Se comprobaron los estados, artefactos PDF reales, número real de páginas, evidencia, retirada de la vinculación temporal y repetición sin aumentar documentos/archivos ni sobrescribir una modificación manual.
- Prueba de protección: rechazo de una base distinta antes de escribir.
- Lanzador ejecutado en el entorno manual y repetido satisfactoriamente. Verificados cinco documentos y conservación del original/credenciales.
- Respaldo conjunto previo verificado con `operations.py backup/verify`: `backend/target/backups/before-prepared-documents-20261023`, una fila documental original y cero archivos registrados antes de la carga. Respaldo e imágenes están ignorados por Git.
- Entorno `utaped-tests` eliminado al terminar. Sin cambios propios en frontend ni pruebas frontend nuevas; se preservan los cinco archivos de interfaz del propietario, incluyendo su ajuste de MatrixEditor.
- `git diff --check` aprobado. Avisos conocidos del conversor de fuentes/FO no impidieron completar las composiciones.

Logs ignorados: `backend/target/prepared-documents-compile.log`, `prepared-documents-tests.log`, `prepared-documents-final-tests.log`, `prepared-documents-backup.log`, `prepared-documents-runtime.log` y `prepared-documents-repeat.log`.

## Para probar

Con Vite abierto, recargar Documentos y buscar **Ejemplo**. Mantener las credenciales actuales de `.env.sample`. Revisar la bandeja con `dev.reviewer`; al completar su etapa, continuar con `dev.approver`. La guía de escenarios está en [Entorno de desarrollo](../development/sample-environment.md).

Solo commit local, sin push ni pull request.
