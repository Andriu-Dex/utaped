# Redacción asistida T1/T2 y bandeja de revisión paginada

Fecha: 2026-10-12. Rama: `feature/modern-workspace-access`. Alcance autorizado: siguiente tanda del producto con interfaz moderna; el agente de inspección de código/Trello lo desarrolla el propietario por separado y no se implementa aquí.

## Resultado

Se integró asistencia de redacción de texto existente en Justificación/Objetivo del Plan T1 y Antecedentes/Conclusiones/Oportunidades de mejora del Informe T2. Panel desplegable con estilo, consentimiento, contadores, comparación de original/propuesta y aplicar/descartar. Aplicar cambia únicamente el formulario; el usuario guarda mediante el circuito existente. Si el texto, documento o versión cambia, una propuesta anterior no se puede aplicar. La edición manual continúa cuando el servicio está deshabilitado o falla.

La bandeja personal de revisión ahora ofrece búsqueda por título/grupo/período, filtros por estado y T1/T2, métricas generales propias, tamaños de página y navegación. Se preservan filtros al volver del inspector. El frontend deja de depender del antiguo límite de 200 tareas. Se conserva la API anterior para compatibilidad. El nuevo backend usa filtros parametrizados, orden estable y un snapshot por respuesta para mantener coherencia entre filas, total e indicadores.

Interfaces integradas con la base visual del propietario, azul institucional, iconos accesibles, tarjetas, feedback toast y modos claro/oscuro. Se verificaron escritorio 1440×1000 y tableta 820×1180; no se añadieron reglas para celular. T1/T2 institucionales, firmas sucesivas y artefactos inmutables mantienen su circuito.

## Integración y límites

