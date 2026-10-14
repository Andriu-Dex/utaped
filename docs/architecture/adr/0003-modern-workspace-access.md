# ADR-0003 — Espacio de trabajo moderno y acceso local

Fecha: 2026-10-06. Estado: implementado en el alcance indicado.

## Contexto y decisión

El propietario sustituyó la interfaz heredada por un rediseño productivo y aportó el Plan Maestro. Se conserva React/TypeScript/Vite y se crea una base visual común, sin dependencias de Prototipos ni bibliotecas nuevas: tokens CSS, iconos SVG propios, navegación lateral, cabecera contextual, panel con datos reales, formularios y tablas coherentes, administración con accesos por sección y adaptación a escritorio/tableta/móvil.

Azul institucional como eje; verde azulado para acentos y confirmaciones, ámbar para advertencias, rojo para errores. Marca tipográfica UTAPED con monograma propio; no se inventa un escudo oficial. Los documentos institucionales mantienen su motor/plantilla y no reciben estilos de dashboard.

Se implementan username único normalizado, nombres y apellidos separados, CAPTCHA alfanumérico, Argon2id y restablecimiento ADMIN. El correo continúa como principal interno estable para sesiones históricas y recuperación; se admite como alias de acceso. No se cambia la identidad al seleccionar contexto. La columna existente `email` mantiene el significado de correo institucional: no se duplica como otra fuente de datos.

## Compatibilidad y seguridad

Flyway V12 agrega perfiles y retos sin modificar migraciones anteriores. Para datos existentes deriva usernames y evita colisiones; no separa nombres históricos por heurística. El alta nueva en UI exige nombres/apellidos. El contrato HTTP anterior con displayName sigue aceptándose para compatibilidad; los registros sin desglose se completan administrativamente. El bootstrap obtiene username a partir del correo. La migración no cambia contraseñas ni elimina cuentas.

Spring Security DelegatingPasswordEncoder escribe `{argon2id}` y sigue leyendo `{bcrypt}`. Argon2id: sal 16 bytes, salida 32, memoria 19 MiB, dos iteraciones y paralelismo 1. El proveedor DAO actualiza el hash tras credenciales correctas mediante comparación del hash previo; una modificación concurrente impide autenticar con credenciales obsoletas. [Documentación oficial de Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html). Los parámetros requieren medir capacidad antes de la puesta en producción; no constituyen una prueba de carga para 2.000 usuarios.

CAPTCHA: imagen PNG, seis caracteres sin símbolos ambiguos, generado en servidor, vigencia de tres minutos, binding aleatorio en sesión JDBC, respuesta almacenada únicamente como hash y consumo mediante DELETE RETURNING. Cualquier intento consume el reto; refrescar lo sustituye. No hay respuestas ni bypass en API, entorno o frontend. Se mantienen CSRF, limitación por IP y cuenta; username y correo comparten contador. Emisión limitada por IP. Las pruebas fijan una respuesta mediante acceso exclusivo a su BD aislada.

Restablecer acceso requiere ADMIN, cuenta activa, versión vigente y contraseña temporal diferente. Revoca sesiones y enlaces de recuperación, obliga cambio y audita actor/objetivo. No permite autorrestablecimiento administrativo; se usa el cambio normal. El cliente borra los campos temporales al finalizar el intento. Coordinadores no reciben permisos globales mientras no exista el nuevo modelo de ámbitos.

## Límites y próximas decisiones técnicas

Esta entrega no migra titularidad de Plan, carreras/pertenencias por período, workflow secuencial, firma→envío, T2 ni evidencias. Permanecen como entregas parciales y no se presentan como módulos completos. JDBC sigue en los módulos existentes; JPA/Hibernate, `/api/v1`, OpenAPI y Testcontainers indicados por el Plan Maestro requieren una transición explícita posterior. No se sustituye el renderizador T1 estable solo por preferencia tecnológica.

El siguiente bloque debe construir carreras, pertenencias por período/cargo y responsable principal, y trasladar autorización/unicidad de Plan a comisión/carrera/período conservando historial. Después, tareas secuenciales, devolución/rondas, firmas incrementales y transición atómica a revisión.
