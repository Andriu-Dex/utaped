# Modal documental y ejemplos institucionales

Fecha: 2026-10-14. Rama: `fix/document-creation-options`.

## Decisiones del propietario

Crear documentos dentro de un modal; mantener los editores extensos en pantalla. Usar **Julio–Diciembre 2026** y las fuentes de `docs/templates` para los ejemplos. Requisitos UT-UI-111 y UT-DEV-111; complementan la regla de formularios por acción sin convertir todos los editores en modales.

## Cambios

`ActionForm` incorpora modalidad opcional mediante `<dialog>` y `showModal()`, aplicada exclusivamente a Crear documento. El fondo queda sin interacción, el desplazamiento de la página se suspende durante la apertura y el contenido puede desplazarse dentro del modal. Tab y Shift+Tab mantienen el foco; Escape y Cancelar respetan la confirmación de cambios y devuelven el foco al botón. No se cierra accidentalmente al pulsar el fondo. Se mantienen T1/T2, búsqueda de Planes aprobados, nombres propuestos y borradores conservados ante errores.

Las semillas dejan de depender del día de arranque. El período es 01/07/2026–31/12/2026 con nombre Julio–Diciembre 2026. Elaboración/revisión usan provisionalmente el semestre completo para que el escenario pueda probarse; no se presentan como fechas oficiales. Los formatos DOCX del producto permanecen sin cambios.

| Datos | Cantidad y fuente |
|---|---|
| Carreras | 3: Sistemas Computacionales e Informáticos, TI y Software; PDF de unidades/comisiones. |
| Grupos | 23: comisiones.xlsx, reconciliado con denominaciones del PDF. |
| Ámbitos | 30 combinaciones de grupo/carrera respaldadas por fuentes o denominación del club; los grupos sin carrera determinable permanecen sin ámbito supuesto. |
| Recursos y medios | 15 recursos y 11 medios tomados de la matriz del Plan de Titulación, páginas 4–5. |
| Actividades | 9 de la matriz de Titulación y 46 propuestas opcionales del Excel de mejoras, 14 hojas. Categorías POA/plan de mejoras/acción de mejora/otras conservadas. |
| Acceso | Las siete cuentas dev.* y sus contraseñas se conservan; pertenencias y revisores/aprobadores del escenario son ficticios. |

Fuentes revisadas: [catálogo de comisiones](../templates/comisiones.xlsx), [unidades/comisiones del período](../templates/UNIDADES%20Y%20COMISIONES%20JULIO_DICIEMBRE_2026_(SISTEMAS-SOFTWARE-TI).pdf), [Plan de Titulación firmado](../templates/TI-JD2026-UTIT-PLAN-V01-signed.pdf) y [propuestas de mejora](../templates/PROPUESTAS%20DE%20MEJORA%20CARRERA%20DE%20TI.xlsx). Se extrajo su texto/datos y se inspeccionaron páginas renderizadas relevantes. Las plantillas DOCX T1/T2 se revisaron como referencia de formatos.

El backend carga un JSON curado y tipado; no necesita leer Excel/PDF en tiempo de ejecución. [Procedencia, correspondencias y límites](../development/institutional-reference.md).

## Conservación de datos

Perfil development-seed y verificación obligatoria de base utaped_dev. Sin cambios en DB principal, archivos fuente o material firmado. La transición cambia únicamente las etiquetas genéricas originales que no están usadas por documentos; el período requiere además estar activo y sin flujos bloqueados. Los catálogos genéricos originales se retiran, sin borrar registros ni reconstruir matrices/PDFs.

Reiniciar no duplica semillas ni cambia contraseñas, denominaciones editadas, flujos deshabilitados o períodos bloqueados. No se importan personas reales como cuentas, firmas del PDF, resoluciones como políticas nuevas, evidencias o aprobaciones ficticias. Las propuestas de mejora son ideas de catálogo, no funcionalidades institucionales implementadas. Las fechas originales de actividad del PDF no se imponen a cada nuevo Plan.

Se conservan los registros adicionales creados previamente en sample por pruebas o revisión manual. No se realizó un reinicio destructivo del escenario.

## Validación

- `npm --prefix frontend run lint`: aprobado.
- `npm --prefix frontend run build`: aprobado, TypeScript y Vite.
- `DevelopmentSeedTests`: **2/2** aprobados. Verifica rechazo de una base incorrecta antes de escribir, carga real en PostgreSQL de pruebas, cantidades y fechas fijas, repetición sin duplicados, transición de etiquetas genéricas y conservación de modificaciones/flujos deshabilitados/período bloqueado. La base temporal se creó y eliminó exclusivamente dentro del servidor aislado de compose.test.yml.
- Playwright general: **7/7** aprobados.
- Playwright sample: **9/9** aprobados, incluidos modal/teclado, cancelación rechazada/aceptada, datos del período, dos ámbitos de Titulación, creación T1/T2, siete roles, importación y circuito completo con firmas/devolución/evidencias/versión/cierre.
- Capturas finales revisadas: modal en PC/claro y tableta/oscuro, sin contenido cortado ni desbordamiento horizontal. Capturas locales ignoradas en frontend/test-results.
- Consulta directa de sample: nombre Julio–Diciembre 2026 y fechas 2026-07-01/2026-12-31 confirmados.
- `git diff --check`: sin errores.

Las pruebas ahora cierran el modal antes de usar acciones del fondo (buscar/cerrar sesión/cambiar tema), respetando su comportamiento modal. El ciclo de repetición alcanzó límites de login y CAPTCHA del entorno de pruebas; se limpiaron únicamente las claves identificadas de cuentas dev.* y del proxy de sample, además de la cuenta ADMIN de E2E aislado. No se bajaron límites de seguridad ni se tocaron contadores del entorno principal. Hubo errores transitorios al recrear servicios durante una primera ejecución; la validación final se realizó con sample estable. No se atribuyen a esta entrega las 137 pruebas Java de entregas anteriores: aquí se ejecutaron las dos pruebas específicas de semillas y los 16 escenarios E2E.

## Uso

Backend/frontend de utaped-sample reconstruidos con estas semillas y el modal. Abrir `http://127.0.0.1:64431` y recargar. Sin variables nuevas ni migraciones. Las credenciales siguen en UTAPED_SEED_PASSWORD de .env.sample, ignorado por Git.

Para Vite local sobre estos mismos datos, desde frontend:

```powershell
$env:API_TARGET="http://127.0.0.1:18082"
npm run dev
```

También puede establecerse `API_TARGET=http://127.0.0.1:18082` en frontend/.env.local y reiniciar Vite. Esto elige el backend de sample, no el principal. No se cambió automáticamente la configuración privada local del propietario.

Semillas, procedencia y pruebas correspondientes en el commit local `f50dc0b`. Modal, pruebas de interfaz y este reporte en un commit separado de la misma rama. Sin push ni actualización del PR, conforme a la instrucción vigente.
