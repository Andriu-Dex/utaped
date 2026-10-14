# Corrección de creación documental y nombres automáticos

Fecha: 2026-10-14. Rama: `fix/document-creation-options`, creada desde `develop` sincronizada con `origin/develop`, después del merge del PR #10. Corrección autorizada por el propietario durante revisión real.

## Resultado

Documentos presenta dos opciones: **Plan de Trabajo** e **Informe**. Seleccionar Plan permite elegir un ámbito disponible y propone `Plan de Trabajo: <grupo/unidad>`; se permite ajustar ese título antes de crear. Seleccionar Informe solicita un Plan aprobado y autorizado, propone su nombre y crea o abre el Informe existente. Se mantiene el acceso desde el propio Plan aprobado.

La corrección está compilada y disponible en el entorno ficticio `http://127.0.0.1:64431`. Recargar con Ctrl+Shift+R. Backend y frontend de `utaped-sample` se reconstruyeron; no se tocaron datos o servicios del proyecto principal.

## Cambios

- Selector accesible mediante controles radio/tarjetas con los códigos de formato T1/T2, estilos PC/tableta y temas existentes. Estados de carga, búsqueda, vacío, error y paginación del selector de Plan fuente.
- Nombre sugerido desde el backend y título automático incluso si el cliente omite el campo al crear un Plan por ámbito. Un reintento idempotente conserva ID/nombre original aunque el grupo se renombre posteriormente.
- Endpoint `GET /api/reports/options`: candidatos paginados (20 por defecto, máximo 50), búsqueda literal, filtros de autorización/estado/ámbito/período. Integrante/revisor/ADMIN sin principal designado no reciben candidatos por su rol.
- Informe nuevo: nombre automático `Informe: <nombre del Plan>`, eliminando el prefijo `Plan de Trabajo: ` cuando corresponde. Un Informe existente muestra y conserva su nombre guardado; se abre su ID sin generar otro.
- Se conserva Informe Final derivado de Plan aprobado, vigente, no archivado y del responsable principal. El endpoint de creación comprueba nuevamente los permisos; no se habilitan Informes independientes ni se alteran firmas/PDFs históricos.
- Selecciones de creación conservadas al filtrar la bandeja. Cambiar de tipo o abrir otro documento confirma descarte cuando corresponde; creación y apertura comparten bloqueo mientras hay operación en curso.
- Ajustes staged del propietario en `App.css` y `CaptchaField.tsx` trasladados íntegros a esta rama y registrados en commit separado: controles iconográficos de CAPTCHA, despliegue/ocultamiento de audio y presentación visual. No se descartaron sus cambios.

No hay migraciones, dependencias, claves ni variables nuevas. Los códigos `UTA-SGC-A-2-1-P7-T1/T2` identifican formatos. El patrón de código documental individual permanece pendiente de confirmación; no se deduce del ejemplo PDF ni se generan códigos DEMO para producción.

## Validación ejecutada

- PostgreSQL aislado: primero suites afectadas (30 ejecuciones), después suite completa: **137 ejecuciones / 20 suites**, sin fallos, errores o casos omitidos.
- Backend nuevo: creación sin título, nombre canónico e idempotencia tras renombrar grupo; candidatos solo para principal/Plan aprobado, filtros y paginación, exclusión de ADMIN sin ámbito, nombre existente conservado, reapertura sin duplicado, fuente/firma intactas y período cerrado excluido.
- Playwright general: **7/7 aprobados**, incluidos audio, temas, acceso, recuperación y flujos.
- Playwright ejemplo: **6/6 aprobados**, nuevo escenario de selector/nombre automático/Plan borrador excluido, cancelación y conservación al filtrar; roles, importación y circuito T1→T2 completo. Se reejecutó el circuito para comprobar específicamente creación y reapertura desde el selector sin duplicar Informe: **1/1 aprobado**.
- `npm run lint`, `npm run build` y TypeScript: correctos; 72 módulos. `git diff --check`: correcto.
- Revisión visual de capturas PC (1440×1000) y tableta (820×1180), modos claro/oscuro; sin desbordamiento horizontal. Capturas en `frontend/test-results/document-creation-*.png`, ignoradas por Git.

Durante las pruebas se ajustó un selector ambiguo de la fixture de recuperación: distingue el aviso de éxito del estado simultáneo de carga del CAPTCHA. También se agotaron límites de acceso al repetir suites; se limpiaron exclusivamente contadores de cuentas ficticias identificadas en `utaped-e2e`/`utaped-sample`. No se deshabilitó throttling ni se modificaron límites de seguridad del producto o la base principal.

## Commits y publicación

- `69eaada` — cambios visuales del propietario y selector de prueba de acceso.
- `c7c40f9` — selector documental, títulos automáticos, candidatos autorizados y pruebas.
- Documentación/reporte en commit posterior.

Commits locales; sin push o nuevo PR de esta rama, conforme a la instrucción vigente. Revisión y eventual fusión a `develop` corresponden al propietario.

## Reproducción local

Desde la raíz `utaped`, para reconstruir el entorno de ejemplo cuando cambien backend/frontend:

```powershell
docker compose --env-file .env.sample -p utaped-sample -f compose.yml -f compose.sample.yml --profile web up -d --build backend frontend
```

No usar `down -v` o reinicialización de semillas. Para solamente cambios visuales, reconstruir `--no-deps frontend` con los mismos archivos/proyecto. Esta entrega incluye API nueva, por lo que requiere ambos servicios. Usar un contexto de navegador dedicado a sample para evitar la colisión de cookies entre puertos identificada en la revisión anterior; la separación de nombres de cookie no forma parte de esta corrección.

## Fuentes y siguiente revisión

Decisión del propietario del 2026-10-14; Plan Maestro §§3.6 y 42; selector del prototipo como referencia funcional; PDF institucional con título por unidad; códigos oficiales de formatos T1/T2. [Contrato](../api/document-circuit.md), [requisitos](../../requirements.md), [estado](../../implementation-status.md).

Siguiente paso: revisión manual del propietario del selector, nombres y recorrido de Informe. Confirmar por separado la convención del código individual (siglas, período, unidad, tipo y versión) antes de implementarla.
