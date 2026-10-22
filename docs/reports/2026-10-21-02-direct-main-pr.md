# Preparación de pull request directo a main

Fecha: 2026-10-21. Rama: `feature/institutional-rules-and-execution-report`.

El propietario autoriza abrir el PR directamente hacia main si no hay diferencias con develop.

## Comparación verificada

- Se actualizaron las referencias con `git fetch origin --prune`.
- `origin/main`: `d18c26437834ef31a9ce73df357902dba8c765b7`.
- La rama remota develop ya no existe. Su copia local conserva `a57130f4720b875217d74dcec3b8ef7b40e4cfda`.
- `git diff --quiet origin/main develop`: código 0, contenido idéntico.
- Historia: main tiene un commit adicional, la fusión del PR #15, confirmada en GitHub el 2026-10-20 a las 22:49 de Ecuador. No hay cambios de archivos exclusivos de develop.
- Árbol de trabajo limpio antes de preparar este reporte.

Por tanto, el PR se dirige a main, sin recrear develop ni abrir un PR intermedio. GitHub requiere publicar la rama de origen para abrirlo. La revisión y la fusión quedan a cargo del propietario; no se fusiona ni se elimina ninguna rama automáticamente. El PR #11 abierto pertenece a otra rama y permanece intacto.

## Alcance y validación

Informe de ejecución, estructuras versionadas, permisos históricos/delegados, constancia Quipux, plazos hábiles, custodia/purga controlada y sus interfaces. Migraciones V25–V33. [Reporte de implementación y pruebas](2026-10-21-01-institutional-completion-and-stabilization.md): 177 pruebas backend, regresiones adicionales, 16 escenarios de aceptación, lint/build y configuración Nginx aprobados.

En esta preparación solo se añade documentación; no se modificó código ni se repitieron las pruebas de implementación. Antes de actualizar una instalación persistente se requiere respaldo conjunto de DB/archivos/imágenes. Las políticas provisionales y pendientes institucionales permanecen documentados.
