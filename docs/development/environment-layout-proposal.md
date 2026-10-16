# Propuesta de organización de entornos locales

Fecha: 2026-10-16. Estado: **PROPUESTA para acordar con el propietario**. No cambia Compose ni detiene/elimina recursos.

## Decisión recomendada

Un solo entorno persistente para desarrollo y revisión manual, con los datos institucionales de ejemplo y cuentas ficticias. La aplicación y el código son los mismos; no necesitamos mantener simultáneamente dos aplicaciones locales para distinguir «sample» de «normal».

Usar por ahora el actual utaped-sample como ese entorno, porque ya contiene las semillas solicitadas. El nombre futuro puede ser utaped-dev. Un cambio de nombre de proyecto Compose no debe hacerse sin mapear o migrar expresamente sus volúmenes y configuración: por defecto otro nombre crea almacenamiento distinto.

| Uso | Organización recomendada | Cuándo activo |
|---|---|---|
| Desarrollo y revisión manual | Un proyecto persistente, hoy utaped-sample. DB, backend y Mailpit; frontend local Vite. | Trabajo diario: 3 contenedores. |
| Revisión totalmente Docker | Mismo proyecto y datos, añadiendo frontend compilado. | Cuando se pruebe la distribución completa: 4 contenedores. |
| E2E / integración / aceptación con semillas | Proyectos y bases desechables separados del entorno manual. Misma aplicación, configuración de pruebas. | Solo durante el comando de pruebas; detener al terminar, conservar evidencia y limpiar recursos temporales de forma controlada. |
| Ensayo de restauración | Proyecto temporal nuevo utaped-restore-* y volúmenes independientes. | Solo durante el ensayo; no mantener copias antiguas encendidas. |
| Despliegue institucional | Configuración de producción en el servidor de destino, sin semillas y con servicios internos/configuración HTTPS/SMTP apropiados. | Al preparar el despliegue. No necesita otra copia permanente encendida en el PC. |

Los tres contenedores del desarrollo con Vite cumplen funciones distintas: PostgreSQL conserva los registros; el backend aplica permisos y reglas; Mailpit captura correos de prueba. Vite se ejecuta como proceso local. Mailpit no representa el proveedor institucional de correo. El frontend Docker y Vite son alternativas de ejecución del frontend, no aplicaciones diferentes.

## Inventario observado

Se observaron siete proyectos Compose relacionados con UTAPED: utaped, utaped-sample, utaped-e2e, utaped-tests y tres utaped-restore-* fechados 20261012. Las filas agrupadas de Docker Desktop representan proyectos, cada uno con varios contenedores.

En el momento de inspección había 24 contenedores UTAPED activos y un frontend de restauración detenido con exit 1. Los tres backends de restauración consumían aproximadamente 1.1 GiB de memoria en conjunto durante la medición, sin necesidad para revisar la interfaz. CPU/memoria varían durante arranque y generación documental; la captura de CPU no demuestra por sí sola un problema funcional.

Las políticas de reinicio inspeccionadas eran `no`; no se atribuye el arranque simultáneo a una política automática configurada por Compose. No se revisaron configuraciones privadas de Docker Desktop. Otros proyectos del equipo quedan fuera de alcance.

## Corrección operativa necesaria

Actualmente tests/sample usa utaped-sample y deja grupos, carreras, períodos y documentos de prueba en la misma base que se usa manualmente. Esos registros explican nombres como «Ámbito recuperación …». Aislar las bases solo entre utaped y sample no evita esta mezcla.

La siguiente reorganización debe mover esa suite a un entorno de aceptación desechable, parametrizar proyecto/puertos/conexión de sus fixtures y verificar que ninguna prueba escriba en el entorno manual. Mantener las semillas trazables de docs/templates y conservar los cambios del propietario. No ejecutar las pruebas destructivas contra una base de revisión o institucional.

## Secuencia para aplicar la propuesta

1. Identificar qué datos conservar de utaped y sample, sin unir sus tablas o volúmenes automáticamente.
2. Respaldar DB y almacenamiento documental antes de depurar datos o reasignar volúmenes.
3. Elegir el entorno manual único y alinear Vite, backend, URL y credenciales para evitar conexiones a bases distintas por accidente.
4. Aislar toda la automatización, con limpieza al terminar y opción explícita de conservar un entorno fallido para diagnóstico.
5. Detener copias auxiliares fuera de uso; eliminar las restauraciones temporales solo después de verificar la evidencia y los datos que deben conservarse. No usar una purga global de Docker.
6. Documentar comandos simples para desarrollo local, distribución Docker, pruebas y restauración. El puerto institucional de despliegue sigue siendo 64430; el cambio de puertos locales se realizará junto con la configuración, sin colisiones.

No se ejecutó esta reorganización en la revisión visual del 2026-10-16. La separación de datos de pruebas sí es necesaria; la duplicación permanente de aplicaciones locales no lo es.
