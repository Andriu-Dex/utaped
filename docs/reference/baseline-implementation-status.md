# FASE ACTUAL

Estado del proyecto:

**MOCKUP INTERACTIVO DE ALTA FIDELIDAD**

El porcentaje o estado de implementación descrito en este archivo se refiere a qué tan completamente está representado cada requisito dentro del mockup.

NO significa porcentaje de implementación del sistema productivo final.

# ESTADO ACTUAL DE IMPLEMENTACIÓN

Fecha de referencia:
Septiembre 2026

Este archivo describe qué funcionalidades:

- existen;
- funcionan parcialmente;
- necesitan corrección;
- faltan.

No debe utilizarse como fuente de requisitos superior a `requirements.md`.

---

# LEYENDA

✅ Implementado correctamente

🟡 Implementado parcialmente / necesita validación

🔴 Pendiente / incorrecto

⚠️ No definido institucionalmente

---

# 1. AUTENTICACIÓN

✅ Login.

✅ Cambio de contraseña en primer ingreso.

✅ Recuperación de contraseña.

✅ Dashboard docente.

🟡 Persistencia todavía principalmente DEMO/local.

---

# 2. GESTIÓN DOCUMENTAL

✅ Pantalla principal `Gestión Documental Académica`.

✅ Modal `Nuevo documento`.

✅ Tipos:

- Plan de Trabajo.
- Informe.

🟡 Existen textos residuales:

- Gestión de Planes de Trabajo.
- Mis Planes de Trabajo.
- Volver a mis planes.

Debe normalizarse la nomenclatura global a Gestión Documental donde corresponda.

---

# 3. WIZARD T1

✅ Secuencia actual:

1. Información general.
2. Contenido.
3. Actividades.
4. Matriz.
5. Anexos.
6. Previsualización.
7. Firma y Finalización.

Esta secuencia ya corresponde a lo solicitado.

---

# 4. INFORMACIÓN GENERAL T1

✅ Docente.

✅ Grupo.

✅ Período.

✅ Versión inicial.

✅ Plantilla.

🟡 Revisar denominaciones:

- Unidad académica.
- Unidad administrativa.
- Carrera.

Debe alinearse exactamente con la decisión institucional y formato final.

---

# 5. CONTENIDO

✅ Justificación.

✅ Objetivo.

✅ Texto DEMO precargable.

✅ Editor.

✅ Estado completo.

✅ Mejora con IA simulada.

🟡 Asegurar que IA nunca sobrescriba directamente.

---

# 6. ACTIVIDADES

✅ Obligatorias.

✅ Opcionales.

✅ Otra.

✅ Filtros por categoría.

✅ Resumen de selección.

✅ Actividad libre.

---

# 7. MATRIZ

✅ Desde/Hasta.

✅ Responsables múltiples.

✅ Recursos.

✅ Medios.

✅ Otro.

✅ Validación de Otro vacío corregida.

✅ Conteo real de elementos válidos.

✅ Bloqueo de Continuar si existe actividad incompleta.

✅ Vista resumen de matriz.

---

# 8. RESPONSABLES

✅ Selección múltiple.

✅ Seleccionar todos.

✅ Denominación colectiva cuando se seleccionan todos los integrantes aplicables.

✅ Etiquetas por tipo: `Responsable de la comisión`, `Responsable de la unidad`, `Responsable del club` y `Responsable del grupo`.

✅ Los nombres e IDs individuales permanecen almacenados para trazabilidad y permisos.

✅ Al desmarcar un integrante, la presentación vuelve a los nombres individuales.

---

# 9. FUENTE DE MATRIZ

✅ Existe campo Fuente.

✅ `Elaborado por` puede derivarse del grupo.

🟡 Revisar que Fuente quede realmente reflejada en el documento formal.

---

# 10. ANEXOS

✅ Sí/No.

✅ Añadir.

✅ Editar.

✅ Eliminar.

✅ Orden.

✅ Archivo DEMO.

---

# 11. PREVISUALIZACIÓN T1

