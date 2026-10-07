# PLAN MAESTRO PARA EL AGENTE DE DESARROLLO
## Gestión Documental Académica — FISEI / Universidad Técnica de Ambato

**Versión consolidada:** 06-10-2026  
**Estado:** fuente maestra para continuar el software completo  
**Nombre canónico en la interfaz:** **Gestión Documental Académica**  
**Nombre histórico/interno encontrado en documentos:** UTAPED

---

# 1. Propósito y regla de precedencia

Este archivo consolida las decisiones funcionales y técnicas acumuladas durante el levantamiento de requisitos, las reuniones anteriores con el cliente, las revisiones de mockups, las correcciones posteriores y la reunión del 06-10-2026.

El agente debe leer este archivo antes de modificar el sistema.

Si aparece una contradicción entre documentos antiguos, aplicar este orden:

1. decisión explícita más reciente del cliente;
2. feedback posterior de mockup aceptado por el cliente;
3. decisiones explícitas de reuniones anteriores;
4. decisiones técnicas del proyecto;
5. datos o comportamientos DEMO.

Nunca convertir un dato DEMO en una regla institucional.

Cuando una regla siga realmente abierta, marcarla `PENDIENTE_REAL` y dejar la arquitectura preparada/configurable en vez de inventarla.

---

# 2. Visión general

El sistema es una aplicación web institucional para gestionar de extremo a extremo:

- Planes de Trabajo;
- actividades;
- responsables;
- recursos;
- medios de verificación;
- anexos;
- generación de documentos institucionales;
- firma electrónica;
- revisión;
- observaciones;
- devoluciones;
- aprobación;
- ejecución;
- evidencias;
- prórrogas;
- informes finales;
- versionamiento;
- administración;
- notificaciones;
- reportes;
- auditoría;
- ayuda de redacción mediante IA.

La trazabilidad debe permitir reconstruir quién hizo cada acción, cuándo ocurrió, qué documento estaba vigente, qué flujo se aplicó, qué versión se aprobó y qué evidencias se cargaron.

---

# 3. Decisiones recientes que reemplazan modelos anteriores

## 3.1 El Plan pertenece a la comisión/unidad

El Plan no pertenece permanentemente al docente que lo creó.

Si el responsable deja la comisión:

- el documento continúa perteneciendo a la comisión;
- otro responsable autorizado puede continuar su gestión;
- no se pierde historial.

Por tanto, `teacher_id` no debe representar la propiedad permanente del Plan.

La propiedad organizacional debe basarse en:

```text
group_id / commission_id
period_id
career_id cuando aplique
```

---

## 3.2 Un solo Plan por comisión, carrera y período

La reunión más reciente estableció que existe un solo Plan para una comisión en un período.

Una persona puede pertenecer a varias comisiones.

Además, una misma persona/comisión puede operar para más de una carrera; en ese caso deben existir planificaciones separadas por carrera y con flujo independiente.

Regla de unicidad recomendada:

```text
period_id + group_id + career_id + document_type
```

Para Plan de Trabajo solo puede existir una planificación vigente por esa combinación.

Los borradores archivados no deben bloquear permanentemente una nueva planificación válida.

---

## 3.3 Carrera automática o seleccionable

Si el usuario tiene una sola carrera:

```text
carrera = automática
```

Si pertenece a varias:

```text
mostrar combobox únicamente con sus carreras autorizadas
```

Facultad y unidad se resuelven automáticamente.

---

## 3.4 Workflow secuencial

El modelo anterior permitía imaginar revisores paralelos.

La decisión actual es:

```text
workflow completamente secuencial
```

Solo existe una tarea de revisión activa por vez.

La siguiente persona no puede revisar hasta que la anterior apruebe y firme.

---

## 3.5 Solo tres estados formales

Los estados formales del documento son:

```text
DRAFT
IN_REVIEW
APPROVED
```

Cuando se devuelve:

```text
IN_REVIEW -> DRAFT
```

No crear un cuarto estado formal `EN_CORRECCION`.

La UI puede mostrar que el borrador proviene de una devolución, pero el estado sigue siendo `DRAFT`.

---

## 3.6 Informe Final 1:1

Para el Informe Final de la planificación:

```text
1 Plan -> 1 Informe Final
```

No existen informes finales parciales de la misma planificación.

Si más adelante existen informes por actividad, deben modelarse como tipos documentales distintos.

---

## 3.7 Evidencias sin workflow independiente de validación

El requisito actual no necesita una cadena independiente de aprobación de evidencias.

Una actividad se considera completada cuando están cargadas todas las evidencias requeridas por sus medios de verificación.

Sí debe existir:

- carga;
- reemplazo/versionado;
- control de plazo;
- preview;
- integridad;
- auditoría.

---

## 3.8 Dispositivos

Objetivo oficial:

```text
Desktop: obligatorio
Tablet: soportado
Teléfono móvil: no es objetivo funcional
```

Mantener CSS responsive para no romperse, pero no diseñar el editor complejo para pantallas de teléfono.

---

## 3.9 Workflow fijo durante el semestre

El flujo se define para el período.

Una vez operativo el semestre:

```text
no modificar el workflow estructural del período activo
```

Los cambios se preparan para el siguiente período.

---

# 4. Arquitectura técnica

## 4.1 Estilo

Usar **monolito modular**.

No usar microservicios.

## 4.2 Stack

### Frontend

- React;
- TypeScript;
- Vite.

Mantener el frontend existente y evolucionarlo. No rehacer todo desde cero.

### Backend

- Java 21;
- Spring Boot;
- Spring Security;
- Spring Data JPA;
- Hibernate;
- Bean Validation;
- Flyway.

### Base de datos

- PostgreSQL.

### API

- REST;
- `/api/v1/...`;
- OpenAPI/Swagger.

