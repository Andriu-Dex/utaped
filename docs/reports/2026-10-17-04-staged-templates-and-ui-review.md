# Revisión del staged: plantillas e interfaz documental

Fecha: 2026-10-17. Rama: `feature/simultaneous-document-preview`.

## Objetivo y alcance

Revisar los 30 archivos que el propietario había dejado en staged, conservar su modernización visual, corregir problemas comprobables y registrar los cambios mediante commits locales. Incluían funcionalidad nueva de plantillas administrables, además de cambios de interfaz; no eran exclusivamente CSS. La revisión se realiza sobre UTAPED productivo. Prototipos permanece congelado.

## Cambios revisados

- Backend: configuración persistente de justificación T1, migración Flyway V23, endpoints ADMIN, control de concurrencia, auditoría, precarga al crear Planes y opciones del editor.
- Administración: nueva pantalla Plantillas, edición por acción explícita, cancelación protegida, restauración y mensajes Toast.
- Navegación: menú lateral plegable con preferencia local, iconos y nombres accesibles. No se guardan credenciales en navegador.
- Documentos: modernización del editor T1, matriz, campos T2, modos editar/vista/dividida, previsualización y navegación compacta por páginas.
- Redacción asistida: estudio modal con comparación, propuesta editable y aplicación explícita al borrador.

## Problemas corregidos antes de los commits

1. **Contenido histórico sustituido al leer:** el backend y el editor completaban automáticamente las justificaciones vacías con la configuración vigente. Eso podía mostrar como guardado un texto inexistente en el documento y alterar su interpretación histórica. Se retiraron ambas sustituciones. Solo se precargan Planes nuevos; restaurar en el editor es una acción explícita que requiere guardar.
2. **Modal de redacción sin aislamiento real de foco:** se convirtió en un dialog modal nativo, integrado con la contención de foco compartida. Se protege el cierre con cambios, incluso cuando se borra toda la propuesta, y se conserva el orden de confirmaciones anidadas/Escape. Recargar con una propuesta editada activa la protección de salida.
3. **Pérdida de propuesta editada:** cerrar, descartar o solicitar otra sugerencia exige confirmar antes de perder modificaciones locales. Cancelar conserva el texto. Se limita la propuesta a 50 000 caracteres antes de aplicarla.
4. **Grupos de matriz sin nombre accesible válido:** se corrigieron los títulos de fieldset, conservando el diseño y la selección de responsables, recursos y medios.
5. **Ampliación de vista obsoleta mediante teclado:** Enter/Espacio respetan ahora las mismas restricciones de carga/actualidad que el ratón.
6. **Menú plegado:** Cerrar sesión conserva nombre accesible aunque el texto visual esté oculto.
7. **Navegación compacta:** se recuperó el salto numérico directo por página, con validación, además de anterior/siguiente y referencias.
8. **Alcance de Plantillas:** se aclaró que administra únicamente el texto inicial T1; no modifica las plantillas oficiales A4 ni documentos existentes.

## Validación

- Revisión de diferencias staged y correcciones finales; comprobación de whitespace antes de commits.
- Backend completo en PostgreSQL desechable: 146 pruebas aprobadas. Posteriormente se ejecutaron los dos casos de DocumentTemplateIntegrationTests, incluido el nuevo caso que conserva un documento vaciado, aplica la plantilla solo al crear y rechaza mutaciones no ADMIN. Los XML resultantes reúnen 147 casos distintos en 22 suites, cero errores/fallos; no se contabiliza la repetición como casos adicionales.
- Frontend: lint y compilación TypeScript/Vite aprobados.
- Aceptación: 13 escenarios aprobados en la ejecución confirmada, con roles, T1/T2 independientes, devolución/rondas, firmas, evidencias, versiones, cierre, plantillas y vista simultánea. La repetición final, incluyendo cancelación al solicitar otra sugerencia con texto editado, también aprobó los 13 escenarios.
- E2E generales: ocho escenarios aprobados, 33,3 segundos. El lanzador original no pudo completar npm ci por EPERM en un binding de Vite en uso en Windows. Se recuperaron las dependencias con npm install --ignore-scripts (sin cambios en package.json ni package-lock.json) y se ejecutó el mismo entorno desechable y Playwright omitiendo únicamente la reinstalación bloqueada, sin detener procesos del propietario.
- Revisión visual de capturas de Administración, redacción asistida y vista simultánea, en PC y tablet, incluyendo modo oscuro. Las capturas de prueba son artefactos locales ignorados, no documentos institucionales.

Una repetición detectó 503 temporal «Hay otra generación en curso» al pasar de la vista simultánea al PDF formal. El servidor conserva su límite de generación compartido. La prueba reintenta solo esa respuesta identificada y acotada; no se eliminó la protección ni se sustituyó la generación real por un mock. En uso manual el botón permite reintentar cuando aparece ese aviso. El transporte IA sí usa fixtures controlados en las pruebas; no se consumieron proveedores externos.

La prueba general de recarga suponía una justificación inicial vacía. Se actualizó para conservar/restaurar el texto realmente guardado antes de abandonar el editor; vaciar una justificación precargada es correctamente un cambio pendiente y requiere confirmación. No se debilitó esa protección para satisfacer el test.

## Límites y operación

- Vite advierte que el bundle principal supera 500 kB minificado (510,84 kB; 136,88 kB gzip). Es una advertencia de optimización, no un error de compilación; queda como mejora posterior de partición/carga.
- Se conserva el texto de justificación aportado por el propietario como contenido configurable. Esta revisión no certifica las referencias normativas de esa redacción como aprobadas institucionalmente.
- No se han modificado formatos oficiales A4, motores PDF, bytes firmados ni políticas de revisión.
- Pruebas exclusivamente en entornos desechables; no se limpió ni reseteó utaped-sample. Los contenedores/volúmenes temporales se eliminan con sus scripts al terminar.
- No se agregan variables de entorno. Al ejecutar el backend actualizado, Flyway aplica V23; no hace falta SQL manual. La interfaz local se actualiza con Vite; la imagen Docker requiere reconstrucción para reflejar código nuevo, que no se desplegó en esta revisión.
- Solo commits locales, sin push ni actualización de PR.

## Commits y reproducción

- `ce0e2cc` — configuración versionada T1, migración, pruebas de backend y contrato.
- `d28fba9` — interfaz del propietario, correcciones de accesibilidad/integridad y pruebas de frontend.
- Este reporte, su índice y el estado de implementación se registran en un commit documental posterior.

Comprobaciones utilizadas: `scripts/test-backend.ps1`; Maven focalizado `DocumentTemplateIntegrationTests` mediante compose.test.yml; `npm run lint`; `npm run build`; `scripts/test-acceptance.ps1`; y `npm run test:e2e` con el arranque/limpieza del mismo proyecto utaped-e2e del lanzador. En E2E se omitió exclusivamente npm ci por el bloqueo Windows descrito; las dependencias se recuperaron antes de la ejecución. Los logs y capturas permanecen en backend/target y frontend/test-results, ambos ignorados.

Resultado final: árbol de trabajo limpio tras los commits, sin push ni cambios al PR. No se ha detectado una regresión pendiente en los escenarios comprobados; esto no sustituye la revisión funcional del propietario ni acredita dimensionamiento institucional.

## Fuentes

AGENTS.md productivo, requirements.md, implementation-status.md, decisiones vigentes, los 30 archivos staged y sus dependencias, contratos de redacción/circuito documental y pruebas existentes. [Contrato de la nueva configuración](../api/document-templates.md).