`AiProvider` separa el permiso documental del transporte real. Adaptador HTTP para Groq Chat Completions y OpenAI Responses; deshabilitado por defecto, sin proveedor ficticio productivo ni fallback automático. Cuenta/modelo/clave se configuran exclusivamente en backend. OpenAI usa `store:false`; no se presenta como garantía de retención cero. [Responses](https://developers.openai.com/api/docs/guides/migrate-to-responses), [datos OpenAI](https://developers.openai.com/api/docs/guides/your-data), [API Groq](https://console.groq.com/docs/api-reference), [datos Groq](https://console.groq.com/docs/your-data).

Solo el texto del campo autorizado se envía tras consentimiento. No se envían PDFs, anexos, matriz, delegaciones ni metadatos de usuarios/documentos automáticamente. La clave y los cuerpos privados de errores no se devuelven al navegador. No se conserva historial de prompts/respuestas; auditoría registra actor, documento y resultado técnico.

Autorización de borrador, período/ventana, archivo y versión antes/después de la llamada, sin mantener bloqueos documentales durante la espera externa. Límite inicial 8000 caracteres para asistencia, 30 segundos, 2000 tokens, dos llamadas concurrentes por instancia y 15 intentos por usuario por 15 minutos. Respuestas mayores de 128 KiB, incompletas, vacías, rechazadas o inválidas se descartan. Los límites son configuración técnica ajustable, no normativa universitaria. No se modifica el límite manual de los textos.

No hacen falta tablas ni migraciones adicionales; Flyway continúa en V21. Se reutiliza el contador PostgreSQL existente. La DB principal, secretos y prototipo congelado no se modificaron ni se restauraron documentos eliminados.

## Validación ejecutada

- Backend completo en PostgreSQL aislado `utaped_test`: **129 ejecuciones, 19 suites, cero fallos/errores/omitidos**. El total incluye casos heredados ejecutados por subclases; no representa 129 escenarios únicos. Contratos HTTP locales Groq/OpenAI; salida, errores privados, truncamiento/tamaño, timeout y servicio deshabilitado. Permisos por documento/tipo, consentimiento/CSRF, versión obsoleta, documentos no editables, cierre durante una llamada, ausencia de mutación y límite de consumo. Se comprobó recuperación tras fallos sin perder capacidad.
- Revisión: fixture con 236 tareas, recorrido de 12 páginas sin duplicados, estabilidad, indicadores independientes del filtro, búsquedas literales, exclusión de actor futuro/ajeno y límites de parámetros.
- Frontend: `npm run lint` sin advertencias y `npm run build` con TypeScript, 67 módulos. Bundle aproximado 450 kB JS / 43 kB CSS; gzip 122/9 kB.
- Playwright general: cinco escenarios aprobados de identidad, sesión, administración, tema y configuración de flujos.
- Playwright de ejemplo: cuatro escenarios aprobados. Bandeja con dataset controlado de 225 tareas, filtros/paginación/vacío, recuperación contextual, importación y cambio obligatorio de contraseña, y circuito real T1→devolución→nueva ronda→firmas→evidencia PDF/HTTPS→T2→versión formal→cierre histórico. La comparación/aplicación/descarte y el fallo de redacción se comprobaron para T1 y T2 con respuestas controladas solo en el navegador; permisos, guardado, PostgreSQL, firma y revisión se mantuvieron reales.
- Inspección visual de capturas en claro/oscuro y escritorio/tableta; sin desbordamiento horizontal. Se corrigió la disposición del consentimiento y se espera la transición del tema antes de capturar.

Los primeros recorridos detectaron errores de fixtures/selectores y un límite de login alcanzado al repetir escenarios. Se corrigieron los fixtures de período/Mockito y los selectores por rol; se consultó el toast fuera del panel porque se renderiza en un portal. Para la repetición final solo se reinició la ventana temporal de login de las cuentas ficticias específicas en `utaped_dev`, con comprobación del nombre de DB. No se cambió la protección del aplicativo ni se borraron usuarios/documentos de ejemplo.

No se consumió una cuenta real de IA: los tests de transporte usaron servidores HTTP locales y los tests visuales respuestas controladas. No se acredita aún disponibilidad, costo o calidad de un modelo real, ni aceptación institucional definitiva. Las instrucciones solicitan preservar hechos, pero la propuesta requiere revisión humana.

## Configuración manual pendiente

La aplicación sigue funcionando sin cambios de variables. Para habilitar redacción: establecer `AI_WRITING_ENABLED=true`, `AI_WRITING_PROVIDER=GROQ` o `OPENAI`, `AI_WRITING_MODEL` con un modelo habilitado en la cuenta y únicamente la clave correspondiente `GROQ_API_KEY` / `OPENAI_API_KEY` en el entorno privado. Recrear backend y probar texto ficticio. [Guía y comandos](../development/writing-configuration.md).

Antes del uso institucional, revisar tratamiento de datos, cuotas, presupuesto y calidad real. [Contrato API](../api/writing-and-review-inbox.md), [ADR-0009](../architecture/adr/0009-writing-provider-abstraction.md), [estado actualizado](../../implementation-status.md).

## Entrega y siguiente implementación

Commits locales separados por backend, frontend y documentación. No push ni actualización del PR, conforme a la instrucción vigente del propietario. Backend: `60cd3c1` — `feat: add protected writing providers and paginated review inbox`. Interfaz: `2796b4d` — `feat(ui): add writing comparisons and modern review filters`. El commit de documentación se verifica mediante el historial de la rama.

Si el entorno habitual ya estaba levantado en Docker con una imagen anterior, reconstruirlo con `docker compose --profile web up -d --build` para cargar los nuevos endpoints. Aquí se reconstruyeron y probaron únicamente los proyectos aislados de pruebas/ejemplo, sin recrear el proyecto principal del propietario.

Siguiente tanda propuesta: aceptación amplia por roles con documentos largos y carga, alternativa accesible al CAPTCHA visual, y preparación operativa de HTTPS/SMTP/respaldo con restauración comprobada. Los pendientes institucionales de retención/reapertura, sustitución de ejecutores y administración adicional permanecen en el estado vigente. No se declara el producto listo para operación institucional únicamente por pasar estos tests.
