# Matriz: catálogos y responsables colectivos

Fecha: 2026-10-20. Rama: `feature/document-authorship-and-review`.

## Evidencia de reuniones y decisión

Se revisó la transcripción aportada por el propietario. L194–224 solicita seleccionar todos los responsables con representación colectiva, fuente editable (comisión o documento) y «Elaborado por» automático desde la comisión. L230–240 pide recursos y medios precargados mediante selección, con «Otros, especifique». L306–337 y L484–486 permiten nuevas actividades libres y fijan el orden contenido → actividades → anexos. L589, L623, L663 y L666 describen futuras listas de docentes/comisiones y asignaciones por período, con elección de carrera según pertenencia.

Las conversaciones no piden sustituir toda captura de texto por listas: la fuente, las narrativas y «Otro» siguen siendo editables. Tampoco autorizan inventar nombramientos oficiales cuando falte la base institucional.

## Implementado

- Ayuda y ejemplo de «Fuente de la matriz» corregidos: admite la misma comisión o un documento de referencia, sin restringirlo a normativa.
- Rótulo «Elaborado por», automático desde el grupo/comisión.
- Al elegir a todos los integrantes, tanto uno a uno como con «Seleccionar todos», se activa automáticamente la denominación colectiva configurada. Se elimina la casilla adicional. Al retirar un integrante se muestran las personas seleccionadas. Sus IDs individuales siguen guardados y se valida la selección en el servidor.
- Si no existe denominación configurada, se conservan los nombres y se explica que Administración debe configurarla; no se inventa una denominación institucional.
- Agregar actividad comienza con «Seleccione una actividad»; el campo de texto aparece exclusivamente al escoger «Otra actividad (especifique)». Los recursos y medios conservan sus listas de selección múltiple y «Otro» obligatorio cuando se selecciona.
- En development-seed se incorporan dos actividades de ejemplo opcionales por cada grupo sin actividades de referencia y sin catálogo activo propio: coordinación/seguimiento e informe de resultados. Son 20 opciones para 10 grupos en la base inicial. Los 55 registros derivados de templates se conservan sin modificarse; quedan 75 opciones activas en el entorno manual verificado.

## Catálogos existentes y límites

| Dato | Tratamiento |
|---|---|
| Actividad | Combo de catálogo administrable y opción Otra; ejemplos adicionales solo en desarrollo. |
| Recursos y medios | Selección múltiple del catálogo existente de 26 opciones procedentes de templates; no se reemplazan por ejemplos inventados. |
| Responsables | Lista de cuentas activas pertenecientes al ámbito; se conservan las personas ficticias realistas del escenario. |
| Carrera/comisión/período | Se eligen conjuntamente mediante el ámbito al crear el Plan. No se permite escribir otra carrera que contradiga su ámbito. |
| Fuente | Texto libre por petición expresa del cliente. |
| Aprobadores y cargos | Configuración existente por ámbito/período; la tabla oficial completa de asignaciones continúa pendiente. No se atribuyen autorizaciones nuevas mediante ejemplos. |

Las nuevas semillas solo actúan con el perfil development-seed y la base utaped_dev, con IDs estables y sin sobrescribir catálogos personalizados. No añaden obligatoriedad, no reinician datos ni cambian documentos o PDFs firmados existentes. No modifican el JSON de referencias oficiales para mezclarlo con ejemplos.

## Verificación

- Lint y compilación TypeScript/Vite aprobados. Aviso no bloqueante del bundle: 515,60 kB, 137,99 kB gzip.
- Dos pruebas backend de DevelopmentSeedTests aprobadas: aislamiento de la base, semillas repetibles, conteos, ausencia de obligatoriedad y conservación de credenciales/perfiles personalizados. Se corrigió una ambigüedad de tipos en la aserción del nuevo conteo antes de la ejecución final.
- Tres pruebas de aceptación aprobadas: ámbito/carrera en creación; selección de actividad precargada sin campo manual, colectivo automático por selección individual y por botón, deselección y persistencia de IDs; circuito completo T1, devolución/revisión, evidencias, T2 y versión formal. El circuito conserva la alternativa Otra actividad.
- Inspección de capturas de responsables en PC y presentación tablet oscura verificada por Playwright sin desbordamiento horizontal. No se agregaron estilos para celulares.
- git diff --check aprobado; entornos automáticos utaped-tests y utaped-acceptance eliminados al terminar. No se reiniciaron datos del entorno manual.

Logs y capturas permanecen ignorados en backend/target y frontend/test-results. No se ejecutó nuevamente la suite backend completa, porque no cambió el servicio de matriz ni el motor documental.

## Uso y entrega

Backend manual actualizado con `scripts/start-development.ps1`, sin variables nuevas ni migraciones. Verificadas siete cuentas conservadas y 75 actividades activas. Con Vite, basta recargar la pantalla y volver a abrir el paso 3 para consultar las opciones nuevas. En un frontend Docker compilado se necesita reconstruir ese servicio.

Los tres archivos del propietario que ya estaban en staging (App.tsx, App.css y la transcripción) permanecen fuera de esta entrega. Solo commit local de los archivos de esta corrección; sin push ni actualización del PR.