✅ Render A4.

✅ Navegación por páginas.

✅ Zoom.

✅ Ajustar.

✅ Paginación.

✅ Sidepanel de flujo.

✅ Firma e historial.

✅ pageCount dinámico después de refactor.

---

# 12. FIDELIDAD DEL T1

🔴 PRINCIPAL DEUDA ACTUAL.

El documento todavía no reproduce fielmente el formato institucional.

Debe corregirse:

## Portada

Actualmente el bloque central:

- UNIDAD ACADÉMICA / ADMINISTRATIVA
- PLAN DE TRABAJO DE
- PERÍODO

está demasiado abajo.

Debe colocarse aproximadamente en la zona media vertical según formato de referencia.

## Carrera

Debe revisarse su ubicación según referencia institucional.

## Footer

Actualmente puede romper:

`Documento de uso interno controlado por la Universidad Técnica de Ambato`

en varias líneas.

Debe quedar en una única fila visual junto a:

`Formato Nº: UTA-SGC-A-2-1-P7-T1`

y número de página.

## Línea del footer

Debe existir una sola línea según referencia.

## Estilos

Revisar:

- gris de encabezados;
- tipografía;
- tamaño;
- bordes;
- espaciados;
- alineación.

Objetivo:

máxima fidelidad al DOCX suministrado.

---

# 13. NOTA DE DATOS PERSONALES

🟡 Existe concepto.

🔴 La condición no debe depender simplemente del nombre del grupo.

Debe modelarse mediante condición explícita.

---

# 14. FIRMA

✅ Modal de firma.

✅ Archivo .p12/.pfx.

✅ Contraseña.

✅ confirmación.

✅ mensaje de uso temporal.

✅ ubicación de firma.

✅ signatureSlots.

✅ firma docente.

✅ ocultamiento del botón si el usuario ya firmó.

✅ `Firmar y finalizar` firma una sola vez y activa automáticamente la siguiente etapa configurada.

✅ No existe una segunda acción `Enviar a revisión`.

✅ Un flujo incompleto continúa bloqueando la firma y no inventa revisores.

---

# 15. AISLAMIENTO MULTIDOCUMENTO

✅ CORRECCIÓN CRÍTICA IMPLEMENTADA.

Se creó lógica basada en:

`targetDocId`

y:

`mutateDocument`

El Informe ya no debe mutar el Plan base.

Debe preservarse estrictamente.

---

# 16. PAGECOUNT

✅ Se eliminó hardcode principal.

✅ Usa páginas dinámicas.

⚠️ Verificar que no queden valores `3` o `5` utilizados como fuente de navegación en otros componentes.

---

# 17. INFORME T2

✅ Wizard.

✅ Información general.

✅ Derivado de Plan.

✅ Antecedentes.

✅ Desarrollo.

✅ Conclusiones.

✅ Oportunidades.

✅ Contactos.

✅ Anexos.

✅ Previsualización.

✅ Firma.

✅ T2 de 5 páginas en escenario DEMO actual.

---

# 18. TÍTULO T2

✅ `normalizeInformeTitle`.

✅ Se corrigió duplicación:

`INFORME DE: INFORME DE...`

🟡 Hacer prueba regresiva en:

- preview Wizard;
- visor posterior;
- revisión.

---

# 19. ACTIVIDADES T2

✅ Derivación desde Plan.

✅ Porcentaje.

✅ Observaciones.

🟡 Verificar que siempre derive la colección del Plan correcto.

---

# 20. NOTAS T2

🔴 REVISAR.

Las notas instructivas del formato no deben mostrarse en el documento final cuando el sistema ya conoce que deriva de Plan.

Realizar revisión visual completa de cada página.

---

# 21. FLUJO DE REVISIÓN

✅ Bandeja.

✅ Visor.

✅ Observaciones.

✅ Devolver.

✅ Aprobar.

✅ Firma.

✅ rondas.

✅ historial.

✅ múltiples revisores en una etapa.

---

