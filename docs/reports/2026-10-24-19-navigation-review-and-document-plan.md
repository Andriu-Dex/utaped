# Revisión de navegación y planificación documental — 2026-10-24

## Alcance del cierre

Revisión del asistente persistente, protección de borradores, acciones SVG de anexos y navegación inferior. Incluye los ajustes posteriores del propietario: menor separación visual y navegador PDF compacto, con salto de página por Enter o al abandonar el campo, límites y nombres accesibles. No incorpora todavía atajos de flechas.

Se conserva la navegación por página real, permisos, artefactos firmados y flujo de firma. No hay cambios de backend en este cierre.

## Próximo alcance propuesto, sin implementar

- Corregir el nombre abreviado de la unidad institucional a su denominación completa conforme a templates. Revisar catálogo, valores por defecto y documentos editables ya existentes; no modificar artefactos firmados ni cambiar unidades distintas mediante reemplazo global.
- Flechas izquierda/derecha dentro del visor enfocado del paso 5, con límites reales y sin capturar eventos de formularios, listas, modales o combinaciones modificadoras.
- Auditar saltos de sección y espacios frente al formato oficial. Mantener la matriz apaisada y los anexos originales. Aprovechar espacio entre bloques compatibles sin reducir tipografía ni alterar firmas, índices o referencias. No prometer continuidad entre orientaciones diferentes.

## Comprobaciones

Lint, TypeScript y build aprobados; permanece el aviso conocido de tamaño del bundle. Aceptación aislada: 21/21 pruebas aprobadas (1,8 minutos), incluido el circuito T1/devolución/firmas/evidencias/T2. Log: `backend/target/2026-10-24-wizard-review.log`. Revisada la captura de navegación persistente en tablet y modo oscuro.
