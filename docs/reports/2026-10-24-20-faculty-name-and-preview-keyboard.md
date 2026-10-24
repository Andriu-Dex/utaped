# Nombre institucional y navegación de la previsualización

Fecha: 2026-10-24. Cambios locales, sin commit por instrucción del propietario.

## Cambios

- V35 corrige la referencia FISEI y únicamente los borradores con la abreviatura conocida que no tienen firmas. Incrementa su row_version para detectar ediciones concurrentes. No actualiza otras unidades, documentos aprobados, snapshots o archivos conservados.
- El nombre completo se usa por defecto y también en nuevos informes/versiones derivados de un documento anterior con esa abreviatura. El documento fuente no cambia.
- El paso 5 ofrece una región de visor enfocada con ayuda visible, foco perceptible y aria-keyshortcuts. Flechas izquierda/derecha cambian la página dentro de los límites reales. No hay listener global. Se excluyen campos, controles, enlaces, contenido editable, diálogos y teclas modificadoras; durante la carga se conserva el bloqueo de navegación existente.
- El T1 omite líneas vacías en los extremos de justificación y objetivo, conservando separaciones interiores y datos guardados. Ambas secciones ya fluyen en una página cuando caben. Se mantiene el salto de sección para la matriz apaisada, así como portada, índices, anexos originales y firmas. No se reducen fuentes o márgenes para ocupar espacio. T1 engine13 evita reutilizar composiciones anteriores como actuales; no modifica bytes conservados.

## Comprobación

Frontend: lint, TypeScript y build aprobados, con el aviso conocido de bundle. Se amplía la aceptación del paso 5 para navegación por teclado, límites, modificadores y aislamiento respecto a campos y controles externos. Backend: prueba de migración selectiva, composición corta frente a texto con líneas vacías extremas, convivencia justificación/objetivo, orientación de matriz y conservación de artefactos anteriores. Resultados finales: 193 pruebas backend, 0 fallos/errores; 21 pruebas de aceptación aprobadas. Logs: `backend/target/2026-10-24-faculty-pagination-verified.log` y `backend/target/2026-10-24-faculty-keyboard-verified.log`. Ambos entornos de pruebas se limpiaron al terminar. Se revisaron las páginas renderizadas de portada, contenido y matriz del T1 corto, y la expansión del T1 largo (12 páginas).


## Entorno de revisión

Se actualizó utaped-sample mediante scripts/start-development.ps1, sin ResetData. Verificada migración V35 exitosa, referencia con nombre completo y ausencia de borradores unsigned con la abreviatura anterior. Los artefactos firmados se conservan. Recargar frontend y abrir paso 5 regenera la composición actual de un borrador sin recrearlo. Ningún commit, push o publicación.