# 22. RESALTADO DE OBSERVACIONES

✅ Selección rectangular sobre la página y observación asociada.

✅ Coordenadas normalizadas, página automática y persistencia DEMO.

✅ Navegación `Ir al resaltado`, feedback visual numerado y estabilidad al cambiar zoom.

✅ El docente visualiza y resuelve la observación durante corrección; la ronda anterior queda histórica.

✅ Los resaltados no modifican las páginas del artefacto firmado.

✅ Acción accesible y cancelación con Escape.

---

# 23. FLUJO CONFIGURABLE

🟡 Existe configuración administrativa.

🔴 Los escenarios actuales todavía representan demasiado un flujo genérico.

Ejemplo actual:

- Elaboración.
- Revisión.
- Revisión.
- Coordinación.
- Validación final.

Debe validarse contra grupo.

No todas las comisiones tienen mismo flujo.

---

# 24. ÓRGANOS COLEGIADOS

🔴 Falta representación adecuada.

Una etapa puede ser:

`Consejo Directivo`

u órgano equivalente.

No necesariamente:

- persona individual;
- firmante.

El modelo debe admitirlo.

---

# 25. QIPOC / PROCESO EXTERNO

⚠️ NO IMPLEMENTAR DEFINITIVO.

La reunión dejó este punto pendiente de validación.

Mantener como:

`PENDIENTE DE VALIDACIÓN INSTITUCIONAL`.

---

# 26. ACTIVIDADES EN EJECUCIÓN

✅ Mis Actividades.

✅ Filtros.

✅ Responsabilidad.

✅ Vista solo lectura para no responsable.

✅ Fechas.

✅ estados.

---

# 27. EVIDENCIAS

✅ PDF.

✅ límite DEMO.

✅ reemplazo.

✅ historial.

✅ deadline.

✅ control de responsable.

✅ Mis Evidencias.

---

# 28. VALIDACIÓN DE EVIDENCIAS

✅ Bandeja.

✅ observación.

✅ validación.

✅ estados:

- pendiente;
- validada;
- observada.

✅ reemplazo obliga a revalidar.

---

# 29. ADMINISTRACIÓN

✅ Usuarios.

✅ Grupos.

✅ Períodos.

✅ Actividades.

✅ Recursos.

✅ medios.

✅ flujos.

✅ feriados.

✅ plantillas.

🟡 Algunas configuraciones siguen siendo DEMO.

---

# 30. NOTIFICACIONES

✅ Bell.

✅ listado.

✅ por usuario.

✅ navegación contextual.

---

# 31. AUDITORÍA

✅ eventos.

✅ filtros.

✅ timeline.

✅ detalles.

✅ IDs internos ocultos en UI.

---

# 32. REPORTES E HISTÓRICO

✅ reportes documentales.

✅ histórico.

✅ versiones.

✅ período cerrado DEMO.

✅ solo lectura histórico.

✅ comparación de versiones.

---

# 33. CIERRE DE PERÍODO

🟡 Implementado únicamente como DEMO.

Debe mantener mensajes como:

`PROBAR CIERRE — DEMO`

y:

`RESTABLECER DEMO`

No presentar esto como reapertura institucional.

---

# 34. ACCIONES DE TABLAS

🟡 Parcial.

Se solicitó migrar las acciones frecuentes a iconos.

Debe revisar todas las tablas.

Objetivo:

- menos texto;
- menor saturación visual;
- tooltip title;
- aria-label.

No aplicar iconos ambiguos sin ayuda.

---

# 35. PALETA

✅ Morado reducido.

✅ Azul institucional predominante.

✅ Verde semántico.

✅ ámbar advertencia.

🟡 Revisar badges residuales morados como `Informe`.

No es crítico mientras sea secundario.

---

# 36. DOCUMENTOS VS UI

✅ Aplicación moderna.

🟡 Documento oficial todavía necesita fidelidad.

No rediseñar A4 como pantalla web.

---

# 37. BUGS YA CORREGIDOS QUE NO DEBEN REGRESAR

