# ALCANCE ACTUAL

Los requerimientos descritos en este documento representan el comportamiento esperado del sistema final, pero la implementación actual corresponde únicamente a un:

**Mockup interactivo de alta fidelidad.**

Durante esta fase los requerimientos deben representarse mediante pantallas, interacciones, estados y datos DEMO suficientemente realistas para permitir su validación institucional.

No todos los requerimientos necesitan una implementación productiva real en esta etapa.

Cuando una funcionalidad pueda ser simulada sin afectar la validación del flujo, debe preferirse la simulación.

# SISTEMA DE GESTIÓN DOCUMENTAL ACADÉMICA — FISEI / UTA

## Estado del documento

Fuente funcional consolidada del proyecto.

Este documento incorpora:

- requerimientos levantados;
- reuniones con personal institucional;
- formatos T1 y T2;
- decisiones posteriores;
- correcciones realizadas durante pruebas del prototipo.

---

# 1. OBJETIVO GENERAL

Desarrollar una aplicación web para facilitar la creación, revisión, aprobación, firma, seguimiento, ejecución y consulta histórica de documentación académica de grupos institucionales de la FISEI.

El sistema debe reducir:

- edición manual repetitiva;
- circulación de documentos por correo;
- errores de formato;
- pérdida de trazabilidad;
- duplicación de información;
- inconsistencias entre planificación e informe.

---

# 2. ACTORES

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

---

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

---

## 2.3 Coordinador

Según el flujo institucional puede:

- revisar;
- coordinar;
- validar etapas;
- gestionar grupos;
- gestionar miembros;
- participar en flujos;
- verificar documentación externa cuando corresponda.

---

## 2.4 Autoridad / validador

Puede intervenir en:

- validación final;
- aprobación;
- firma;

según el flujo configurado.

No asumir una autoridad universal.

---

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

---

# 3. AUTENTICACIÓN

## FR-AUTH-001

El sistema debe permitir autenticación mediante credenciales.

## FR-AUTH-002

Usuarios importados pueden recibir contraseña temporal.

## FR-AUTH-003

En el primer ingreso con contraseña temporal debe solicitar cambio obligatorio.

## FR-AUTH-004

Debe existir recuperación de contraseña.

## FR-AUTH-005

El usuario autenticado debe conservar identidad única durante la sesión.

Cambiar contexto o rol no cambia la persona autenticada.

---

# 4. GRUPOS INSTITUCIONALES

## FR-GRP-001

El sistema debe manejar una entidad configurable:

`Grupo institucional`

## FR-GRP-002

Tipos posibles:

- Comisión.
- Unidad.
- Club.
- Otro.

## FR-GRP-003

Un docente puede pertenecer a varios grupos.

## FR-GRP-004

Puede existir un Plan diferente para cada combinación:

`docente + grupo + período`

## FR-GRP-005

No permitir duplicado de esa combinación cuando corresponda.

## FR-GRP-006

Los roles de pertenencia pueden incluir:

- Miembro.
- Coordinador.
- Otro.

No utilizar “Responsable” como rol permanente del grupo porque ese concepto pertenece a las actividades.

---

# 5. PERÍODOS

## FR-PER-001

El administrador define períodos académicos.

Ejemplo actual:

`Julio – Diciembre 2026`

## FR-PER-002

El período debe disponer de:

- fecha inicio;
- fecha fin;
- ventana de elaboración;
- ventana de revisión.

## FR-PER-003

Estas fechas deben ser configurables.

---

# 6. PLAN DE TRABAJO T1

Formato:

`UTA-SGC-A-2-1-P7-T1`

---

# 7. SECUENCIA DEL WIZARD T1

El orden funcional será:

1. Información general.
2. Contenido.
3. Actividades.
4. Matriz de actividades.
5. Anexos.
6. Previsualización.
7. Firma y Finalización.

No alterar este orden sin nueva validación institucional.

---

# 8. INFORMACIÓN GENERAL DEL PLAN

## FR-T1-001

