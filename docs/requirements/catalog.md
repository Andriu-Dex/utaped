# Catálogo trazable de requisitos

Actualización productiva: ver [estado de entrega](../../implementation-status.md).

Extracción de la línea base; no es ratificación institucional ni catálogo atómico terminado. IDs FR-* preservados; UT-SEC-* identifican bloques antes sin ID. Declaraciones compuestas requieren desglosarse antes de implementar.

[Datos estructurados](catalog.json) · [Fuente íntegra](../reference/baseline-requirements.md) · [Aceptación por módulo](functional.md) · [Plan de validación](../validation/validation-plan.md).

Cada registro conserva texto original, sección, estado y situación productiva. Prioridades y aceptación individual están pendientes; la aceptación agrupada documentada no acredita pruebas ejecutadas.

## UT-SEC-001

Fuente: §1 — OBJETIVO GENERAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Desarrollar una aplicación web para facilitar la creación, revisión, aprobación, firma, seguimiento, ejecución y consulta histórica de documentación académica de grupos institucionales de la FISEI.

El sistema debe reducir:

- edición manual repetitiva;
- circulación de documentos por correo;
- errores de formato;
- pérdida de trazabilidad;
- duplicación de información;
- inconsistencias entre planificación e informe.

## UT-SEC-002

Fuente: §2 — ACTORES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

## 2.1 Docente

Puede:

- autenticarse;
- cambiar contraseña temporal;
- recuperar contraseña;
- consultar grupos;
- crear Planes de Trabajo;
- elaborar contenido;
- seleccionar actividades;
- configurar matriz;
- adjuntar anexos;
- previsualizar;
- firmar;
- finalizar;
- consultar estado;
- corregir documentos devueltos;
- ejecutar actividades;
- subir evidencias;
- crear Informes;
- consultar historial.


## 2.2 Revisor

Puede:

- consultar documentos asignados;
- visualizar artefacto formal;
- registrar observaciones;
- resaltar partes observadas;
- devolver;
- aprobar;
- firmar cuando corresponda;
- revisar evidencias.


## 2.3 Coordinador

Según el flujo institucional puede:

- revisar;
- coordinar;
- validar etapas;
- gestionar grupos;
- gestionar miembros;
- participar en flujos;
- verificar documentación externa cuando corresponda.


## 2.4 Autoridad / validador

Puede intervenir en:

- validación final;
- aprobación;
- firma;

según el flujo configurado.

No asumir una autoridad universal.


## 2.5 Administrador

Puede gestionar:

- usuarios;
- importaciones;
- grupos;
- miembros;
- períodos;
- actividades;
- recursos;
- medios de verificación;
- feriados;
- plantillas documentales;
- flujos;
- configuraciones institucionales.

## FR-AUTH-001

Fuente: §3 — AUTENTICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El sistema debe permitir autenticación mediante credenciales.

## FR-AUTH-002

Fuente: §3 — AUTENTICACIÓN. Estado: HEREDADO. Producción: PARCIAL.

Usuarios importados pueden recibir contraseña temporal.

## FR-AUTH-003

Fuente: §3 — AUTENTICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

En el primer ingreso con contraseña temporal debe solicitar cambio obligatorio.

## FR-AUTH-004

Fuente: §3 — AUTENTICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Debe existir recuperación de contraseña.

## FR-AUTH-005

Fuente: §3 — AUTENTICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El usuario autenticado debe conservar identidad única durante la sesión.

Cambiar contexto o rol no cambia la persona autenticada.

## FR-GRP-001

Fuente: §4 — GRUPOS INSTITUCIONALES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El sistema debe manejar una entidad configurable:

`Grupo institucional`

## FR-GRP-002

Fuente: §4 — GRUPOS INSTITUCIONALES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Tipos posibles:

- Comisión.
- Unidad.
- Club.
- Otro.

## FR-GRP-003

Fuente: §4 — GRUPOS INSTITUCIONALES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Un docente puede pertenecer a varios grupos.

## FR-GRP-004

Fuente: §4 — GRUPOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Puede existir un Plan diferente para cada combinación:

`docente + grupo + período`

## FR-GRP-005

Fuente: §4 — GRUPOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