## Aislamiento multidocumento

No permitir que Informe modifique Plan.

## React Hooks

No reintroducir:

`Rendered fewer hooks than expected`

## Histórico

No reintroducir filtro que oculte períodos históricos por heredar período activo.

## PageCount

No hardcodear.

## Firma

No mostrar `FIRMAR Y CONTINUAR` si el actor ya firmó.

## Título Informe

No duplicar `INFORME DE`.

## Otro vacío

No permitir recurso/medio personalizado vacío.

---

# 38. PRIORIDAD RECOMENDADA DE TRABAJO

## PRIORIDAD 1

Fidelidad documental T1.

- portada;
- footer;
- encabezado;
- carrera;
- espacios;
- tipografía;
- colores;
- tablas.

## PRIORIDAD 2

Flujos por grupo/órgano colegiado.

## PRIORIDAD 3

Revisión completa T2 contra formato oficial.

## PRIORIDAD 4

Normalización de nomenclatura `Gestión Documental`.

## PRIORIDAD 5

Acciones mediante iconos.

## PRIORIDAD 6

Pruebas end-to-end.

---

# 39. PRUEBAS END-TO-END REQUERIDAS

## Escenario A — Plan

1. Restablecer DEMO.
2. Crear Plan.
3. Seleccionar grupo.
4. Completar contenido.
5. Seleccionar actividades.
6. Agregar Otra.
7. Configurar matriz.
8. Seleccionar varios responsables.
9. Probar Seleccionar todos.
10. Agregar recurso Otro.
11. Agregar medio Otro.
12. Anexos.
13. Revisar todas las páginas.
14. Firmar.
15. Finalizar.
16. Verificar estado.

---

## Escenario B — Revisión

1. Abrir documento como primer revisor.
2. Revisar artefacto exacto.
3. Crear observación.
4. Crear resaltado.
5. Devolver.
6. Corregir.
7. Reenviar.
8. comprobar nueva ronda.
9. comprobar misma versión formal.
10. aprobar.

---

## Escenario C — Multidocumento

1. Tener Plan validado.
2. Crear Informe desde dicho Plan.
3. Confirmar actividades importadas.
4. Firmar Informe.
5. Verificar que Plan no cambia.
6. Enviar Informe.
7. Verificar nuevamente Plan.

---

## Escenario D — Evidencia

1. Abrir actividad responsable.
2. Subir PDF por cada medio.
3. Reemplazar.
4. revisar versión.
5. observar.
6. reemplazar observada.
7. validar.

---

# 40. DEFINICIÓN DE TERMINADO

La corrección general se considera terminada cuando:

- compila;
- no hay errores TS;
- T1 coincide visualmente con referencia;
- T2 coincide visualmente con referencia;
- Plan e Informe están aislados;
- todos los flujos esenciales funcionan;
- ningún estado se contradice;
- todas las reglas confirmadas de reunión están implementadas;
- no se han inventado reglas pendientes.
````

---

# 41. PASADA FOCALIZADA UI Y DOCUMENTOS — 14/09/2026

Estado: **IMPLEMENTADA Y VALIDADA EN MOCKUP**.

- Asistente de redacción T1/T2: aplicar y descartar actúan sobre el campo de origen; T1 guarda mediante el autosave del borrador por documento y T2 conserva el borrador DEMO en `localStorage`.
- T1: encabezado de cuatro filas alineado con el PDF firmado; Facultad y Carrera comparten la celda institucional; portada mantiene Carrera separada cuando la unidad es académica; fecha se presenta en formato institucional largo.
- Unidades: catálogo DEMO compartido con tipo `ACADEMIC`/`ADMINISTRATIVE`, carreras opcionales según tipo, altas, edición, activación/desactivación y persistencia de configuración.
- T2: título semántico normalizado, encabezado propio sin fila Carrera, validación obligatoria del Plan relacionado, importación por copia de actividades y medios, y aislamiento del Plan origen.
- Dashboard: conteos documentales, evidencias, flujos, período y actividad reciente; navegación a módulos operativos y ausencia de métricas de evaluación personal.
- Plantillas: builder HTML5 drag-and-drop, secciones institucionales bloqueadas, visibilidad de secciones permitidas, vista previa, persistencia, restauración y configuraciones T1/T2 separadas. Los nuevos documentos capturan la configuración vigente; documentos existentes y firmados conservan su snapshot.
- Cobertura agregada: `ai-writing-assistant.spec.ts`, `institutional-units.spec.ts`, `t2-derived-validation.spec.ts`, `admin-dashboard.spec.ts` y `template-builder.spec.ts`.
- Regresión visual: seis snapshots revisados y actualizados de forma selectiva después de contrastar T1 con el PDF firmado y T2 con el DOCX oficial.

