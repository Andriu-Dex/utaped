# Decisiones para cerrar con el cliente

Preparado el 30/10/2026 desde la transcripción completa y la checklist de requisitos. Este documento reúne preguntas concretas; no contiene respuestas institucionales inventadas. El propietario puede revisarlas con el cliente en una sola sesión.

Registrar para cada decisión: respuesta exacta, autoridad que confirma, fecha, fuente/archivo y alcance por comisión/carrera/período. No incluir contraseñas, .p12 ni claves API.

| ID | Pregunta que necesita respuesta | Propuesta para revisar | Qué habilita / estado |
|---|---|---|---|
| D01 | ¿Cuál es la tabla completa de unidades, docentes/carreras, principales, revisores, aprobadores y órganos por tipo documental/período? | Entregar tabla institucional validada, con cargos y suplencias; las referencias de Templates no convierten cuentas ficticias en autoridades reales. | Configuración final. Pendiente cliente. |
| D02 | Cuando un revisor no está disponible, ¿se acepta conservar su nombre previsto en el PDF firmado y que el sustituto aparezca en la nueva firma y en el historial? | Conservar artefacto; firma incremental propia identificada como sustitución. Si debe cambiar el roster impreso, utilizar corrección/nuevo artefacto con firmas nuevas. | Backend preparado; aceptación de representación pendiente. |
| D03 | En la unidad unipersonal multicarrera, ¿basta crear explícitamente borradores independientes desde la misma planificación? | Seleccionar carreras, copiar textos/estructura/matriz; no firmas, anexos ni evidencia. Evitar creación silenciosa. | Backend preparado; UI y aceptación pendientes. |
| D04 | ¿Quipux es condición previa a aprobación o constancia posterior? ¿Para qué flujos y quién registra? | Mantener el gate configurable actual, sin integración externa automática, hasta ratificar secuencia/casos. | No cambiar el orden por interpretación de las propuestas contradictorias. |
| D05 | ¿Qué texto exacto era la nota «quemada» de la matriz y debe ser siempre obligatoria? | Comparar captura/plantilla oficial con el aviso de privacidad actual, que es opcional. | No imponer una nota sin identificarla. |
| D06 | ¿Los cinco días de elaboración y los tres de extensión son hábiles o calendario? ¿Qué ocurre con ausencia temporal de un revisor todavía activo? | Revisar la parametrización hábil actual y límites por ámbito/persona. Para sustituir actores, usar inactividad comprobable hasta acordar otra causa. | Ajuste de configuración y excepciones. |
| D07 | ¿Se debe impedir por hash reutilizar un PDF en medios/actividades de la misma comisión? | Mantener asociaciones explícitas y versión; no deduplicar ni bloquear por similitud sin una regla confirmada. | Regla adicional solo si cliente la exige. |
| D08 | ¿Cuánto se conserva el período cerrado y quién autoriza purga? | Archivo/copia verificada, acceso histórico y purga manual protegida. Nunca borrado automático sin política. | Retención y custodios pendientes. |
| D09 | ¿Se aceptan T1/T2/T3 y su paginación/firmas con ejemplos cortos y largos? ¿Hay más formatos oficiales? | Sesión de cotejo visual contra archivos originales, incluyendo anexos y notas. | Acta de aceptación documental y lista final de tipos. |
| D10 | ¿Qué certificados y política de confianza/revocación/tiempo admite la institución? | Ensayo con certificados autorizados y revisión del contrato técnico de firma, sin atribuir equivalencia automática cuenta↔titular. | Aceptación operativa de firma pendiente. Documentar en docs, no agregar avisos de limitaciones en UI. |
| D11 | ¿Cuál es el servidor, dominio/TLS, red, almacenamiento, respaldo y remitente SMTP finales? | Aplicación por 64430, SMTP autorizado, almacenamiento privado y copia fuera del servidor con prueba de restauración. | Falta información del operador/DTIC y prueba en destino. |
| D12 | ¿Qué cuenta/modelo de redacción, cuotas y tratamiento de datos se autorizan? | Cuenta destinada al sistema, secretos solo backend y prueba con texto ficticio. | Proveedor real pendiente de autorización/configuración; edición manual siempre disponible. |

## Tabla de entrega de datos

- Comisiones/unidades: nombre, tipo, carrera/facultad, período y principal.
- Personas: username, correo, nombres/apellidos y pertenencias/cargos; no teléfono/cédula salvo nueva instrucción justificada.
- Flujo: tipo documental, ámbito, orden, cargo, titular/registrador, requiere firma, regla Quipux y vigencia.
- Operación: dominio/IP, TLS, puertos de entrada/salida, almacenamiento, custodios, respaldo/retención y contacto responsable.

## Estado de cierre

Todos los D01–D12 quedan PENDIENTES de respuesta/aceptación; una propuesta del desarrollador no es una respuesta del cliente. Las capacidades ya implementadas no se deshabilitan por este documento y las pruebas técnicas no sustituyen las decisiones externas.
