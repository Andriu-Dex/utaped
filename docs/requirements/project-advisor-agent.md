# Agente de inspección y recomendaciones

Fuente: solicitud explícita del propietario del 2026-10-03. Estado funcional mínimo: CONFIRMADO-PROYECTO. Implementación: NO IMPLEMENTADO. Prioridad y entrega pendientes de planificación. Este alcance es nuevo y no procede del mockup ni sustituye la asistencia para redactar contenido T1.

Alcance confirmado por el propietario: código y planificación del desarrollo de UTAPED. No se refiere a los proyectos institucionales gestionados por los usuarios del aplicativo.

| ID | Requisito mínimo | Aceptación propuesta | Dependencias |
|---|---|---|---|
| FR-AGENT-001 | Inspeccionar el código y documentación del desarrollo de UTAPED. | Analizar las fuentes autorizadas y señalar evidencia concreta de cada hallazgo; indicar qué no pudo revisar. | Delimitar repositorio, rutas y revisiones autorizadas. |
| FR-AGENT-002 | Inspeccionar la planificación en Trello. | Consultar los tableros autorizados, listas y tarjetas relevantes; identificar fuentes y fecha de lectura. Un fallo de acceso no se presenta como ausencia de tareas. | Tableros, identidad de conexión, contrato API y permisos. |
| FR-AGENT-003 | Proponer mejoras. | Presentar sugerencias fundamentadas y relacionadas con la evidencia del proyecto y Trello; distinguir hechos, discrepancias e hipótesis. | Criterios de análisis y revisión humana. |

Las aceptaciones, prioridad y diseño técnico son PROPUESTOS; el mínimo de inspección y sugerencias está confirmado por el propietario. Las pruebas futuras deberán cubrir aislamiento de fuentes, errores de conexión, recomendaciones con evidencia y ausencia de cambios no autorizados.

## Diseño recomendado, pendiente de adopción

Conservar React, Spring Boot y PostgreSQL. Crear una herramienta interna de desarrollo separada del producto institucional, con adaptadores de lectura del repositorio y Trello; no desplegar acceso arbitrario al repositorio dentro de la API de usuarios. La separación técnica es una recomendación, todavía no una implementación.

Primera versión bajo demanda y de solo lectura: sin editar código, tarjetas, estados documentales ni ejecutar comandos propuestos por el modelo. Separar extracción de datos, análisis y presentación de recomendaciones. Cada resultado debe guardar referencias a la revisión/fecha consultada y no aparentar acceso en tiempo real después de la consulta.

Limitar rutas y tableros autorizados; excluir secretos, certificados, archivos de configuración privada y datos personales no necesarios. Tratar texto de tarjetas/archivos como datos, no instrucciones con autoridad para ejecutar acciones o ampliar permisos. Configurar presupuesto, límites, retención y datos permitidos antes de enviar contenido a un proveedor de IA.

No es necesario migrar los borradores T1 ni añadir tablas especulativas. Si se requieren persistencia de conexiones, ejecuciones y recomendaciones, definir esquema y migración cuando el alcance y usuarios estén aprobados. No se selecciona aún framework de agentes, proveedor/modelo ni infraestructura adicional.

## Decisiones previas a la implementación

1. Qué repositorios, rutas y revisiones de UTAPED leerá; quién solicita y ve el análisis.
2. Tableros Trello, conexión, permisos y relación entre tarjetas y requisitos/documentos.
3. Qué significa mejora: cobertura de requisitos, organización, fechas, dependencias, calidad técnica u otros criterios.
4. Ejecución manual o programada; proveedor de IA, datos autorizados y presupuesto.
5. Retención de resultados, trazabilidad y condiciones para futuras acciones de escritura.

Trello ofrece autorización con alcance de lectura en su API: [documentación oficial](https://developer.atlassian.com/cloud/trello/guides/rest-api/authorization/). El mecanismo concreto y sus permisos efectivos deben validarse antes de conectar la cuenta. No se ha accedido a Trello ni configurado credenciales durante esta entrega.