Pendiente institucional: confirmar el catálogo definitivo de unidades/carreras, autoridades, libertad permitida para ordenar secciones y la integración técnica real de firma/IA. Estas decisiones permanecen configurables o identificadas como DEMO.

# 42. MICRO-PASADA FUNCIONAL FINAL — 14/09/2026

- T2 canónico: Andrea dispone tras reset de un Plan validado y en ejecución de la Comisión de Vinculación con la Sociedad, con actividades, recursos y medios importables.
- Stepper T2: cabecera compacta, ocho pasos en una fila con overflow horizontal, estados accesibles e iconos SVG.
- Iconografía: se retiraron pictogramas emoji del código ejecutable y se añadió una regresión automática.
- A4: tamaño centralizado para portrait y landscape; el visor expone metadata A4 y conserva la matriz T1 horizontal.
- Plantillas: todas las secciones de contenido requeridas son reordenables; solo encabezado y pie permanecen como marco fijo. Guardar y restaurar requieren confirmación.
- Renderer: nuevos T1/T2 componen páginas, numeración e índice desde el snapshot de plantilla. Documentos existentes conservan su snapshot.
- Arquitectura futura documentada: Groq DEV / OpenAI PROD mediante backend y PostgreSQL local/on-premise. El mockup sigue sin conexiones reales.

# 43. AUDITORÍA IA, ADMINISTRACIÓN Y RESET DEMO — 15/09/2026

Estado: **IMPLEMENTADA Y VALIDADA EN MOCKUP**.

- Asistente DEMO T1/T2: aplicar y descartar conservan el campo de origen, no comparten estado entre campos y persisten al navegar o recargar. Una solicitud con el campo vacío se rechaza con un mensaje inline y no genera contenido.
- Plantillas T1/T2: la configuración guardada sobrevive a recarga, se mantiene independiente por tipo documental, gobierna documentos nuevos y no altera snapshots existentes. La restauración predeterminada permanece limitada a la plantilla seleccionada.
- Catálogo institucional: altas, activación/desactivación y tipos académico/administrativo alimentan los selectores T1/T2; las unidades administrativas eliminan Carrera y los documentos históricos conservan su snapshot.
- Dashboard: los conteos documentales se calculan desde la colección DEMO actual, reaccionan al crear un documento y vuelven al valor canónico después del reset documental.
- Reset documental: restaura documentos, actividades/evidencias y borradores documentales; conserva plantillas y unidades institucionales administrativas, incluso después de recargar.
- Cobertura: se amplió `ai-writing-assistant.spec.ts`, `admin-dashboard.spec.ts` y `template-builder.spec.ts`; se añadió `demo-reset-boundaries.spec.ts`.
- Validación final: TypeScript y build correctos; 4 pruebas Node aprobadas; 32 pruebas E2E aprobadas; snapshots T1/T2 sin cambios.

Limitación vigente: el asistente continúa siendo una simulación local y no integra Groq, OpenAI ni servicios externos.

# 44. CAPTCHA DE ACCESO — 16/09/2026

Estado: **IMPLEMENTADO Y VALIDADO EN MOCKUP**.