No permitir duplicado de esa combinación cuando corresponda.

## FR-GRP-006

Fuente: §4 — GRUPOS INSTITUCIONALES. Estado: HEREDADO. Producción: PARCIAL.

Los roles de pertenencia pueden incluir:

- Miembro.
- Coordinador.
- Otro.

No utilizar “Responsable” como rol permanente del grupo porque ese concepto pertenece a las actividades.

## FR-PER-001

Fuente: §5 — PERÍODOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El administrador define períodos académicos.

Ejemplo actual:

`Julio – Diciembre 2026`

## FR-PER-002

Fuente: §5 — PERÍODOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El período debe disponer de:

- fecha inicio;
- fecha fin;
- ventana de elaboración;
- ventana de revisión.

## FR-PER-003

Fuente: §5 — PERÍODOS. Estado: HEREDADO. Producción: PARCIAL.

Estas fechas deben ser configurables.

## UT-SEC-006

Fuente: §6 — PLAN DE TRABAJO T1. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Formato:

`UTA-SGC-A-2-1-P7-T1`

## UT-SEC-007

Fuente: §7 — SECUENCIA DEL WIZARD T1. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El orden funcional será:

1. Información general.
2. Contenido.
3. Actividades.
4. Matriz de actividades.
5. Anexos.
6. Previsualización.
7. Firma y Finalización.

No alterar este orden sin nueva validación institucional.

## FR-T1-001

Fuente: §8 — INFORMACIÓN GENERAL DEL PLAN. Estado: HEREDADO. Producción: PARCIAL: identidad, grupo y periodo; datos institucionales manuales.

Debe obtener automáticamente:

- docente elaborador;
- período;
- grupo;
- información institucional conocida.

## FR-T1-002

Fuente: §8 — INFORMACIÓN GENERAL DEL PLAN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La unidad/facultad debe poder derivarse de información institucional.

## FR-T1-003

Fuente: §8 — INFORMACIÓN GENERAL DEL PLAN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La carrera debe aparecer correctamente vinculada.

## FR-T1-004

Fuente: §8 — INFORMACIÓN GENERAL DEL PLAN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL: fecha civil al crear borrador.

La fecha de elaboración debe registrarse.

## FR-T1-005

Fuente: §8 — INFORMACIÓN GENERAL DEL PLAN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Al finalizar el documento, la fecha de elaboración debe quedar congelada.

No debe cambiar posteriormente porque otro usuario revise o firme.

## FR-T1-CONT-001

Fuente: §9 — CONTENIDO DEL PLAN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL: un campo persistido por borrador.

Debe existir exactamente una Justificación.

## FR-T1-CONT-002

Fuente: §9 — CONTENIDO DEL PLAN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL: un campo persistido por borrador.

Debe existir exactamente un Objetivo.

## FR-T1-CONT-003

Fuente: §9 — CONTENIDO DEL PLAN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Puede existir texto base precargado editable.

El docente puede:

- editar;
- reemplazar;
- eliminar.

## FR-T1-CONT-004

Fuente: §9 — CONTENIDO DEL PLAN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Puede existir asistente IA de redacción.

La IA puede sugerir:

- mejora de redacción;
- gramática;
- claridad;
- tono formal.

## FR-T1-CONT-005

Fuente: §9 — CONTENIDO DEL PLAN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La IA nunca reemplaza automáticamente el contenido.

Debe mostrar:

- original;
- sugerencia.

El usuario decide:

- aplicar;
- descartar.

## FR-ACT-001

Fuente: §10 — ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

El administrador puede configurar catálogo de actividades.

## FR-ACT-002

Fuente: §10 — ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Las actividades pueden clasificarse como:

- POA.
- Plan de Mejoras.
- Acción de Mejora.
- Otra.

## FR-ACT-003

Fuente: §10 — ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Puede haber actividades obligatorias por grupo.

## FR-ACT-004

Fuente: §10 — ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Las obligatorias:

- aparecen seleccionadas;
- no pueden eliminarse.

## FR-ACT-005

Fuente: §10 — ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Puede haber actividades opcionales.

## FR-ACT-006

Fuente: §10 — ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

El docente puede crear libremente una actividad `Otra`.

No debe obligarse a clasificar toda actividad como POA/Plan/Acción.

