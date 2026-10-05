# Guía de reunión para confirmar decisiones de UTAPED

Fecha: 2026-10-05. Base: develop 248eeba, con PR #8 fusionado. Rama: docs/client-decision-guide. Entrega exclusivamente documental; aplicativo y mockup congelado sin modificaciones.

## Resultado

Se creó [client-decision-guide.md](../validation/client-decision-guide.md), listo para llevar a la reunión y registrar respuestas. Reúne las cuestiones Q-001–Q-019 y las desglosa en 132 preguntas concretas con ID único, información esperada, formularios y criterios para no confundir decisiones del proyecto con aprobación institucional.

Incluye titularidad/duplicados, asistente T1, flujos por grupo/tipo, paralelismo y regla de avance, observaciones/corrección/versiones, QIPOC, plazos/cierre, aceptación de firma, formatos oficiales, actividades/evidencias, T2, permisos/bajas, acceso/importación, datos/retención, infraestructura/respaldo, avisos/reportes, accesibilidad/piloto y las dos funciones distintas de IA. Código/Trello se consulta al propietario/equipo técnico, no se atribuye automáticamente al cliente institucional.

Se señalan decisiones ya establecidas para no reiniciar selección de stack, credenciales locales o mecanismo .p12/.pfx. Las preguntas de firma explican el alcance entregado y solicitan pronunciamiento sobre cadena/revocación y requisitos adicionales; no presuponen un servicio obligatorio. No se solicitan secretos ni datos personales innecesarios.

Contiene ficha de etapas por grupo/T1/T2, tabla de transiciones, matriz de permisos, plantilla por respuesta, checklist mínimo para desbloquear la siguiente implementación, lista de evidencias y registro de pendientes con responsable/fecha. Ninguna pregunta se declara resuelta y ninguna regla nueva se implementa.

## Validación y seguimiento

Revisión de requisitos, implementación, preguntas abiertas, catálogo, reconciliación, permisos, firma y alcance del agente. Se comprobaron cobertura de Q-001–Q-019, unicidad de IDs de preguntas, enlaces locales y formato con git diff --check. No se ejecutaron pruebas de aplicación: no cambió código, configuración ni migraciones.

Se actualizaron índice documental, referencia desde cuestiones abiertas, estado de implementación e índice de reportes. Se conserva el contenido de reportes anteriores y no se restauran archivos eliminados. Commit/push/PR hacia develop conforme a autorización permanente; revisión y fusión corresponden al propietario.

Siguiente paso: realizar la reunión, devolver la guía con respuestas/evidencias y actualizar requisitos/contratos/casos de aceptación antes de desarrollar las transiciones dependientes.
