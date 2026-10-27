# Apartado de anexos sin archivos

El propietario solicita conservar el apartado ANEXOS con el texto «(No aplica)» cuando esté vacío. El T1 omitía completamente el apartado y su entrada de índice; el T2 también lo omitía si no tenía referencias, y el T3 mostraba una etiqueta de enlace vacío.

Se conserva el título y su numeración institucional; el texto aparece debajo en tipografía normal. T1/T2 mantienen el apartado en el índice y la composición dinámica. El T3 usa la misma indicación únicamente si no tiene anexos, referencias externas ni enlace de evidencias. Los documentos con archivos o referencias conservan su presentación.

Se incrementa la versión técnica de los motores de plantilla para actualizar las preparaciones de borradores al regenerar, sin cambiar versiones formales, firmas, ni bytes conservados. No se modifica ningún archivo oficial en docs/templates.

Validación: 57 pruebas documentales y de revisión correctas; frontend lint y build (incluye TypeScript) correctos. Revisados visualmente los PDF reales de T1/T2/T3 sin anexos; se comprobaron índice, ubicación de firmas, páginas dinámicas y navegación a ANEXOS. La primera ejecución detectó que el mapa del visor omitía el apartado vacío al perderse el destino nombrado durante la composición; se corrigió para localizarlo igual que las demás secciones y la repetición completa pasó.

El número del título de anexos del T1 usa la tipografía de los otros títulos institucionales, eliminando el tamaño/color de marcador opcional heredado. El entorno manual quedó actualizado con sus datos conservados. Las pruebas usaron únicamente el entorno desechable utaped-tests, ya limpiado.

Evidencia: `backend/target/2026-10-27-no-annex-final-tests.log` y `backend/target/document-qa/t1-no-annex.png`, `t2-no-annex.png`, `t3-no-annex.png` (PDF correspondientes en el mismo directorio). Cierre en commit local, sin push.