- El inicio de sesión incluye una verificación matemática renovable y bloquea el acceso hasta recibir el resultado correcto.
- Una respuesta inválida muestra un error inline accesible; al cambiar el desafío se limpia la respuesta anterior.
- La autenticación DEMO conserva sus credenciales y flujos existentes. Las ayudas E2E resuelven el desafío visible antes de iniciar sesión.
- Validación: TypeScript y build correctos; 4 pruebas Node aprobadas; 34 pruebas E2E aprobadas; snapshots sin cambios.

# 45. AJUSTES T1 POSTERIORES A VALIDACIÓN CON CLIENTE — 19/09/2026

Estado: **IMPLEMENTADOS Y VALIDADOS EN MOCKUP**.

- Información general T1: el período activo, unidad y carrera se muestran como datos institucionales automáticos y de solo lectura; el diálogo de creación conserva únicamente la selección del grupo institucional.
- Contenido: la justificación es obligatoria y muestra el mensaje inline solicitado. El asistente IA DEMO no sustituye la necesidad de ingresar contenido.
- Actividades: el recorrido normal T1 se consolidó en una sola pantalla de actividades. Cada registro incluye tipo, descripción, duración, responsables, recursos y medios de verificación; la matriz del documento se compone desde esos datos.
- Anexos: se conserva la decisión Sí/No; cuando se habilitan se aceptan documentos de cualquier tipo para la simulación y no se mezclan con evidencias de ejecución.
- Firma: antes de abrir la simulación de certificado aparece una confirmación explícita. Cancelar mantiene el documento sin firmas y permite volver a editar; confirmar continúa con la firma y finalización existentes.
- Regresión: TypeScript y build correctos; 4 pruebas Node aprobadas; 36 pruebas E2E aprobadas, incluidas dos pruebas específicas de los ajustes T1. Snapshots sin cambios.

# 46. MICROCORRECCIÓN VISUAL T1 — 20/09/2026

Estado: **IMPLEMENTADA Y VALIDADA EN MOCKUP**.

- Actividades: `activityType` es la fuente de verdad visual y las filas ya no combinan una clasificación con `Tipo: Otra`; se conserva compatibilidad con datos DEMO heredados.
- Anexos: la pantalla se redujo a Sí/No; al elegir Sí el modal solicita únicamente un archivo, acepta `.docx` e imágenes y asigna la letra del anexo automáticamente.
- Información general: se retiraron ayudas redundantes y `Unidad` se presenta como `Facultad`, sin modificar IDs ni lógica institucional.
- Validación: TypeScript, build, cuatro pruebas Node y 36 pruebas E2E aprobadas; snapshots sin cambios.

# 47. RENDER DE ANEXOS E IDS ÚNICOS — 20/09/2026

Estado: **IMPLEMENTADA Y VALIDADA EN MOCKUP**.

- El visor T1 dejó de imprimir nombre y archivo como campos separados cuando representan el mismo filename; la descripción solo aparece si tiene contenido.
- Se conserva la numeración automática de anexos y los IDs nuevos se calculan a partir del máximo ID persistido, incluso después de recargar.
- Validación: TypeScript, build, cuatro pruebas Node y 36 pruebas E2E aprobadas; snapshots sin cambios.

# 48. REDISEÑO INSTITUCIONAL DEL LOGIN — 21/09/2026

Estado: **IMPLEMENTADO Y VALIDADO EN MOCKUP**.

- Login: se reemplazó la composición rígida azul/blanca por una tarjeta institucional central sobre un fondo azul con gradientes y geometría CSS sutil.
- Identidad: se conserva el logo existente y se redujo el contenido a identidad, credenciales, verificación y acceso.
- CAPTCHA: se sustituyó el desafío matemático por un código alfanumérico visual DEMO regenerable, con validación case-insensitive y error inline.
- Accesibilidad: se conservaron labels, controles de contraseña accesibles y acciones SVG con nombres accesibles.
- Validación: TypeScript, build, prueba de emojis y 36 pruebas E2E aprobadas; no se modificaron snapshots.