### Pruebas

- JUnit 5;
- Mockito;
- Testcontainers;
- Playwright.

### PDF y firma

Recomendación open source:

- OpenHTMLtoPDF para composición institucional;
- Apache PDFBox para merge/manipulación;
- PDFBox + Bouncy Castle para firma.

---

# 5. Módulos backend

Organizar el backend por dominio:

```text
auth
users
organization
periods
catalogs
documents
plans
activities
templates
rendering
workflow
reviews
files
signatures
evidence
reports
notifications
audit
ai
admin
shared
```

Dentro de cada módulo separar razonablemente:

```text
domain
application
infrastructure
web
```

No crear una capa excesivamente ceremonial.

---

# 6. Autenticación y usuarios

## 6.1 No existe registro público

Solo existe Login.

No implementar botón de registro abierto.

## 6.2 Creación de usuarios

Debe existir:

- alta manual;
- importación masiva;
- asignación de carrera;
- asignación de comisión;
- asignación de cargo;
- activación/desactivación.

Permisos por defecto:

- Admin: global;
- Coordinador autorizado: limitado a su ámbito.

## 6.3 Datos mínimos

Guardar:

- nombres;
- apellidos;
- username;
- correo institucional;
- carrera(s);
- cargo(s);
- comisión(es);
- activo/inactivo.

No pedir por defecto:

- cédula;
- teléfono.

## 6.4 Username

El cliente indicó acceso mediante identificador derivado del correo institucional, normalmente sin `@dominio`.

Guardar por separado:

```text
username
institutional_email
```

## 6.5 Contraseñas

- Argon2id;
- jamás texto plano;
- contraseña temporal al crear/importar;
- `must_change_password = true`;
- cambio obligatorio en primer login.

## 6.6 Recuperación

Soportar dos vías:

### Por correo

- token aleatorio de un solo uso;
- hash del token;
- expiración;
- SMTP.

### Por reset administrativo

Si el correo no está disponible:

- Admin/Coordinador autorizado genera contraseña temporal;
- obliga cambio al próximo login;
- registra auditoría.

El cliente pidió mantener ambas alternativas.

## 6.7 CAPTCHA

El Login rediseñado usa CAPTCHA alfanumérico.

En backend real:

- challenge generado/validado por servidor;
- TTL;
- asociado a sesión;
- regeneración;
- error inline;
- no borrar correo/contraseña al regenerar.

El CAPTCHA no reemplaza rate limiting ni bloqueo de intentos.

---

# 7. Persona autenticada vs contexto

Separar:

```text
currentUser
activeContext
```

Una persona puede ser:

- responsable en una comisión;
- miembro en otra;
- revisor de un documento;
- coordinador en un ámbito.

Cambiar de contexto no cambia la identidad de la persona.

---

# 8. Organización institucional

Entidades base:

```text
faculty
career
organizational_group
group_membership
institutional_role
period_assignment
```

## 8.1 Grupo institucional

Tipos configurables:

- Comisión;
- Unidad;
- Club;
- Otro.

No asumir que comisión = departamento.

## 8.2 Membresía

Guardar:

- usuario;
- grupo;
- período;
- carrera si aplica;
- cargo/rol;
- principal;
- fecha inicio/fin;
- activo.

Roles posibles:

```text
MEMBER
COORDINATOR
RESPONSIBLE
OTHER
```

## 8.3 Responsable principal

Cada comisión/unidad tiene un principal para el período.

Esa persona:

- crea/edita la planificación;
- es el editor principal;
- puede registrar al resto de elaboradores/responsables.

Los demás miembros no editan el Plan directamente salvo reasignación.

---

# 9. Períodos

Admin gestiona períodos.

Campos:

```text
id
name
start_date
end_date
status
```

El sistema debe detectar el período actual automáticamente.

Ejemplo DEMO:

```text
Julio – Diciembre 2026
```

Al inicio del período se configuran:

- miembros;
- responsables;
- carreras;
- workflows;
- plantillas;
- feriados.

Una vez iniciado el período, su workflow queda bloqueado.

---

# 10. T1 — Plan de Trabajo

Mantener el wizard aprobado de 6 pasos:

```text
1. Información general
2. Contenido
3. Actividades
4. Anexos
5. Previsualización
6. Firma y Finalización
```

No volver a crear una pestaña independiente de Matriz.

## 10.1 Información general

Automático:

- período;
- Facultad;
- unidad;
- carrera cuando solo existe una.

Seleccionable:

- comisión/grupo;
- carrera únicamente cuando el usuario tiene varias autorizadas.

## 10.2 Contenido

La Justificación es obligatoria.

La ayuda IA puede mejorar texto existente, pero no generar una justificación desde un campo vacío.

---

# 11. Actividades y Matriz

## 11.1 Fuente única

Solo existe `activity`.

La Matriz del documento se genera a partir de las actividades.

No crear `matrix_row` editable como segunda fuente.

## 11.2 Tipos actuales

```text
POA
PLAN_MEJORAS
ACCION_MEJORA
OTRA
```

## 11.3 Obligatoriedad

Separar de `activity_type`.

```text
MANDATORY
OPTIONAL
```

La obligatoriedad depende de la comisión/configuración.

## 11.4 Datos

Cada actividad maneja:

- descripción;
- tipo;
- Desde;
- Hasta;
- responsables;
- recursos;
- medios de verificación;
- obligatoriedad;
- orden;
- estado de ejecución.

## 11.5 Responsables

Mínimo uno.

Se puede asignar:

- uno;
- varios;
- todos.

Si se seleccionan todos, conservar los IDs individuales internamente.

---

# 12. Fechas y feriados

Reglas:

```text
from_date <= to_date
from_date >= period.start_date
to_date <= period.end_date
```