## UT-SEC-011

Fuente: §11 — MATRIZ DE ACTIVIDADES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Cada actividad debe poder definir:

- fecha Desde;
- fecha Hasta;
- responsables;
- recursos;
- medios de verificación.

## FR-DATE-001

Fuente: §12 — FECHAS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Usar fecha completa:

`día / mes / año`

## FR-DATE-002

Fuente: §12 — FECHAS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Las fechas deben estar dentro del período cuando corresponda.

## FR-DATE-003

Fuente: §12 — FECHAS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Una actividad puede atravesar un feriado.

## FR-DATE-004

Fuente: §12 — FECHAS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Una actividad no debe iniciar o finalizar en un feriado cuando esa restricción esté configurada.

## FR-RESP-001

Fuente: §13 — RESPONSABLES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Debe seleccionarse mínimo un responsable.

## FR-RESP-002

Fuente: §13 — RESPONSABLES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Puede seleccionarse más de uno.

## FR-RESP-003

Fuente: §13 — RESPONSABLES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Debe existir:

`Seleccionar todos`

## FR-RESP-004

Fuente: §13 — RESPONSABLES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

La lista seleccionable debe corresponder a integrantes aplicables del grupo.

## FR-RESP-005

Fuente: §13 — RESPONSABLES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Cuando todos los miembros aplicables sean seleccionados, el documento puede representar el conjunto mediante denominación colectiva.

Ejemplo solicitado en reunión:

`Responsable de la comisión`

La denominación debe ser configurable según el tipo de grupo.

Internamente deben conservarse las identidades individuales para trazabilidad.

## FR-REC-001

Fuente: §14 — RECURSOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Los recursos se seleccionan de catálogo.

Ejemplos:

- Matriz de seguimiento.
- Almacenamiento institucional.
- Reglamento.
- Sistema institucional.
- Sala de reuniones.
- Equipos.

## FR-REC-002

Fuente: §14 — RECURSOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Debe existir `Otro`.

## FR-REC-003

Fuente: §14 — RECURSOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Al seleccionar `Otro` debe requerirse descripción.

## FR-REC-004

Fuente: §14 — RECURSOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

No permitir guardar `Otro` vacío.

## FR-REC-005

Fuente: §14 — RECURSOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Los catálogos deben ser administrables.

## FR-MED-001

Fuente: §15 — MEDIOS DE VERIFICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Seleccionar desde catálogo.

Ejemplos:

- Informe.
- Acta.
- Oficio.
- Registro.
- Resolución.
- Registro fotográfico.
- Certificado.
- Ficha.

## FR-MED-002

Fuente: §15 — MEDIOS DE VERIFICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Debe existir `Otro`.

## FR-MED-003

Fuente: §15 — MEDIOS DE VERIFICACIÓN. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

Otro requiere descripción obligatoria.

## FR-MAT-001

Fuente: §16 — FUENTE Y ELABORACIÓN DE MATRIZ. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

La matriz debe permitir registrar:

`Fuente`

Puede ser:

- documento;
- grupo;
- comisión;
- otra fuente.

Debe ser editable cuando corresponda.

## FR-MAT-002

Fuente: §16 — FUENTE Y ELABORACIÓN DE MATRIZ. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL DE BORRADORES.

`Elaborado por` debe derivarse automáticamente del grupo/comisión responsable.

## UT-SEC-017

Fuente: §17 — NOTA DE DATOS PERSONALES. Estado: HEREDADO. Producción: PARCIAL: CONDICIÓN EXPLÍCITA EN BORRADOR.

La plantilla institucional contempla una nota de protección de datos personales.

Solo debe incluirse cuando el documento recopile datos personales que hagan aplicable dicha nota.

No decidir esto únicamente por el nombre del grupo.

La condición debe ser explícita/configurable.

## FR-ANX-001

Fuente: §18 — ANEXOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El docente selecciona:

- Sí.
- No.

## FR-ANX-002

Fuente: §18 — ANEXOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Si selecciona Sí:

- puede crear anexos;
- editar;
- eliminar;
- ordenar.

## FR-ANX-003

Fuente: §18 — ANEXOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

La numeración será automática:

- Anexo A.
- Anexo B.
- Anexo C.

