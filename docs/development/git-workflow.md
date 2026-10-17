# Flujo Git

main contiene versiones terminadas. develop integra entregas mediante PR. Implementaciones en feature/*, correcciones en fix/* y documentación en docs/*. PR hacia develop, con validación y reporte; versiones completas por PR develop → main.

Autorización permanente del propietario, 2026-10-03: al finalizar cada implementación y sus verificaciones, realizar commit, push de su rama y PR hacia develop automáticamente, salvo indicación contraria. El propietario conserva exclusivamente revisión y fusión. No implica autorización para desplegar ni modificar main. CI verifica frontend/backend/E2E al publicar; consultar resultados reales en GitHub.

Entrega ampliada autorizada el 2026-10-03: una misma rama puede contener varios bloques funcionales; cada bloque se verifica y se confirma con su commit. Al completar la entrega se hace un único push y PR hacia develop. El propietario revisa y fusiona.

## Ramas temporales y limpieza — 2026-10-17

main y develop son ramas permanentes. Las ramas feature/* y fix/* son temporales y se conservan mientras tengan trabajo pendiente o PR abierto. Tras la fusión se pueden eliminar; el historial sigue accesible desde develop y el PR. No borrar ramas solo por su antigüedad o prefijo: verificar que su punta esté integrada, que no existan cambios adicionales y que no tengan PR abierto.

El propietario autorizó publicar fix/document-creation-options y abrir su PR hacia develop; la revisión/fusión sigue siendo exclusivamente suya. Se eliminaron nueve ramas remotas feature ya integradas comprobando ascendencia Git y PRs. La rama agent/8b075407-abf0-4dbd-904c-127b57d98087-v2 se conserva por su PR #11 abierto. No se eliminaron ramas locales ni commits.

Se habilita en GitHub la eliminación automática de la rama de origen después de fusionar un PR. Las ramas con trabajo pendiente se conservan. La siguiente entrega prevista es la edición con previsualización simultánea A4, comenzando por Contenido de T1; no está implementada en este cierre.
