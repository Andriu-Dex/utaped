# Preparación de entrega amplia y revisión de interfaz

Fecha: 2026-10-11. Rama: `feature/modern-workspace-access`.

## Recomendación

Ampliar la próxima entrega hasta un circuito documental integrado, con varios commits por capacidades y una validación de aceptación de extremo a extremo. No aplazar todas las pruebas al final: verificar permisos, persistencia, transiciones y artefactos en cada bloque permite detectar fallos antes de construir módulos dependientes. Después reservar una etapa específica para pruebas integrales del cliente, usabilidad y correcciones.

Esta preparación no implementa esos módulos ni acredita que el producto esté completo. Las decisiones institucionales pendientes y la puesta en producción permanecen diferenciadas de una versión funcional de desarrollo.

## Revisión del staged del propietario

Se revisaron los cambios de `Access.tsx`, `Administration.tsx`, `DirectoryAdministration.tsx` y `WorkPlans.tsx`, además de sus estilos compartidos. Los documentos staged corresponden a la especificación y revisión anteriores; no son cambios de interfaz.

| Área | Referencia visual que se conservará |
|---|---|
| Acceso y contraseña | Controles de visibilidad, etiquetas explícitas, iconos accesibles y estructura de seguridad con jerarquía. |
| Grupos y pertenencias | Tarjetas independientes para crear, asignar y gestionar; encabezados con icono, texto contextual, campos agrupados y ayudas. |
| Períodos | Separación visual del ciclo académico y las ventanas operativas; historial legible con fechas y etiquetas. |
| Edición de perfiles | Encabezado contextual, cierre accesible, acciones diferenciadas y separación visual del área de guardado. |
| Documentos | Títulos destacados, filtros organizados, estados vacíos explicativos y acciones con icono y texto. |

Las nuevas pantallas seguirán esta dirección: azul institucional y colores semánticos, tarjetas con propósito, espacios consistentes, jerarquía clara, estados visibles y acciones fáciles de localizar. Reutilizar los tokens, iconos y patrones existentes; extraer estilos repetidos cuando aporte consistencia sin rehacer el diseño aprobado. Mantener PC/tableta, ambos temas, navegación por teclado y documentos institucionales independientes de los colores de la interfaz.

## Ajustes detectados para la implementación

- **Carga de integrantes:** al cambiar de grupo se vacía `members` antes de terminar la consulta. El nuevo texto «Este grupo no tiene integrantes» puede aparecer durante la carga. Distinguir cargando, vacío real y error.
- **Estado documental y permiso:** la bandeja sustituye el estado por «Solo lectura» cuando `editable` es falso. Presentar por separado el estado real y el permiso; será especialmente necesario al incorporar revisión y aprobación.
- **Creación de usuario, comportamiento preexistente:** el formulario se oculta inmediatamente después de llamar a `submit`, sin esperar su resultado. Un error puede ocultar los datos; cancelar también requiere revisar la protección de cambios. Corregir conservando la presentación actual.
- **Contraseñas:** en recuperación, nueva contraseña y confirmación comparten la visibilidad; en cambio de contraseña ya son independientes. Unificar la interacción para evitar sorpresas.
- **Movimiento reducido, estilo preexistente:** la regla compartida desactiva transiciones, pero no la animación `reveal-card`. Incluir las animaciones en la preferencia de movimiento reducido.
- **Etiquetas:** «Registrado» no equivale a período activo. Cuando se implemente el ciclo de períodos, mostrar estado temporal real y ventanas sin inferirlo del color de la etiqueta.

Los estilos de tema oscuro ya incluyen reglas para estados vacíos y etiquetas; no se concluye que estos componentes carezcan de soporte oscuro. La apariencia, contraste y composición final requieren revisión en navegador.

## Alcance preparado para la próxima tanda

| Orden / bloque | Resultado esperado | Verificación necesaria |
|---|---|---|
| 1. Reconciliación y organización | Aplicar decisiones posteriores de UI/toast/temas; carreras, cargos, pertenencias por ámbito y período, responsable principal; titularidad y unicidad organizacional de Planes. | Acceso autorizado y denegado, aislamiento entre ámbitos, historial y creación concurrente. |
| 2. Datos de desarrollo | Semillas reproducibles de usuarios ficticios, integrantes, revisores, aprobadores, catálogos, períodos y flujos; reinicio explícito del entorno de desarrollo. | Repetibilidad, escenarios completos y ausencia de ejecución automática destructiva en producción. No borrar ahora la base existente. |
| 3. T1 y revisión completos | Asistente completo, responsables múltiples, anexos, artefacto firmado, asignación secuencial, bandejas, observaciones con resaltado, devolución/corrección/rondas y aprobación. | Circuito completo por roles, restricciones de firma, bytes firmados inmutables, tareas futuras sin acceso indebido y prevención de doble procesamiento. |
| 4. Ejecución y evidencias | Actividades, PDF por medio requerido, reemplazos e historial, vencimientos y extensión configurable según lo confirmado. | Archivos privados, límites, concurrencia, fechas de servidor y versiones de evidencia independientes. |
| 5. T2 | Informe derivado del Plan aprobado, flujo propio, evidencias y documento institucional, firmas y aprobación. | Identidades independientes, conservación del Plan, estructura T2 y métricas que no confundan evidencia con ejecución física. |
| 6. Cierre funcional | Consulta histórica, archivo lógico, reportes reales, notificaciones y capacidades administrativas restantes con diseño consistente. | Permisos históricos, filtros, totales verificables y recorrido integrado T1→ejecución→T2. |

Los bloques constituyen una meta amplia; no deben declararse entregados por existir pantallas o tablas. Cada commit deberá representar una capacidad verificada. Se conservarán migraciones versionadas y mecanismos productivos existentes, aunque los datos de desarrollo sean reemplazables.

La integración de asistencia de redacción y el agente interno de inspección de código/Trello conservan su alcance separado y sus dependencias de proveedores/accesos. SMTP institucional, TLS, respaldo/restauración y aceptación institucional de firma siguen siendo condiciones de operación; no se sustituyen con datos de ejemplo. Las políticas todavía pendientes se mantendrán explícitas y configurables donde sea viable.

## Comprobaciones realizadas y límites

- `npm --prefix frontend run lint`: correcto.
- `npm --prefix frontend run build`: correcto, incluyendo TypeScript.
- Revisión estática de los cuatro diffs staged y estilos compartidos.
- No se ejecutaron E2E ni inspección visual en navegador en esta preparación; no se acredita validación visual definitiva ni funcionamiento completo de los formularios.
- Sin cambios de código, datos, variables de entorno, staging, commits, push ni pull request. Se añadió este reporte y su entrada de índice; los cambios del propietario permanecen intactos.