## FR-ANX-004

Fuente: §18 — ANEXOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Eliminar un anexo reordena la secuencia.

## FR-ANX-005

Fuente: §18 — ANEXOS. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Anexos del documento no son evidencias de actividades.

## UT-SEC-019

Fuente: §19 — PREVISUALIZACIÓN T1. Estado: HEREDADO. Producción: PARCIAL: PREVISUALIZACIÓN T1; ACEPTACIÓN INSTITUCIONAL PENDIENTE.

El documento debe representar fielmente el formato oficial.

## UT-SEC-020

Fuente: §20 — ENCABEZADO T1. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Debe contener:

- logo / Sistema de Gestión de la Calidad;
- Universidad Técnica de Ambato;
- Plan de Trabajo;
- información institucional;
- fecha;
- carrera cuando corresponda.

La estructura exacta debe seguir el formato institucional de referencia.

## UT-SEC-021

Fuente: §21 — PORTADA T1. Estado: HEREDADO. Producción: PARCIAL: PORTADA T1; CAMPOS INSTITUCIONALES MANUALES.

La composición central debe respetar el formato institucional real.

Los elementos:

- Unidad Académica.
- Facultad.
- Carrera.
- Plan de Trabajo de.
- Período.

deben colocarse en la posición correspondiente al documento original.

No colocarlos excesivamente cerca del borde inferior.

El bloque principal debe quedar aproximadamente en la zona media vertical del A4 según el formato.

## UT-SEC-022

Fuente: §22 — PIE DE PÁGINA T1. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Debe mostrarse en una única fila visual.

Debe contener:

- `Documento de uso interno controlado por la Universidad Técnica de Ambato`
- `Formato Nº: UTA-SGC-A-2-1-P7-T1`
- número de página.

No dividir innecesariamente el primer texto en dos líneas.

No agregar doble línea superior si el formato original utiliza una.

## UT-SEC-023

Fuente: §23 — ÍNDICES. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

El T1 debe generar automáticamente:

- índice de contenido;
- índice de tablas;

cuando correspondan.

La numeración debe derivarse de las páginas reales.

## UT-SEC-024

Fuente: §24 — FIRMAS T1. Estado: HEREDADO. Producción: PARCIAL: ELABORADOR; FLUJO Y FIRMA PENDIENTES.

Sección:

`FIRMAS DE RESPONSABILIDAD`

Columnas:

- ACCIONES.
- NOMBRE.
- CARGO.
- FIRMA.

Posibles acciones:

- Elaborado por.
- Revisado por.
- Validado por.
- Aprobado por.

Debe derivarse del flujo real configurado.

## UT-SEC-025

Fuente: §25 — HISTORIAL. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

Título exacto:

`CONTROL DE HISTORIAL DE CAMBIOS`

Columnas:

- Versión.
- Descripción del Cambio.
- Fecha de Actualización.

Registro inicial recomendado:

Versión:

`v1.0`

Descripción:

`Elaboración del Plan de Trabajo`

Fecha:

fecha de elaboración.

## UT-SEC-026

Fuente: §26 — FIRMA Y FINALIZACIÓN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La última etapa del docente debe denominarse:

`Firma y Finalización`

No:

`Firma y Envío`

Al finalizar:

- se firma;
- el documento deja de ser editable;
- continúa automáticamente al siguiente nivel configurado.

## FR-SIGN-001

Fuente: §27 — FIRMA ELECTRÓNICA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Firmante usa archivo:

- `.p12`
- `.pfx`

y contraseña.

## FR-SIGN-002

Fuente: §27 — FIRMA ELECTRÓNICA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Certificado y contraseña son temporales.

## FR-SIGN-003

Fuente: §27 — FIRMA ELECTRÓNICA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

No almacenarlos permanentemente.

## FR-SIGN-004

Fuente: §27 — FIRMA ELECTRÓNICA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La firma debe aplicarse al artefacto exacto.

## FR-SIGN-005

Fuente: §27 — FIRMA ELECTRÓNICA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La metadata de ubicación debe formar parte del artefacto.

## FR-REV-001

Fuente: §28 — REVISIÓN DOCUMENTAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Las etapas son secuenciales.

