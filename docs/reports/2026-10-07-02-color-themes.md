# Modo claro y oscuro para PC y tablet

Fecha: 2026-10-07. Rama: `feature/modern-workspace-access`. Entrega local; por instrucción del propietario se realiza únicamente commit, sin push ni actualización del PR.

## Implementación

- Botón flotante inferior derecho, visible durante el acceso, carga y sesión autenticada. Muestra únicamente luna/sol; el texto «Modo oscuro»/«Modo claro» describe la acción disponible en tooltip al pasar el puntero o enfocar con teclado. Nombre accesible independiente del icono, botón de 48 px y foco visible.
- Preferencia `utaped-theme` en localStorage. Sin elección válida se usa la preferencia del sistema y se siguen sus cambios. La selección manual prevalece, se conserva al recargar y se sincroniza entre pestañas. Si el almacenamiento está bloqueado se permite cambiar en la pestaña actual.
- Inicialización del tema antes del montaje de React para evitar iniciar con una elección distinta de la guardada. El cambio visual no remonta formularios ni modifica identidad, credenciales o contexto.
- Paleta azul/gris oscuro aplicada a fondos, tarjetas, campos, tablas, enlaces, estados, administración, navegación y toast. Se mantienen colores semánticos y contraste entre texto/superficie.
- CAPTCHA y páginas institucionales conservan sus colores, sin filtros de inversión ni cambios de contenido. El tema no regenera PDFs ni altera artefactos firmados. Selector y toast ocultos al imprimir.
- Se reserva espacio inferior y junto al pie para el botón. No se agregan breakpoints ni estilos específicos de celular; destinos de esta entrega: PC y tablet. Las reglas y comprobaciones históricas de celular permanecen sin convertirse en requisito de esta entrega.

## Validación

- `npm --prefix frontend run lint`: aprobado sin advertencias.
- `npm --prefix frontend run build`: aprobado (TypeScript/Vite).
- Cinco escenarios Playwright completos contra Docker/Nginx y PostgreSQL aislados: regresiones de identidad, documentos/firma, flujos y administración; dos escenarios nuevos dedicados a temas.
- Comprobaciones de preferencia del sistema, teclado/tooltip, persistencia, sincronización entre pestañas, almacenamiento no disponible, campos conservados, toast oscuro, impresión sin selector y previsualización T1 original sin inversión.
- Capturas revisadas en PC 1440×1000 y tablet 820×1180; login, espacio autenticado y documento institucional. Sin desbordamiento horizontal en las comprobaciones de tablet/espacio de trabajo.
- Pruebas en puertos separados 28081/28080/25433/28025/21025. No se modificaron datos ni se detuvo el Docker del propietario. No se ejecutó nuevamente la suite backend: no hay cambios backend en esta entrega. Sin CI remoto nuevo porque no se publica la rama.

Las verificaciones no constituyen una auditoría completa de accesibilidad. El documento T2 conserva su formato por no intervenir el motor ni aplicar filtros a las páginas; todavía no se entrega su flujo productivo.

## Configuración y pendientes

No se requieren variables de entorno, migraciones ni servicios nuevos. Vite refleja los cambios; para Docker reconstruir la interfaz con `docker compose --profile web up -d --build --no-deps frontend`.

Se conservaron fuera del commit los cambios previos del propietario en `.gitignore`, `backend/src/main/resources/application.yml` y `frontend/src/shared/Toast.tsx`/`Toast.css`. La nueva capa de tema adapta los toast sin sobrescribir esos cambios.

La continuidad de sesión después de cambiar la contraseña temporal, los datos semilla y los próximos módulos funcionales no forman parte de esta entrega visual.
