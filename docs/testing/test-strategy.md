# Estrategia de pruebas

Propuesta productiva. Las pruebas del mockup sirven como referencia; no prueban backend/persistencia nuevos.

- Unitarias: fechas, responsables, Otro, transiciones, rondas/versiones y paginación.
- Integración: PostgreSQL de pruebas, transacciones/concurrencia, permisos por objeto, migraciones, archivos y contratos externos mediante dobles.
- E2E: T1 completo, devolución/corrección, T2 derivado, evidencias/reemplazo/validación, administración, histórico y sesiones.
- Documentales: originales T1/T2, A4, contenido largo, índices, slots e integridad de artefactos previos.
- Seguridad: accesos cruzados, roles revocados, archivos inválidos y ausencia de secretos en logs.
- Operación: instalación/actualización, recuperación conjunta DB/archivos y fallos de proveedores.
- UX: teclado, foco, errores, permisos, resoluciones y referencia visual aprobada.

Cada requisito entregado requiere evidencia reproducible y estado actualizado. Build exitoso no acredita flujo correcto. Datos de prueba ficticios y aislados.

Sin frameworks ni comandos seleccionados aún. Tras stack: registrar lint/tipos/unitarias/integración/E2E. Evitar sleeps arbitrarios y pruebas que reflejen detalles internos.