Una etapa futura no debe acceder al documento antes de terminar la anterior, salvo que el flujo configurado defina revisores paralelos dentro de la misma etapa.

## FR-REV-002

Fuente: §28 — REVISIÓN DOCUMENTAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Si existen varios revisores obligatorios en la misma etapa, pueden trabajar en paralelo.

La siguiente etapa solo avanza cuando todos hayan aprobado.

## FR-REV-003

Fuente: §28 — REVISIÓN DOCUMENTAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El revisor visualiza el artefacto firmado exacto.

## UT-SEC-029

Fuente: §29 — OBSERVACIONES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El revisor puede:

- crear;
- editar las propias activas;
- eliminar las propias activas;
- asociar observación a página/sección;
- registrar observación general.

Después de decidir:

- las observaciones se congelan como historial.

## UT-SEC-030

Fuente: §30 — RESALTADO. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Debe existir función sencilla de resaltado sobre el documento.

El revisor podrá:

1. seleccionar/resaltar una zona;
2. registrar observación relacionada.

No se requiere editor PDF complejo.

## UT-SEC-031

Fuente: §31 — DEVOLUCIÓN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Cuando un documento se devuelve:

- cambia a DEVUELTO / EN CORRECCIÓN;
- docente puede corregir;
- aprobaciones anteriores quedan como historial;
- no son válidas para el artefacto corregido;
- al reenviar comienza nueva ronda.

No crear automáticamente versión formal 2.0.

## UT-SEC-032

Fuente: §32 — VERSIÓN FORMAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Formato:

- 1.0
- 2.0
- 3.0

Una nueva versión formal solo ocurre por decisión institucional.

No utilizar:

- 1.1;
- 1.2;

hasta confirmación institucional.

## FR-FLOW-001

Fuente: §33 — FLUJOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El flujo depende del grupo.

No existe un único flujo para todos.

## FR-FLOW-002

Fuente: §33 — FLUJOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El administrador debe poder configurar etapas.

## FR-FLOW-003

Fuente: §33 — FLUJOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Una etapa puede corresponder a:

- persona;
- rol;
- coordinación;
- órgano colegiado.

## FR-FLOW-004

Fuente: §33 — FLUJOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Algunos órganos colegiados pueden no requerir:

- nombre individual;
- firma personal.

## FR-FLOW-005

Fuente: §33 — FLUJOS INSTITUCIONALES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

No asumir que Consejo Directivo, Consejo Académico u otro órgano siguen el mismo proceso.

## UT-SEC-034

Fuente: §34 — PROCEDIMIENTO EXTERNO / QIPOC. Estado: PENDIENTE DE VALIDACIÓN INSTITUCIONAL. Producción: NO IMPLEMENTADO.

PENDIENTE DE VALIDACIÓN INSTITUCIONAL.

En reunión se discutió la posibilidad de que parte del proceso continúe mediante procedimiento externo.

No implementar todavía como norma definitiva:

- quién descarga;
- cuándo descarga;
- quién sube QIPOC;
- cuándo se considera aprobado.

Mantener esta integración configurable/provisional.

## UT-SEC-035

Fuente: §35 — INFORME T2. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Formato:

`UTA-SGC-A-2-1-P7-T2`

## UT-SEC-036

Fuente: §36 — CREACIÓN DEL INFORME. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El docente puede crear un Informe:

- derivado de un Plan;
- independiente, únicamente si el requisito sigue vigente según configuración institucional.

Para el flujo principal validado durante reunión, el Informe relacionado con planificación debe derivar automáticamente del Plan.

## UT-SEC-037

Fuente: §37 — INFORME DERIVADO. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Debe importar:

- actividades;
- medios de verificación;
- datos institucionales;
- Plan relacionado.

La correspondencia Plan → Informe debe mantenerse.

## UT-SEC-038

Fuente: §38 — SECCIONES T2. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El Informe incluye:

1. Antecedentes.
2. Desarrollo de actividades.
3. Conclusiones.
4. Oportunidades de mejora.
5. Registro de contactos y gestiones, cuando aplique.
6. Anexos.
7. Firmas.
8. Historial.

## UT-SEC-039

Fuente: §39 — DESARROLLO DE ACTIVIDADES DEL INFORME. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Las actividades del Plan se cargan automáticamente.

