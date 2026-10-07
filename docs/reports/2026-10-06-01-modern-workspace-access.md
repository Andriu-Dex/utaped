# Entrega: espacio de trabajo moderno y acceso local

Fecha: 2026-10-06. Rama: `feature/modern-workspace-access`. Base: `develop`, 248eeba, PR #8 fusionado. Entrega por bloques; revisión y fusión del PR a cargo del propietario.

## Resultado

Se inicia la implementación del Plan Maestro y del nuevo diseño autorizado. Esta entrega reúne rediseño transversal de las pantallas actuales, acceso productivo y administración de credenciales. No declara terminado todo UTAPED ni convierte la referencia Prototipos en dependencia de producción.

| Bloque | Implementado |
|---|---|
| Fuentes | Plan Maestro y seis documentos institucionales incorporados; precedencia reconciliada en AGENTS, requisitos y decisiones pendientes. No se recuperó el reporte de auditoría eliminado ni client-decision-guide.md. |
| Diseño | Sistema visual propio con tokens CSS, iconos SVG/monograma, azul principal y acentos complementarios. Login, navegación lateral, cabecera, inicio, formularios, tablas, administración, notificaciones y seguridad coherentes. |
| Panel | Datos reales, filtros, recientes, estados vacíos, contexto de integrantes y períodos expandibles; sin cifras de aprobación ficticias. |
| Perfiles | Username separado del correo, normalizado y único; nombres/apellidos para altas nuevas; búsqueda y edición administrativa; nombres históricos conservados sin separarlos artificialmente. |
| Contraseñas | Argon2id en nuevas escrituras; compatibilidad bcrypt con actualización tras login correcto. Comparación del hash anterior evita actualizar o aceptar credenciales cambiadas concurrentemente. |
| CAPTCHA | PNG generado/validado en backend, seis caracteres, TTL de tres minutos, vinculado a sesión, respuesta con hash y consumo atómico de un solo uso. Renovación sin borrar usuario/contraseña; conserva CSRF y throttling. |
| Restablecimiento | ADMIN, cuenta activa y versión vigente; contraseña temporal diferente, cambio obligatorio, revocación de sesiones/enlaces y auditoría. Confirmación UI y limpieza de campos sensibles. |
| Sesiones | Nuevo login espera CSRF antes de cargar CAPTCHA; solicitudes CSRF simultáneas comparten petición. Las respuestas previas al logout no restauran una identidad anterior. Cuenta revocada permite volver a iniciar sesión con otra identidad. |

Flyway V12 agrega perfiles y retos sin alterar migraciones anteriores. Mantiene correo como principal interno estable y alias compatible de acceso. Se usan dependencias existentes; no se añaden paquetes frontend ni servicios externos para el CAPTCHA. La imagen cambia de URL en cada emisión para evitar reutilización de imágenes decodificadas después de cerrar sesión.

## Verificación

- `npm --prefix frontend run lint`: aprobado.
- `npm --prefix frontend run build`: aprobado; TypeScript y compilación Vite.
- Suite backend en `compose.test.yml`, PostgreSQL aislado `utaped_test`: **59 pruebas, cero fallos/errores/omitidas**. Incluye sesión/CSRF/permisos, CAPTCHA ausente/incorrecto/vencido/renovado/otra sesión/concurrencia, alias con contador compartido, migración bcrypt, perfiles, reset/revocación y regresión documental/criptográfica.
- Playwright contra la aplicación construida en Docker/Nginx: **3 escenarios aprobados**. Cobertura de alta/administración, contexto/aislamiento, recuperación SMTP, creación/edición/concurrencia T1, matriz/Otro, anexos, PDF/preparación/firma/historial, auditoría/exportación, notificaciones, flujos configurables y acceso/reset nuevos.
- E2E adicional de respuesta administrativa retrasada durante logout: no recupera la identidad anterior. Renovación conserva usuario/contraseña y borra solamente código; mostrar/ocultar contraseña funciona.
- Regresión de notificación de pertenencia al grupo ya seleccionado: conserva el contexto y muestra integrantes, sin quedar en carga indefinida.
- Capturas revisadas de acceso y espacio de trabajo en 1440×1000, tableta 820×1180 y móvil 390×844; sin desbordamiento horizontal en los escenarios comprobados. Se conservaron verificaciones móviles de los módulos documentales existentes. Capturas locales ignoradas en `frontend/test-results/`.
- Se corrigieron cuatro fallos previos de pruebas que usaban CURRENT_DATE UTC para comparar con fechas civiles America/Guayaquil. Los fixtures ahora expresan la zona explícitamente; se mantuvieron las aserciones de ventanas/autorización.

Los fallos intermedios de navegador permitieron corregir reutilización de imágenes, creación paralela de sesión/CSRF y selectores ambiguos al añadir navegación. No se deshabilitaron pruebas ni se retiraron controles para hacerlas pasar. La inspección visual y los E2E no constituyen auditoría WCAG completa ni prueba de carga institucional.

## Configuración manual

**No hay variables de entorno nuevas obligatorias para esta entrega.** V12 se aplica automáticamente al iniciar la versión actualizada. En un entorno con datos, realizar respaldo habitual antes de actualizar y revisar los usernames migrados en el directorio. No volver a crear cuentas ni cambiar contraseñas para migrar bcrypt.

El propietario revisa y fusiona el PR hacia develop. No se modifica main ni se despliega. SMTP institucional, HTTPS, dominio, proxy y respaldos continúan como configuración de publicación; Mailpit solo valida el correo local. Debe verificarse la identidad de IP cliente en el proxy de producción para que la limitación por IP no agrupe a toda la institución.

## Alcance pendiente y siguiente implementación

1. Carreras, pertenencias por período/cargo, responsable principal y autorización por ámbito. El PDF institucional proporciona parte de las asignaciones, pero no todos los usuarios/correos/principales ni el flujo de firmas; no se importaron permisos por suposición.
2. Migrar Plan a comisión/carrera/período, unicidad con familias de versiones y archivo de borradores. Los borradores actuales siguen siendo individuales: no se afirma que ya cumplan esa nueva titularidad.
3. Workflow secuencial con todos los participantes, tareas/observaciones/devolución/rondas, múltiples firmas y firma→envío atómico. La firma aislada existente no ejecuta revisión ni aprobación.
4. Ejecución/evidencias/prórrogas e Informe T2 derivado; anexos de otros formatos y decisiones pendientes de representación.
5. Transición técnica a JPA, API v1/OpenAPI y Testcontainers; IA de redacción y agente código/Trello; operación y manuales completos.

Coordinadores no reciben reset global: se habilitará solo con sus nuevos ámbitos. Falta una alternativa institucional accesible al CAPTCHA visual y la revisión estética final del cliente. Los formatos T1/T2 no se rediseñaron con estilos web.

Contratos: [acceso local](../api/local-access.md), [ADR-0003](../architecture/adr/0003-modern-workspace-access.md), [base visual](../design/interface-foundations.md). Las decisiones históricas contradichas por el Plan Maestro no se mantienen como bloqueo universal.

## Commits de implementación

- `725ce4d`: Plan Maestro, insumos institucionales, precedencia y contrato de acceso/interfaz.
- `b37877c`: perfiles, Argon2id, CAPTCHA, reset administrativo y pruebas backend.
- `b0cee27`: interfaz moderna, integración de cuentas, navegación/CSRF y escenarios E2E.
- La actualización de estado, índices y este reporte se registra en un commit documental final de la misma rama.
