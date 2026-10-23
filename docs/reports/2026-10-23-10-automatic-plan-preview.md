# Previsualización automática del Plan T1

## Comportamiento

Al abrir el paso 5 se consulta la preparación y se muestra el artefacto vigente. Si falta o está desactualizado, se genera automáticamente solo para borradores editables que cumplen las validaciones. Durante la operación aparece Preparando documento; los errores ofrecen Reintentar previsualización sin bucles automáticos.

Los pendientes tienen accesos a Información general, Contenido, Actividades y matriz o Anexos cuando corresponda. Las secciones adicionales conservan su editor. El avance a Firma requiere un artefacto actual, datos completos y ausencia de operaciones o cambios pendientes.

Se redujo el texto técnico superior y se eliminó la tarjeta de generación manual del paso 5. El selector histórico aparece solo cuando hay más de una previsualización. Firma, T2 y T3 conservan sus controles propios. No hay cambios de backend, firmas, artefactos históricos ni contratos.

Se preservan los ajustes visuales previos staged sin hacer commit de ellos.

## Validación

- Lint y compilación TypeScript/Vite aprobados; persiste la advertencia anterior de tamaño del bundle.
- Suite aislada de aceptación: 18 pruebas aprobadas, incluyendo generación real T1, devolución/nueva ronda, firmas secuenciales, T2/T3, permisos y navegación con pendientes.
- Prueba focalizada adicional aprobada: error de generación, reintento, reutilización sin POST adicional, documento de solo lectura con artefacto marcado desactualizado y recuperación de fallo de consulta.
- La prueba de transporte controlado verifica las decisiones de la interfaz; el circuito completo verifica la generación y firmas reales en el entorno desechable. No se realizó revisión visual manual adicional.
- Entorno de aceptación eliminado tras las pruebas; datos manuales conservados. Sin commit ni push.