El docente registra:

- porcentaje de ejecución;
- observaciones.

No debe tener que reescribir manualmente las actividades.

## UT-SEC-040

Fuente: §40 — NOTAS INSTRUCTIVAS T2. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Para el documento formal generado no deben mostrarse notas de plantilla que sean únicamente instrucciones de edición.

Especialmente las notas que expliquen cuándo usar la tabla deben omitirse cuando el sistema ya conoce el origen del Informe.

## UT-SEC-041

Fuente: §41 — PAGECOUNT. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

T1 y T2 deben tener cantidad de páginas dinámica.

Nunca fijar 5, 3 u otro número.

La cantidad depende de:

```ts
artifact.pages.length
````

## UT-SEC-042

Fuente: §42 — EJECUCIÓN DE ACTIVIDADES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Una vez validado un Plan, sus actividades pasan a ejecución.

Vista:

`Mis Actividades`

Debe permitir:

* filtrar;
* consultar;
* abrir detalle;
* revisar fechas;
* revisar responsables;
* revisar recursos;
* revisar medios.

## UT-SEC-043

Fuente: §43 — AUTORIZACIÓN DE ACTIVIDADES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Solo responsables de la actividad pueden:

* cargar;
* reemplazar;
* modificar evidencia.

Usuarios no responsables:

* solo lectura.

## UT-SEC-044-INTRO

Fuente: §44 — EVIDENCIAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Por cada medio seleccionado:

`exactamente 1 PDF`

## FR-EVI-001

Fuente: §44 — EVIDENCIAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

PDF únicamente.

## FR-EVI-002

Fuente: §44 — EVIDENCIAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Tamaño configurable.

Demo actual:

10 MB.

## FR-EVI-003

Fuente: §44 — EVIDENCIAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Antes del límite:

puede reemplazarse.

## FR-EVI-004

Fuente: §44 — EVIDENCIAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El reemplazo crea nueva versión de evidencia.

## FR-EVI-005

Fuente: §44 — EVIDENCIAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

No modifica versión formal del Plan.

## UT-SEC-045

Fuente: §45 — FECHA LÍMITE DE EVIDENCIA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Por defecto relacionada con la fecha Hasta de actividad.

Puede reemplazarse hasta:

`23:59`

del día límite.

No implementar extensión extraordinaria automática.

## UT-SEC-046

Fuente: §46 — ESTADOS DE EVIDENCIA. Estado: PENDIENTE DE VALIDACIÓN INSTITUCIONAL. Producción: NO IMPLEMENTADO.

* PENDIENTE DE VALIDACIÓN.
* VALIDADA.
* OBSERVADA.

Diferenciar:

`evidencia completa`

de:

`evidencia validada`

## UT-SEC-047

Fuente: §47 — VALIDACIÓN DE EVIDENCIA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Un revisor autorizado puede:

* visualizar PDF;
* observar;
* validar.

No requiere firma electrónica salvo decisión institucional futura.

## UT-SEC-048

Fuente: §48 — REEMPLAZO DE EVIDENCIA. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Si una evidencia observada o validada se reemplaza dentro del plazo:

* crea nueva versión;
* requiere nueva validación.

## UT-SEC-049

Fuente: §49 — ADMINISTRACIÓN. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Panel administrativo debe incluir:

* usuarios;
* importación;
* grupos;
* períodos;
* catálogo de actividades;
* recursos;
* medios;
* flujos;
* feriados;
* plantillas.

## UT-SEC-050

Fuente: §50 — USUARIOS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

No asumir cédula como campo obligatorio si no fue validado.

Búsqueda principal:

* nombre;
* correo.

Importación:

* CSV/Excel u otro formato configurable.

## UT-SEC-051

Fuente: §51 — NOTIFICACIONES. Estado: HEREDADO. Producción: PARCIAL.

Alcance productivo: Bandeja interna privada, lectura y navegación; avisos de pertenencias y previsualización. Revisión, evidencias, vencimientos y canales externos pendientes.

Notificaciones por usuario.

Ejemplos:

* documento asignado;
* documento observado;
* documento devuelto;
* evidencia observada;
* vencimiento próximo.

Cada notificación debe navegar al objeto correcto.

## UT-SEC-052

Fuente: §52 — AUDITORÍA. Estado: HEREDADO. Producción: PARCIAL.

Alcance productivo: Auditoría existente, visor e historial con permisos y exportación CSV; acciones de firma/revisión/evidencias y retención institucional pendientes.

Registrar eventos relevantes:

* creación;
* firma;
* envío;
* observación;
* devolución;
* corrección;
* aprobación;
* evidencia;
* cambio administrativo.

No mostrar IDs técnicos innecesarios al usuario.

## UT-SEC-053

Fuente: §53 — REPORTES. Estado: HEREDADO. Producción: PARCIAL.

Alcance productivo: Seguimiento personal de borradores y exportación administrativa inicial; reportes institucionales completos pendientes.

Los reportes tienen carácter:

* documental;
* administrativo;
* de estado.

No implementar:

* ranking de docentes;
* scoring;
* sanciones;
* predicciones;
* productividad personal.

## UT-SEC-054

Fuente: §54 — HISTÓRICO. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Períodos cerrados:

* solo lectura.

Debe conservar:

* Planes;
* versiones;
* actividades;
* evidencias;
* firmas;
* trazabilidad.

## UT-SEC-055

Fuente: §55 — CIERRE DE PERÍODO. Estado: PENDIENTE DE VALIDACIÓN INSTITUCIONAL. Producción: NO IMPLEMENTADO.

La lógica institucional definitiva aún no está confirmada.

Para DEMO puede existir simulación.

No afirmar procedimiento normativo no confirmado.

## UT-SEC-056

Fuente: §56 — ESTADOS VACÍOS Y ERRORES. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Toda pantalla debe considerar:

* cargando;
* vacío;
* error;
* permiso insuficiente;
* acción exitosa.

Evitar pantallas rotas o tablas vacías sin explicación.

## UT-SEC-057

Fuente: §57 — ACCIONES EN TABLAS. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Priorizar iconos.

Cada icono debe incluir:

* `title`;
* `aria-label`.

Evitar columnas llenas de botones textuales.

## UT-SEC-058

Fuente: §58 — IDENTIDAD VISUAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Principal:

* azul institucional.

Semánticos:

* verde éxito;
* amarillo advertencia;
* rojo error.

Evitar morado como color dominante.

## UT-SEC-059

Fuente: §59 — RESPONSIVIDAD. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Prioridad:

desktop institucional.

Debe adaptarse razonablemente a resoluciones medianas.

No sacrificar legibilidad del documento A4.

## UT-SEC-060

Fuente: §60 — REGLA FINAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Toda implementación debe preservar:

* trazabilidad;
* aislamiento entre documentos;
* fidelidad del formato;
* separación de conceptos;
* configurabilidad institucional;
* integridad del artefacto firmado.

````

## FR-DOC-A4-001

Fuente: §61 — DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Todos los documentos institucionales T1 y T2 se generan en tamaño A4. Las páginas verticales usan 210 × 297 mm y las horizontales 297 × 210 mm.

## FR-TPL-001

Fuente: §61 — DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

El administrador puede reordenar todas las secciones de contenido de T1 y T2. Una sección requerida no puede desactivarse, pero sí moverse. Encabezado y pie son el marco fijo de página.

## FR-TPL-002

Fuente: §61 — DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

Cada documento nuevo conserva un snapshot con plantilla, versión, orden y visibilidad. El renderer, el índice y la numeración siguen ese orden. Los cambios posteriores de Administración no modifican documentos existentes ni artefactos firmados.

## FR-ARCH-AI-001

Fuente: §61 — DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL. Estado: HEREDADO. Producción: NO IMPLEMENTADO.

La integración futura de asistencia de redacción usa una abstracción de proveedor en backend: GroqCloud para desarrollo y OpenAI API para producción. El mockup mantiene un proveedor simulado y no expone claves en React.

## FR-ARCH-DB-001

Fuente: §61 — DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL. Estado: HEREDADO. Producción: IMPLEMENTADO EN ALCANCE INICIAL.

La base de datos productiva objetivo es PostgreSQL local o institucional on-premise, gratuita y relacional. El mockup no incorpora todavía backend ni sustituye `localStorage`.