Una actividad puede atravesar feriados.

No puede comenzar ni terminar en feriado.

Admin mantiene calendario de feriados.

Interno/API:

```text
YYYY-MM-DD
```

UI/documento:

```text
DD/MM/YYYY
```

Usar `LocalDate` para fechas civiles.

Zona temporal de negocio:

```text
America/Guayaquil
```

---

# 13. Recursos y medios de verificación

Deben ser catálogos administrables.

El cliente entregará los catálogos reales.

Mientras tanto usar seeds DEMO claramente identificados.

No hardcodear recursos inventados para llenar una demo.

---

# 14. Anexos del Plan

UX aprobada:

```text
¿Este Plan contiene anexos?
Sí / No
```

Si No:

- no mostrar uploader.

Si Sí:

- permitir varios archivos.

Los anexos del Plan pueden ser documentos habituales:

- PDF;
- DOCX;
- XLSX;
- PPTX;
- JPG;
- PNG;
- TXT.

Bloquear ejecutables/scripts peligrosos.

Guardar:

- orden;
- nombre;
- MIME;
- tamaño;
- SHA-256;
- storage key.

---

# 15. Borrador creado por error

No eliminar físicamente.

Archivar:

```text
archived = true
archive_reason = USER_DISCARDED
```

El borrador archivado:

- queda en historial;
- solo se muestra a usuarios administrativos autorizados;
- no debe bloquear una nueva planificación válida.

---

# 16. Edición

No hay coedición simultánea.

Regla:

```text
un único responsable/editor principal
```

Otros participantes pueden aparecer como:

- elaboradores;
- responsables de actividades;
- firmantes;

pero no modificar el Plan directamente.

---

# 17. Estados y ciclo de vida

Enum:

```text
DRAFT
IN_REVIEW
APPROVED
```

Transiciones:

```text
NEW -> DRAFT

DRAFT -> IN_REVIEW
al firmar correctamente

IN_REVIEW -> DRAFT
si se devuelve

IN_REVIEW -> APPROVED
cuando finaliza el workflow
```

Un documento APPROVED es inmutable.

No se puede:

- editar;
- revocar;
- rechazar retroactivamente;
- eliminar.

Para cambiarlo se crea una nueva versión formal.

---

# 18. Firma y envío

La firma y el envío no deben ser dos acciones independientes.

Decisión actual:

```text
firma correcta -> pasa directamente a revisión
```

Antes de firmar debe existir diálogo de confirmación.

Cancelar:

- no cambia estado;
- no firma;
- permite seguir editando.

---

# 19. Firma electrónica

Usar credencial PKCS#12:

```text
.p12 / .pfx
```

Solicitar:

- archivo;
- contraseña.

Nunca persistir:

- archivo privado;
- contraseña;
- clave privada.

Proceso:

```text
1. generar PDF canónico en backend;
2. cargar credencial temporalmente;
3. validar;
4. firmar PDF;
5. colocar apariencia en slot;
6. guardar PDF firmado;
7. guardar solo metadatos públicos;
8. descartar credencial;
9. avanzar workflow.
```

Metadatos permitidos:

- certificado público;
- fingerprint;
- subject;
- issuer;
- serial;
- vigencia.

---

# 20. Quién firma

Firman todas las personas que aparecen en la matriz de responsabilidades:

- elaborador(es);
- revisor(es);
- aprobador final.

Si alguien devuelve:

- coloca observación;
- no cuenta como aprobación.

---

# 21. PDF canónico

El PDF oficial lo genera el backend.

React solo previsualiza.

No confiar en un PDF arbitrario generado por navegador como fuente oficial.

T1 debe respetar el formato:

```text
UTA-SGC-A-2-1-P7-T1
```

Conservar:

- estructura;
- tamaño de letra;
- espaciado;
- tablas;
- encabezado;
- pie;
- matriz;
- firmas;
- historial.

A4.

Matriz puede ser horizontal.

---

# 22. Plantillas documentales

Admin puede configurar estructura permitida:

- orden;
- secciones;
- obligatoriedad;
- contenido opcional.

Toda modificación crea `template_version`.

Un documento conserva snapshot/version de plantilla.

No modificar documentos históricos al cambiar una plantilla.

---

# 23. Workflow institucional

Determinar workflow por:

```text
period
document_type
group
career
```

No hardcodear nombres de personas.

Ejemplo de Plan:

```text
Responsable de revisión / unidad correspondiente
        ↓
Coordinador
        ↓
Decano O Subdecano
```

El aprobador final depende de la comisión.

El cliente entregará una tabla oficial de:

- comisión;
- quién revisa;
- quién aprueba;
- cargo.

---

# 24. Cargos vs personas

El flujo debe configurarse por función/cargo.

La persona concreta cambia por período.

Ejemplo:

```text
workflow_step.role = COORDINATOR

period_assignment:
COORDINATOR -> User X
```

---

# 25. Flujo secuencial

Ejemplo:

```text
Step 1 ACTIVE
Step 2 LOCKED
Step 3 LOCKED
```

Al aprobar Step 1:

```text
Step 1 APPROVED
Step 2 ACTIVE
```

La siguiente autoridad no puede revisar antes de su turno.

---

# 26. Varios revisores obligatorios

Si existen varios revisores:

- todos deben aprobar;
- actúan uno detrás de otro;
- nunca en paralelo.

Ejemplo:

```text
Reviewer A
  ↓
Reviewer B
  ↓
Coordinator
  ↓
Final Approver
```

---

# 27. Misma persona en varias etapas

No permitido dentro del mismo documento.

Validar que una persona no sea:

```text
reviewer + final approver
```

en la misma planificación.

Sí puede cumplir otro rol en otra planificación.

---

# 28. Reasignación

Si un actor deja el cargo:

- Coordinador autorizado reasigna;
- guardar historial;
- no cambiar quién realizó acciones anteriores.

Las asignaciones normales deben definirse al inicio del semestre.

---

# 29. Workflow inmutable por período

Modelo recomendado:

```text
workflow_definition
workflow_version
workflow_step
period_workflow_assignment
workflow_instance
workflow_task
```

Al activar el semestre:

```text
locked = true
```

No aplicar una modificación estructural al mismo período activo.

---

# 30. Observaciones

Permitir:

- observación general;
- por sección;
- por página/elemento.

No limitar el número.

Guardar:

- autor;
- fecha;
- etapa;
- texto;
- referencia;
- resolución.

---

# 31. Devolución

Una devolución exige observación.

No permitir:

```text
RETURN sin motivo
```

El revisor no edita contenido.

Al devolver:

```text
IN_REVIEW -> DRAFT
```

No se habilita la siguiente etapa.

---

# 32. Corrección

Al volver a DRAFT:

- el editor puede modificar todo;
- resaltar lo observado;
- conservar todas las observaciones.

Después:

- generar nuevo artefacto;
- volver a firmar;
- reiniciar el workflow desde su primera etapa aplicable.

No crear nueva versión formal solo porque hubo devolución.

---

# 33. Versiones formales

Solo se crea una nueva versión cuando el documento anterior ya estaba APPROVED.

Ejemplo:

```text
v1 aprobado
-> Actualizar
-> v2 borrador
```

v2:

- copia datos de v1;
- permite modificar;
- recorre workflow completo;
- v1 permanece inmutable.

No crear versión nueva por:

- devolución normal;
- una corrección previa a aprobación;
- pequeño error ortográfico.

Sí por cambio material, por ejemplo:

- actividad faltante;
- actividad incorrecta;
- cambio importante de planificación.

Guardar:

```text
formal_version = 1,2,3...
```

Si el formato muestra `1.0`, `2.0`, renderizarlo sin convertir las correcciones en `1.1`.

---

# 34. Descargas

Regla más reciente:

```text
antes de APPROVED -> no descargar el documento final
después de APPROVED -> descarga disponible para usuarios autorizados
```

Aplicar permisos por documento, período y ámbito.

Decano/Subdecano pueden tener acceso histórico ampliado.

---

# 35. Ejecución de actividades

Un Plan aprobado habilita ejecución.

Pantalla de seguimiento:

- actividad;
- fechas;
- responsables;
- medios;
- evidencias;
- tiempo restante;
- completada/pendiente.

---

# 36. Evidencias

Por cada medio de verificación requerido debe existir evidencia.

Para evidencia documental, mantener PDF.

No usar DOCX/XLSX editable como sustituto del respaldo documental final.

También puede existir un medio configurado como:

- repositorio;
- URL;
- recurso digital.

La actividad queda completada cuando todos sus medios obligatorios tienen evidencia.

---

# 37. Evidencia y versionado

Modelar:

```text
evidence
evidence_version
```

Mientras el plazo esté abierto:

- permitir reemplazo;
- conservar versión anterior;
- registrar auditoría.

No sobrescribir físicamente.

---

# 38. Plazo de evidencia

Fecha límite normal:

```text
activity.to_date 23:59
America/Guayaquil
```

Después:

```text
bloquear carga/reemplazo
```

Para continuar se necesita prórroga.

---

# 39. Prórroga

La reunión más reciente definió:

- individual;
- autorizada por Coordinador;
- limitada;
- para actividad/evidencia se indicó un máximo aproximado de 3 días.

Entidad:

```text
deadline_extension
- user_id
- activity_id
- old_deadline
- new_deadline
- reason
- authorized_by
- created_at
```

Nunca modificar silenciosamente el deadline original.

---

# 40. Alertas

Notificar:

- actividad próxima a vencer;
- evidencia pendiente;
- documento asignado;
- documento devuelto;
- documento aprobado;
- prórroga concedida.

La frecuencia concreta puede configurarse.

---

# 41. Reutilización de evidencia

No crear una restricción global que prohíba dos archivos iguales por hash.

El mismo archivo puede tener uso válido en otra comisión.

Dentro de la misma comisión, actividades distintas normalmente tienen evidencias distintas.

El SHA-256 sirve para integridad/detección, no como `UNIQUE` global.

---

# 42. Informe Final

Regla:

```text
Plan 1 -> 1 Informe Final
```

No se crea sin Plan.

La misma persona responsable del Plan realiza el Informe Final.

Al final del período/ejecución:

- precargar Plan;
- actividades;
- cumplimiento;
- evidencias;
- información institucional.

El responsable completa campos narrativos y firma.

T2 nunca modifica el Plan fuente.

---

# 43. Secciones T2 actuales

Mantener mientras no llegue plantilla oficial nueva:

```text
1. Información General
2. Antecedentes
3. Desarrollo de Actividades
4. Conclusiones y Oportunidades
5. Registro de Contactos
6. Anexos
7. Previsualización
8. Firma y Finalización
```

---

# 44. Evidencias dentro del Informe Final

El cliente indicó que las evidencias deben mostrarse al final como páginas seguidas.

Proceso:

1. generar cuerpo;
2. anexos/listado;
3. anexar PDFs de evidencia;
4. mantener paginación.

Si es URL/repositorio:

- mostrar referencia textual;
- no intentar convertirlo en PDF.

---

# 45. Otros tipos documentales

El cliente confirmó que existen más formatos:

- planificaciones;
- informes;
- ejecución;
- informe final;
- documentos por actividad/evento.

La lista oficial aún será entregada.

No diseñar el backend exclusivamente para T1/T2.

Crear:

```text
document_type
```

Cada tipo puede definir:

- plantilla;
- secciones;
- workflow;
- fuente;
- Plan previo;
- actividades;
- anexos;
- evidencias;
- slots de firma.

---

# 46. Administración

Admin debe poder gestionar:

- usuarios;
- carreras;
- grupos;
- membresías;
- cargos;
- períodos;
- feriados;
- catálogos;
- tipos documentales;
- plantillas;
- workflows;
- asignaciones;
- auditoría;
- notificaciones;
- reportes.

---

# 47. Datos DEMO

Mientras el cliente entregue:

- comisiones;
- docentes;
- carreras;
- cargos;
- revisores;
- aprobadores;
- catálogos;
- documentos oficiales;

usar seeds DEMO.

Puede registrarse:

```text
DEMO
PROJECT_DEFAULT
INSTITUTIONAL_CONFIRMED
```

No confundir DEMO con configuración oficial.

---

# 48. Catálogos

Usar desactivación lógica:

```text
active = false
```

No borrar físicamente registros que ya forman parte de documentos históricos.

Guardar snapshots textuales en documentos cuando sea necesario.

---

# 49. Storage

No guardar PDFs/archivos grandes como BLOB en PostgreSQL.

Abstracción:

```text
DocumentStorage
├── LocalPrivateFileSystemStorage
└── S3CompatibleStorage
```

Inicialmente usar filesystem privado.

Tabla de metadatos:

```text
stored_file
- id
- original_name
- storage_key
- mime_type
- size
- sha256
- created_by
- created_at
```

Nunca exponer la carpeta directamente por web.

---

# 50. Artefactos inmutables

Crear:

```text
document_artifact
signed_document_artifact
artifact_signature
```

Nunca hacer UPDATE de bytes firmados.

Si cambia el contenido:

```text
nuevo artifact
```

---

# 51. Auditoría

Crear `audit_event` append-only.

Registrar:

- login;
- cambio contraseña;
- creación usuario;
- asignación;
- creación Plan;
- edición;
- archivado;
- firma;
- revisión;
- observación;
- devolución;
- aprobación;
- nueva versión;
- evidencia;
- reemplazo;
- prórroga;
- configuración;
- cierre período;
- exportación.

No guardar secretos.

---

# 52. Acceso histórico

Regla reciente:

- usuario normal/revisor ve su período y ámbito autorizado;
- cambiar de comisión no da acceso automático al histórico anterior;
- Decano/Subdecano pueden consultar historial institucional de su ámbito.

Autorización contextual:

```text
period + group + career + role + authority_scope
```

---

# 53. Cierre del período — contradicción real

En la misma reunión aparecieron dos ideas:

1. Decano/Subdecano deben poder ver historial.
2. Al final del semestre se habló de descargar el historial y eliminar lo existente.

No interpretar esto como permiso para destruir automáticamente datos institucionales.

Hasta aclaración:

```text
NO borrar físicamente de forma automática.
```

Implementar:

```text
ACTIVE -> CLOSED
```

Al cerrar:

1. generar export/archivo;
2. bloquear edición;
3. archivar datos;
4. conservar PDFs aprobados;
5. conservar auditoría;
6. conservar metadatos para historial;
7. permitir descarga del archivo histórico.

Marcar:

```text
PENDIENTE_REAL_RETENCION
```

para una futura política de purga.

---

# 54. Reportes

Implementar reportes por comisión y globales.

## Por comisión

- número de planificaciones;
- número de actividades;
- actividades completadas;
- pendientes;
- porcentaje de ejecución.

Fórmula propuesta:

```text
completed_required_activities / total_required_activities * 100
```

## Global

- total Planes;
- aprobados;
- actividades;
- completadas;
- distribución por comisión;
- comparación de número/porcentaje de actividades entre comisiones.

No crear rankings de docentes ni score de desempeño personal.

---

# 55. Dashboard

Mostrar:

- Planes del período;
- DRAFT;
- IN_REVIEW;
- APPROVED;
- actividades próximas a vencer;
- evidencias faltantes;
- porcentajes de ejecución;
- distribución por comisión.

---

# 56. Notificaciones

Bandeja interna obligatoria.

Eventos base:

```text
DOCUMENT_ASSIGNED
DOCUMENT_RETURNED
DOCUMENT_APPROVED
ACTIVITY_DEADLINE_NEAR
EVIDENCE_MISSING
EXTENSION_GRANTED
PASSWORD_RESET
```

El correo es complemento, no requisito para que el workflow funcione.

---

# 57. IA de redacción

Arquitectura:

```text
AiWritingService
      ↓
AiProvider
 ├── MockAiProvider
 ├── GroqAiProvider
 └── OtherAiProvider
```

La credencial debe vivir solo en backend:

- variable de entorno;
- secret file/store.

Nunca:

- React;
- localStorage;
- Git;
- logs.

El cliente aceptó usar una cuenta genérica destinada al sistema.

No conservar historial completo de prompts/respuestas.

La IA:

- propone;
- usuario aplica/descarta;
- no sobrescribe sola;
- no genera mejora desde campo vacío;
- si falla, se sigue editando manualmente.

---

# 58. UI/UX

El cliente pidió que el sistema se vea más moderno.

Mantener identidad institucional:

- azul;
- blanco;
- grises;
- iconos;
- jerarquía;
- menor saturación textual.

No alterar flujos aprobados únicamente por estética.

## Login

Conservar el rediseño:

- fondo institucional;
- tarjeta central;
- logo;
- campos con iconos;
- CAPTCHA alfanumérico;
- recuperación;
- composición azul/blanco.

## Visor

Conservar:

- página por página;
- zoom;
- scroll;
- full-screen real;
- toolbar;
- panel;
- ESC;
- A4.

---

# 59. Autorización backend

No autorizar únicamente por rol global.

Validar:

```text
user
role
period
group
career
document
active workflow step
assignment
```

Un revisor solo ve/actúa en la tarea que tiene activa.

---

# 60. Matriz de permisos base

## Responsable editor

Puede:

- crear;
- editar DRAFT;
- actividades;
- anexos;
- previsualizar;
- firmar;
- corregir devolución;
- cargar evidencias;
- crear Informe Final.

No puede aprobar su propio Plan ni editar APPROVED.

## Miembro

Puede consultar y participar como responsable/elaborador.

No edita Plan salvo reasignación.

## Revisor

Puede:

- visualizar;
- observar;
- devolver;
- aprobar y firmar cuando su tarea está activa.

No modifica contenido.

## Aprobador final

Puede revisar, devolver, aprobar y firmar.

## Coordinador

Puede gestionar su ámbito, reasignaciones, prórrogas y membresías según permiso.

## Admin

Configura sistema globalmente.

Ser Admin no implica permiso automático para aprobar documentos.

---

# 61. Modelo de datos mínimo

```text
users
credentials
roles

faculties
careers
organizational_groups
group_memberships
periods
holidays

document_types
documents
document_versions
document_contributors
document_sections

activities
activity_responsibles
resources
verification_media
activity_resources
activity_verification_media

annexes

template_definitions
template_versions

workflow_definitions
workflow_versions
workflow_steps
period_workflow_assignments
workflow_instances
workflow_tasks

review_observations

stored_files
document_artifacts
artifact_signatures

evidences
evidence_versions
deadline_extensions

final_reports

notifications
audit_events

ai_configuration
system_configuration
```

---

# 62. Campos clave de documento

```text
id UUID
type_id
period_id
group_id
career_id
responsible_user_id
state
formal_version
review_round
template_version_id
workflow_version_id
archived
created_at
updated_at
row_version
```

Usar UUID.

Usar optimistic locking con `@Version`.

---

# 63. Concurrencia e idempotencia

Ante modificación concurrente:

```text
409 CONFLICT
```

No sobrescribir silenciosamente.

Operaciones críticas deben ser idempotentes:

- firmar;
- aprobar;
- devolver;
- crear versión;
- cargar evidencia;
- prórroga.

Usar `Idempotency-Key` o mecanismo equivalente.

---

# 64. Transacciones

Firmar/finalizar debe ser atómico:

```text
validar
-> generar PDF
-> firmar
-> guardar artifact
-> crear workflow
-> cambiar estado
-> auditar
-> notificar
```

Si falla una parte:

```text
rollback
```

---

# 65. API REST

Grupos orientativos:

```text
/api/v1/auth/**
/api/v1/users/**
/api/v1/organization/**
/api/v1/periods/**
/api/v1/catalogs/**
/api/v1/documents/**
/api/v1/plans/**
/api/v1/activities/**
/api/v1/files/**
/api/v1/artifacts/**
/api/v1/signatures/**
/api/v1/workflows/**
/api/v1/reviews/**
/api/v1/evidence/**
/api/v1/reports/**
/api/v1/notifications/**
/api/v1/admin/**
/api/v1/ai/**
```

Errores consistentes:

```json
{
  "code": "DOCUMENT_NOT_EDITABLE",
  "message": "El documento no puede modificarse en su estado actual.",
  "details": {},
  "traceId": "..."
}
```

---

# 66. Seguridad web

Implementar:

- Spring Security;
- sesión de servidor;
- cookie HttpOnly;
- Secure en producción;
- SameSite;
- CSRF;
- rate limiting;
- headers de seguridad;
- validación;
- sanitización;
- autorización en backend.

No almacenar JWT en localStorage como estrategia principal.

---

# 67. Seguridad de archivos

Validar:

- tamaño;
- extensión;
- MIME real;
- nombre;
- ruta;
- hash.

Evitar path traversal.

Usar storage key interno, no nombre original como path físico.

---

# 68. Infraestructura

El cliente indicó como puerto externo de la aplicación:

```text
64430
```

Diseño:

```text
Red
 ↓ :64430
Reverse Proxy
 ├── frontend
 └── backend
       ↓
   PostgreSQL
```

PostgreSQL no debe exponerse públicamente.

---

# 69. SMTP

Se identificó:

```text
587
```

como puerto de correo a verificar/habilitar.

Si el servidor no permite SMTP:

- mantener reset administrativo;
- el sistema sigue siendo utilizable.

---

# 70. Servidor — PENDIENTE_REAL

El cliente consultará:

- sistema operativo;
- CPU;
- RAM;
- almacenamiento;
- IP/dominio;
- SSH;
- TLS;
- Docker permitido;
- firewall;
- backups;
- puerto SMTP;
- ubicación de datos.

Hasta recibirlo, construir despliegue portable y contenedorizado.

---

# 71. Docker y despliegue

Preparar Docker Compose:

```text
reverse-proxy
frontend
backend
postgres
```

Volúmenes:

- DB;
- documentos;
- backups.

Crear procedimiento de:

- backup PostgreSQL;
- backup storage;
- restore.

Un backup solo de DB no es suficiente.

---

# 72. Pruebas mínimas

## Unitarias

- estados;
- fechas;
- unicidad;
- permisos;
- versiones;
- deadlines.

## Integración

- PostgreSQL con Testcontainers;
- Flyway;
- transacciones;
- repositories.

## Seguridad

- IDOR;
- acceso fuera de grupo;
- descarga no autorizada;
- reviewer fuera de turno;
- escalada de rol.

## Workflow

- secuencia;
- devolución;
- reinicio;
- múltiples revisores obligatorios;
- doble rol prohibido;
- actor inactivo.

## Firma

- contraseña errónea;
- certificado inválido;
- firma correcta;
- bytes alterados.

## PDF