Debe obtener automáticamente:

- docente elaborador;
- período;
- grupo;
- información institucional conocida.

## FR-T1-002

La unidad/facultad debe poder derivarse de información institucional.

## FR-T1-003

La carrera debe aparecer correctamente vinculada.

## FR-T1-004

La fecha de elaboración debe registrarse.

## FR-T1-005

Al finalizar el documento, la fecha de elaboración debe quedar congelada.

No debe cambiar posteriormente porque otro usuario revise o firme.

---

# 9. CONTENIDO DEL PLAN

## FR-T1-CONT-001

Debe existir exactamente una Justificación.

## FR-T1-CONT-002

Debe existir exactamente un Objetivo.

## FR-T1-CONT-003

Puede existir texto base precargado editable.

El docente puede:

- editar;
- reemplazar;
- eliminar.

## FR-T1-CONT-004

Puede existir asistente IA de redacción.

La IA puede sugerir:

- mejora de redacción;
- gramática;
- claridad;
- tono formal.

## FR-T1-CONT-005

La IA nunca reemplaza automáticamente el contenido.

Debe mostrar:

- original;
- sugerencia.

El usuario decide:

- aplicar;
- descartar.

---

# 10. ACTIVIDADES

## FR-ACT-001

El administrador puede configurar catálogo de actividades.

## FR-ACT-002

Las actividades pueden clasificarse como:

- POA.
- Plan de Mejoras.
- Acción de Mejora.
- Otra.

## FR-ACT-003

Puede haber actividades obligatorias por grupo.

## FR-ACT-004

Las obligatorias:

- aparecen seleccionadas;
- no pueden eliminarse.

## FR-ACT-005

Puede haber actividades opcionales.

## FR-ACT-006

El docente puede crear libremente una actividad `Otra`.

No debe obligarse a clasificar toda actividad como POA/Plan/Acción.

---

# 11. MATRIZ DE ACTIVIDADES

Cada actividad debe poder definir:

- fecha Desde;
- fecha Hasta;
- responsables;
- recursos;
- medios de verificación.

---

# 12. FECHAS

## FR-DATE-001

Usar fecha completa:

`día / mes / año`

## FR-DATE-002

Las fechas deben estar dentro del período cuando corresponda.

## FR-DATE-003

Una actividad puede atravesar un feriado.

## FR-DATE-004

Una actividad no debe iniciar o finalizar en un feriado cuando esa restricción esté configurada.

---

# 13. RESPONSABLES

## FR-RESP-001

Debe seleccionarse mínimo un responsable.

## FR-RESP-002

Puede seleccionarse más de uno.

## FR-RESP-003

Debe existir:

`Seleccionar todos`

## FR-RESP-004

La lista seleccionable debe corresponder a integrantes aplicables del grupo.

## FR-RESP-005

Cuando todos los miembros aplicables sean seleccionados, el documento puede representar el conjunto mediante denominación colectiva.

Ejemplo solicitado en reunión:

`Responsable de la comisión`

La denominación debe ser configurable según el tipo de grupo.

Internamente deben conservarse las identidades individuales para trazabilidad.

---

# 14. RECURSOS

## FR-REC-001

Los recursos se seleccionan de catálogo.

Ejemplos:

- Matriz de seguimiento.
- Almacenamiento institucional.
- Reglamento.
- Sistema institucional.
- Sala de reuniones.
- Equipos.

## FR-REC-002

Debe existir `Otro`.

## FR-REC-003

Al seleccionar `Otro` debe requerirse descripción.

## FR-REC-004

No permitir guardar `Otro` vacío.

## FR-REC-005

Los catálogos deben ser administrables.

---

# 15. MEDIOS DE VERIFICACIÓN

## FR-MED-001

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

Debe existir `Otro`.

## FR-MED-003

Otro requiere descripción obligatoria.

---

# 16. FUENTE Y ELABORACIÓN DE MATRIZ

## FR-MAT-001

La matriz debe permitir registrar:

`Fuente`

Puede ser:

- documento;
- grupo;
- comisión;
- otra fuente.

Debe ser editable cuando corresponda.

## FR-MAT-002

`Elaborado por` debe derivarse automáticamente del grupo/comisión responsable.

---

# 17. NOTA DE DATOS PERSONALES

La plantilla institucional contempla una nota de protección de datos personales.

Solo debe incluirse cuando el documento recopile datos personales que hagan aplicable dicha nota.

No decidir esto únicamente por el nombre del grupo.

La condición debe ser explícita/configurable.

---

# 18. ANEXOS

## FR-ANX-001

El docente selecciona:

- Sí.
- No.

## FR-ANX-002

Si selecciona Sí:

- puede crear anexos;
- editar;
- eliminar;
- ordenar.

## FR-ANX-003

La numeración será automática:

- Anexo A.
- Anexo B.
- Anexo C.

## FR-ANX-004

Eliminar un anexo reordena la secuencia.

## FR-ANX-005

Anexos del documento no son evidencias de actividades.

---

# 19. PREVISUALIZACIÓN T1

El documento debe representar fielmente el formato oficial.

---

# 20. ENCABEZADO T1

Debe contener:

- logo / Sistema de Gestión de la Calidad;
- Universidad Técnica de Ambato;
- Plan de Trabajo;
- información institucional;
- fecha;
- carrera cuando corresponda.

La estructura exacta debe seguir el formato institucional de referencia.

---

# 21. PORTADA T1

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

---

# 22. PIE DE PÁGINA T1

Debe mostrarse en una única fila visual.

Debe contener:

- `Documento de uso interno controlado por la Universidad Técnica de Ambato`
- `Formato Nº: UTA-SGC-A-2-1-P7-T1`
- número de página.

No dividir innecesariamente el primer texto en dos líneas.

No agregar doble línea superior si el formato original utiliza una.

---

# 23. ÍNDICES

El T1 debe generar automáticamente:

- índice de contenido;
- índice de tablas;

cuando correspondan.

La numeración debe derivarse de las páginas reales.

---

# 24. FIRMAS T1

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

---

# 25. HISTORIAL

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

---

# 26. FIRMA Y FINALIZACIÓN

La última etapa del docente debe denominarse:

`Firma y Finalización`

No:

`Firma y Envío`

Al finalizar:

- se firma;
- el documento deja de ser editable;
- continúa automáticamente al siguiente nivel configurado.

---

# 27. FIRMA ELECTRÓNICA

## FR-SIGN-001

Firmante usa archivo:

- `.p12`
- `.pfx`

y contraseña.

## FR-SIGN-002

Certificado y contraseña son temporales.

## FR-SIGN-003

No almacenarlos permanentemente.

## FR-SIGN-004

La firma debe aplicarse al artefacto exacto.

## FR-SIGN-005

La metadata de ubicación debe formar parte del artefacto.

---

# 28. REVISIÓN DOCUMENTAL

## FR-REV-001

Las etapas son secuenciales.

Una etapa futura no debe acceder al documento antes de terminar la anterior, salvo que el flujo configurado defina revisores paralelos dentro de la misma etapa.

## FR-REV-002

Si existen varios revisores obligatorios en la misma etapa, pueden trabajar en paralelo.

La siguiente etapa solo avanza cuando todos hayan aprobado.

## FR-REV-003

El revisor visualiza el artefacto firmado exacto.

---

# 29. OBSERVACIONES

El revisor puede:

- crear;
- editar las propias activas;
- eliminar las propias activas;
- asociar observación a página/sección;
- registrar observación general.

Después de decidir:

- las observaciones se congelan como historial.

---

# 30. RESALTADO

Debe existir función sencilla de resaltado sobre el documento.

El revisor podrá:

1. seleccionar/resaltar una zona;
2. registrar observación relacionada.

No se requiere editor PDF complejo.

---

# 31. DEVOLUCIÓN

Cuando un documento se devuelve:

- cambia a DEVUELTO / EN CORRECCIÓN;
- docente puede corregir;
- aprobaciones anteriores quedan como historial;
- no son válidas para el artefacto corregido;
- al reenviar comienza nueva ronda.

No crear automáticamente versión formal 2.0.

---

# 32. VERSIÓN FORMAL

Formato:

- 1.0
- 2.0
- 3.0

Una nueva versión formal solo ocurre por decisión institucional.

No utilizar:

- 1.1;
- 1.2;

hasta confirmación institucional.

---

# 33. FLUJOS INSTITUCIONALES

## FR-FLOW-001

El flujo depende del grupo.

No existe un único flujo para todos.

## FR-FLOW-002

El administrador debe poder configurar etapas.

## FR-FLOW-003

Una etapa puede corresponder a:

- persona;
- rol;
- coordinación;
- órgano colegiado.

## FR-FLOW-004

Algunos órganos colegiados pueden no requerir:

- nombre individual;
- firma personal.

## FR-FLOW-005

No asumir que Consejo Directivo, Consejo Académico u otro órgano siguen el mismo proceso.

---

# 34. PROCEDIMIENTO EXTERNO / QIPOC

PENDIENTE DE VALIDACIÓN INSTITUCIONAL.

En reunión se discutió la posibilidad de que parte del proceso continúe mediante procedimiento externo.

No implementar todavía como norma definitiva:

- quién descarga;
- cuándo descarga;
- quién sube QIPOC;
- cuándo se considera aprobado.

Mantener esta integración configurable/provisional.

---

# 35. INFORME T2

Formato:

`UTA-SGC-A-2-1-P7-T2`

---

# 36. CREACIÓN DEL INFORME

El docente puede crear un Informe:

- derivado de un Plan;
- independiente, únicamente si el requisito sigue vigente según configuración institucional.

Para el flujo principal validado durante reunión, el Informe relacionado con planificación debe derivar automáticamente del Plan.

---

# 37. INFORME DERIVADO

Debe importar:

- actividades;
- medios de verificación;
- datos institucionales;
- Plan relacionado.

La correspondencia Plan → Informe debe mantenerse.

---

# 38. SECCIONES T2

El Informe incluye:

1. Antecedentes.
2. Desarrollo de actividades.
3. Conclusiones.
4. Oportunidades de mejora.
5. Registro de contactos y gestiones, cuando aplique.
6. Anexos.
7. Firmas.
8. Historial.

---

# 39. DESARROLLO DE ACTIVIDADES DEL INFORME

Las actividades del Plan se cargan automáticamente.

El docente registra:

- porcentaje de ejecución;
- observaciones.

No debe tener que reescribir manualmente las actividades.

---

# 40. NOTAS INSTRUCTIVAS T2

Para el documento formal generado no deben mostrarse notas de plantilla que sean únicamente instrucciones de edición.

Especialmente las notas que expliquen cuándo usar la tabla deben omitirse cuando el sistema ya conoce el origen del Informe.

---

# 41. PAGECOUNT

T1 y T2 deben tener cantidad de páginas dinámica.

Nunca fijar 5, 3 u otro número.

La cantidad depende de:

