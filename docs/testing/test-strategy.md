# Estrategia de pruebas

Estrategia productiva con pruebas ejecutables. Las pruebas del mockup sirven como referencia; no prueban backend/persistencia nuevos. [Aceptación por roles y documentos largos](role-and-document-acceptance.md) y [preparación operativa](../operations/production-readiness.md).

- Unitarias: fechas, responsables, Otro, transiciones, rondas/versiones y paginación.
- Integración: PostgreSQL de pruebas, transacciones/concurrencia, permisos por objeto, migraciones, archivos y contratos externos mediante dobles.
- E2E: T1 completo, devolución/corrección, T2 derivado, evidencias/reemplazo/validación, administración, histórico y sesiones.
- Documentales: originales T1/T2, A4, contenido largo, índices, slots e integridad de artefactos previos.
- Seguridad: accesos cruzados, roles revocados, archivos inválidos y ausencia de secretos en logs.
- Operación: instalación/actualización, recuperación conjunta DB/archivos y fallos de proveedores.
- UX: teclado, foco, errores, permisos, resoluciones y referencia visual aprobada.

Cada requisito entregado requiere evidencia reproducible y estado actualizado. Build exitoso no acredita flujo correcto. Datos de prueba ficticios y aislados.

Stack de pruebas vigente: JUnit/Spring Boot/PostgreSQL 18 aislado, lint/TypeScript/build y Playwright para navegador. Operación: unittest de Python y ensayos reales de backup/restore/HTTPS sobre recuperaciones ficticias. Evitar sleeps arbitrarios y pruebas que reflejen detalles internos. La espera de salud de servicios tiene límites explícitos y termina al estar disponible, sin pausas fijas para sustituir condiciones.