- A4;
- matriz;
- footer;
- páginas;
- firma slots.

## E2E

- login;
- cambio contraseña;
- Plan;
- firma;
- devolución;
- corrección;
- aprobación;
- evidencias;
- Informe Final.

---

# 73. Flujo E2E obligatorio

```text
1. Responsable entra.
2. Cambia contraseña temporal.
3. Selecciona contexto.
4. Crea Plan.
5. Registra actividades.
6. Adjunta anexos.
7. Previsualiza.
8. Firma.
9. Estado IN_REVIEW.
10. Revisor devuelve con observación.
11. Estado DRAFT.
12. Responsable corrige.
13. Firma otra vez.
14. Revisor 1 aprueba/firma.
15. Revisor 2 aprueba/firma si existe.
16. Aprobador final aprueba/firma.
17. Estado APPROVED.
18. Se ejecutan actividades.
19. Se cargan evidencias.
20. Actividades quedan completas.
21. Se crea Informe Final.
22. Se firma/aprueba Informe Final.
23. Se consulta auditoría.
```

---

# 74. E2E de versión

```text
Plan v1 APPROVED
-> Actualizar
-> crear v2 DRAFT copiando v1
-> modificar actividad
-> v1 sigue inmutable
-> v2 recorre workflow
-> v2 APPROVED
-> historial muestra ambas
```

---

# 75. Caso multicarrera obligatorio

Usuario pertenece a:

```text
Software
TI
```

Debe:

- seleccionar carrera;
- crear Plan separado por carrera;
- tener workflow independiente;
- no colisionar en unicidad.

---

# 76. Caso histórico obligatorio

Usuario A:

```text
Periodo 1 -> Comisión X
Periodo 2 -> Comisión Y
```

No debe obtener acceso automático a X histórico por estar en Y.

Decano/Subdecano sí pueden consultar el historial de su ámbito.

---

# 77. Caso prórroga obligatorio

```text
actividad vencida
-> carga bloqueada
-> Coordinador concede extensión individual
-> carga habilitada hasta nueva fecha
-> auditoría registra todo
```

---

# 78. Escala

Objetivo conocido:

```text
fase inicial ~200 usuarios
pico potencial ~2000 docentes
```

Monolito modular es suficiente.

Usar:

- índices;
- paginación;
- streaming;
- pool conexiones;
- queries eficientes.

---

# 79. Migraciones

Todo cambio DB con Flyway.

No modificar una migración ya aplicada.

Ejemplo:

```text
V001__create_users.sql
V002__create_organization.sql
...
```

Separar schema de seeds DEMO.

---

# 80. No hardcodear

No hardcodear:

- nombres de personas;
- comisión;
- carrera;
- decano/subdecano;
- período;
- feriados;
- actividades;
- recursos;
- medios;
- workflow;
- plantilla;
- email;
- API key.

Reglas institucionales en DB/configuración.

Secretos en entorno/secrets.

---

# 81. Documentación obligatoria

Mantener:

```text
docs/
  requirements/
  architecture/
  decisions/
  api/
  database/
  security/
  workflows/
  testing/
  deployment/
  operations/

manuals/
  user/
  reviewer/
  coordinator/
  admin/
```

Además:

- README;
- CHANGELOG;
- OpenAPI;
- ERD;
- diccionario de datos;
- matriz permisos;
- trazabilidad;
- instalación;
- backup/restore.

---

# 82. ADRs recomendados

```text
ADR-001 Monolito modular
ADR-002 Spring Boot + PostgreSQL
ADR-003 Sesión web segura
ADR-004 PDF generado por backend
ADR-005 Workflow secuencial por período
ADR-006 Artefactos inmutables
ADR-007 Firma PKCS#12 temporal
ADR-008 Storage privado
ADR-009 IA mediante provider abstraction
ADR-010 Archivo y retención de período
```

---

# 83. Orden de implementación

## Fase A — Fundación

1. estructura;
2. configuración;
3. PostgreSQL;
4. Flyway;
5. error handling;
6. auditoría base;
7. Docker DEV.

## Fase B — Seguridad

8. usuarios;
9. permisos;
10. login;
11. contraseña temporal;
12. cambio obligatorio;
13. CAPTCHA;
14. recuperación/reset.

## Fase C — Organización

15. facultad;
16. carreras;
17. grupos;
18. membresías;
19. períodos;
20. feriados.

## Fase D — Catálogos

21. tipos actividad;
22. actividades;
23. recursos;
24. medios.

## Fase E — Plan

25. document base;
26. Plan;
27. persistencia wizard;
28. actividades;
29. anexos.

## Fase F — PDF/Firma

30. renderer;
31. T1;
32. artefactos;
33. PKCS#12;
34. firma.

## Fase G — Workflow

35. definitions;
36. assignments;
37. instances;
38. tasks;
39. observaciones;
40. devolver;
41. aprobar/firma.

## Fase H — Versiones

42. historial;
43. nueva versión;
44. archivado.

## Fase I — Ejecución

45. seguimiento;
46. evidencias;
47. deadlines;
48. prórrogas;
49. notificaciones.

## Fase J — Informe Final

50. generación;
51. evidencia/anexos;
52. PDF;
53. workflow/firma.

## Fase K — Administración

54. plantillas;
55. workflows por período;
56. dashboard;
57. reportes;
58. auditoría.

## Fase L — IA

59. provider;
60. integración;
61. apply/discard.

## Fase M — Producción

62. infraestructura real;
63. reverse proxy;
64. TLS;
65. SMTP;
66. backups;
67. pruebas;
68. capacitación;
69. entrega.

---

# 84. Reglas para modificar el frontend actual

Antes de cambiar una pantalla:

1. comprobar si ya fue aceptada;
2. conservar flujo;
3. integrar backend;
4. modernizar sin romper funcionalidad;
5. mantener accesibilidad;
6. reducir texto redundante;
7. usar iconos útiles.

T1 actual se considera base funcional aprobada.

No reabrirlo sin bug, integración real o nueva instrucción del cliente.

---

# 85. Pendientes reales

El agente no debe inventar estos datos.

## Cliente prometió entregar

- lista oficial de comisiones;
- lista de docentes;
- carreras por docente;
- responsables/coordinadores;
- quién revisa;
- quién aprueba;
- Decano/Subdecano aplicable;
- catálogos reales;
- formatos/documentos adicionales;
- Drive institucional de referencia.

## Infraestructura

- características del servidor;
- SO;
- dominio/TLS;
- política de red;
- backups;
- storage;
- SMTP.

## Retención

Resolver contradicción entre consulta histórica y eliminación al cierre.

## Tipos documentales

Lista y plantillas definitivas más allá de Plan/Informe Final.

---

# 86. Decisiones que ya no deben volver a preguntarse

No volver a preguntar como si estuvieran abiertas:

- existe Admin;
- Admin gestiona períodos;
- no hay registro público;
- primer login obliga cambio de contraseña;
- existen POA/Plan de Mejoras/Acción de Mejora/Otra;
- mínimo un responsable;
- recursos/medios son catálogos;
- actividad debe estar dentro del período;
- puede atravesar feriados pero no iniciar/terminar en feriado;
- evidencias documentales son PDF;
- workflow es secuencial;
- todos los revisores obligatorios deben aprobar;
- una persona no puede ocupar varias etapas del mismo documento;
- firmar envía automáticamente a revisión;
- devolver exige observación;
- aprobado es inmutable;
- nueva versión se crea después de aprobación;
- un Plan corresponde a comisión/carrera/período;
- Informe Final requiere Plan;
- Informe Final es único;
- el mismo responsable realiza Plan e Informe Final;
- desktop es objetivo;
- tablet es aceptable;
- teléfono no es objetivo;
- IA no necesita historial textual;
- UI debe modernizarse.

---

# 87. Definición de Done

Una feature no está terminada solo porque compila.

Debe cumplir:

```text
[ ] regla de negocio
[ ] autorización
[ ] validación
[ ] migración
[ ] auditoría
[ ] errores
[ ] pruebas unitarias
[ ] integración
[ ] E2E cuando aplique
[ ] OpenAPI
[ ] documentación
[ ] frontend integrado
```

---

# 88. Protocolo operativo para el agente

Para cada tarea:

1. inspeccionar código existente;
2. identificar reglas impactadas;
3. no modificar fuera de alcance;
4. crear migración si cambia DB;
5. implementar;
6. integrar frontend si hace falta;
7. ejecutar pruebas;
8. actualizar docs;
9. entregar reporte.

El reporte debe incluir:

- archivos modificados;
- migraciones;
- endpoints;
- reglas;
- pruebas;
- resultados;
- supuestos;
- pendientes reales.

---

# 89. Prohibiciones

El agente no debe:

- borrar documentos aprobados;
- editar APPROVED;
- almacenar PKCS#12;
- almacenar contraseñas;
- exponer API keys;
- activar reviewers fuera de turno;
- asignar una persona a dos etapas del mismo documento;
- implementar workflow paralelo;
- cambiar workflow del período activo;
- considerar al docente como dueño permanente del Plan;
- duplicar Matriz como fuente editable;
- crear Informe Final sin Plan;
- crear varios Informes Finales para el mismo Plan;
- aceptar evidencia documental editable como respaldo final;
- borrar historial físicamente hasta resolver retención;
- crear rankings de docentes;
- inventar autoridades;
- inventar nombres de revisores;
- inventar códigos institucionales;
- cambiar formato oficial sin plantilla.

---

# 90. Resultado final esperado

El sistema completo debe permitir que:

1. la institución configure el período;
2. cargue usuarios/comisiones/carreras;
3. asigne responsables y flujo;
4. el responsable cree el Plan;
5. registre actividades;
6. genere el documento institucional;
7. firme;
8. el documento pase automáticamente a workflow;
9. pueda volver con observaciones;
10. quede aprobado e inmutable;
11. se ejecuten actividades;
12. se carguen evidencias;
13. se concedan prórrogas;
14. se genere Informe Final;
15. se firme/apruebe;
16. se conserve trazabilidad;
17. existan reportes;
18. se cierre el período;
19. se exporte/archive el histórico;
20. Admin pueda configurar el sistema sin hardcodear reglas por nombre.

---

# 91. Primera tarea que debe ejecutar el agente ahora

Antes de seguir creando módulos de backend, el agente debe auditar el repositorio actual contra este Plan Maestro y producir:

```text
REPORTE_AUDITORIA_IMPLEMENTACION_VS_PLAN_MAESTRO.md
```

La auditoría debe clasificar cada punto como:

```text
IMPLEMENTADO
PARCIAL
FALTA
CONTRADICE
```

Debe revisar especialmente:

- titularidad del Plan;
- clave de unicidad;
- multicarrera;
- workflow secuencial;
- estados formales;
- firma -> revisión automática;
- versión formal;
- T2 1:1;
- evidencias;
- permisos históricos;
- retención;
- puertos/configuración.

No reescribir componentes correctos.

Corregir primero las contradicciones de dominio y después continuar por el orden de implementación definido en este archivo.

---

# 92. Fuente de verdad

A partir de ahora:

```text
este archivo = especificación consolidada actual del proyecto
```

Si aparece documentación anterior que contradice este Plan Maestro, seguir este archivo, excepto cuando exista una decisión posterior explícita del cliente.

Cuando exista una nueva decisión:

1. actualizar este archivo;
2. registrar el cambio;
3. después modificar el software.