```ts
artifact.pages.length
````

---

# 42. EJECUCIÓN DE ACTIVIDADES

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

---

# 43. AUTORIZACIÓN DE ACTIVIDADES

Solo responsables de la actividad pueden:

* cargar;
* reemplazar;
* modificar evidencia.

Usuarios no responsables:

* solo lectura.

---

# 44. EVIDENCIAS

Por cada medio seleccionado:

`exactamente 1 PDF`

## FR-EVI-001

PDF únicamente.

## FR-EVI-002

Tamaño configurable.

Demo actual:

10 MB.

## FR-EVI-003

Antes del límite:

puede reemplazarse.

## FR-EVI-004

El reemplazo crea nueva versión de evidencia.

## FR-EVI-005

No modifica versión formal del Plan.

---

# 45. FECHA LÍMITE DE EVIDENCIA

Por defecto relacionada con la fecha Hasta de actividad.

Puede reemplazarse hasta:

`23:59`

del día límite.

No implementar extensión extraordinaria automática.

---

# 46. ESTADOS DE EVIDENCIA

* PENDIENTE DE VALIDACIÓN.
* VALIDADA.
* OBSERVADA.

Diferenciar:

`evidencia completa`

de:

`evidencia validada`

---

# 47. VALIDACIÓN DE EVIDENCIA

Un revisor autorizado puede:

* visualizar PDF;
* observar;
* validar.

No requiere firma electrónica salvo decisión institucional futura.

---

# 48. REEMPLAZO DE EVIDENCIA

Si una evidencia observada o validada se reemplaza dentro del plazo:

* crea nueva versión;
* requiere nueva validación.

---

# 49. ADMINISTRACIÓN

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

---

# 50. USUARIOS

No asumir cédula como campo obligatorio si no fue validado.

Búsqueda principal:

* nombre;
* correo.

Importación:

* CSV/Excel u otro formato configurable.

---

# 51. NOTIFICACIONES

Notificaciones por usuario.

Ejemplos:

* documento asignado;
* documento observado;
* documento devuelto;
* evidencia observada;
* vencimiento próximo.

Cada notificación debe navegar al objeto correcto.

---

# 52. AUDITORÍA

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

---

# 53. REPORTES

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

---

# 54. HISTÓRICO

Períodos cerrados:

* solo lectura.

Debe conservar:

* Planes;
* versiones;
* actividades;
* evidencias;
* firmas;
* trazabilidad.

---

# 55. CIERRE DE PERÍODO

La lógica institucional definitiva aún no está confirmada.

Para DEMO puede existir simulación.

No afirmar procedimiento normativo no confirmado.

---

# 56. ESTADOS VACÍOS Y ERRORES

Toda pantalla debe considerar:

* cargando;
* vacío;
* error;
* permiso insuficiente;
* acción exitosa.

Evitar pantallas rotas o tablas vacías sin explicación.

---

# 57. ACCIONES EN TABLAS

Priorizar iconos.

Cada icono debe incluir:

* `title`;
* `aria-label`.

Evitar columnas llenas de botones textuales.

---

# 58. IDENTIDAD VISUAL

Principal:

* azul institucional.

Semánticos:

* verde éxito;
* amarillo advertencia;
* rojo error.

Evitar morado como color dominante.

---

# 59. RESPONSIVIDAD

Prioridad:

desktop institucional.

Debe adaptarse razonablemente a resoluciones medianas.

No sacrificar legibilidad del documento A4.

---

# 60. REGLA FINAL

Toda implementación debe preservar:

* trazabilidad;
* aislamiento entre documentos;
* fidelidad del formato;
* separación de conceptos;
* configurabilidad institucional;
* integridad del artefacto firmado.

````

---

# 61. DECISIONES CONFIRMADAS DE LA MICRO-PASADA FINAL

## FR-DOC-A4-001

Todos los documentos institucionales T1 y T2 se generan en tamaño A4. Las páginas verticales usan 210 × 297 mm y las horizontales 297 × 210 mm.

## FR-TPL-001

El administrador puede reordenar todas las secciones de contenido de T1 y T2. Una sección requerida no puede desactivarse, pero sí moverse. Encabezado y pie son el marco fijo de página.

## FR-TPL-002

Cada documento nuevo conserva un snapshot con plantilla, versión, orden y visibilidad. El renderer, el índice y la numeración siguen ese orden. Los cambios posteriores de Administración no modifican documentos existentes ni artefactos firmados.

## FR-ARCH-AI-001

La integración futura de asistencia de redacción usa una abstracción de proveedor en backend: GroqCloud para desarrollo y OpenAI API para producción. El mockup mantiene un proveedor simulado y no expone claves en React.

## FR-ARCH-DB-001

La base de datos productiva objetivo es PostgreSQL local o institucional on-premise, gratuita y relacional. El mockup no incorpora todavía backend ni sustituye `localStorage`.
