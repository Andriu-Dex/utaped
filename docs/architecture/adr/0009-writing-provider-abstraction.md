# ADR-0009 — Proveedores de redacción detrás de una interfaz backend

Fecha: 2026-10-12. Estado: aceptado en alcance de desarrollo, por autorización del propietario y dirección del Plan Maestro §57. Habilitación institucional y prueba con cuenta real pendientes.

## Contexto

T1/T2 necesitan mejorar texto existente mediante propuestas que el usuario compara y aplica/descarta. La edición manual debe continuar si el proveedor falla. La clave no puede estar en React/Git y no se conserva historial completo de prompts/respuestas. La herramienta de inspección de código/Trello la construye el propietario aparte y queda fuera de esta entrega.

## Opciones

- Integración directa desde React: incompatible con custodia de credenciales y autorización documental.
- Proveedor simulado como mecanismo productivo: no realiza la integración real solicitada.
- Interfaz backend y adaptadores HTTP con activación explícita: permite contratos reales y pruebas aisladas sin condicionar el núcleo documental a una cuenta externa.

## Decisión

`WritingService` autoriza documento/campo/versión antes y después de consultar `AiProvider`. `HttpAiProvider` implementa los contratos de Groq y OpenAI con selección explícita, sin fallback. Se reutilizan HttpClient de Java 21, JsonMapper y el contador PostgreSQL existentes; no se añade SDK, cola, Redis ni tablas de prompts.

Asistencia deshabilitada por defecto. Cuenta/modelo/clave configurados en backend; endpoints HTTPS fijos, timeout, tamaño de respuesta limitado, límites de consumo y concurrencia por instancia. OpenAI utiliza Responses con `store:false`; Groq utiliza Chat Completions. La ausencia de herramientas/contexto adicional evita enviar archivos o metadatos documentales por defecto.

El servicio devuelve una propuesta sin mutar documentos. React conserva la comparación solo en memoria y bloquea aplicar si cambió el texto o versión. El guardado usa la misma concurrencia/ventana/permiso documental. Solo se audita solicitud/resultado técnico, sin contenido textual.

## Consecuencias

No hace falta una migración para la integración. Las cuotas/retención del proveedor y el presupuesto agregado de futuras réplicas siguen siendo condiciones operativas externas. Las instrucciones solicitan preservar hechos; la corrección factual se verifica mediante revisión humana, no se garantiza por el modelo. El ajuste de modelo requiere prueba con una cuenta habilitada, sin reutilizar nombres históricos por suposición.

## Fuentes y validación

[Contrato UTAPED](../../api/writing-and-review-inbox.md), [configuración](../../development/writing-configuration.md), [OpenAI Responses](https://developers.openai.com/api/docs/guides/migrate-to-responses), [API Groq](https://console.groq.com/docs/api-reference). Pruebas locales del transporte, errores/tamaño/timeout, autorización/concurrencia y circuito visual documentadas en el reporte de entrega; no constituyen consumo de proveedor real.
