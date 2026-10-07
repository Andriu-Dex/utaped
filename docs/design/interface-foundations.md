# Base de interfaz productiva

Fecha: 2026-10-06. Decisión del propietario: modernizar toda la interfaz, conservando el azul como color principal y los procesos aprobados. Este documento describe la base implementada; no sustituye la validación visual del cliente.

## Fundamentos

| Elemento | Implementación |
|---|---|
| Identidad | UTAPED / Gestión Documental Académica. Monograma propio de la aplicación, sin escudo institucional inventado. |
| Color principal | Azul oscuro de navegación `#0c2944`; acción principal `#175f94`; superficies claras. |
| Acentos | Verde azulado `#177369`, confirmaciones verdes, advertencias ámbar y error/destrucción rojo. |
| Tipografía | Segoe UI / fuentes de sistema; sin servicios de fuentes externos. |
| Componentes comunes | Tokens en `frontend/src/styles/tokens.css`, Icon/Brand, tarjetas, tablas, filtros, badges, avisos, campos y paginación. |
| Escritorio | Menú lateral persistente, cuenta/identidad visible, cabecera contextual y contenido con jerarquía de secciones. |
| Tableta/móvil | Menú compacto visible, cuadrículas adaptables y scroll interno de tablas extensas; nunca ocultar acciones esenciales. |
| Movimiento | Transiciones discretas; respetar prefers-reduced-motion. |

## Pantallas y comportamiento

- Acceso: historia visual propia con ilustración CSS, usuario, contraseña con control de visibilidad, CAPTCHA real, recuperación y confirmación de contraseña nueva. Sin registro público ni indicadores ficticios.
- Inicio: bienvenida, acceso a documentos, contadores reales, recientes, filtros, grupos/integrantes y períodos. Cuatro períodos iniciales con expansión; se conserva el listado completo. Los estados vacíos no simulan actividad.
- Documentos: conserva edición, matriz, anexos, preparación, firma e historial existentes con estilo común. El asistente completo T1 de seis pasos, las carreras relacionales y las transiciones definitivas se implementarán en los siguientes módulos.
- Administración: accesos por sección para personas/grupos, períodos, directorio, planificación, flujos, auditoría y certificados. Alta con nombres/apellidos/usuario, perfiles y restablecimiento explícito con confirmación.
- Notificaciones/seguridad: integración en navegación y cabecera, bandeja privada y cambio de contraseña con estados de proceso/error.

## Accesibilidad y seguridad del estado

Labels, nombres accesibles en botones iconográficos, estado activo con aria-current, enlace de salto al contenido, foco visible y campos de contraseña separados del botón de visibilidad. El contexto no cambia la identidad autenticada. Se conserva la confirmación de abandono de cambios.

El nuevo reto usa una URL única por emisión, también tras cerrar sesión, para evitar reutilizar imágenes decodificadas del navegador. El login aparece después de renovar CSRF; se evita crear simultáneamente sesiones anónimas desde la imagen y el token. Respuestas de actualización anteriores al logout no restauran una identidad antigua.

No se afirma conformidad WCAG completa: el CAPTCHA visual necesita una alternativa accesible acordada con la institución. Tampoco se rediseña el contenido formal de los PDFs T1/T2 mediante estos estilos.

## Validación visual

Playwright cubre las pantallas actuales y capturas en `frontend/test-results/` (ignoradas por Git): acceso, inicio, escritorio/tableta/móvil, matrices, preparación, firma, historial y auditoría. Las capturas con cuentas y catálogos de prueba no se presentan como información institucional real. La validación de estética por el cliente sigue siendo una revisión visual, separada de las pruebas funcionales.

## Avisos toast (2026-10-07)

Los errores, advertencias y confirmaciones operativas usan Toast en una capa común adaptable, sin desplazar el formulario. Error/advertencia persisten hasta cierre o cambio de estado; éxito dura ocho segundos y pausa con puntero/foco. Botón de cierre accesible, roles alert/status y movimiento reducido. Se conservan la validación junto a campos, los estados de carga y las confirmaciones que protegen cambios o acciones sensibles.
