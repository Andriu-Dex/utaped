# ADR-0001 — Stack y autenticación local

Estado: aceptado para el proyecto por delegación explícita del propietario. Fecha: 2026-10-02. No implica aprobación institucional de procedimientos pendientes.

## Decisión

Monolito modular: React 19.2.8 / TypeScript 6.0.2 / Vite 8.3.2; backend Java 21 y Spring Boot 4.1.1; PostgreSQL 18.6; Flyway para migraciones; Spring Security y Spring Session JDBC; Maven Wrapper 3.9.16 y Node 24. Pruebas JUnit con HTTP real/PostgreSQL aislado y Playwright 1.63.0. Oxlint para frontend. Versiones directas npm fijadas y lockfile conservado.

La selección preserva la dirección heredada y aporta separación modular, transacciones relacionales y control central de identidad. La aplicación institucional no requiere SEO/SSR; Vite permite una SPA sencilla. Spring JDBC facilita SQL explícito y evita agregar ORM antes de necesitarlo. Monolito y sesiones JDBC evitan microservicios, Redis y JWT innecesarios para esta primera aplicación web.

Java 21 se elige como plataforma estable dentro de los requisitos soportados, no por ser la versión más reciente. Las versiones se verificaron en registros oficiales y por compilación/ejecución; Initializr devolvió un sufijo RELEASE inexistente en Maven Central, corregido a 4.1.1 antes de compilar.

## Identidad

Credenciales locales CONFIRMADO-PROYECTO por petición del propietario. No hay registro público. Bootstrap de administrador solo con DB sin usuarios y secretos externos; usuarios creados por administrador con contraseña temporal.

Contraseñas mediante DelegatingPasswordEncoder y bcrypt; mínimo 12 caracteres y límite de 72 bytes UTF-8, restricción técnica de la opción elegida. Sesión JDBC de 30 minutos, cookie HttpOnly/SameSite=Lax y Secure por defecto fuera de Compose local. CSRF activo en login y mutaciones; rotación de sesión gestionada por Spring Security. Cambio/reset revoca sesiones.

Permisos ADMIN/USER del sistema separados de MEMBER/COORDINATOR por grupo. Roles/contexto no cambian identidad. Backend verifica usuario activo y privilegios actuales en cada petición; coordinador no obtiene administración global.

## Consecuencias

PostgreSQL también persiste sesiones, auditoría y hashes de recuperación. Archivos documentales y firma real quedan fuera de esta entrega. Recuperación asíncrona con SMTP: Mailpit local captura mensajes; producción debe configurar SMTP institucional y TLS. Tokens aleatorios de 256 bits, hash SHA-256 persistido, expiración de 20 minutos y un solo uso. Cola de recuperación acotada en memoria; no es una cola durable. Se puede solicitar un enlace nuevo tras una interrupción.

Límites técnicos iniciales: login 10 intentos por correo / 15 minutos y 100 por dirección remota; recuperación 3 por correo y 30 por dirección; reset 30 por dirección. Revisar capacidad y proxy de confianza antes de publicación institucional. No sustituir estas protecciones por el CAPTCHA simulado.

## Fuentes primarias consultadas

- [Spring Boot: compatibilidad Java](https://docs.spring.io/spring-boot/system-requirements.html).
- [Spring Security: PasswordEncoder](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/password-encoder.html) y [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).
- [React: versiones](https://react.dev/versions) y [Vite: guía](https://vite.dev/guide/).
- [PostgreSQL: versiones soportadas](https://www.postgresql.org/support/versioning/).
- [Flyway](https://documentation.red-gate.com/flyway) y [Playwright](https://playwright.dev/docs/intro).

La conveniencia de esta combinación es una decisión de ingeniería del proyecto; las fuentes acreditan capacidades/compatibilidad, no una clasificación universal de herramientas.
